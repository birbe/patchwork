package party.stoat.patchwork.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import party.stoat.patchwork.Patchwork;

import java.util.UUID;

public record OpenRemoteMachineServerboundPayload(UUID patch, UUID node, BlockPos pos) implements CustomPacketPayload {

    public static final Identifier REMOTE_OPEN = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "remote_open");
    public static final CustomPacketPayload.Type<OpenRemoteMachineServerboundPayload> TYPE = new CustomPacketPayload.Type<>(REMOTE_OPEN);
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRemoteMachineServerboundPayload> CODEC = StreamCodec
            .composite(
                    UUIDUtil.STREAM_CODEC, OpenRemoteMachineServerboundPayload::patch,
                    UUIDUtil.STREAM_CODEC, OpenRemoteMachineServerboundPayload::node,
                    BlockPos.STREAM_CODEC, OpenRemoteMachineServerboundPayload::pos,
                    OpenRemoteMachineServerboundPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
