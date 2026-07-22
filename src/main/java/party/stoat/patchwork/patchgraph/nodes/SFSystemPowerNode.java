package party.stoat.patchwork.patchgraph.nodes;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import mekanism.api.chemical.ChemicalResource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.patchgraph.*;
import party.stoat.patchwork.block.sf_controller.SFControllerBlockEntity;

import java.util.UUID;

public class SFSystemPowerNode extends Node {

    public static final Identifier IDENTIFIER = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "patch_nodes/system_power");

    public SFSystemPowerNode(UUID uuid, NodeDescriptor descriptor) {
        super(uuid, descriptor);
    }

    @Override
    public void tick(StorageConfiguration config, PatchInstance patch, ServerLevel level, BlockGraph network, TransactionContext context, SFControllerBlockEntity controller) {
        var outputs = this.getOutputConnections(patch.graph);

        for(var connection : outputs) {
            var connectedNode = patch.nodes.get(connection.to());
            var foreignPort = connectedNode.getDescriptor().getPort(connection.keyTo());

            if(foreignPort.d().d() != NodeDescriptor.DataType.Energy) continue;

            var storage = controller.storage;

            var foreignStorage = connectedNode.getEnergyHandler(level, foreignPort, patch);

            if(foreignStorage == null) continue;

            succeedAll: try(Transaction inner = Transaction.open(context)) {

                int toInsert = 0;

                try(Transaction initial = Transaction.open(inner)) {
                    var extracted = storage.extract(foreignStorage.getCapacityAsInt(), initial);
                    var inserted = foreignStorage.insert(extracted, initial);

                    if(inserted < extracted) {
                        toInsert = inserted;
                    } else if(inserted == extracted) {
                        initial.commit();
                        inner.commit();
                        break succeedAll;
                    }
                }

                var extracted = storage.extract(toInsert, inner);
                var inserted = foreignStorage.insert(toInsert, inner);

                if(inserted == extracted) inner.commit();
            }
        }
    }

    @Override
    public Identifier getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public void acceptConfiguration(String string) {

    }

}
