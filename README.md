# IndustrialCraft 2.0 – rekonstruierte Quellen mit Platzierungsfix

Vollständige Dekompilierung der veröffentlichten `ic2.0all.jar`:
IC2 **2.8.222**, Minecraft **26.3**, NeoForge **26.3.0.101**.
Die Fixes für `Registry is already frozen` beim Platzieren von Maschinen
und die vertauschten Netzwerk-Handler sowie der GUI-Zeichenkontext sind im rekonstruierten Quellcode
und im ausführbaren JAR-Patcher enthalten.

**Dies ist ein rekonstruiertes Quellarchiv, kein wiederhergestelltes Original-Entwicklungsprojekt.**
Ein vollständiger Build der Mod aus diesen Java-Dateien ist nicht eingerichtet
oder nachgewiesen. Der geprüfte Build unten patcht die Original-JAR.
Änderungen an anderen dekompilierten Klassen werden dadurch nicht kompiliert
oder in die Ausgabe übernommen.

## Inhalt

- `src/main/java/ic2/`: 1.253 rekonstruierte Java-Dateien für sämtliche
  1.870 direkt in der Mod enthaltenen Klassen, einschließlich innerer Klassen,
  API, Integrationen und eingebetteter EJML-Klassen.
- `src/main/resources/`: alle 3.603 Originalressourcen, einschließlich
  Texturen, Modellen, Rezepten, Konfigurationen, Metadaten und der verschachtelten
  Trove-Bibliothek. Diese Bibliothek bleibt als Original-JAR erhalten.
- `src/main/java/PatchPlacement.java`: eigenständiger JAR-Patcher (JDK 25).
- `patches/placement-fix.diff`: lesbarer Fix.
- `patches/network-routing-fix.diff`: Korrektur der Paketzuordnung.
- `patches/gui-extraction-fix.diff`: Zeichenkontext für den separat aufgerufenen GUI-Hintergrund.
- `scripts/TestGuiExtraction.java`: Ausführung der tatsächlichen GUI-Methoden mit API-Testdoubles.
- `scripts/TestNetworkRouting.java`: Regressionstest mit dem echten Registrierungsbytecode
  und minimalen API-Testdoubles.
- `scripts/reconstruct.ps1`: reproduzierbare Dekompilierung und Anwendung des Fixes.
- `scripts/AuditReconstruction.java`: Abdeckungs- und Ressourcenprüfung.
- `docs/reconstruction-inventory.txt`: Liste der rekonstruierten Quelldateien.

## Gepatchte Mod bauen

