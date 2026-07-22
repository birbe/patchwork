package party.stoat.patchwork.patchgraph.nodes;

import net.minecraft.resources.Identifier;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.patchgraph.Node;
import party.stoat.patchwork.patchgraph.NodeDescriptor;

import java.util.HashMap;
import java.util.UUID;

public class Nodes {

    public static HashMap<Identifier, NodeConstructor> nodeConstructors = new HashMap<>();

    static {

        register("interface", InterfaceNode::new);
        register("virtual", VirtualizedBlockNode::new);
        register("patch_nodes/system_power", SFSystemPowerNode::new);
        register("splitter", SplitterNode::new);
    }

    public static void register(String identifier, NodeConstructor constructor) {
        nodeConstructors.put(Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, identifier), constructor);
    }

    @FunctionalInterface
    public interface NodeConstructor {

        Node create(UUID id, NodeDescriptor descriptor);

    }
}
