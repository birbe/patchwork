package party.stoat.patchwork.virtual;

import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

public interface LevelVirtualDrops {

    boolean patchwork$get();

    void patchwork$set(boolean state);

    List<ItemEntity> patchwork$getDrops();

    void patchwork$addDrop(ItemEntity e);

}