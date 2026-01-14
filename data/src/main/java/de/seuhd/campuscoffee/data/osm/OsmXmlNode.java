package de.seuhd.campuscoffee.data.osm;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * DTO representing parsed data from an OpenStreetMap XML node.
 *
 * @param nodeId    the OpenStreetMap node ID
 * @param name      the name of the location (from tag k="name")
 * @param latitude  the latitude coordinate (from node lat attribute)
 * @param longitude the longitude coordinate (from node lon attribute)
 * @param street    the street address (from tag k="addr:street")
 * @param city      the city name (from tag k="addr:city")
 * @param address   the combined address string (street + city)
 */
@Builder
public record OsmXmlNode(
        @NonNull Long nodeId,
        @Nullable String name,
        double latitude,
        double longitude,
        @Nullable String street,
        @Nullable String city,
        @NonNull String address
) {
}

