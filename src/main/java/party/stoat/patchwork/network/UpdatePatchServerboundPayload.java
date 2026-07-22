package party.stoat.patchwork.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.patchgraph.PatchGraph;

import java.util.UUID;

public record UpdatePatchServerboundPayload(UUID id, PatchGraph graph, BlockPos pos) implements CustomPacketPayload {

    public static final Identifier UPDATE_GRAPH = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "update_patch");
    public static final CustomPacketPayload.Type<UpdatePatchServerboundPayload> TYPE = new CustomPacketPayload.Type<>(UPDATE_GRAPH);
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePatchServerboundPayload> CODEC = StreamCodec
            .composite(
                    UUIDUtil.STREAM_CODEC, UpdatePatchServerboundPayload::id,
                    ByteBufCodecs.fromCodec(PatchGraph.CODEC), UpdatePatchServerboundPayload::graph,
                    BlockPos.STREAM_CODEC, UpdatePatchServerboundPayload::pos,
                    UpdatePatchServerboundPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
