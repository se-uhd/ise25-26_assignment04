package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.data.osm.OsmXmlAdapter;
import de.seuhd.campuscoffee.data.osm.OsmXmlNode;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.ports.OsmXmlDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/**
 * Implementation of OsmXmlDataService that parses OSM XML files and converts them to POS domain models.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OsmXmlDataServiceImpl implements OsmXmlDataService {
    private final OsmXmlAdapter osmXmlAdapter;

    @Override
    public @NonNull Pos importFromOsm(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Importing POS from OSM XML file for node ID: {}", nodeId);

        try {
            // Parse the OSM XML file using the adapter
            OsmXmlNode osmXmlNode = osmXmlAdapter.parseXml(nodeId);
            log.debug("Successfully parsed OSM XML for node ID: {}", nodeId);

            // Convert OsmXmlNode to Pos domain model
            Pos pos = convertToPos(osmXmlNode);
            log.info("Successfully converted OSM XML data to POS for node ID: {}, name: {}", 
                    nodeId, pos.name());

            return pos;
        } catch (OsmNodeNotFoundException e) {
            log.error("Failed to import POS from OSM XML for node ID: {}", nodeId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error importing POS from OSM XML for node ID: {}", nodeId, e);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }

    /**
     * Converts an OsmXmlNode DTO to a Pos domain model.
     * Handles null values by providing defaults for required fields.
     *
     * @param osmXmlNode the OSM XML node DTO
     * @return the POS domain model
     */
    private @NonNull Pos convertToPos(@NonNull OsmXmlNode osmXmlNode) {
        // Handle null name - use empty string or a default value
        String name = osmXmlNode.name() != null && !osmXmlNode.name().isEmpty() 
                ? osmXmlNode.name() 
                : "Unnamed Location";

        // Address is already combined in OsmXmlNode, use it directly
        // If address is empty, provide a default
        String address = osmXmlNode.address() != null && !osmXmlNode.address().isEmpty()
                ? osmXmlNode.address()
                : "Address not available";

        return new Pos(
                null, // id is null for new POS
                name,
                osmXmlNode.latitude(),
                osmXmlNode.longitude(),
                address
        );
    }
}

