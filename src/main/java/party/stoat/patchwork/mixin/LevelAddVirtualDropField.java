package party.stoat.patchwork.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import party.stoat.patchwork.virtual.LevelVirtualDrops;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public class LevelAddVirtualDropField implements LevelVirtualDrops {

    @Unique
    public boolean patchwork$virtualDrop = false;

    @Unique
    public ArrayList<ItemEntity> patchwork$drops;

    @Inject(method = "<init>*", at = @At("RETURN"))
    public void init(MinecraftServer server, Executor executor, LevelStorageSource.LevelStorageAccess levelStorage, ServerLevelData levelData, ResourceKey dimension, LevelStem levelStem, boolean isDebug, long biomeZoomSeed, List customSpawners, boolean tickTime, CallbackInfo ci) {
        this.patchwork$drops = new ArrayList<>();
    }

    @Override
    public boolean patchwork$get() {
        return this.patchwork$virtualDrop;
    }

    @Override
    public void patchwork$set(boolean state) {
        this.patchwork$virtualDrop = state;
    }

    @Override
    public List<ItemEntity> patchwork$getDrops() {
        var drops = new ArrayList<>(patchwork$drops);
        patchwork$drops.clear();

        return drops;
    }

    @Override
    public void patchwork$addDrop(ItemEntity e) {
        patchwork$drops.add(e);
    }
}
