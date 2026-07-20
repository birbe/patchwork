package party.stoat.patchwork.block;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.NodePos;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.controller.SFControllerBlockEntity;
import party.stoat.patchwork.graphlib.SFCableNode;
import party.stoat.patchwork.graphlib.SFControllerNode;

import java.util.List;

public class SFTerminal extends DirectionalBlock implements SFNetworkConnectable {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public SFTerminal(Properties properties) {
        super(properties);
        this.registerDefaultState(defaultBlockState());

    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(SFTerminal::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, false).setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    @Override
    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, @UpdateFlags int updateFlags, int updateLimit) {
        if(level instanceof ServerLevel serverLevel) {
            Patchwork.UNIVERSE.getGraphWorld(serverLevel).updateNodes(pos);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if(level.isClientSide()) return;

        SFControllerBlockEntity thisEntity = (SFControllerBlockEntity) level.getBlockEntity(pos);
        if(thisEntity == null) return;

        Patchwork.UNIVERSE.getGraphWorld((ServerLevel) level).getNodeAt(new NodePos(pos, SFCableNode.INSTANCE));

        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, @NonNull Player player, @NonNull BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SFControllerBlockEntity e) {
            player.openMenu(e);
            e.watcher = (ServerPlayer) player;
//            PacketDistributor.sendToPlayer((ServerPlayer) player, new PatchControllerSyncClientboundPayload(new Gson().toJson(e.patches), pos));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public List<BlockNode> createNodes() {
        return List.of(SFControllerNode.INSTANCE);
    }

}