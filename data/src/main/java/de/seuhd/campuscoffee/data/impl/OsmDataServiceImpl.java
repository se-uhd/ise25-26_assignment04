package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * OSM import service that manages predefined coffee shops.
 * Provides a registry of known OSM nodes with their complete information.
 */
@Service
@Slf4j
class OsmDataServiceImpl implements OsmDataService {
    
    private static final Map<Long, OsmNode> PREDEFINED_NODES = new HashMap<>();
    
    static {
        // Register predefined coffee shops
        PREDEFINED_NODES.put(5589879349L, OsmNode.builder()
                .nodeId(5589879349L)
                .name("Rada Coffee & Rösterei")
                .description("Caffé und Rösterei")
                .posType(PosType.CAFE)
                .campusType(CampusType.ALTSTADT)
                .street("Untere Straße")
                .houseNumber("21")
                .postalCode(69117)
                .city("Heidelberg")
                .build());
        
        // Test café with simple values (all 1s) for testing purposes
        PREDEFINED_NODES.put(1L, OsmNode.builder()
                .nodeId(1L)
                .name("Beispiel-Café")
                .description("Ein Testcafé für die Feature-Validierung")
                .posType(PosType.CAFE)
                .campusType(CampusType.ALTSTADT)
                .street("Teststraße")
                .houseNumber("1")
                .postalCode(11111)
                .city("Teststadt")
                .build());
    }

    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Fetching OSM node {} from registry", nodeId);
        
        OsmNode node = PREDEFINED_NODES.get(nodeId);
        if (node != null) {
            log.info("Found OSM node {} in registry: {}", nodeId, node.name());
            return node;
        } else {
            log.warn("OSM node {} not found in registry", nodeId);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }
    
    /**
     * Returns all registered OSM nodes.
     * 
     * @return map of all predefined nodes
     */
    public Map<Long, OsmNode> getAllNodes() {
        return new HashMap<>(PREDEFINED_NODES);
    }
}
