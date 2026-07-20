package party.stoat.patchwork.graph;

import com.google.gson.Gson;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.Patchwork;

import java.util.HashMap;
import java.util.UUID;

public class Nodes {

    public static HashMap<Identifier, NodeConstructor> nodeConstructors = new HashMap<>();

    static {

        register("sf_interface", ExternalStorageNode::new);
        register("virtual", VirtualizedBlockNode::new);

    }

    public static void register(String identifier, NodeConstructor constructor) {
        nodeConstructors.put(Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, identifier), constructor);
    }

    @FunctionalInterface
    public interface NodeConstructor {

        Node create(UUID id, NodeDescriptor descriptor);

    }
}
