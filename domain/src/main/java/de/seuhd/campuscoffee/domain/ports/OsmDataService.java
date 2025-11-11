package de.seuhd.campuscoffee.domain.ports;

import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * Port for importing Point of Sale data from OpenStreetMap.
 * This interface defines the contract for fetching OSM node data.
 * Implementations should handle the external API communication.
 */
public interface OsmDataService {
    /**
     * Fetches an OpenStreetMap node by its ID.
     *
     * @param nodeId the OpenStreetMap node ID to fetch
     * @return the OSM node data with tags
     * @throws OsmNodeNotFoundException if the node doesn't exist or can't be fetched
     */
    @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException;
    
    /**
     * Returns all available OSM nodes in the system.
     * Useful for displaying a list of available cafes to register.
     *
     * @return a map of node IDs to OsmNode objects
     */
    Map<Long, OsmNode> getAllNodes();
}

