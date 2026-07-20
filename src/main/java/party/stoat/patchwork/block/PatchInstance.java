package party.stoat.patchwork.block;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import party.stoat.patchwork.graph.Node;
import party.stoat.patchwork.graph.NodeDescriptor;
import party.stoat.patchwork.graph.Nodes;
import party.stoat.patchwork.graph.PatchGraph;

import java.util.*;
import java.util.stream.Collectors;

public class PatchInstance {

    public Map<UUID, Node> nodes = new HashMap<>();
    public List<PatchGraph.Connection> connections;
    public final PatchGraph graph;

    public PatchInstance(PatchGraph graph) {
        this.graph = graph;
    }

    public boolean initialized = false;

    public static PatchInstance build(PatchGraph graph) {
        var instance = new PatchInstance(graph);

        for(var nodeId : graph.nodeDescriptors.keySet()) {
            var nodeDescriptor = graph.nodeDescriptors.get(nodeId);
            var node = Nodes.nodeConstructors.get(nodeDescriptor.identifier()).create(nodeId, nodeDescriptor);

            if(nodeDescriptor.configuration() != null) node.acceptConfiguration(nodeDescriptor.configuration());

            instance.nodes.put(nodeId, node);
        }

        instance.connections = new ArrayList<>(graph.connections.stream().filter(connection -> instance.nodes.containsKey(connection.from()) && instance.nodes.containsKey(connection.to())).toList());

        return instance;
    }

    public void initialize(MinecraftServer server) {
        if(this.initialized) return;

        for(var node : nodes.values()) {
            node.init(server);
        }

        this.initialized = true;
    }

}
