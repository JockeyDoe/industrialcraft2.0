import java.io.*;
import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;

/** Reproducible, dependency-free patch for the exact CurseForge release. Requires JDK 25. */
public class PatchPlacement {
    static final String TARGET = "ic2/core/item/block/ItemBlockTileEntity.class";
    static final String NETWORK_TARGET = "ic2/core/network/Ic2PayloadRegistration.class";
    static final String ORIGINAL_SHA256 = "815a1d6e192e6a18eb7ef213baece5e743005957107595983e4e6341ab1add34";
    static final String OLD = "(Ljava/lang/Class;)Lic2/core/block/TileEntityBlock;";
    static final String NEW = "(Ljava/lang/Class;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lic2/core/block/TileEntityBlock;";
    static final String PLACE = "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;FFFLnet/minecraft/world/level/block/state/BlockState;)Z";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: java src/main/java/PatchPlacement.java original.jar patched.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(input)));
        if (!hash.equals(ORIGINAL_SHA256)) throw new IllegalArgumentException("Unsupported input SHA-256: " + hash);
        if (Files.exists(output)) throw new IOException("Output already exists: " + output);
        try (JarFile jar = new JarFile(input.toFile())) {
            byte[] original = jar.getInputStream(jar.getJarEntry(TARGET)).readAllBytes();
            // Minecraft is not distributed with this patch. Supply only the external
            // superclass relations needed by the verifier; parse IC2 classes from the JAR.
            var external = new HashMap<ClassDesc, ClassDesc>();
            for (String[] pair : new String[][] {
                {"net.minecraft.world.item.BlockItem", "net.minecraft.world.item.Item"},
                {"net.minecraft.world.item.Item", "java.lang.Object"},
                {"net.minecraft.world.entity.player.Player", "net.minecraft.world.entity.LivingEntity"},
                {"net.minecraft.world.entity.LivingEntity", "net.minecraft.world.entity.Entity"},
                {"net.minecraft.world.entity.Entity", "java.lang.Object"},
                {"net.minecraft.world.level.block.entity.BlockEntity", "java.lang.Object"}
            }) external.put(ClassDesc.of(pair[0]), ClassDesc.of(pair[1]));
            var resolver = ClassHierarchyResolver.ofResourceParsing(desc -> {
                var entry = jar.getJarEntry(desc.descriptorString().substring(1, desc.descriptorString().length() - 1) + ".class");
                try { return entry == null ? null : jar.getInputStream(entry); }
                catch (IOException e) { throw new UncheckedIOException(e); }
            }).orElse(ClassHierarchyResolver.of(List.of(), external)).orElse(ClassHierarchyResolver.defaultResolver());
            ClassFile cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver));
            int[] changes = {0};
            byte[] patched = cf.transformClass(cf.parse(original), ClassTransform.transformingMethodBodies(
                m -> m.methodName().equalsString("placeBlockAt") && m.methodType().equalsString(PLACE),
                (code, element) -> {
                    if (element instanceof InvokeInstruction call
                        && call.opcode() == Opcode.INVOKESTATIC
                        && call.owner().asInternalName().equals("ic2/core/block/TileEntityBlock")
                        && call.name().equalsString("instantiate") && call.type().equalsString(OLD)) {
                        // Instance method: pos is local 4, newState is local 9 (floats each occupy one slot).
                        code.aload(4).aload(9).invokestatic(ClassDesc.of("ic2.core.block.NativeBlockEntities"),
                            "instantiate", MethodTypeDesc.ofDescriptor(NEW));
                        changes[0]++;
                    } else code.with(element);
                }));
            if (changes[0] != 1) throw new IllegalStateException("Expected one placement call, found " + changes[0]);
            var errors = cf.verify(patched);
            if (!errors.isEmpty()) throw new IllegalStateException("Class verification failed: " + errors);
            int[] networkChanges = {0};
            byte[] networkOriginal = jar.getInputStream(jar.getJarEntry(NETWORK_TARGET)).readAllBytes();
            byte[] networkPatched = cf.transformClass(cf.parse(networkOriginal), ClassTransform.transformingMethodBodies(
                m -> m.methodName().equalsString("register"), (code, element) -> {
                    if (element instanceof InvokeInstruction call
                        && call.opcode() == Opcode.INVOKEVIRTUAL
                        && call.owner().asInternalName().equals("net/neoforged/neoforge/network/registration/PayloadRegistrar")
                        && call.name().equalsString("playBidirectional")) {
                        // NeoForge 26.3 takes serverHandler first, clientHandler second.
                        // Both references are category-one values; swap preserves lambda/RPC behavior.
                        code.swap().with(call);
                        networkChanges[0]++;
                    } else code.with(element);
                }));
            if (networkChanges[0] != 1) throw new IllegalStateException("Expected one network registration");
            var networkErrors = cf.verify(networkPatched);
            if (!networkErrors.isEmpty()) throw new IllegalStateException("Network verification failed: " + networkErrors);
            Map<String, byte[]> replacements = Map.of(TARGET, patched, NETWORK_TARGET, networkPatched);
            Files.createDirectories(output.toAbsolutePath().getParent());
            try (var out = new JarOutputStream(Files.newOutputStream(output, StandardOpenOption.CREATE_NEW))) {
                for (var entries = jar.entries(); entries.hasMoreElements();) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().matches("(?i)META-INF/[^/]+\\.(SF|RSA|DSA|EC)"))
                        throw new IllegalStateException("Signed JAR is unsupported");
                    JarEntry copy = new JarEntry(entry.getName());
                    copy.setTime(entry.getTime());
                    out.putNextEntry(copy);
                    if (replacements.containsKey(entry.getName())) out.write(replacements.get(entry.getName()));
                    else try (var stream = jar.getInputStream(entry)) { stream.transferTo(out); }
                    out.closeEntry();
                }
            }
            // Compare every uncompressed entry; only the two explicitly patched classes may differ.
            int changed = 0;
            try (JarFile result = new JarFile(output.toFile())) {
                if (jar.size() != result.size()) throw new AssertionError("Entry count changed");
                for (var entries = jar.entries(); entries.hasMoreElements();) {
                    var entry = entries.nextElement();
                    byte[] before = jar.getInputStream(entry).readAllBytes();
                    byte[] after = result.getInputStream(result.getJarEntry(entry.getName())).readAllBytes();
                    if (!Arrays.equals(before, after)) {
                        if (!replacements.containsKey(entry.getName())) throw new AssertionError("Unexpected change: " + entry.getName());
                        changed++;
                    }
                }
            }
            if (changed != replacements.size()) throw new AssertionError("Expected exactly two changed entries");
            System.out.println("PASS: bytecode verification; placement and network routing patched; all other JAR entries unchanged.");
            System.out.println("Created " + output);
        }
    }
}
