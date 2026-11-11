import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.ToolProvider;

/** Executes the real registration bytecode against a minimal NeoForge API test double. JDK 25. */
public class TestNetworkRouting {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: java scripts/TestNetworkRouting.java mod.jar");
        Files.createDirectories(Path.of("build"));
        Path root = Files.createTempDirectory(Path.of("build"), "network-test-").toAbsolutePath();
        Path classes = Files.createDirectories(root.resolve("classes"));
        String target = "ic2/core/network/Ic2PayloadRegistration.class";
        try (var jar = new JarFile(args[0])) {
            Path output = classes.resolve(target);
            Files.createDirectories(output.getParent());
            Files.write(output, jar.getInputStream(jar.getJarEntry(target)).readAllBytes());
        }
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("net.minecraft.world.entity.player.Player", "public class Player {}");
        sources.put("net.minecraft.network.protocol.common.custom.CustomPacketPayload", "public interface CustomPacketPayload { public static class Type<T> {} }");
        sources.put("net.minecraft.network.codec.StreamCodec", "public interface StreamCodec<B,T> {}");
        sources.put("net.neoforged.neoforge.network.registration.HandlerThread", "public enum HandlerThread { NETWORK, MAIN }");
        sources.put("net.neoforged.neoforge.network.handling.IPayloadContext", """
            public interface IPayloadContext {
                java.util.concurrent.CompletableFuture<Void> enqueueWork(Runnable work);
                net.minecraft.world.entity.player.Player player();
            }
            """);
        sources.put("net.neoforged.neoforge.network.handling.IPayloadHandler", """
            public interface IPayloadHandler<T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> {
                void handle(T packet, IPayloadContext context);
            }
            """);
        sources.put("net.neoforged.neoforge.network.registration.PayloadRegistrar", """
            import net.neoforged.neoforge.network.handling.*;
            import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
            import net.minecraft.network.codec.StreamCodec;
            public class PayloadRegistrar {
                public static IPayloadHandler serverHandler, clientHandler;
                public PayloadRegistrar executesOn(HandlerThread thread) { return this; }
                public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type,
                    StreamCodec<?, T> codec, IPayloadHandler<T> server, IPayloadHandler<T> client) {
                    serverHandler = server; clientHandler = client; return this;
                }
            }
            """);
        sources.put("net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent", """
            public class RegisterPayloadHandlersEvent {
                public net.neoforged.neoforge.network.registration.PayloadRegistrar registrar(String version) {
                    return new net.neoforged.neoforge.network.registration.PayloadRegistrar();
                }
            }
            """);
        sources.put("ic2.core.network.Ic2Payload", """
            public record Ic2Payload(byte[] bytes) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
                public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Ic2Payload> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>();
                public static final net.minecraft.network.codec.StreamCodec<Object,Ic2Payload> CODEC = new net.minecraft.network.codec.StreamCodec<>() {};
            }
            """);
        sources.put("ic2.core.network.NetworkManager", """
            public class NetworkManager {
                public static String lastSide;
                public final String side;
                public NetworkManager(String side) { this.side = side; }
                public void receive(byte[] bytes, net.minecraft.world.entity.player.Player player) { lastSide = side; }
            }
            """);
        sources.put("ic2.core.util.SideGateway", """
            public class SideGateway<T> {
                public Object get(boolean simulating) { return new ic2.core.network.NetworkManager(simulating ? "server" : "client"); }
            }
            """);
        sources.put("ic2.core.IC2", "public class IC2 { public static final ic2.core.util.SideGateway network = new ic2.core.util.SideGateway(); }");
        sources.put("ic2.core.network.SubPacketType", "public enum SubPacketType { Rpc; public int getId() { return 1; } }");
        sources.put("ic2.core.network.GrowingBuffer", "public class GrowingBuffer { public static GrowingBuffer wrap(byte[] bytes) { return new GrowingBuffer(); } }");
        sources.put("ic2.core.network.RpcHandler", "public class RpcHandler { public static int responses; public static void processRpcResponse(GrowingBuffer buffer) { responses++; } }");
        sources.put("RoutingAssertions", """
            import ic2.core.network.*;
            import net.neoforged.neoforge.network.registration.PayloadRegistrar;
            import net.neoforged.neoforge.network.handling.*;
            import java.util.*;
            import java.util.concurrent.*;
            public class RoutingAssertions {
                static class Context implements IPayloadContext {
                    List<Runnable> queued = new ArrayList<>();
                    public CompletableFuture<Void> enqueueWork(Runnable work) { queued.add(work); return CompletableFuture.completedFuture(null); }
                    public net.minecraft.world.entity.player.Player player() { return new net.minecraft.world.entity.player.Player(); }
                    void drain() { queued.forEach(Runnable::run); queued.clear(); }
                }
                public static void main(String[] args) throws Exception {
                    Class.forName("ic2.core.network.Ic2PayloadRegistration").getMethod("register",
                        net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent.class).invoke(null,
                        new net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent());
                    Context context = new Context();
                    // Real packet IDs: LargePacket and GuiDisplay to client; KeyUpdate and RequestGUI to server.
                    for (int id : new int[] {8, 9, 14, 16}) {
                        boolean serverbound = id >= 14;
                        var handler = serverbound ? PayloadRegistrar.serverHandler : PayloadRegistrar.clientHandler;
                        NetworkManager.lastSide = null;
                        handler.handle(new Ic2Payload(new byte[] {(byte)id}), context);
                        if (NetworkManager.lastSide != null || context.queued.size() != 1) throw new AssertionError("Main thread scheduling lost");
                        context.drain();
                        String expected = serverbound ? "server" : "client";
                        if (!expected.equals(NetworkManager.lastSide)) throw new AssertionError("Packet " + id + " routed to " + NetworkManager.lastSide + ", expected " + expected);
                    }
                    PayloadRegistrar.clientHandler.handle(new Ic2Payload(new byte[] {1}), context);
                    if (RpcHandler.responses != 1 || !context.queued.isEmpty()) throw new AssertionError("Client RPC response handling changed");
                    PayloadRegistrar.serverHandler.handle(new Ic2Payload(new byte[] {1}), context);
                    context.drain();
                    if (!"server".equals(NetworkManager.lastSide) || RpcHandler.responses != 1) throw new AssertionError("Server RPC misrouted");
                    System.out.println("PASS: real JAR registration routes GUI/tile updates to client, key/GUI requests to server, preserves RPC and main-thread scheduling.");
                }
            }
            """);
        List<String> compileArgs = new ArrayList<>(List.of("-classpath", classes.toString(), "-d", classes.toString(), "-Xlint:none"));
        for (var source : sources.entrySet()) {
            int dot = source.getKey().lastIndexOf('.');
            String pkg = dot < 0 ? "" : "package " + source.getKey().substring(0, dot) + ";\n";
            Path file = root.resolve("sources/" + source.getKey().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, pkg + source.getValue());
            compileArgs.add(file.toString());
        }
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null, compileArgs.toArray(String[]::new));
        if (exit != 0) throw new AssertionError("Test fixtures failed to compile");
        try (var loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            try { loader.loadClass("RoutingAssertions").getMethod("main", String[].class).invoke(null, (Object)new String[0]); }
            catch (InvocationTargetException e) { throw new AssertionError("Routing regression", e.getCause()); }
        }
    }
}
