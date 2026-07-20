package party.stoat.patchwork.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import party.stoat.patchwork.Patchwork;

public record CreatePatchServerboundPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Identifier CREATE_PATCH_ID = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "create_patch");
    public static final CustomPacketPayload.Type<CreatePatchServerboundPayload> TYPE = new CustomPacketPayload.Type<>(CREATE_PATCH_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, CreatePatchServerboundPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, CreatePatchServerboundPayload::pos, CreatePatchServerboundPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
