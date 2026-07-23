package party.stoat.patchwork.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import party.stoat.patchwork.virtual.LevelVirtualBlockCache;
import party.stoat.patchwork.virtual.VirtualSingleChunk;

import java.util.HashMap;
import java.util.Optional;

@Mixin(Level.class)
abstract class AddCachedBlockToClientLevel implements LevelVirtualBlockCache {

    @Shadow
    public abstract RegistryAccess registryAccess();

    @Shadow
    public abstract boolean isClientSide();

    @Unique
    private HashMap<BlockPos, VirtualSingleChunk> patchwork$cachedChunk;

    @Inject(method = "<init>*", at = @At("TAIL"))
    private void populate(WritableLevelData levelData, ResourceKey dimension, RegistryAccess registryAccess, Holder dimensionTypeRegistration, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates, CallbackInfo ci) {
        patchwork$cachedChunk = new HashMap<>();
    }

    @Override
    public HashMap<BlockPos, VirtualSingleChunk> patchwork$getCache() {
        return this.patchwork$cachedChunk;
    }

    @Override
    public void patchwork$cacheBlock(BlockPos blockPos, BlockState state, Optional<CompoundTag> nbt) {
        BlockEntity entity = null;

        if(nbt.isPresent()) {
            entity = BlockEntity.loadStatic(blockPos, state, nbt.get(), this.registryAccess());
            entity.setLevel((Level) (Object) this);
            entity.setBlockState(state);
        }

        var chunk = new VirtualSingleChunk(new ChunkPos(blockPos.getX() / 16, blockPos.getZ() / 16), (Level) (Object) this, blockPos, state, entity);
        this.patchwork$cachedChunk.put(blockPos, chunk);
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At("RETURN"), cancellable = true)
    public void virtualizeChunk(int chunkX, int chunkZ, ChunkStatus status, boolean loadOrGenerate, CallbackInfoReturnable<ChunkAccess> cir) {
        if(cir.getReturnValue() == null || cir.getReturnValue() instanceof EmptyLevelChunk && this.isClientSide()) {
            var blockPos = new BlockPos(chunkX * 16, 0, chunkZ * 16);
            if(this.patchwork$cachedChunk.containsKey(blockPos)) {
                cir.setReturnValue(this.patchwork$cachedChunk.get(blockPos));
            }
        }
    }

    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    public void tryToRedirectToCachedBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if(cir.getReturnValue() == null && this.isClientSide()) {
            if(this.patchwork$cachedChunk.containsKey(pos)) {
                cir.setReturnValue(this.patchwork$cachedChunk.get(pos).getBlockState(pos));
            }
        }
    }

    @Inject(method = "getBlockEntity", at = @At("RETURN"), cancellable = true)
    public void tryToRedirectBlockEntity(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        if(cir.getReturnValue() == null && this.isClientSide()) {
            if(this.patchwork$cachedChunk.containsKey(pos)) {
                cir.setReturnValue(this.patchwork$cachedChunk.get(pos).getBlockEntity(pos));
            }
        }
    }



}
