package party.stoat.patchwork.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class DontCloseContainer {

//    @Inject(method = "closeContainer", at = @At("HEAD"), cancellable = true)
//    public void dontClose(CallbackInfo ci) {
//        ci.cancel();
//    }
//
//    @Inject(method = "doCloseContainer", at = @At("HEAD"), cancellable = true)
//    public void plsDont(CallbackInfo ci) {
//        ci.cancel();
//    }

}
