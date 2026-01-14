package de.seuhd.campuscoffee.domain.model;

import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Domain class for POS (Point of Sale) with location information.
 *
 * @param id        the unique identifier; null when the POS has not been created yet
 * @param name      the name of the POS
 * @param latitude  the latitude coordinate
 * @param longitude the longitude coordinate
 * @param address   the combined address string
 */
@Value
public class Pos {
    @Nullable Long id;
    @NonNull String name;
    double latitude;
    double longitude;
    @NonNull String address;
}
