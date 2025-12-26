# 🎉 CampusCoffee - Café-Registrierungsfeature

## 📋 Übersicht

Das neue Feature ermöglicht es Cafés, sich einfach beim Campus-Café-Programm anzuschließen, indem sie ihre OpenStreetMap-Knoten-ID verwenden. Das System ruft automatisch alle relevanten Informationen vom OSM-Knoten ab und erstellt den POS-Eintrag.

## ✨ Features

### 1. **API-Endpoint zum Importieren**
```bash
POST /api/pos/import/osm/{nodeId}
```
Beispiel:
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349
```

### 2. **Interaktive Konsolen-CLI**
Starte die Anwendung ohne das `dev`-Profil, um das interaktive Menü zu nutzen:
```bash
java -jar application-0.0.1.jar
```

Das CLI bietet:
- ☕ Neues Café registrieren
- 📋 Verfügbare Cafés anzeigen
- 👀 Alle registrierten Cafés anzeigen
- ❌ Beenden

### 3. **Vordefinierte Café-Liste**
Aktuell sind folgende Cafés vordefiniert und können direkt registriert werden:

#### 1. **Rada Coffee & Rösterei**
- **OSM Knoten-ID**: 5589879349
- **Adresse**: Untere Straße 21, 69117 Heidelberg
- **Campus**: ALTSTADT
- **Typ**: CAFE
- **Beschreibung**: Caffé und Rösterei

#### 2. **Beispiel-Café** (Test-Café)
- **OSM Knoten-ID**: 1
- **Adresse**: Teststraße 1, 11111 Teststadt
- **Campus**: ALTSTADT
- **Typ**: CAFE
- **Beschreibung**: Ein Testcafé für die Feature-Validierung

---

## 🏗️ Technische Architektur

### Komponenten

1. **OsmNode** (`domain/model/OsmNode.java`)
   - Erweiterte Datenstruktur für OSM-Informationen
   - Entält: name, street, houseNumber, postalCode, city, posType, campusType, description

2. **OsmDataServiceImpl** (`data/impl/OsmDataServiceImpl.java`)
   - Registry für vordefinierte OSM-Knoten
   - `fetchNode(nodeId)`: Abrufen von Knoteninformationen
   - `getAllNodes()`: Alle verfügbaren Knoten auflisten

3. **PosServiceImpl** (`domain/impl/PosServiceImpl.java`)
   - `importFromOsmNode(nodeId)`: Importiert einen POS aus einem OSM-Knoten
   - `convertOsmNodeToPos()`: Konvertiert OSM-Knoten zu Pos-Objekt (generisch)

4. **CafeRegistrationCli** (`application/CafeRegistrationCli.java`)
   - Benutzerfreundliche Kommandozeilen-Schnittstelle
   - Interaktive Menüs mit klaren Anweisungen
   - Validierung und Fehlerbehandlung

---

## 🧪 Tests

### Unit/Integration Tests

Systemtests für die OSM-Import-Funktionalität:

```bash
# Test: OSM-Import für Rada Coffee & Rösterei
mvn test -Dtest=PosSystemTests#importPosFromOsmNode

# Test: OSM-Import für Beispiel-Café
mvn test -Dtest=PosSystemTests#importTestCafeFromOsmNode

# Alle Tests ausführen
mvn test
```

### Test-Szenarien

#### Test 1: Rada Coffee & Rösterei importieren
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349
```
**Erwartet**: Café wird mit korrekten Daten erstellt

#### Test 2: Beispiel-Café importieren
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/1
```
**Erwartet**: Test-Café mit allen einfachen Werten (1) wird erstellt

#### Test 3: Nicht existierender OSM-Knoten
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/999999999
```
**Erwartet**: HTTP 404 - `OsmNodeNotFoundException`

#### Test 4: Duplikat-Name
```bash
# Ersten Import durchführen
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349

# Zweiten Import durchführen (sollte fehlschlagen)
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349
```
**Erwartet**: HTTP 400 - `DuplicatePosNameException`

---

## 📝 Verwendungsbeispiel - CLI

### Beispiel-Ablauf:

