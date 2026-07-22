package party.stoat.patchwork.patchgraph.nodes;

import com.google.gson.Gson;
import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.util.NodePos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.patchgraph.*;
import party.stoat.patchwork.block.sf_controller.SFControllerBlockEntity;
import party.stoat.patchwork.graphlib.SFInterfaceNode;

import java.util.UUID;

public class InterfaceNode extends VirtualizedBlockNode {

    public record Configuration(BlockPos interfacePos, Direction facing) implements NodeConfiguration {}

    private Configuration config;

    public InterfaceNode(UUID uuid, NodeDescriptor descriptor) {
        super(uuid, descriptor);
    }

    @Override
    public void tick(StorageConfiguration config, PatchInstance patchInstance, ServerLevel level, BlockGraph network, TransactionContext context, SFControllerBlockEntity controller) {
        if(network.getNodeAt(new NodePos(this.config.interfacePos, SFInterfaceNode.INSTANCE)) != null) super.tick(config, patchInstance, level, network, context, controller);
    }

    @Override
    public @Nullable ResourceHandler<ItemResource> getItemHandler(ServerLevel level, NodeDescriptor.IO port, PatchInstance graph) {
        if(Patchwork.UNIVERSE.getGraphWorld(level).getNodeAt(new NodePos(this.config.interfacePos, SFInterfaceNode.INSTANCE)) != null) return super.getItemHandler(level, port, graph);

        return null;
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(ServerLevel level, NodeDescriptor.IO port, PatchInstance graph) {
        if(Patchwork.UNIVERSE.getGraphWorld(level).getNodeAt(new NodePos(this.config.interfacePos, SFInterfaceNode.INSTANCE)) != null) return super.getEnergyHandler(level, port, graph);

        return null;
    }

    @Override
    public void acceptConfiguration(String string) {
        this.config = new Gson().fromJson(string, Configuration.class);
        this.proxyPos = this.config.interfacePos.relative(this.config.facing);
    }
}
