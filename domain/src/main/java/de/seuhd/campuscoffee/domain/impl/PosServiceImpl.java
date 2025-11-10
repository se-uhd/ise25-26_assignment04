package de.seuhd.campuscoffee.domain.impl;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of the POS service that handles business logic related to POS entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;

    @Override
    public void clear() {
        log.warn("Clearing all POS data");
        posDataService.clear();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        log.debug("Retrieving all POS");
        return posDataService.getAll();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        log.debug("Retrieving POS with ID: {}", id);
        return posDataService.getById(id);
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException {
        if (pos.id() == null) {
            // Create new POS
            log.info("Creating new POS: {}", pos.name());
            return performUpsert(pos);
        } else {
            // Update existing POS
            log.info("Updating POS with ID: {}", pos.id());
            // POS ID must be set
            Objects.requireNonNull(pos.id());
            // POS must exist in the database before the update
            posDataService.getById(pos.id());
            return performUpsert(pos);
        }
    }

    @Override
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Importing POS from OpenStreetMap node {}...", nodeId);

        // Fetch the OSM node data using the port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);

        // Convert OSM node to POS domain object and upsert it
        // TODO: Implement the actual conversion (the response is currently hard-coded).
        Pos savedPos = upsert(convertOsmNodeToPos(osmNode));
        log.info("Successfully imported POS '{}' from OSM node {}", savedPos.name(), nodeId);

        return savedPos;
    }

    /**
     * Converts an OSM node to a POS domain object.
     * Extracts relevant information from OSM tags and maps them to POS fields.
     * <p>
     * Required fields:
     * <ul>
     *   <li>name: extracted from "name" tag</li>
     *   <li>street: extracted from "addr:street" tag</li>
     *   <li>houseNumber: extracted from "addr:housenumber" tag</li>
     *   <li>postalCode: extracted from "addr:postcode" tag (must be parseable as integer)</li>
     *   <li>city: extracted from "addr:city" tag</li>
     * </ul>
     * <p>
     * Optional fields with defaults:
     * <ul>
     *   <li>description: constructed from "cuisine", "amenity", or "name" tags</li>
     *   <li>type: mapped from "cuisine" or "amenity" tags (defaults to CAFE)</li>
     *   <li>campus: determined from city/address (defaults to ALTSTADT for Heidelberg)</li>
     * </ul>
     *
     * @param osmNode the OSM node to convert; must not be null
     * @return a POS domain object with mapped fields
     * @throws OsmNodeMissingFieldsException if required fields are missing or invalid
     */
    private @NonNull Pos convertOsmNodeToPos(@NonNull OsmNode osmNode) throws OsmNodeMissingFieldsException {
        log.debug("Converting OSM node {} to POS", osmNode.nodeId());
        var tags = osmNode.tags();

        // Extract required fields
        String name = tags.get("name");
        if (name == null || name.isBlank()) {
            log.error("OSM node {} is missing required 'name' tag", osmNode.nodeId());
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        String street = tags.get("addr:street");
        if (street == null || street.isBlank()) {
            log.error("OSM node {} is missing required 'addr:street' tag", osmNode.nodeId());
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        String houseNumber = tags.get("addr:housenumber");
        if (houseNumber == null || houseNumber.isBlank()) {
            log.error("OSM node {} is missing required 'addr:housenumber' tag", osmNode.nodeId());
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        String postalCodeStr = tags.get("addr:postcode");
        Integer postalCode = null;
        if (postalCodeStr != null && !postalCodeStr.isBlank()) {
            try {
                postalCode = Integer.parseInt(postalCodeStr.trim());
            } catch (NumberFormatException e) {
                log.error("OSM node {} has invalid postal code '{}': {}", osmNode.nodeId(), postalCodeStr, e.getMessage());
                throw new OsmNodeMissingFieldsException(osmNode.nodeId());
            }
        } else {
            log.error("OSM node {} is missing required 'addr:postcode' tag", osmNode.nodeId());
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        String city = tags.get("addr:city");
        if (city == null || city.isBlank()) {
            log.error("OSM node {} is missing required 'addr:city' tag", osmNode.nodeId());
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        // Extract optional fields with defaults
        String description = buildDescription(tags);
        PosType type = mapPosType(tags);
        CampusType campus = mapCampusType(city, tags);

        log.debug("Mapped OSM node {} to POS: name='{}', type={}, campus={}", 
                osmNode.nodeId(), name, type, campus);

        return Pos.builder()
                .name(name.trim())
                .description(description)
                .type(type)
                .campus(campus)
                .street(street.trim())
                .houseNumber(houseNumber.trim())
                .postalCode(postalCode)
                .city(city.trim())
                .build();
    }

    /**
     * Builds a description string from available OSM tags.
     * Uses cuisine, amenity, or name tags to construct a meaningful description.
     *
     * @param tags the OSM tags map
     * @return a description string, never null or empty
     */
    private @NonNull String buildDescription(@NonNull java.util.Map<String, String> tags) {
        String cuisine = tags.get("cuisine");
        String amenity = tags.get("amenity");
        String name = tags.get("name");

        if (cuisine != null && !cuisine.isBlank()) {
            // Format cuisine tag (e.g., "coffee_shop" -> "Coffee Shop")
            String formattedCuisine = formatCuisine(cuisine);
            return formattedCuisine;
        } else if (amenity != null && !amenity.isBlank()) {
            return formatAmenity(amenity);
        } else if (name != null && !name.isBlank()) {
            return name;
        } else {
            return "Point of Sale";
        }
    }

    /**
     * Formats a cuisine tag value for display.
     * Converts underscores to spaces and capitalizes words.
     *
     * @param cuisine the cuisine tag value
     * @return formatted cuisine string
     */
    private @NonNull String formatCuisine(@NonNull String cuisine) {
        String normalized = cuisine.replace("_", " ").toLowerCase();
        String[] words = normalized.split(" ");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }
        return result.toString();
    }

    /**
     * Formats an amenity tag value for display.
     *
     * @param amenity the amenity tag value
     * @return formatted amenity string
     */
    private @NonNull String formatAmenity(@NonNull String amenity) {
        String normalized = amenity.replace("_", " ").toLowerCase();
        String[] words = normalized.split(" ");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }
        return result.toString();
    }

    /**
     * Maps OSM tags to a PosType enum value.
     * Uses cuisine and amenity tags to determine the appropriate POS type.
     *
     * @param tags the OSM tags map
     * @return the mapped PosType, defaults to CAFE if no match is found
     */
    private @NonNull PosType mapPosType(@NonNull java.util.Map<String, String> tags) {
        String cuisine = tags.get("cuisine");
        String amenity = tags.get("amenity");

        // Check cuisine tag first
        if (cuisine != null) {
            String cuisineLower = cuisine.toLowerCase();
            if (cuisineLower.contains("coffee") || cuisineLower.equals("coffee_shop")) {
                return PosType.CAFE;
            } else if (cuisineLower.contains("bakery") || cuisineLower.contains("baker")) {
                return PosType.BAKERY;
            }
        }

        // Check amenity tag
        if (amenity != null) {
            String amenityLower = amenity.toLowerCase();
            if (amenityLower.equals("cafe") || amenityLower.equals("coffee_shop")) {
                return PosType.CAFE;
            } else if (amenityLower.equals("vending_machine")) {
                return PosType.VENDING_MACHINE;
            } else if (amenityLower.equals("cafeteria") || amenityLower.equals("restaurant")) {
                return PosType.CAFETERIA;
            } else if (amenityLower.equals("bakery")) {
                return PosType.BAKERY;
            }
        }

        // Default to CAFE if no match found
        log.debug("No matching PosType found for cuisine='{}', amenity='{}', defaulting to CAFE", 
                cuisine, amenity);
        return PosType.CAFE;
    }

    /**
     * Maps city and address information to a CampusType enum value.
     * Uses city name and potentially address information to determine the campus location.
     *
     * @param city the city name from OSM tags
     * @param tags the OSM tags map (for potential future use with address details)
     * @return the mapped CampusType, defaults to ALTSTADT for Heidelberg
     */
    private @NonNull CampusType mapCampusType(@NonNull String city, @NonNull java.util.Map<String, String> tags) {
        // For now, we default to ALTSTADT for Heidelberg
        // In the future, this could be enhanced to use coordinates or specific address information
        if (city != null && city.toLowerCase().contains("heidelberg")) {
            // Could potentially use coordinates or street names to determine specific campus
            // For now, default to ALTSTADT
            return CampusType.ALTSTADT;
        }

        // Default to ALTSTADT if city is not Heidelberg or unknown
        log.debug("Could not determine campus for city '{}', defaulting to ALTSTADT", city);
        return CampusType.ALTSTADT;
    }

    /**
     * Performs the actual upsert operation with consistent error handling and logging.
     * Database constraint enforces name uniqueness - data layer will throw DuplicatePosNameException if violated.
     * JPA lifecycle callbacks (@PrePersist/@PreUpdate) set timestamps automatically.
     *
     * @param pos the POS to upsert
     * @return the persisted POS with updated ID and timestamps
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    private @NonNull Pos performUpsert(@NonNull Pos pos) throws DuplicatePosNameException {
        try {
            Pos upsertedPos = posDataService.upsert(pos);
            log.info("Successfully upserted POS with ID: {}", upsertedPos.id());
            return upsertedPos;
        } catch (DuplicatePosNameException e) {
            log.error("Error upserting POS '{}': {}", pos.name(), e.getMessage());
            throw e;
        }
    }
}
