package party.stoat.patchwork.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import party.stoat.patchwork.Patchwork;

public class SFControllerMenu extends AbstractContainerMenu {

    public SFControllerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1));
    }

    public SFControllerMenu(int containerId, Inventory inventory, Container container) {
        super(Patchwork.CONTROLLER_MENU_TYPE.get(), containerId);

        this.addSlot(new Slot(
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