package party.stoat.patchwork.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import party.stoat.patchwork.Patchwork;

@Mixin(Player.class)
public class YesThePlayerIsInRange {

    @Inject(method = "isWithinBlockInteractionRange", at = @At("RETURN"), cancellable = true)
    public void yesInRange(BlockPos pos, double buffer, CallbackInfoReturnable<Boolean> cir) {
        if(Patchwork.isPosTotallyEffedTheFuckUp(pos.getX(), pos.getZ())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isWithinEntityInteractionRange(Lnet/minecraft/world/entity/Entity;D)Z", at = @At("RETURN"), cancellable = true)
    public void yupTotallyYep(Entity entity, double buffer, CallbackInfoReturnable<Boolean> cir) {
        if(Patchwork.isPosTotallyEffedTheFuckUp(entity.getX(), entity.getZ())) {
            cir.setReturnValue(true);
        }
    }

}
