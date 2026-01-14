package de.seuhd.campuscoffee.systest;

import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.ports.PosService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * System tests for OSM POS import functionality.
 * Verifies end-to-end integration of the OSM import endpoint.
 */
public class OsmImportSystemTests extends AbstractSysTest {

    private static final Long OSM_NODE_ID = 5589879349L;

    @Test
    void importPosFromOsm_Success() {
        // Import POS from OSM XML file
        // Note: Using the endpoint path as specified: /api/pos/import/{osmNodeId}
        // If this endpoint doesn't exist, it may need to be created in the controller
        PosDtoResponse response = given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/pos/import/{osmNodeId}", OSM_NODE_ID)
                .then()
                .statusCode(201)
                .extract()
                .as(PosDtoResponse.class);

        // Verify response contains all required fields
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isNotNull().isNotEmpty();
        assertThat(response.latitude()).isNotNull();
        assertThat(response.longitude()).isNotNull();
        assertThat(response.address()).isNotNull().isNotEmpty();

        // Verify specific values from the XML file
        assertThat(response.name()).isEqualTo("Rada Coffee & Rösterei");
        assertThat(response.latitude()).isEqualTo(49.412345);
        assertThat(response.longitude()).isEqualTo(8.705678);
        assertThat(response.address()).contains("Hauptstraße 123");
        assertThat(response.address()).contains("Heidelberg");

        // Verify the POS can be retrieved from the repository
        Pos retrievedPos = posService.getById(response.id());
        assertThat(retrievedPos).isNotNull();
        assertThat(retrievedPos.id()).isEqualTo(response.id());
        assertThat(retrievedPos.name()).isEqualTo(response.name());
        assertThat(retrievedPos.latitude()).isEqualTo(response.latitude());
        assertThat(retrievedPos.longitude()).isEqualTo(response.longitude());
        assertThat(retrievedPos.address()).isEqualTo(response.address());
    }

    @Test
    void importPosFromOsm_NodeNotFound() {
        // Try to import a non-existent OSM node
        Long nonExistentNodeId = 9999999999L;

        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/pos/import/{osmNodeId}", nonExistentNodeId)
                .then()
                .statusCode(404); // OsmNodeNotFoundException should return 404
    }

    /**
     * DTO class for deserializing the POS import response.
     * This matches the expected JSON structure with id, name, latitude, longitude, and address.
     */
    private record PosDtoResponse(
            Long id,
            String name,
            Double latitude,
            Double longitude,
            String address
    ) {}
}

