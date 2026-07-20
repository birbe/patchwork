package party.stoat.patchwork.block;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.kneelawk.graphlib.api.graph.BlockGraph;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import party.stoat.patchwork.block.sf_interface.SFInterface;
import party.stoat.patchwork.graph.ExternalStorageNode;
import party.stoat.patchwork.graph.NodeDescriptor;
import party.stoat.patchwork.graph.PatchGraph;
import party.stoat.patchwork.graphlib.SFInterfaceNode;

import java.util.*;

public class ControllerConfiguration {

    public HashMap<UUID, PatchInstance> instances = new HashMap<>();
    public List<PatchGraph> graphs = new ArrayList<>();
    public boolean initialized = false;

    public void save(ValueOutput output) {
        output.putString("graphs", new Gson().toJson(this.graphs));
    }

    public static List<NodeDescriptor> getNodesFromNetworkResources(BlockGraph graph) {
        List<NodeDescriptor> nodes = new ArrayList<>();

        for(var node : graph.getNodes().toList()) {
            if(node.getNode() instanceof SFInterfaceNode) {
                var facing = node.getBlockState().getValue(SFInterface.FACING);
                var config = new ExternalStorageNode.Configuration(node.getBlockPos(), node.getBlockWorld().dimension(), facing);

                var proxiedName = node.getBlockWorld().getBlockState(node.getBlockPos().relative(facing)).getBlock().getName().getString();

                nodes.add(
                        new NodeDescriptor(
                                "Interface (" + proxiedName + ")",
                                List.of(
                                        new NodeDescriptor.IO("north", "northin", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH)
                                ),
                                List.of(
                                        new NodeDescriptor.IO("north", "northout", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH)
                                ),
                                0xff000000,
                                Identifier.fromNamespaceAndPath("patchwork", "sf_interface"),
                                config
                        )
                );
            }
        }

        return nodes;
    }

    public void initializeIfNeeded(MinecraftServer server) {
        if(this.initialized) return;

        for(var graph : this.graphs) {
            var instance = PatchInstance.build(graph);
            instance.initialize(server);
            this.instances.put(graph.graphId, instance);
        }

        this.initialized = true;
    }

    public static ControllerConfiguration load(ValueInput input) {
        var controllerConfig = new ControllerConfiguration();

        controllerConfig.graphs = new Gson().fromJson(input.getString("graphs").get(), new TypeToken<List<PatchGraph>>() {}.getType());

        return controllerConfig;
    }

}
