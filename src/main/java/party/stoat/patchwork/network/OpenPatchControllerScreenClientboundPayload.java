package party.stoat.patchwork.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import party.stoat.patchwork.Patchwork;

public record OpenPatchControllerScreenClientboundPayload(String state, BlockPos pos) implements CustomPacketPayload {
    public static final Identifier OPEN_PATCH_CONTROLLER_SCREEN = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "open");
    public static final CustomPacketPayload.Type<OpenPatchControllerScreenClientboundPayload> TYPE = new CustomPacketPayload.Type<>(OPEN_PATCH_CONTROLLER_SCREEN);
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPatchControllerScreenClientboundPayload> CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.STRING_UTF8, OpenPatchControllerScreenClientboundPayload::state,
                    BlockPos.STREAM_CODEC, OpenPatchControllerScreenClientboundPayload::pos,
                    OpenPatchControllerScreenClientboundPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
