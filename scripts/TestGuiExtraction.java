import java.lang.classfile.*;
import java.lang.constant.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.ToolProvider;

/** Executes the actual GUI background/getGraphics methods with unrelated GUI methods omitted. */
public class TestGuiExtraction {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: java scripts/TestGuiExtraction.java mod.jar");
        Files.createDirectories(Path.of("build"));
        Path root = Files.createTempDirectory(Path.of("build"), "gui-test-").toAbsolutePath();
        Path classes = Files.createDirectories(root.resolve("classes"));
        ClassFile cf = ClassFile.of();
        ClassDesc owner = ClassDesc.of("ic2.core.GuiIC2");
        ClassDesc graphics = ClassDesc.of("net.minecraft.client.gui.GuiGraphicsExtractor");
        ClassDesc parent = ClassDesc.of("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen");
        ClassModel original;
        try (var jar = new JarFile(args[0])) {
            original = cf.parse(jar.getInputStream(jar.getJarEntry("ic2/core/GuiIC2.class")).readAllBytes());
        }
        byte[] probe = cf.build(owner, builder -> {
            builder.withFlags(ClassFile.ACC_PUBLIC).withSuperclass(parent);
            for (var field : original.fields()) if (field.fieldName().equalsString("graphics")) builder.with(field);
            for (var method : original.methods()) {
                if (Set.of("getGraphics", "extractBackground", "ic2$extractBackgroundBody").contains(method.methodName().stringValue())) builder.with(method);
            }
            builder.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), ClassFile.ACC_PUBLIC,
                code -> code.aload(0).invokespecial(parent, "<init>", MethodTypeDesc.ofDescriptor("()V")).return_());
            // Replace legacy background drawing with a probe that calls the real getGraphics().
            builder.withMethodBody("func_146976_a", MethodTypeDesc.ofDescriptor("(FII)V"), ClassFile.ACC_PROTECTED,
                code -> code.aload(0).invokevirtual(owner, "getGraphics", MethodTypeDesc.of(graphics))
                    .invokestatic(ClassDesc.of("probe.Drawing"), "draw", MethodTypeDesc.of(ConstantDescs.CD_void, graphics)).return_());
        });
        Path probePath = classes.resolve("ic2/core/GuiIC2.class");
        Files.createDirectories(probePath.getParent());
        Files.write(probePath, probe);
        Map<String, String> sources = Map.of(
            "net.minecraft.client.gui.GuiGraphicsExtractor", "public class GuiGraphicsExtractor {}",
            "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen", """
                public class AbstractContainerScreen {
                    public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, float partial) {}
                }
                """,
            "ic2.core.gui.GuiTextures", "public class GuiTextures { public static int clears; public static void clear() { clears++; } }",
            "probe.Drawing", """
                public class Drawing {
                    public static Object last;
                    public static boolean fail;
                    public static final RuntimeException FAILURE = new RuntimeException("drawing failure");
                    public static void draw(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
                        last = graphics;
                        if (fail) throw FAILURE;
                    }
                }
                """);
        List<String> compile = new ArrayList<>(List.of("-d", classes.toString()));
        for (var source : sources.entrySet()) {
            int dot = source.getKey().lastIndexOf('.');
            Path path = root.resolve("sources/" + source.getKey().replace('.', '/') + ".java");
            Files.createDirectories(path.getParent());
            Files.writeString(path, "package " + source.getKey().substring(0, dot) + ";\n" + source.getValue());
            compile.add(path.toString());
        }
        if (ToolProvider.getSystemJavaCompiler().run(null, null, null, compile.toArray(String[]::new)) != 0)
            throw new AssertionError("Fixture compilation failed");
        try (var loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> guiType = loader.loadClass("ic2.core.GuiIC2");
            Class<?> graphicsType = loader.loadClass("net.minecraft.client.gui.GuiGraphicsExtractor");
            Class<?> drawing = loader.loadClass("probe.Drawing");
            Class<?> textures = loader.loadClass("ic2.core.gui.GuiTextures");
            Object gui = guiType.getConstructor().newInstance();
            Object current = graphicsType.getConstructor().newInstance();
            Object previous = graphicsType.getConstructor().newInstance();
            Field context = guiType.getDeclaredField("graphics");
            context.setAccessible(true);
            Method background = guiType.getMethod("extractBackground", graphicsType, int.class, int.class, float.class);
            for (boolean nested : new boolean[] {false, true}) {
                for (boolean failure : new boolean[] {false, true}) {
                    Object expected = nested ? previous : null;
                    context.set(gui, expected);
                    drawing.getField("fail").setBoolean(null, failure);
                    int clears = textures.getField("clears").getInt(null);
                    try {
                        background.invoke(gui, current, 10, 20, 0.5f);
                        if (failure) throw new AssertionError("Drawing exception swallowed");
                    } catch (InvocationTargetException e) {
                        if (!failure || e.getCause() != drawing.getField("FAILURE").get(null))
                            throw new AssertionError("Background extraction failed", e.getCause());
                    }
                    if (drawing.getField("last").get(null) != current) throw new AssertionError("Wrong drawing context");
                    if (context.get(gui) != expected) throw new AssertionError("Previous graphics not restored");
                    if (textures.getField("clears").getInt(null) != clears + (nested ? 0 : 1))
                        throw new AssertionError("Texture cleanup has wrong scope");
                }
            }
            context.set(gui, null);
            try { guiType.getMethod("getGraphics").invoke(gui); throw new AssertionError("Out-of-phase guard removed"); }
            catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof IllegalStateException)) throw e;
            }
            System.out.println("PASS: actual GUI bytecode supports standalone/nested background extraction; restores context and textures on success/failure; out-of-phase drawing still rejected.");
        }
    }
}
