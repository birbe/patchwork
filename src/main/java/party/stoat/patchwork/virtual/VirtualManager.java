package party.stoat.patchwork.virtual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class VirtualManager {

    public HashMap<UUID, BlockPos> proxies = new HashMap<>();

    public BlockPos allocate(Level level, UUID uuid, ItemStack stack) {
        if(stack.getItem() instanceof BlockItem blockItem) {
            int count = proxies.size();
            int x = (count % 16) * 4;
            int y = 0;
            int z = (count / 16) * 4;

            var pos = new BlockPos(x, y, z);

            level.setBlockAndUpdate(pos, blockItem.getBlock().defaultBlockState());

            this.proxies.put(uuid, pos);
            return pos;
        } else return null;
    }

}
