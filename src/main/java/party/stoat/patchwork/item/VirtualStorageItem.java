package party.stoat.patchwork.item;

import net.minecraft.world.item.Item;

public class VirtualStorageItem extends Item {

    public final int maxGraphs;
    public final int maxVirtualized;

    public VirtualStorageItem(Properties properties, int maxGraphs, int maxVirtualized) {
        super(properties);
        this.maxGraphs = maxGraphs;
        this.maxVirtualized = maxVirtualized;
    }

}
