package party.stoat.patchwork.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import party.stoat.patchwork.Patchwork;

public record SFControllerSyncClientboundPayload(String patches, String nodeDescriptors, BlockPos controllerPos) implements CustomPacketPayload {

    public static final Identifier PATCH_CONTROLLER_SYNC = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "patch_sync");
    public static final CustomPacketPayload.Type<SFControllerSyncClientboundPayload> TYPE = new CustomPacketPayload.Type<>(PATCH_CONTROLLER_SYNC);
    public static final StreamCodec<RegistryFriendlyByteBuf, SFControllerSyncClientboundPayload> CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.STRING_UTF8, SFControllerSyncClientboundPayload::patches,
                    ByteBufCodecs.STRING_UTF8, SFControllerSyncClientboundPayload::nodeDescriptors,
                    BlockPos.STREAM_CODEC, SFControllerSyncClientboundPayload::controllerPos,
                    SFControllerSyncClientboundPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
