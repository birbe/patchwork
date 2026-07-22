package party.stoat.patchwork.patchgraph;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import mekanism.api.chemical.ChemicalResource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.block.sf_controller.SFControllerBlockEntity;

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

    public @Nullable ResourceHandler<ChemicalResource> getChemicalHandler(ServerLevel level, NodeDescriptor.IO port, PatchInstance graph) {
        return null;
    }

    public @Nullable ResourceHandler<ItemResource> getItemHandler(ServerLevel level, NodeDescriptor.IO port, PatchInstance graph) {
        return null;
    }

    public @Nullable ResourceHandler<FluidResource> getFluidHandler(ServerLevel level, NodeDescriptor.IO port,PatchInstance graph) {
        return null;
    }

    public @Nullable EnergyHandler getEnergyHandler(ServerLevel level, NodeDescriptor.IO port, PatchInstance graph) {
        return null;
    }

    public void tick(StorageConfiguration config, PatchInstance patch, ServerLevel level, BlockGraph network, TransactionContext context, SFControllerBlockEntity controller) {

    }

    public NodeDescriptor getDescriptor() {
        return this.descriptor;
    }

    public abstract Identifier getIdentifier();

    public abstract void acceptConfiguration(String string);

}
