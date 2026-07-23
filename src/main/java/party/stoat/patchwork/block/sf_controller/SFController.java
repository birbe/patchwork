package party.stoat.patchwork.block.sf_controller;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.NodePos;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.MyBlocks;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.SFNetworkConnectable;
import party.stoat.patchwork.graphlib.SFCableNode;
import party.stoat.patchwork.graphlib.SFControllerNode;

import java.util.List;

public class SFController extends BaseEntityBlock implements SFNetworkConnectable {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty ERROR = BooleanProperty.create("error");

    public SFController(Properties properties) {
        super(properties);
    }
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SFController::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, ERROR);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, false).setValue(ERROR, false);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        if(level instanceof ServerLevel serverLevel) {
            Patchwork.UNIVERSE.getGraphWorld(serverLevel).updateNodes(pos);
        }
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
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        return createTickerHelper(type, MyBlocks.SF_CONTROLLER_BLOCK_ENTITY.get(), SFControllerBlockEntity::tick);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new SFControllerBlockEntity(worldPosition, blockState);
    }

    @Override
    public List<BlockNode> createNodes() {
        return List.of(SFControllerNode.INSTANCE);
    }

}