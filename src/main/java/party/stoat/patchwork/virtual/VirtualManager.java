package party.stoat.patchwork.virtual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class VirtualManager {

    public BlockPos allocate(ServerLevel level, UUID uuid, ItemStack stack) {
        if(stack.getItem() instanceof BlockItem blockItem) {
            var data = level.getDataStorage().computeIfAbsent(MachineLevelSavedData.ID);

            int count = data.increment();
            int x = -(level.getWorldBorder().getAbsoluteMaxSize() + 16);
            int y = 0;
            int z = -(level.getWorldBorder().getAbsoluteMaxSize() + 16) + (count * 160);

            level.setChunkForced(x / 16, z / 16, true);

            var pos = new BlockPos(x, y, z);
            data.virtualized.add(pos);

            for(int xD=-8;xD<5;xD++) {
                for(int yD=-5;yD<5;yD++) {
                    for(int zD=-5;zD<5;zD++) {
                        var posD = new BlockPos(
                                xD + pos.getX(), yD + pos.getY(), zD + pos.getZ()
                        );

                        if(xD == -8 || xD == 4 || yD == -5 || yD == 4 || zD == -5 || zD == 4) {
                            level.setBlock(
                                    posD,
                                    Blocks.BEDROCK.defaultBlockState(),
                                    Block.UPDATE_NONE
                            );
                        } else {
                            level.setBlock(posD, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
                        }
                    }
                }
            }

            level.setBlockAndUpdate(pos, blockItem.getBlock().defaultBlockState());

            data.setDirty();
//
//            BlockPos supportPos = pos.below();
//
//            level.setBlockAndUpdate(supportPos, Blocks.BEDROCK.defaultBlockState());
//
//            Vec3 hitLocation = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
//
//            BlockHitResult hitResult = new BlockHitResult(
//                    hitLocation,
//                    Direction.UP,
//                    supportPos,
//                    false
//            );
//
//            UseOnContext useOnContext = new UseOnContext(
//                    level,
//                    null,
//                    InteractionHand.MAIN_HAND,
//                    stack,
//                    hitResult
//            );
//
//            blockItem.place(new BlockPlaceContext(
//                    useOnContext
//            ));

            return pos;
        } else return null;
    }

}
