package de.seuhd.campuscoffee.data.osm;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

/**
 * Adapter for reading and parsing OpenStreetMap XML files.
 * Parses OSM XML files from the classpath and extracts relevant POS information.
 */
@Component
@Slf4j
public class OsmXmlAdapter {

    /**
     * Parses an OSM XML file for the given node ID and extracts POS information.
     *
     * @param nodeId the OpenStreetMap node ID
     * @return an OsmXmlNode object containing the parsed data
     * @throws OsmNodeNotFoundException if the file does not exist or cannot be parsed
     */
    public @NonNull OsmXmlNode parseXml(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.debug("Parsing OSM XML file for node ID: {}", nodeId);

        try {
            Resource resource = new ClassPathResource("osm/" + nodeId + ".xml");
            
            if (!resource.exists()) {
                log.error("OSM XML file not found for node ID: {}", nodeId);
                throw new OsmNodeNotFoundException(nodeId);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            try (InputStream inputStream = resource.getInputStream()) {
                var document = builder.parse(inputStream);
                document.getDocumentElement().normalize();

                var nodeElement = document.getElementsByTagName("node").item(0);
                if (nodeElement == null) {
                    log.error("No node element found in OSM XML for node ID: {}", nodeId);
                    throw new OsmNodeNotFoundException(nodeId);
                }

                var nodeAttributes = nodeElement.getAttributes();
                
                // Extract latitude and longitude from node attributes
                var latAttr = nodeAttributes.getNamedItem("lat");
                var lonAttr = nodeAttributes.getNamedItem("lon");
                
                if (latAttr == null || lonAttr == null) {
                    log.error("Missing lat/lon attributes in OSM XML for node ID: {}", nodeId);
                    throw new OsmNodeNotFoundException(nodeId);
                }
                
                String latStr = latAttr.getNodeValue();
                String lonStr = lonAttr.getNodeValue();
                
                double latitude;
                double longitude;
                try {
                    latitude = Double.parseDouble(latStr);
                    longitude = Double.parseDouble(lonStr);
                } catch (NumberFormatException e) {
                    log.error("Invalid lat/lon values in OSM XML for node ID: {} (lat: {}, lon: {})", 
                            nodeId, latStr, lonStr, e);
                    throw new OsmNodeNotFoundException(nodeId);
                }

                // Extract tags
                String name = extractTagValue(nodeElement, "name");
                String street = extractTagValue(nodeElement, "addr:street");
                String city = extractTagValue(nodeElement, "addr:city");

                // Combine street and city into address
                String address = combineAddress(street, city);

                log.debug("Successfully parsed OSM XML for node ID: {}, name: {}, address: {}", 
                        nodeId, name, address);

                return OsmXmlNode.builder()
                        .nodeId(nodeId)
                        .name(name)
                        .latitude(latitude)
                        .longitude(longitude)
                        .street(street)
                        .city(city)
                        .address(address)
                        .build();

            }
        } catch (OsmNodeNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing OSM XML file for node ID: {}", nodeId, e);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }

    /**
     * Extracts the value of a tag with the given key from the node element.
     *
     * @param nodeElement the node element containing tags
     * @param tagKey      the key of the tag to extract
     * @return the tag value, or null if not found
     */
    private String extractTagValue(org.w3c.dom.Node nodeElement, String tagKey) {
        var tagNodes = ((org.w3c.dom.Element) nodeElement).getElementsByTagName("tag");
        
        for (int i = 0; i < tagNodes.getLength(); i++) {
            var tagNode = tagNodes.item(i);
            var attributes = tagNode.getAttributes();
            var keyAttr = attributes.getNamedItem("k");
            
            if (keyAttr != null && tagKey.equals(keyAttr.getNodeValue())) {
                var valueAttr = attributes.getNamedItem("v");
                if (valueAttr != null) {
                    return valueAttr.getNodeValue();
                }
            }
        }
        
        return null;
    }

    /**
     * Combines street and city into a single address string.
     * Handles null values gracefully.
     *
     * @param street the street address (may be null)
     * @param city   the city name (may be null)
     * @return the combined address string, or empty string if both are null
     */
    private @NonNull String combineAddress(String street, String city) {
        if (street == null && city == null) {
            return "";
        }
        if (street == null) {
            return city;
        }
        if (city == null) {
            return street;
        }
        return street + ", " + city;
    }
}

