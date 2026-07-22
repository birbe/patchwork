package party.stoat.patchwork.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import party.stoat.patchwork.Patchwork;

public record EjectVirtualizedMachineServerboundPayload(BlockPos controllerPos, BlockPos virtualPos) implements CustomPacketPayload {
    public static final Identifier EJECT_VIRTUAL = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "eject_virtual");
    public static final CustomPacketPayload.Type<EjectVirtualizedMachineServerboundPayload> TYPE = new CustomPacketPayload.Type<>(EJECT_VIRTUAL);
    public static final StreamCodec<RegistryFriendlyByteBuf, EjectVirtualizedMachineServerboundPayload> CODEC = StreamCodec
            .composite(
                    BlockPos.STREAM_CODEC, EjectVirtualizedMachineServerboundPayload::controllerPos,
                    BlockPos.STREAM_CODEC, EjectVirtualizedMachineServerboundPayload::virtualPos,
                    EjectVirtualizedMachineServerboundPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
