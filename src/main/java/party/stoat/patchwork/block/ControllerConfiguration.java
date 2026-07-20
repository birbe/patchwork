package party.stoat.patchwork.block;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.kneelawk.graphlib.api.graph.BlockGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import party.stoat.patchwork.MyBlocks;
import party.stoat.patchwork.Patchwork;
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
    public List<BlockPos> virtualized = new ArrayList<>();

    public void save(ValueOutput output) {
        var virts = output.childrenList("virtualized");

        for (var pos : virtualized) {
            virts.addChild().putLong("pos", pos.asLong());
        }

        output.putString("graphs", new Gson().toJson(this.graphs));
    }

    public List<NodeDescriptor> getNodesFromNetworkResources(BlockGraph graph, MinecraftServer server) {
        List<NodeDescriptor> nodes = new ArrayList<>();
        var machineLevel = server.getLevel(MyBlocks.MACHINE_LEVEL);

        for (var virtualizedPos : virtualized) {
            BlockState state = machineLevel.getBlockState(virtualizedPos);

            var posConfiguration = new Gson().toJson(virtualizedPos);

            if (state.getBlock() instanceof FurnaceBlock) {
                nodes.add(
                        new NodeDescriptor("Furnace", List.of(
                                new NodeDescriptor.IO("In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.UP),
                                new NodeDescriptor.IO("Fuel", "fuelin", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH)
                        ), List.of(
                                new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.DOWN)
                        ), ARGB.color(255, 40, 40, 40), Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "virtual"), posConfiguration));

            } else if (state.getBlock() instanceof ChestBlock) {
                nodes.add(
                        new NodeDescriptor(
                                state.getBlock().getName().getString(),
                                List.of(
                                        new NodeDescriptor.IO("In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.UP)
                                ),
                                List.of(
                                        new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.DOWN)
                                ),
                                ARGB.color(255, 110, 100, 105),
                                Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "virtual"),
                                BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(state.getBlock())),
                                posConfiguration
                        )
                );
            } else {
                nodes.add(
                        new NodeDescriptor(
                                state.getBlock().getName().getString(),
                                List.of(
                                        new NodeDescriptor.IO("North", "northin", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH),
                                        new NodeDescriptor.IO("East", "eastin", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.EAST),
                                        new NodeDescriptor.IO("South", "southin", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.SOUTH),
                                        new NodeDescriptor.IO("West", "westin", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.WEST),
                                        new NodeDescriptor.IO("Up", "upin", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.UP),
                                        new NodeDescriptor.IO("Down", "downin", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.DOWN)
                                ),
                                List.of(
                                        new NodeDescriptor.IO("North", "northout", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH),
                                        new NodeDescriptor.IO("East", "eastout", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.EAST),
                                        new NodeDescriptor.IO("South", "southout", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.SOUTH),
                                        new NodeDescriptor.IO("West", "westout", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.WEST),
                                        new NodeDescriptor.IO("Up", "upout", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.UP),
                                        new NodeDescriptor.IO("Down", "downout", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.DOWN)
                                ),
                                ARGB.color(255, 110, 100, 105),
                                Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "virtual"),
                                BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(state.getBlock())),
                                posConfiguration
                        )
                );
            }
        }

        for (var node : graph.getNodes().toList()) {
            if (node.getNode() instanceof SFInterfaceNode) {
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
        if (this.initialized) return;

        for (var graph : this.graphs) {
            var instance = PatchInstance.build(graph);
            instance.initialize(server);
            this.instances.put(graph.graphId, instance);
        }

        this.initialized = true;
    }

    public static ControllerConfiguration load(ValueInput input) {
        var controllerConfig = new ControllerConfiguration();

        var virtualized = input.childrenList("virtualized").get();

        for (var virt : virtualized) {
            var pos = BlockPos.of(virt.getLong("pos").get());
            controllerConfig.virtualized.add(pos);
        }

        controllerConfig.graphs = new Gson().fromJson(input.getString("graphs").get(), new TypeToken<List<PatchGraph>>() {
        }.getType());

        return controllerConfig;
    }

}
