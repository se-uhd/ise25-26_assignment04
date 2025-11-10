package de.seuhd.campuscoffee.systest;

import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.tests.TestFixtures;
import org.junit.jupiter.api.Test;
import java.util.List;

import de.seuhd.campuscoffee.TestUtils;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * System tests for the operations related to POS (Point of Sale).
 */
public class PosSystemTests extends AbstractSysTest {

    @Test
    void createPos() {
        Pos posToCreate = TestFixtures.getPosFixturesForInsertion().getFirst();
        Pos createdPos = posDtoMapper.toDomain(TestUtils.createPos(List.of(posDtoMapper.fromDomain(posToCreate))).getFirst());

        assertThat(createdPos)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt") // prevent issues due to differing timestamps after conversions
                .isEqualTo(posToCreate);
    }

    @Test
    void getAllCreatedPos() {
        List<Pos> createdPosList = TestFixtures.createPosFixtures(posService);

        List<Pos> retrievedPos = TestUtils.retrievePos()
                .stream()
                .map(posDtoMapper::toDomain)
                .toList();

        assertThat(retrievedPos)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "updatedAt") // prevent issues due to differing timestamps after conversions
                .containsExactlyInAnyOrderElementsOf(createdPosList);
    }

    @Test
    void getPosById() {
        List<Pos> createdPosList = TestFixtures.createPosFixtures(posService);
        Pos createdPos = createdPosList.getFirst();

        Pos retrievedPos = posDtoMapper.toDomain(
                TestUtils.retrievePosById(createdPos.id())
        );

        assertThat(retrievedPos)
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt") // prevent issues due to differing timestamps after conversions
                .isEqualTo(createdPos);
    }

    @Test
    void updatePos() {
        List<Pos> createdPosList = TestFixtures.createPosFixtures(posService);
        Pos posToUpdate = createdPosList.getFirst();

        // Update fields using toBuilder() pattern (records are immutable)
        posToUpdate = posToUpdate.toBuilder()
                .name(posToUpdate.name() + " (Updated)")
                .description("Updated description")
                .build();

        Pos updatedPos = posDtoMapper.toDomain(TestUtils.updatePos(List.of(posDtoMapper.fromDomain(posToUpdate))).getFirst());

        assertThat(updatedPos)
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(posToUpdate);

        // Verify changes persist
        Pos retrievedPos = posDtoMapper.toDomain(TestUtils.retrievePosById(posToUpdate.id()));

        assertThat(retrievedPos)
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(posToUpdate);
    }

    @Test
    void importPosFromOsmNode() {
        // Import Rada Coffee & Rösterei from OSM node 5589879349
        Pos importedPos = posDtoMapper.toDomain(TestUtils.importPosFromOsm(5589879349L));

        assertThat(importedPos)
                .isNotNull()
                .satisfies(pos -> {
                    assertThat(pos.name()).isEqualTo("Rada Coffee & Rösterei");
                    assertThat(pos.description()).isEqualTo("Caffé und Rösterei");
                    assertThat(pos.street()).isEqualTo("Untere Straße");
                    assertThat(pos.houseNumber()).isEqualTo("21");
                    assertThat(pos.postalCode()).isEqualTo(69117);
                    assertThat(pos.city()).isEqualTo("Heidelberg");
                });
    }

    @Test
    void importTestCafeFromOsmNode() {
        // Import test cafe from OSM node 1 (Beispiel-Café with simple test values)
        Pos importedPos = posDtoMapper.toDomain(TestUtils.importPosFromOsm(1L));

        assertThat(importedPos)
                .isNotNull()
                .satisfies(pos -> {
                    assertThat(pos.name()).isEqualTo("Beispiel-Café");
                    assertThat(pos.description()).isEqualTo("Ein Testcafé für die Feature-Validierung");
                    assertThat(pos.street()).isEqualTo("Teststraße");
                    assertThat(pos.houseNumber()).isEqualTo("1");
                    assertThat(pos.postalCode()).isEqualTo(11111);
                    assertThat(pos.city()).isEqualTo("Teststadt");
                    assertThat(pos.id()).isNotNull();
                    assertThat(pos.createdAt()).isNotNull();
                    assertThat(pos.updatedAt()).isNotNull();
                });
    }
}