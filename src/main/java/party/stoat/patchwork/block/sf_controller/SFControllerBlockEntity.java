package party.stoat.patchwork.block.sf_controller;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.util.NodePos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.ticks.ContainerSingleItem;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.MyBlocks;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.patchgraph.StorageConfiguration;
import party.stoat.patchwork.block.SFEnergyHandler;
import party.stoat.patchwork.patchgraph.nodes.VirtualizedBlockNode;
import party.stoat.patchwork.graphlib.SFControllerNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class SFControllerBlockEntity extends BlockEntity implements MenuProvider, ContainerSingleItem {

    private ItemStack theItem = ItemStack.EMPTY;
    private List<ItemStack> spawnIn = new ArrayList<>();

    public SimpleEnergyHandler storage = new SimpleEnergyHandler(1000000, 1000000, 1000000);

    public HashSet<BlockPos> loaded = new HashSet<>();

    public ServerPlayer watcher;

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, SFControllerBlockEntity entity) {
        ServerLevel serverLevel;

        if(entity.watcher != null && !(entity.watcher.containerMenu instanceof SFControllerMenu)) {
            entity.watcher = null;
        }

        if(level instanceof ServerLevel s) {
            serverLevel = s;
        } else return;

        var thisNode = new NodePos(entity.worldPosition, SFControllerNode.INSTANCE);
        BlockGraph sfNetworkGraph = Patchwork.UNIVERSE.getGraphWorld(serverLevel).getGraphForNode(thisNode);

        if(sfNetworkGraph == null) return;

        if(entity.storage.getAmountAsLong() > 0) {
            if(!blockState.getValue(SFController.POWERED)) {
                level.setBlockAndUpdate(blockPos, blockState.setValue(SFController.POWERED, true));
            }
        } else {
            if (blockState.getValue(SFController.POWERED)) {
                level.setBlockAndUpdate(blockPos, blockState.setValue(SFController.POWERED, false));
            }
        }

        if(sfNetworkGraph.getNodes().filter(c -> c.getNode() == SFControllerNode.INSTANCE).count() > 1) {
            serverLevel.setBlockAndUpdate(blockPos, blockState.setValue(SFController.ERROR, true));
            return;
        } else {
            if(blockState.getValue(SFController.ERROR)) serverLevel.setBlockAndUpdate(blockPos, blockState.setValue(SFController.ERROR, false));
        }

        var configs = StorageConfiguration.getConfigurationsFromNetwork(sfNetworkGraph);

        for(var config : configs) {
            for(var instance : config.instances.values()) {
                for(var node : instance.nodes.values()) {
                    if(node instanceof VirtualizedBlockNode virtual) {
//                        if(!entity.loaded.contains(virtual.proxyPos)) {
                            serverLevel.setChunkForced(virtual.proxyPos.getX() / 16, virtual.proxyPos.getZ() / 16, true);
                            entity.loaded.add(virtual.proxyPos);
//                        }
                    }
                }
            }

            config.initializeIfNeeded(serverLevel.getServer());
        }

        outer: try(Transaction transaction = Transaction.openRoot()) {
            for(var sfNode : sfNetworkGraph.getNodes().toList()) {
                if(sfNode.getBlockState().getBlock() instanceof SFEnergyHandler energyHandler) {
                    var desired = energyHandler.desiredAmount();
                    var extractedAmount = entity.storage.extract(desired, transaction);

                    if(extractedAmount < desired) break outer;

                    energyHandler.insert(extractedAmount, transaction);
                }
            }

            transaction.commit();
        }

        for(var sfNode : sfNetworkGraph.getNodes().toList()) {
            if(sfNode.getBlockState().getBlock() instanceof SFEnergyHandler energyHandler) {
                energyHandler.checkPowered(sfNode);
            }
        }

        if(!entity.spawnIn.isEmpty()) {
            for(ItemStack stack : entity.spawnIn) {
                for(var i=0;i<stack.count();i++) {
                    var id = UUID.randomUUID();

                    var pos = Patchwork.VIRTUAL_MANAGER.allocate(serverLevel, id, stack);

                    for(var config : configs) {
                        if(config.virtualized.size() < config.maxVirtualized) {
                            config.virtualized.add(pos);
                            break;
                        }
                    }
                }
            }
        }

        for(var config : configs) {
            var nodeGraph = Patchwork.UNIVERSE.getGraphWorld(serverLevel).getGraphForNode(new NodePos(blockPos, SFControllerNode.INSTANCE));
//            entity.storage.amount -= cost;


            outer: try(Transaction transaction = Transaction.openRoot()) {
                for(var patchInstance : config.instances.values()) {
                    for(var node : patchInstance.nodes.values()) {
                        try(Transaction inner = Transaction.open(transaction)) {
                            node.tick(config, patchInstance, serverLevel, nodeGraph, inner, entity);
                            var amount = entity.storage.getAmountAsInt() - 10;
                            entity.storage.set(Math.max(amount, 0));

                            if(amount >= 0) inner.commit();
                            else {
                                break outer;
                            }
                        }
                    }
                }

                transaction.commit();
            }
        }

        if(!entity.spawnIn.isEmpty() && entity.watcher != null) {
            StorageConfiguration.syncToPlayer(configs, sfNetworkGraph, serverLevel, entity.watcher, blockPos);
        }

        entity.spawnIn.clear();
    }

    @Override
    public @NonNull ItemStack getTheItem() {
        return theItem;
    }

    @Override
    public void setTheItem(@NonNull ItemStack itemStack) {
        this.spawnIn.add(itemStack.copyAndClear());
    }

    public static class ExternalStorage {
        public BlockPos pos;

    }

//    public Object2IntMap<Item> reserves = new Object2IntArrayMap<>();

    public SFControllerBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(MyBlocks.SF_CONTROLLER_BLOCK_ENTITY.get(), worldPosition, blockState);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        ContainerHelper.saveAllItems(output, NonNullList.of(theItem));
        output.putInt("energy", this.storage.getAmountAsInt());

        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        ContainerHelper.loadAllItems(input, NonNullList.of(theItem));

        input.getInt("energy").ifPresent(e -> this.storage.set(e));

        super.loadAdditional(input);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if(level == null) return;

        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return false;
    }


    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("block.patchwork.patch_controller");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NonNull Inventory inventory, @NonNull Player player) {
        return new SFControllerMenu(containerId, inventory, this);
    }
}
