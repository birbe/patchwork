package party.stoat.patchwork.graph;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kneelawk.graphlib.api.graph.BlockGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import party.stoat.patchwork.MyBlocks;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.ControllerConfiguration;
import party.stoat.patchwork.block.PatchInstance;

import java.util.UUID;

public class VirtualizedBlockNode extends Node {

    private static final Identifier IDENTIFIER = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "patch_nodes/container_node");

    public BlockPos proxyPos;

    public VirtualizedBlockNode(UUID uuid, NodeDescriptor descriptor) {
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
        if(stack.getItem() == Items.AIR) return false;
        if(storage != null) {
            try(Transaction nested = Transaction.open(transaction)) {
                var resource = ItemResource.of(stack);
                if(resource.isEmpty()) return false;
                var inserted = storage.insert(resource, stack.count(), nested);
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
        this.proxyPos = new Gson().fromJson(string, new TypeToken<BlockPos>() {}.getType());
    }

}
