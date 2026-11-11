import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.ToolProvider;

/** Tests the real syncBlockState/rerender bytecode with a minimal world fixture. */
public class TestBlockStateSync {
    public static void main(String[] args) throws Exception {
        ClassModel original;
        try (var jar = new JarFile(args[0])) {
            original = ClassFile.of().parse(jar.getInputStream(jar.getJarEntry("ic2/core/block/TileEntityBlock.class")).readAllBytes());
        }
        for (String name : List.of("setFacing", "setActive", "onLoaded", "rerender")) {
            var method = original.methods().stream().filter(m -> m.methodName().equalsString(name)).findFirst().orElseThrow();
            long calls = method.code().orElseThrow().elementStream().filter(e -> e instanceof InvokeInstruction call
                && call.name().equalsString("syncBlockState")).count();
            if (calls != 1) throw new AssertionError("Missing synchronization hook in " + name);
        }
        Files.createDirectories(Path.of("build"));
        Path root = Files.createTempDirectory(Path.of("build"), "state-test-").toAbsolutePath();
        Path classes = Files.createDirectories(root.resolve("classes"));
        ClassDesc owner = ClassDesc.of("ic2.core.block.TileEntityBlock");
        Map<String, ClassDesc> fields = Map.of("world", ClassDesc.of("net.minecraft.world.level.Level"),
            "pos", ClassDesc.of("net.minecraft.core.BlockPos"), "desired", ClassDesc.of("net.minecraft.world.level.block.state.BlockState"),
            "block", ClassDesc.of("ic2.core.block.BlockTileEntity"));
        byte[] probe = ClassFile.of().build(owner, b -> {
            b.withFlags(ClassFile.ACC_PUBLIC);
            fields.forEach((name, type) -> b.withField(name, type, ClassFile.ACC_PUBLIC));
            for (var method : original.methods()) if (Set.of("syncBlockState", "rerender").contains(method.methodName().stringValue())) b.with(method);
            b.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), ClassFile.ACC_PUBLIC,
                c -> c.aload(0).invokespecial(ConstantDescs.CD_Object, "<init>", MethodTypeDesc.ofDescriptor("()V")).return_());
            Map.of("getLevel", "world", "getBlockPos", "pos", "getBlockState", "desired", "getBlockType", "block").forEach((getter, field) ->
                b.withMethodBody(getter, MethodTypeDesc.of(fields.get(field)), ClassFile.ACC_PUBLIC,
                    c -> c.aload(0).getfield(owner, field, fields.get(field)).areturn()));
        });
        Path target = classes.resolve("ic2/core/block/TileEntityBlock.class");
        Files.createDirectories(target.getParent()); Files.write(target, probe);
        Map<String,String> sources = Map.of(
            "net.minecraft.core.BlockPos", "public class BlockPos {}",
            "net.minecraft.world.level.block.Block", "public class Block {}",
            "ic2.core.block.BlockTileEntity", "public class BlockTileEntity extends net.minecraft.world.level.block.Block {}",
            "net.minecraft.world.level.block.state.BlockState", """
                public class BlockState {
                    public final net.minecraft.world.level.block.Block block;
                    public BlockState(net.minecraft.world.level.block.Block block) { this.block = block; }
                    public net.minecraft.world.level.block.Block getBlock() { return block; }
                }
                """,
            "net.minecraft.world.level.Level", """
                import net.minecraft.core.BlockPos;
                import net.minecraft.world.level.block.state.BlockState;
                public class Level {
                    public BlockState current;
                    public int writes, updates, flags;
                    public BlockState getBlockState(BlockPos pos) { return current; }
                    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
                        current = state; this.flags = flags; writes++; return true;
                    }
                    public void sendBlockUpdated(BlockPos pos, BlockState old, BlockState state, int flags) { updates++; }
                }
                """);
        List<String> compile = new ArrayList<>(List.of("-d", classes.toString()));
        for (var entry : sources.entrySet()) {
            int dot = entry.getKey().lastIndexOf('.');
            Path path = root.resolve("sources/" + entry.getKey().replace('.', '/') + ".java");
            Files.createDirectories(path.getParent());
            Files.writeString(path, "package " + entry.getKey().substring(0,dot) + ";\n" + entry.getValue());
            compile.add(path.toString());
        }
        if (ToolProvider.getSystemJavaCompiler().run(null,null,null,compile.toArray(String[]::new)) != 0) throw new AssertionError("Fixtures failed");
        try (var loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> machineType = loader.loadClass("ic2.core.block.TileEntityBlock");
            Class<?> blockType = loader.loadClass("net.minecraft.world.level.block.Block");
            Class<?> stateType = loader.loadClass("net.minecraft.world.level.block.state.BlockState");
            Class<?> worldType = loader.loadClass("net.minecraft.world.level.Level");
            Object machine = machineType.getConstructor().newInstance();
            Object block = loader.loadClass("ic2.core.block.BlockTileEntity").getConstructor().newInstance();
            Object old = stateType.getConstructor(blockType).newInstance(block);
            Object desired = stateType.getConstructor(blockType).newInstance(block);
            Object world = worldType.getConstructor().newInstance();
            machineType.getField("block").set(machine, block);
            machineType.getField("desired").set(machine, desired);
            machineType.getField("pos").set(machine, loader.loadClass("net.minecraft.core.BlockPos").getConstructor().newInstance());
            Method sync = machineType.getDeclaredMethod("syncBlockState"); sync.setAccessible(true);
            sync.invoke(machine); // Detached instances must not crash or place blocks.
            machineType.getField("world").set(machine, world);
            worldType.getField("current").set(world, old);
            sync.invoke(machine);
            if (worldType.getField("current").get(world) != desired || worldType.getField("flags").getInt(world) != 2) throw new AssertionError("State not synchronized");
            int writes = worldType.getField("writes").getInt(world);
            Object foreign = stateType.getConstructor(blockType).newInstance(blockType.getConstructor().newInstance());
            worldType.getField("current").set(world, foreign);
            sync.invoke(machine);
            if (worldType.getField("current").get(world) != foreign || worldType.getField("writes").getInt(world) != writes) throw new AssertionError("Foreign block overwritten");
            worldType.getField("current").set(world, old);
            Method rerender = machineType.getDeclaredMethod("rerender"); rerender.setAccessible(true); rerender.invoke(machine);
            if (worldType.getField("current").get(world) != desired || worldType.getField("updates").getInt(world) != 1) throw new AssertionError("Rerender failed to update actual world state");
            System.out.println("PASS: actual synchronization bytecode updates world state, guards detached/foreign blocks, preserves render notification; all four hooks present.");
        }
    }
}
