package party.stoat.patchwork.graph;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import party.stoat.patchwork.block.ControllerConfiguration;
import party.stoat.patchwork.block.PatchInstance;

import java.util.List;
import java.util.UUID;

public abstract class Node {

    public final UUID uuid;
    private final NodeDescriptor descriptor;

    public Node(UUID uuid, NodeDescriptor descriptor) {
        this.uuid = uuid;
        this.descriptor = descriptor;
    }

    public List<PatchGraph.Connection> getOutputConnections(PatchGraph graph) {
        return graph.connections.stream().filter(connection -> connection.from().equals(this.uuid)).toList();
    }

    public UUID getId() {
        return this.uuid;
    }

    public abstract boolean receiveItemStack(NodeDescriptor.IO port, ItemStack stack, TransactionContext transaction, MinecraftServer server);

    public abstract void tick(ControllerConfiguration config, PatchInstance patch, ServerLevel level, BlockGraph network);

    public NodeDescriptor getDescriptor() {
        return this.descriptor;
    }

    public abstract Identifier getIdentifier();

    public abstract void acceptConfiguration(String string);

    public void init(MinecraftServer level) { }

}
