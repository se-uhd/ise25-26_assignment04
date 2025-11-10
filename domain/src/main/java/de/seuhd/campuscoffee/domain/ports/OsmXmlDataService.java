package de.seuhd.campuscoffee.domain.ports;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.Pos;
import org.jspecify.annotations.NonNull;

/**
 * Port for importing Point of Sale data from OpenStreetMap XML files.
 * This interface defines the contract for parsing OSM XML files and converting them to POS domain models.
 */
public interface OsmXmlDataService {
    /**
     * Imports a POS from an OpenStreetMap XML file by node ID.
     *
     * @param nodeId the OpenStreetMap node ID
     * @return the POS domain model with data from the OSM XML file
     * @throws OsmNodeNotFoundException if the OSM XML file does not exist or cannot be parsed
     */
    @NonNull Pos importFromOsm(@NonNull Long nodeId) throws OsmNodeNotFoundException;
}

