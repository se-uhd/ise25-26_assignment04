package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the OSM data service that fetches node data from the OpenStreetMap API.
 * This service handles HTTP communication with the OSM REST API and parses the XML response.
 */
@Service
@Slf4j
class OsmDataServiceImpl implements OsmDataService {
    private static final String OSM_API_BASE_URL = "https://www.openstreetmap.org/api/0.6/node/";
    private final RestTemplate restTemplate;
    private final DocumentBuilderFactory documentBuilderFactory;

    /**
     * Constructs a new OsmDataServiceImpl with a RestTemplate instance.
     * The RestTemplate is used to make HTTP requests to the OpenStreetMap API.
     */
    public OsmDataServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        // Disable external entity resolution for security
        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception e) {
            log.warn("Failed to configure XML parser security features: {}", e.getMessage());
        }
    }

    /**
     * Fetches an OpenStreetMap node by its ID from the OSM REST API.
     * Makes an HTTP GET request to the OSM API, parses the XML response, and extracts
     * node attributes (id, lat, lon) and tags.
     *
     * @param nodeId the OpenStreetMap node ID to fetch; must not be null
     * @return the OSM node with parsed attributes and tags; never null
     * @throws OsmNodeNotFoundException if the node doesn't exist (404) or cannot be fetched
     */
    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Fetching OSM node {} from API...", nodeId);
        String url = OSM_API_BASE_URL + nodeId;

        try {
            // Fetch XML response from OSM API
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("Failed to fetch OSM node {}: HTTP status {}", nodeId, response.getStatusCode());
                throw new OsmNodeNotFoundException(nodeId);
            }

            log.debug("Successfully fetched XML response for OSM node {}", nodeId);

            // Parse XML and extract node data
            return parseOsmXml(nodeId, response.getBody());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("OSM node {} not found (404)", nodeId);
                throw new OsmNodeNotFoundException(nodeId);
            } else {
                log.error("HTTP error fetching OSM node {}: {} {}", nodeId, e.getStatusCode(), e.getMessage());
                throw new OsmNodeNotFoundException(nodeId);
            }
        } catch (RestClientException e) {
            log.error("Error fetching OSM node {}: {}", nodeId, e.getMessage(), e);
            throw new OsmNodeNotFoundException(nodeId);
        } catch (Exception e) {
            log.error("Unexpected error while fetching OSM node {}: {}", nodeId, e.getMessage(), e);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }

    /**
     * Parses the XML response from the OSM API and extracts node attributes and tags.
     * Uses DocumentBuilder to parse the XML and extract the node element with its attributes
     * (id, lat, lon) and child tag elements.
     *
     * @param nodeId the expected node ID (used for validation)
     * @param xmlContent the XML content to parse
     * @return the parsed OsmNode with attributes and tags
     * @throws OsmNodeNotFoundException if the XML cannot be parsed or the node is missing
     */
    private @NonNull OsmNode parseOsmXml(@NonNull Long nodeId, @NonNull String xmlContent) throws OsmNodeNotFoundException {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes("UTF-8"));
            var document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            // Find the node element
            var nodeElements = document.getElementsByTagName("node");
            if (nodeElements.getLength() == 0) {
                log.error("No node element found in XML response for node {}", nodeId);
                throw new OsmNodeNotFoundException(nodeId);
            }

            var nodeElement = nodeElements.item(0);
            var attributes = nodeElement.getAttributes();

            // Extract node ID and validate
            String idAttr = attributes.getNamedItem("id") != null
                    ? attributes.getNamedItem("id").getNodeValue()
                    : null;
            if (idAttr == null || !idAttr.equals(String.valueOf(nodeId))) {
                log.warn("Node ID mismatch: expected {}, found {}", nodeId, idAttr);
            }

            // Extract latitude and longitude
            Double latitude = null;
            Double longitude = null;
            if (attributes.getNamedItem("lat") != null) {
                try {
                    latitude = Double.parseDouble(attributes.getNamedItem("lat").getNodeValue());
                } catch (NumberFormatException e) {
                    log.warn("Invalid latitude value for node {}: {}", nodeId, e.getMessage());
                }
            }
            if (attributes.getNamedItem("lon") != null) {
                try {
                    longitude = Double.parseDouble(attributes.getNamedItem("lon").getNodeValue());
                } catch (NumberFormatException e) {
                    log.warn("Invalid longitude value for node {}: {}", nodeId, e.getMessage());
                }
            }

            // Extract tags
            Map<String, String> tags = new HashMap<>();
            var tagElements = document.getElementsByTagName("tag");
            for (int i = 0; i < tagElements.getLength(); i++) {
                var tagElement = tagElements.item(i);
                var tagAttributes = tagElement.getAttributes();
                String key = tagAttributes.getNamedItem("k") != null
                        ? tagAttributes.getNamedItem("k").getNodeValue()
                        : null;
                String value = tagAttributes.getNamedItem("v") != null
                        ? tagAttributes.getNamedItem("v").getNodeValue()
                        : null;
                if (key != null && value != null) {
                    tags.put(key, value);
                }
            }

            log.debug("Parsed OSM node {} with {} tags", nodeId, tags.size());

            return OsmNode.builder()
                    .nodeId(nodeId)
                    .latitude(latitude)
                    .longitude(longitude)
                    .tags(tags)
                    .build();

        } catch (OsmNodeNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing XML for OSM node {}: {}", nodeId, e.getMessage(), e);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }
}
