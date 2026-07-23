package party.stoat.patchwork.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import party.stoat.patchwork.Patchwork;

@Mixin(Entity.class)
public class FuckUpTheDistances {

    @Inject(method = "distanceToSqr(DDD)D", at = @At("RETURN"), cancellable = true)
    public void yuuup(double x2, double y2, double z2, CallbackInfoReturnable<Double> cir) {
        if(Patchwork.isPosTotallyEffedTheFuckUp(x2, z2)) {
            cir.setReturnValue(0.0);
        }
    }

    @Inject(method = "distanceTo", at = @At("RETURN"), cancellable = true)
    public void yeeeep(Entity entity, CallbackInfoReturnable<Float> cir) {
        if(Patchwork.isPosTotallyEffedTheFuckUp(entity.getX(), entity.getZ())) {
            cir.setReturnValue(0.0f);
        }
    }

    @Inject(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At("RETURN"), cancellable = true)
    public void yuupNumbertwo(Vec3 pos, CallbackInfoReturnable<Double> cir) {
        if(Patchwork.isPosTotallyEffedTheFuckUp(pos.x, pos.z)) {
            cir.setReturnValue(0.0);
        }
    }

}
