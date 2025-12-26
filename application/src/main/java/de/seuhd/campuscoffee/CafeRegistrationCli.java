package de.seuhd.campuscoffee;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.PosService;
import de.seuhd.campuscoffee.data.impl.OsmDataServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Scanner;

/**
 * Command Line Interface for registering new cafes.
 * This tool provides an interactive console interface for adding new Points of Sale
 * using OpenStreetMap data.
 *
 * Usage: Start the application in "dev" or "prod" profile and follow the on-screen instructions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!dev")  // Run in production profile, not in dev (which has LoadInitialData)
public class CafeRegistrationCli implements CommandLineRunner {
    
    private final PosService posService;
    private final OsmDataServiceImpl osmDataService;
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public void run(String... args) throws Exception {
        displayWelcomeMessage();
        
        boolean running = true;
        while (running) {
            displayMainMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1" -> registerNewCafe();
                case "2" -> listAvailableCafes();
                case "3" -> viewAllRegisteredCafes();
                case "4" -> {
                    displayGoodbyeMessage();
                    running = false;
                }
                default -> System.out.println("❌ Ungültige Eingabe. Bitte versuche es erneut.\n");
            }
        }
        
        scanner.close();
    }

    private void displayWelcomeMessage() {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║      ☕ CampusCoffee - Café Registrierungs-System ☕          ║");
        System.out.println("║                                                                ║");
        System.out.println("║  Willkommen! Dieses System ermöglicht es neuen Cafés,         ║");
        System.out.println("║  sich beim Campus-Café-Programm anzuschließen.                ║");
        System.out.println("║                                                                ║");
        System.out.println("║  Um ein Café hinzuzufügen, benötigst du die OpenStreetMap     ║");
        System.out.println("║  Knoten-ID deines Cafés.                                      ║");
        System.out.println("║                                                                ║");
        System.out.println("║  Lerne mehr unter: https://www.openstreetmap.org             ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
    }

    private void displayMainMenu() {
        System.out.println("┌─ Hauptmenü ──────────────────────────────────────────────────┐");
        System.out.println("│                                                              │");
        System.out.println("│  1️⃣  Neues Café registrieren (mit OSM-Knoten-ID)            │");
        System.out.println("│  2️⃣  Verfügbare Cafés ansehen (vordefinierte Liste)         │");
        System.out.println("│  3️⃣  Alle registrierten Cafés anzeigen                     │");
        System.out.println("│  4️⃣  Beenden                                                 │");
        System.out.println("│                                                              │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");
        System.out.print("Bitte wähle eine Option (1-4): ");
    }

    private void registerNewCafe() {
        System.out.println("\n┌─ Neues Café registrieren ────────────────────────────────────┐");
        System.out.println("│                                                              │");
        System.out.println("│  Schritt 1: Gib die OpenStreetMap Knoten-ID deines         │");
        System.out.println("│  Cafés ein. Du findest diese auf:                           │");
        System.out.println("│  https://www.openstreetmap.org/                            │");
        System.out.println("│                                                              │");
        System.out.println("│  Beispiel: 5589879349 (Rada Coffee & Rösterei)             │");
        System.out.println("│                                                              │");
        System.out.println("└──────────────────────────────────────────────────────────────┘\n");
        
        System.out.print("🔍 OpenStreetMap Knoten-ID eingeben: ");
        String nodeIdInput = scanner.nextLine().trim();
        
        Long nodeId;
        try {
            nodeId = Long.parseLong(nodeIdInput);
        } catch (NumberFormatException e) {
            System.out.println("❌ Fehler: Die Knoten-ID muss eine Zahl sein.\n");
            return;
        }
        
        System.out.println("\n⏳ Lädt Informationen vom OpenStreetMap...\n");
        
        try {
            // Import the cafe from OSM
            Pos importedPos = posService.importFromOsmNode(nodeId);
            
            System.out.println("✅ Café erfolgreich registriert!\n");
            displayPosDetails(importedPos);
            
        } catch (OsmNodeNotFoundException e) {
            System.out.println("❌ Fehler: OpenStreetMap Knoten nicht gefunden!");
            System.out.println("   Bitte überprüfe, ob die Knoten-ID korrekt ist.\n");
        } catch (DuplicatePosNameException e) {
            System.out.println("❌ Fehler: Ein Café mit diesem Namen existiert bereits!");
            System.out.println("   Dieses Café ist möglicherweise bereits im System registriert.\n");
        } catch (Exception e) {
            System.out.println("❌ Fehler bei der Registrierung: " + e.getMessage());
            System.out.println("   Bitte überprüfe die OSM-Knoten-Daten.\n");
        }
    }

    private void listAvailableCafes() {
        System.out.println("\n┌─ Verfügbare Cafés ────────────────────────────────────────────┐");
        System.out.println("│                                                              │");
        System.out.println("│  Diese Cafés können direkt registriert werden:              │");
        System.out.println("│                                                              │");
        System.out.println("└──────────────────────────────────────────────────────────────┘\n");
        
        Map<Long, OsmNode> availableNodes = osmDataService.getAllNodes();
        
        if (availableNodes.isEmpty()) {
            System.out.println("⚠️  Keine Cafés verfügbar.\n");
            return;
        }
        
        int index = 1;
        for (Map.Entry<Long, OsmNode> entry : availableNodes.entrySet()) {
            OsmNode node = entry.getValue();
            System.out.println("─────────────────────────────────────────────────────────────");
            System.out.printf("%d️⃣  Knoten-ID: %d%n", index, node.nodeId());
            System.out.printf("   ☕ Name: %s%n", node.name());
            System.out.printf("   📍 Adresse: %s %s, %d %s%n",
                    node.street(),
                    node.houseNumber(),
                    node.postalCode(),
                    node.city());
            System.out.printf("   📝 Beschreibung: %s%n", node.description() != null ? node.description() : "Keine");
            System.out.printf("   🏢 Campus: %s%n", node.campusType());
            System.out.printf("   🏷️  Typ: %s%n", node.posType());
            index++;
        }
        System.out.println("─────────────────────────────────────────────────────────────\n");
    }

    private void viewAllRegisteredCafes() {
        System.out.println("\n┌─ Alle registrierten Cafés ────────────────────────────────────┐");
        
        try {
            var allPos = posService.getAll();
            
            if (allPos.isEmpty()) {
                System.out.println("│ Noch keine Cafés registriert.                                 │");
                System.out.println("└──────────────────────────────────────────────────────────────┘\n");
                return;
            }
            
            System.out.println("└──────────────────────────────────────────────────────────────┘\n");
            
            int index = 1;
            for (Pos pos : allPos) {
                System.out.println("─────────────────────────────────────────────────────────────");
                System.out.printf("%d️⃣  ID: %d%n", index, pos.id());
                System.out.printf("   ☕ Name: %s%n", pos.name());
                System.out.printf("   📍 Adresse: %s %s, %d %s%n",
                        pos.street(),
                        pos.houseNumber(),
                        pos.postalCode(),
                        pos.city());
                System.out.printf("   📝 Beschreibung: %s%n", pos.description());
                System.out.printf("   🏢 Campus: %s%n", pos.campus());
                System.out.printf("   🏷️  Typ: %s%n", pos.type());
                index++;
            }
            System.out.println("─────────────────────────────────────────────────────────────\n");
            
        } catch (Exception e) {
            System.out.println("❌ Fehler beim Abrufen der Cafés: " + e.getMessage() + "\n");
        }
    }

    private void displayPosDetails(Pos pos) {
        System.out.println("┌─ Café Details ────────────────────────────────────────────────┐");
        System.out.println("│                                                              │");
        System.out.printf("│ ☕ Name:         %-50s │%n", truncate(pos.name(), 50));
        System.out.printf("│ 📝 Beschreibung: %-50s │%n", truncate(pos.description(), 50));
        System.out.printf("│ 📍 Adresse:      %-50s │%n", 
                truncate(pos.street() + " " + pos.houseNumber(), 50));
        System.out.printf("│ 📮 PLZ:          %-50s │%n", pos.postalCode());
        System.out.printf("│ 🏙️  Stadt:       %-50s │%n", truncate(pos.city(), 50));
        System.out.printf("│ 🏢 Campus:       %-50s │%n", pos.campus());
        System.out.printf("│ 🏷️  Typ:         %-50s │%n", pos.type());
        System.out.println("│                                                              │");
        System.out.println("└──────────────────────────────────────────────────────────────┘\n");
    }

    private void displayGoodbyeMessage() {
        System.out.println("\n┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                              │");
        System.out.println("│        Auf Wiedersehen! ☕                                   │");
        System.out.println("│        Danke, dass du das CampusCoffee-Programm nutzt!       │");
        System.out.println("│                                                              │");
        System.out.println("└──────────────────────────────────────────────────────────────┘\n");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) {
            return String.format("%-" + maxLength + "s", text);
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
