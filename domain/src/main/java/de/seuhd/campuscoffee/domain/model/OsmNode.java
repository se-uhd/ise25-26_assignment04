package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an OpenStreetMap node with relevant Point of Sale information.
 * This is the domain model for OSM data before it is converted to a POS object.
 *
 * @param nodeId The OpenStreetMap node ID.
 * @param name The name of the establishment.
 * @param description A description of the establishment.
 * @param street Street name where the establishment is located.
 * @param houseNumber House number (may include suffix).
 * @param postalCode Postal code.
 * @param city City name.
 * @param posType The type of point of sale (cafe, bakery, etc.).
 * @param campusType The campus location.
 */
@Builder
public record OsmNode(
        @NonNull Long nodeId,
        @Nullable String name,
        @Nullable String description,
        @Nullable String street,
        @Nullable String houseNumber,
        @Nullable Integer postalCode,
        @Nullable String city,
        @Nullable PosType posType,
        @Nullable CampusType campusType
) {
}
