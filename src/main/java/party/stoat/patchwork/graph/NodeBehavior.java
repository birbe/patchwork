package party.stoat.patchwork.graph;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface NodeBehavior {

    NodePushResult receiveItems(Level level, String key, ItemStack stack);

    void tick(  );

}