JDK 25 installieren. Die Originaldatei von
[CurseForge, Datei 9072436](https://www.curseforge.com/minecraft/mc-mods/industrialcraft-2-0/files/9072436)
herunterladen und unter `input/ic2.0all.jar` ablegen:

```powershell
./build.ps1 -InputJar input/ic2.0all.jar -JavaHome 'C:/Pfad/zum/jdk-25'
```

Mit passendem `JAVA_HOME` kann `-JavaHome` entfallen. Auf anderen Systemen:

```text
java src/main/java/PatchPlacement.java input/ic2.0all.jar dist/ic2.0all-placement-network-gui-fix.jar
```

Die Ausgabe darf noch nicht existieren. Der Patcher akzeptiert ausschließlich
die unveränderte Originaldatei mit SHA-256
`815a1d6e192e6a18eb7ef213baece5e743005957107595983e4e6341ab1add34`.
Er prüft den geänderten Bytecode und vergleicht alle JAR-Einträge:
Nur `ItemBlockTileEntity.class`, `Ic2PayloadRegistration.class` und `GuiIC2.class` dürfen sich ändern.

## Installation und Spieltest

Minecraft beenden und die bisherige IC2-JAR im `mods`-Ordner durch
`dist/ic2.0all-placement-network-gui-fix.jar` ersetzen. Nur eine IC2-JAR gleichzeitig installieren.
In einer Testwelt BatBox und weitere Maschinen platzieren, speichern und neu laden.
Ein Minecraft-Laufzeittest wurde hier noch nicht durchgeführt.

## Ursache und Fix

`ItemBlockTileEntity.placeBlockAt` erzeugt die Maschine ursprünglich über
`TileEntityBlock.instantiate(teClass)` ohne den vorgesehenen Konstruktionskontext.
`constructionType()` versucht dadurch, nach dem Einfrieren der Registry
einen neuen `BlockEntityType` anzulegen.

Der Fix verwendet `NativeBlockEntities.instantiate(teClass, pos, newState)`.
Diese vorhandene Methode setzt den registrierten Typ, Position und Zustand
und stellt den vorherigen ThreadLocal-Kontext im `finally`-Block wieder her.

NeoForge 26.3 erwartet bei `playBidirectional` zuerst den Server-Handler und
danach den Client-Handler. Die Portierung registriert sie umgekehrt. Dadurch
verwirft IC2 unter anderem `LargePacket`-Updates und `KeyUpdate`-Pakete auf
der jeweils falschen Seite; auch GUI-Pakete werden falsch zugestellt.
Der zweite Fix vertauscht die Handler und erhält die spezielle RPC-Behandlung
auf dem Netzwerkthread sowie das Einreihen anderer Pakete auf dem Hauptthread.

```text
java scripts/TestNetworkRouting.java dist/ic2.0all-placement-network-gui-fix.jar
```

Der Test führt den Registrierungsbytecode aus der JAR aus. Er prüft beide Richtungen,
GUI-/Maschinenpakete, RPC und die Hauptthread-Weiterleitung. Die Original-JAR
scheitert an diesem Test, die korrigierte JAR besteht ihn. Dies ersetzt keinen Spieltest.

Minecraft ruft `extractBackground` auch vor `extractRenderState` auf. Der dritte
Fix bindet deshalb den `GuiGraphicsExtractor` während des Hintergrundaufrufs.
Ein `finally`-Block stellt einen eventuell vorhandenen Kontext wieder her und
räumt Texturzustand nur nach einem eigenständigen Aufruf auf. Der Schutz gegen
Zeichnen außerhalb einer Renderphase bleibt erhalten.

```text
java scripts/TestGuiExtraction.java dist/ic2.0all-placement-network-gui-fix.jar
```

Der GUI-Test übernimmt die tatsächlichen Methoden aus der JAR in eine isolierte
Testklasse. Er prüft eigenständige und verschachtelte Hintergrundaufrufe sowie
Aufräumen und Weitergabe von Zeichenfehlern. Andere GUI-Methoden werden dabei
nicht ausgeführt; ein kompletter Minecraft-Spieltest bleibt erforderlich.

## Rekonstruktion reproduzieren und prüfen

```powershell
./scripts/reconstruct.ps1 -InputJar input/ic2.0all.jar -JavaHome 'C:/Pfad/zum/jdk-25'
```

Das Skript lädt bei Bedarf Vineflower **1.11.2** von Maven Central und prüft
dessen SHA-256. Die neue Ausgabe liegt unter `build/reconstructed/src`;
vorhandene Repository-Quellen werden nicht überschrieben.

Die eingecheckten Quellen und Ressourcen mit JDK 25 prüfen:

```text
java scripts/AuditReconstruction.java input/ic2.0all.jar
```

Die Prüfung vergleicht Ressourcen byteweise und kontrolliert für jede Klasse
deren `SourceFile`-Attribut sowie das Vorhandensein des Quellcode-Fixes.
Sie beweist weder die Kompilierbarkeit noch die semantische Gleichheit aller
dekompilierten Methoden. Vineflower meldete bei der Rekonstruktion keine Warnungen.

Für einen echten Quellbuild fehlen insbesondere die ursprüngliche Build-Konfiguration
und die Wiederherstellung des vollständigen Abhängigkeits-/Access-Transformer-Setups.
Decompiler-Ausgaben können zusätzliche Korrekturen an Typen und Kontrollfluss benötigen.

## Herkunft und Lizenzangaben

Die Quellen wurden aus der oben genannten veröffentlichten Mod rekonstruiert.
Originalkommentare, Versionshistorie und ursprüngliche Build-Dateien lassen sich
daraus nicht zurückgewinnen. Dieses Repository ist kein offizielles IC2-Repository.
Die ursprünglichen Autoren- und Lizenzangaben in den Mod-Metadaten wurden unverändert
übernommen; `META-INF/neoforge.mods.toml` nennt `All Rights Reserved`.
Es wird keine neue Lizenz für fremde Quellen oder Ressourcen vergeben.

Original- und Ausgabe-JARs sowie lokale Analyseartefakte werden von Git ausgeschlossen.
Die mitgelieferte Trove-Ressource ist die ausdrücklich ausgenommene verschachtelte JAR.
