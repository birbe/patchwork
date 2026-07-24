package party.stoat.patchwork.block.sf_controller;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.patchgraph.StorageConfiguration;

public class SFControllerMenu extends AbstractContainerMenu {

    static class SFControllerInputSlot extends Slot {

        public SFControllerInputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            if(!(itemStack.getItem() instanceof BlockItem)) return false;

            if(this.container instanceof SFControllerBlockEntity controller) {
                var graph = controller.getGraphLibGraph();
                if(graph == null) return false;
                var configs = StorageConfiguration.getConfigurationsFromNetwork(graph);

                for(var config : configs) {
                    if(config.virtualized.size() < config.maxVirtualized) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    public SFControllerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1));
    }

    public SFControllerMenu(int containerId, Inventory inventory, Container container) {
        super(Patchwork.CONTROLLER_MENU_TYPE.get(), containerId);

        this.addSlot(new SFControllerInputSlot(
                container,
                0,
                -9999,
                -9999
        ));

        this.addStandardInventorySlots(inventory, 0, 0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.getSlot(slotIndex);

        if(slotIndex > 0) {
            this.moveItemStackTo(slot.getItem(), 0, 1, false);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

}