package party.stoat.patchwork.block.controller;

import com.kneelawk.graphlib.api.util.NodePos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.MyBlocks;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.ControllerConfiguration;
import party.stoat.patchwork.block.SFControllerMenu;
import party.stoat.patchwork.graph.*;
import party.stoat.patchwork.graphlib.SFControllerNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SFControllerBlockEntity extends BlockEntity implements MenuProvider, ContainerSingleItem, WorldlyContainer {

    private ItemStack theItem = ItemStack.EMPTY;
    private List<ItemStack> spawnIn = new ArrayList<>();

    public ControllerConfiguration config = new ControllerConfiguration();

//    public SimpleEnergyStorage storage = new SimpleEnergyStorage(1000L, 1000L, 0L);

    public ServerPlayer watcher;

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, SFControllerBlockEntity entity) {
        ServerLevel serverLevel;
        if(level instanceof ServerLevel s) {
            serverLevel = s;
        } else return;

        entity.config.initializeIfNeeded(serverLevel.getServer());

        var machineLevel = level.getServer().getLevel(MyBlocks.MACHINE_LEVEL);

//        entity.storage.amount = Math.max(0, entity.storage.amount);

//        if(entity.storage.getAmount() > 0) {
//            if(!blockState.getValue(SFController.POWERED)) {
//                level.setBlockAndUpdate(blockPos, blockState.setValue(SFController.POWERED, true));
//            }
//        } else {
//            if(blockState.getValue(SFController.POWERED)) {
//                level.setBlockAndUpdate(blockPos, blockState.setValue(SFController.POWERED, false));
//            }
//        }

        if(!entity.spawnIn.isEmpty()) {
            for(ItemStack stack : entity.spawnIn) {
                var id = UUID.randomUUID();

                var pos = Patchwork.VIRTUAL_MANAGER.allocate(machineLevel, id, stack);

                entity.config.virtualized.add(pos);
            }
        }

//        if(entity.storage.amount < cost) {
//            entity.storage.amount = 0;
//        }

//        if(machineLevel != null && entity.storage.amount >= cost) {
        if(machineLevel != null && entity.config != null) {
            var nodeGraph = Patchwork.UNIVERSE.getGraphWorld(serverLevel).getGraphForNode(new NodePos(blockPos, SFControllerNode.INSTANCE));
//            entity.storage.amount -= cost;
            for(var patchInstance : entity.config.instances.values()) {
                for(var node : patchInstance.nodes.values()) node.tick(entity.config, patchInstance, serverLevel, nodeGraph);
            }
        }

        if(!entity.spawnIn.isEmpty() && entity.watcher != null) {
//            PacketDistributor.sendToPlayer(entity.watcher, new PatchControllerSyncClientboundPayload(
//                    new Gson().toJson(entity.patches), entity.getBlockPos()
//            ));
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

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return false;
    }

    public static class ExternalStorage {
        public BlockPos pos;

    }

//    public Object2IntMap<Item> reserves = new Object2IntArrayMap<>();

    public SFControllerBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(MyBlocks.SF_CONTROLLER_BLOCK_ENTITY.get(), worldPosition, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        this.config.save(output.child("config"));
        ContainerHelper.saveAllItems(output, NonNullList.of(theItem));

        super.saveAdditional(output);
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
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        input.child("config").ifPresentOrElse(
                config -> {
                    this.config = ControllerConfiguration.load(config);
                },
                () -> this.config = new ControllerConfiguration()
        );

        ContainerHelper.loadAllItems(input, NonNullList.of(theItem));

        super.loadAdditional(input);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.patchwork.patch_controller");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SFControllerMenu(containerId, inventory, this);
    }
}
