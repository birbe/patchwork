package party.stoat.patchwork.patchgraph.nodes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kneelawk.graphlib.api.graph.BlockGraph;
import mekanism.api.chemical.ChemicalResource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
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

public class VirtualizedBlockNode extends Node {

    private static final Identifier IDENTIFIER = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "patch_nodes/container_node");

    public BlockPos proxyPos;

    public VirtualizedBlockNode(UUID uuid, NodeDescriptor descriptor) {
        super(uuid, descriptor);
    }

    @Override
    public @Nullable ResourceHandler<ChemicalResource> getChemicalHandler(ServerLevel level, NodeDescriptor.IO port, PatchInstance graph) {
        if(this.proxyPos == null) return null;
        if(level == null) return null;
        return level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), this.proxyPos, port.direction().orElse(null));
    }

    @Override
    public @Nullable ResourceHandler<ItemResource> getItemHandler(ServerLevel level, NodeDescriptor.IO port, PatchInstance graph) {
        if(this.proxyPos == null) return null;
        if(level == null) return null;
        return level.getCapability(Capabilities.Item.BLOCK, this.proxyPos, port.direction().orElse(null));
    }

    @Override
    public @Nullable ResourceHandler<FluidResource> getFluidHandler(ServerLevel level, NodeDescriptor.IO port, PatchInstance graph) {
        if(this.proxyPos == null) return null;
        if(level == null) return null;
        return level.getCapability(Capabilities.Fluid.BLOCK, this.proxyPos, port.direction().orElse(null));
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(ServerLevel level, NodeDescriptor.IO port, PatchInstance graph) {
        if(this.proxyPos == null) return null;
        if(level == null) return null;
        return level.getCapability(Capabilities.Energy.BLOCK, this.proxyPos, port.direction().orElse(null));
    }

    @Override
    public void tick(StorageConfiguration config, PatchInstance patchInstance, ServerLevel level, BlockGraph network, TransactionContext context, SFControllerBlockEntity entity) {
        var outputs = this.getOutputConnections(patchInstance.graph);

        if(this.proxyPos == null) return;

        var descriptor = this.getDescriptor();

        for(var connection : outputs) {
            var connectedNode = patchInstance.nodes.get(connection.to());
            var port = descriptor.getPort(connection.keyFrom());
            var foreignPort = connectedNode.getDescriptor().getPort(connection.keyTo());

            switch(port.d().d()) {
                case Chemical -> {
                    var storage = level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), this.proxyPos, port.direction().orElse(null));
                    var foreignStorage = connectedNode.getChemicalHandler(level, foreignPort, patchInstance);

                    if(foreignStorage == null) continue;

                    if(storage != null) try(Transaction transaction = Transaction.open(context)) {

                        for(int localIndex=0;localIndex<storage.size();localIndex++) {
                            var resource = storage.getResource(localIndex);
                            if(resource.isEmpty()) continue;

                            for(int foreignIndex=0;foreignIndex<foreignStorage.size();foreignIndex++) {
                                succeedAll: try(Transaction inner = Transaction.open(transaction)) {

                                    int toInsert = 0;

                                    try(Transaction initial = Transaction.open(inner)) {
                                        var extracted = storage.extract(resource, storage.getCapacityAsInt(foreignIndex, resource), initial);
                                        var inserted = foreignStorage.insert(resource, extracted, initial);

                                        if(inserted < extracted) {
                                            toInsert = inserted;
                                        } else if(inserted == extracted) {
                                            initial.commit();
                                            inner.commit();
                                            break succeedAll;
                                        }
                                    }

                                    var extracted = storage.extract(foreignIndex, resource, toInsert, inner);
                                    var inserted = foreignStorage.insert(resource, toInsert, inner);

                                    if(inserted == extracted) inner.commit();
                                }
                            }

                        }

                        transaction.commit();
                    }
                }
                case Item -> {
                    var storage = level.getCapability(Capabilities.Item.BLOCK, this.proxyPos, port.direction().orElse(null));

                    if(storage != null) try(Transaction transaction = Transaction.open(context)) {

                        for(int i=0;i<storage.size();i++) {
                            try(Transaction actualAttempt = Transaction.open(transaction)) {
                                var toExtract = 0;

                                var resource = storage.getResource(i);
                                if(resource.isEmpty()) continue;

                                var foreignStorage = connectedNode.getItemHandler(level, foreignPort, patchInstance);
                                if(foreignStorage == null) continue;

                                try(Transaction inner = Transaction.open(actualAttempt)) {
                                    toExtract = storage.extract(resource, 9999, inner);
                                }

                                var inserted = foreignStorage.insert(resource, toExtract, actualAttempt);
                                var extracted = storage.extract(resource, inserted, actualAttempt);

                                if(inserted == extracted) actualAttempt.commit();
                            }
                        }

                        transaction.commit();
                    }
                }
                case Fluid -> {
                    var storage = level.getCapability(Capabilities.Fluid.BLOCK, this.proxyPos, port.direction().orElse(null));

                    if(storage != null) try(Transaction transaction = Transaction.open(context)) {

                        for(int i=0;i<storage.size();i++) {
                            try(Transaction actualAttempt = Transaction.open(transaction)) {
                                var toExtract = 0;

                                var resource = storage.getResource(i);
                                if(resource.isEmpty()) continue;

                                var foreignStorage = connectedNode.getFluidHandler(level, foreignPort, patchInstance);
                                if(foreignStorage == null) continue;

                                try(Transaction inner = Transaction.open(actualAttempt)) {
                                    toExtract = storage.extract(resource, 9999, inner);
                                }

                                var inserted = foreignStorage.insert(resource, toExtract, actualAttempt);
                                var extracted = storage.extract(resource, inserted, actualAttempt);

                                if(inserted == extracted) actualAttempt.commit();
                            }
                        }

                        transaction.commit();
                    }
                }
                case Energy -> {
//                    var storage = level.getCapability(Capabilities.Energy.BLOCK, this.proxyPos, port.direction());
//
//                    if(storage != null) try(Transaction transaction = Transaction.open(context)) {
//
//                        try(Transaction inner = Transaction.open(transaction)) {
//                            var extracted = storage.extract( 1, inner);
//                            var foreignStorage = connectedNode.getItemHandler(level, foreignPort);
//                            var inserted = foreignStorage.insert(resource, extracted, inner);
//                            if(inserted == extracted) inner.commit();
//                        }
//
//                        transaction.commit();
//                    }
                }
            }
        }
    }

    @Override
    public Identifier getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public void acceptConfiguration(String string) {
        this.proxyPos = new Gson().fromJson(string, new TypeToken<BlockPos>() {}.getType());
    }

}
