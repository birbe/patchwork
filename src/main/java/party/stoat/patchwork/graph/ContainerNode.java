package party.stoat.patchwork.graph;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.util.NodePos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import party.stoat.patchwork.MyBlocks;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.ControllerConfiguration;
import party.stoat.patchwork.block.PatchInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ContainerNode extends Node {

    private static final Identifier IDENTIFIER = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "patch_nodes/container_node");

    public BlockPos proxyPos;

    public ContainerNode(UUID uuid, NodeDescriptor descriptor) {
        super(uuid, descriptor);
    }

    @Override
    public void tick(ControllerConfiguration config, PatchInstance patchInstance, ServerLevel level, BlockGraph network) {
        var outputs = this.getOutputConnections(patchInstance.graph);

        if(this.proxyPos == null) return;

        var descriptor = this.getDescriptor();

        for(var connection : outputs) {
            var connectedNode = patchInstance.nodes.get(connection.to());
            var port = descriptor.getPort(connection.keyFrom());
            var foreignPort = connectedNode.getDescriptor().getPort(connection.keyTo());

            switch(port.d().d()) {
                case Item -> {
                    var storage = level.getCapability(Capabilities.Item.BLOCK, this.proxyPos, port.direction());

                    if(storage != null) try(Transaction transaction = Transaction.openRoot()) {

                        for(int i=0;i<storage.size();i++) {
                            try(Transaction inner = Transaction.open(transaction)) {
                                var resource = storage.getResource(i);
                                if(resource.isEmpty()) continue;

                                var extracted = storage.extract(resource, 1, inner);
                                var insertedAll = connectedNode.receiveItemStack(foreignPort, resource.toStack(extracted), inner, level.getServer());
                                if(insertedAll) inner.commit();
                            }
                        }

                        transaction.commit();
                    }
                }
                case Inventory -> {
                }
                case Fluid -> {
                }
                case Energy -> {
                }
                case Any -> {
                }
            }
        }
    }

    protected ServerLevel getLevel(MinecraftServer server) {
        return server.getLevel(MyBlocks.MACHINE_LEVEL);
    }

    public boolean receiveItemStack(NodeDescriptor.IO port, ItemStack stack, TransactionContext transaction, MinecraftServer server) {
        if(this.proxyPos == null) return false;
        var level = this.getLevel(server);
        if(level == null) return false;
        var storage = level.getCapability(Capabilities.Item.BLOCK, this.proxyPos, port.direction());

//        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, this.proxyPos, port.direction());
        if(storage != null) {
            try(Transaction nested = Transaction.open(transaction)) {
                var inserted = storage.insert(ItemResource.of(stack), stack.count(), nested);
                if(inserted == stack.count()) {
                    nested.commit();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Identifier getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public void acceptConfiguration(String string) {

    }

}