```
╔════════════════════════════════════════════════════════════════╗
║      ☕ CampusCoffee - Café Registrierungs-System ☕          ║
║                                                                ║
║  Willkommen! Dieses System ermöglicht es neuen Cafés,         ║
║  sich beim Campus-Café-Programm anzuschließen.                ║
╚════════════════════════════════════════════════════════════════╝

┌─ Hauptmenü ──────────────────────────────────────────────────┐
│                                                              │
│  1️⃣  Neues Café registrieren (mit OSM-Knoten-ID)            │
│  2️⃣  Verfügbare Cafés ansehen (vordefinierte Liste)         │
│  3️⃣  Alle registrierten Cafés anzeigen                     │
│  4️⃣  Beenden                                                 │
│                                                              │
└──────────────────────────────────────────────────────────────┘
Bitte wähle eine Option (1-4): 1

┌─ Neues Café registrieren ────────────────────────────────────┐
│                                                              │
│  Schritt 1: Gib die OpenStreetMap Knoten-ID deines         │
│  Cafés ein...                                               │
│                                                              │
└──────────────────────────────────────────────────────────────┘

🔍 OpenStreetMap Knoten-ID eingeben: 1

⏳ Lädt Informationen vom OpenStreetMap...

✅ Café erfolgreich registriert!

┌─ Café Details ────────────────────────────────────────────────┐
│ ☕ Name:         Beispiel-Café                               │
│ 📝 Beschreibung: Ein Testcafé für die Feature-Validierung   │
│ 📍 Adresse:      Teststraße 1                                │
│ 📮 PLZ:          11111                                       │
│ 🏙️  Stadt:       Teststadt                                   │
│ 🏢 Campus:       ALTSTADT                                    │
│ 🏷️  Typ:         CAFE                                        │
└──────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Neue Café-Einträge hinzufügen

Um ein neues vordefiniertes Café hinzuzufügen, bearbeite `OsmDataServiceImpl.java`:

```java
PREDEFINED_NODES.put(NEUE_NODE_ID, OsmNode.builder()
        .nodeId(NEUE_NODE_ID)
        .name("Name des Cafés")
        .description("Beschreibung")
        .posType(PosType.CAFE)        // oder BAKERY, VENDING_MACHINE, CAFETERIA
        .campusType(CampusType.ALTSTADT)  // oder INF, BERGHEIM
        .street("Straßenname")
        .houseNumber("Hausnummer")
        .postalCode(12345)
        .city("Stadtname")
        .build());
```

---

## 📊 Datenfluss

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP POST Request                         │
│         /api/pos/import/osm/{nodeId}                        │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│            PosController.create(nodeId)                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│        PosService.importFromOsmNode(nodeId)                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│     OsmDataService.fetchNode(nodeId)                        │
│              ↓                                               │
│     Suche in PREDEFINED_NODES Registry                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│    PosService.convertOsmNodeToPos(osmNode)                  │
│              ↓                                               │
│    Validiere alle erforderlichen Felder                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│       PosService.upsert(pos)                                │
│              ↓                                               │
│     Speichere in der Datenbank                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  HTTP Response 201 Created                                  │
│  + PosDto mit ID und Timestamps                            │
└──────────────────────────────────────────────────────────────┘
```

---

## ⚙️ Fehlerbehandlung

| Szenario | HTTP-Status | Fehler | Grund |
|----------|------------|--------|-------|
| OSM-Knoten nicht registriert | 404 | `OsmNodeNotFoundException` | Die Knoten-ID existiert nicht in der Registry |
| Fehlende erforderliche Felder | 400 | `OsmNodeMissingFieldsException` | OSM-Knoten hat unvollständige Daten |
| Doppelter Name | 400 | `DuplicatePosNameException` | Ein Café mit diesem Namen existiert bereits |
| Ungültige Eingabe | 400 | `NumberFormatException` (CLI) | Knoten-ID ist keine gültige Zahl |

---

## 🔄 Zukünftige Erweiterungen

1. **Echte OSM-API-Integration**
   - HTTP-Requests zur OpenStreetMap API statt lokaler Registry
   - Automatisches Abrufen beliebiger OSM-Knoten

2. **Geografische Filterung**
   - Cafés nach Campus-Nähe filtern
   - Karten-Anzeige mit Café-Positionen

3. **Web-Interface**
   - Benutzerfreundliches Frontend zum Registrieren
   - Formular-Validierung vor Submission

4. **Weitere Café-Typen**
   - Bakeries, Vending Machines, Cafeterias
   - Automatische Typ-Erkennung aus OSM-Daten

---

## 📚 Relevante Dateien

| Datei | Beschreibung |
|-------|-------------|
| `domain/model/OsmNode.java` | Erweiterte OSM-Knotendatenstruktur |
| `domain/model/Pos.java` | Punkt-of-Sale Domänenmodell |
| `domain/ports/OsmDataService.java` | Port-Interface für OSM-Daten |
| `data/impl/OsmDataServiceImpl.java` | OSM-Daten Registry-Implementierung |
| `domain/impl/PosServiceImpl.java` | Service-Logik für Import und Konvertierung |
| `api/controller/PosController.java` | REST-API Endpoint |
| `application/CafeRegistrationCli.java` | Kommandozeilen-Interface |
| `application/src/test/java/de/seuhd/campuscoffee/systest/PosSystemTests.java` | Systemtests |

---

## 🎯 Zusammenfassung

Das Feature bietet eine **vollständige Lösung** zum Registrieren neuer Cafés:

✅ **Vordefinierte Cafés** für schnelle Integration  
✅ **API-Endpoint** für programmgesteuerten Zugriff  
✅ **Benutzerfreundliches CLI** für Administratoren  
✅ **Validierung** aller erforderlichen Daten  
✅ **Fehlerbehandlung** mit aussagekräftigen Meldungen  
✅ **Tests** zur Überprüfung der Funktionalität  

**Bereit zum Einsatz!** 🚀
