import java.io.*;
import java.lang.classfile.*;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;

/** Checks every class has reconstructed source and every resource is byte-identical. */
public class AuditReconstruction {
    public static void main(String[] args) throws Exception {
        Path jarPath = Path.of(args.length == 0 ? "investigation/ic2.0all.jar" : args[0]);
        int classes = 0, resources = 0;
        Set<String> sources = new TreeSet<>();
        List<String> missing = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            for (var entries = jar.entries(); entries.hasMoreElements();) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                byte[] data = jar.getInputStream(entry).readAllBytes();
                if (entry.getName().endsWith(".class")) {
                    classes++;
                    var model = ClassFile.of().parse(data);
                    String name = model.findAttribute(java.lang.classfile.Attributes.sourceFile())
                        .map(SourceFileAttribute::sourceFile).map(Object::toString)
                        .orElseThrow(() -> new IllegalStateException("No source filename: " + entry.getName()));
                    String pkg = entry.getName().substring(0, entry.getName().lastIndexOf('/') + 1);
                    String source = pkg + name;
                    sources.add(source);
                    if (!Files.isRegularFile(Path.of("src/main/java", source))) missing.add(source);
                } else {
                    resources++;
                    Path path = Path.of("src/main/resources", entry.getName());
                    if (!Files.isRegularFile(path) || !Arrays.equals(data, Files.readAllBytes(path)))
                        missing.add("Missing/changed resource: " + entry.getName());
                }
            }
        }
        if (!missing.isEmpty()) throw new IllegalStateException(String.join("\n", missing));
        String source = Files.readString(Path.of("src/main/java/ic2/core/item/block/ItemBlockTileEntity.java"));
        if (!source.contains("NativeBlockEntities.instantiate(teClass, pos, newState)")
            || source.contains("TileEntityBlock.instantiate(teClass)")) throw new AssertionError("Source fix missing");
        String network = Files.readString(Path.of("src/main/java/ic2/core/network/Ic2PayloadRegistration.java"));
        if (network.indexOf("IC2.network.get(true)") > network.indexOf("SubPacketType.Rpc")
            || !network.contains("IC2.network.get(false)")) throw new AssertionError("Network source fix missing");
        String gui = Files.readString(Path.of("src/main/java/ic2/core/GuiIC2.java"));
        if (!gui.contains("GuiGraphicsExtractor previous = this.graphics;")
            || !gui.contains("this.graphics = previous;")) throw new AssertionError("GUI source fix missing");
        long actualSources;
        try (var files = Files.walk(Path.of("src/main/java/ic2"))) {
            actualSources = files.filter(p -> p.toString().endsWith(".java")).count();
        }
        if (actualSources != sources.size()) throw new AssertionError("Unexpected source count: " + actualSources);
        System.out.printf("PASS: %d class files covered by %d Java files; %d resources byte-identical; source fix present.%n",
            classes, sources.size(), resources);
        Files.createDirectories(Path.of("docs"));
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jarPath)));
        Files.writeString(Path.of("docs/reconstruction-inventory.txt"),
            "Original SHA-256: " + hash + "\nClass files: " + classes + "\nJava source files: " + sources.size()
            + "\nResources: " + resources + "\n\n" + String.join("\n", sources) + "\n");
    }
}
