package party.stoat.patchwork.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import party.stoat.patchwork.MyBlocks;

@Mixin(Container.class)
public interface ContainerInMachineLevelYupThatsValidYup {

    @Inject(method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;F)Z", at = @At("HEAD"), cancellable = true)
    private static void patchwork$yuupPointsUpThatsValid(BlockEntity blockEntity, Player player, float distanceBuffer, CallbackInfoReturnable<Boolean> cir) {
        if(blockEntity.getLevel() == blockEntity.getLevel().getServer().getLevel(MyBlocks.MACHINE_LEVEL)) {
            cir.setReturnValue(true);
        }
    }

}
