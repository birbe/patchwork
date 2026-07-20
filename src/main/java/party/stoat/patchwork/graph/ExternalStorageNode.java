package party.stoat.patchwork.graph;

import com.google.gson.Gson;
import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.util.NodePos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.ControllerConfiguration;
import party.stoat.patchwork.block.PatchInstance;
import party.stoat.patchwork.graphlib.SFInterfaceNode;

import java.util.UUID;

public class ExternalStorageNode extends VirtualizedBlockNode {

    public record Configuration(BlockPos interfacePos, ResourceKey<Level> level, Direction facing) implements NodeConfiguration {}

    private Configuration config;

    public ExternalStorageNode(UUID uuid, NodeDescriptor descriptor) {
        super(uuid, descriptor);
    }

    @Override
    protected ServerLevel getLevel(MinecraftServer server) {
        for(ServerLevel level : server.getAllLevels()) {
            if(level.dimension().registry().equals(this.config.level.registry()) && level.dimension().identifier().equals(this.config.level.identifier())) return level;
        }

        return null;
    }

    @Override
    public void tick(ControllerConfiguration config, PatchInstance patchInstance, ServerLevel level, BlockGraph network) {
        if(network.getNodeAt(new NodePos(this.config.interfacePos, SFInterfaceNode.INSTANCE)) != null) super.tick(config, patchInstance, level, network);
    }

    @Override
    public boolean receiveItemStack(NodeDescriptor.IO port, ItemStack stack, TransactionContext transaction, MinecraftServer server) {
        if(Patchwork.UNIVERSE.getGraphWorld(this.getLevel(server)).getNodeAt(new NodePos(this.config.interfacePos, SFInterfaceNode.INSTANCE)) != null) return super.receiveItemStack(port, stack, transaction, server);

        return false;
    }

    @Override
    public void acceptConfiguration(String string) {
        this.config = new Gson().fromJson(string, Configuration.class);
        this.proxyPos = this.config.interfacePos.relative(this.config.facing);
    }
}
