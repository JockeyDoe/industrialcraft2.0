# IC2 26.3: Fix für den Absturz beim Platzieren

Gezielter Binärpatch für `ic2.0all.jar` (CurseForge-Datei 9072436,
IC2 2.8.222, Minecraft 26.3 / NeoForge).

Dieses Repository enthält den ausführbaren Patch-Quellcode, kein vollständiges
IC2-Quellprojekt. Es benötigt weder Gradle noch externe Java-Bibliotheken.

## Projektstruktur

- `src/main/java/PatchPlacement.java`: Patcher und Bytecode-/Integritätsprüfung.
- `build.ps1`: Build-Aufruf für PowerShell mit frei wählbarem JDK und JAR-Pfaden.
- `patches/placement-fix.diff`: lesbare Darstellung der Mod-Änderung.
- `dist/`: lokal erzeugte gepatchte Mod (von Git ausgeschlossen).

## Nach dem Klonen

JDK 25 installieren. Die Originaldatei `ic2.0all.jar` selbst von
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/industrialcraft-2-0/files/9072436)
herunterladen und unter `input/ic2.0all.jar` ablegen. Anschließend:

```powershell
./build.ps1 -InputJar input/ic2.0all.jar -JavaHome 'C:/Pfad/zum/jdk-25'
```

Mit passendem `JAVA_HOME` kann `-JavaHome` entfallen. Alternativ lässt sich
der Quellcode auf jedem Betriebssystem direkt mit JDK 25 ausführen:

```text
java src/main/java/PatchPlacement.java input/ic2.0all.jar dist/ic2.0all-placement-fix.jar
```

Originaldatei, Decompiler, dekompilierte Mod-Dateien und erzeugte JARs werden
über `.gitignore` ausgeschlossen. Für einen Upload werden nur der eigene
Patcher, das Build-Skript, der kleine Änderungsdiff und die Dokumentation benötigt.

## Installation

Minecraft beenden. Die bisherige `ic2.0all.jar` aus dem `mods`-Ordner
verschieben und durch `dist/ic2.0all-placement-fix.jar` ersetzen.
Nur eine IC2-JAR gleichzeitig installieren. Vor dem Test die Welt sichern.

## Ursache und Änderung

`ItemBlockTileEntity.placeBlockAt` ruft den alten reflektierenden Konstruktor
ohne den von der Portierung vorgesehenen Konstruktionskontext auf.
Dadurch versucht `constructionType()` beim ersten Platzieren, einen neuen
`BlockEntityType` in der bereits eingefrorenen Registry anzulegen.

Der Patch nutzt stattdessen die bereits vorhandene Methode
`NativeBlockEntities.instantiate(teClass, pos, newState)`. Diese verwendet
den registrierten Typ, setzt Position und Zustand und stellt den vorherigen
ThreadLocal-Kontext in einem `finally`-Block wieder her.
Es werden keine Registrierungen hinzugefügt oder Registry-Sperren umgangen.

Die lesbare Änderung steht in `patches/placement-fix.diff`.
Die mit Vineflower rekonstruierten Dateien in `investigation/decompiled`
sind Analyseartefakte, kein vollständiges, kompilierbares Mod-Projekt.

## Reproduzieren

JDK 25 benötigt; keine Minecraft- oder NeoForge-Build-Abhängigkeiten:

```powershell
./build.ps1 -JavaHome 'C:/Pfad/zum/jdk-25'
```

Der Ausgabepfad muss noch frei sein. Der Patcher akzeptiert ausschließlich
die unveränderte Originaldatei mit SHA-256
`815a1d6e192e6a18eb7ef213baece5e743005957107595983e4e6341ab1add34`.
Er ersetzt genau einen Methodenaufruf in genau einer Klasse.

## Prüfung und Grenzen

- JDK-Classfile-Verifikation des gepatchten Bytecodes erfolgreich.
  Die benötigten externen Vererbungsbeziehungen sind im Patcher explizit
  angegeben; Minecraft selbst ist dabei nicht geladen.
- Alle anderen entpackten JAR-Einträge sind bytegleich zur Originaldatei.
- Kein Minecraft-Laufzeittest durchgeführt.

Im Spiel zuerst BatBox und weitere Maschinen platzieren, anschließend
Welt speichern und neu laden. Auch auf einem dedizierten Server testen,
falls die Mod dort eingesetzt werden soll. Der Patch behebt den konkreten
Platzierungspfad; weitere Fehler dieser Portierung wurden nicht geprüft.
