package party.stoat.patchwork.block.sf_cable;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.SFNetworkConnectable;
import party.stoat.patchwork.graphlib.SFCableNode;

import java.util.List;

public class SFCable extends Block implements SFNetworkConnectable {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public SFCable(Properties properties) {
        super(properties);
        this.registerDefaultState(defaultBlockState());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public void onNeighborChange(@NonNull BlockState state, @NonNull LevelReader level, @NonNull BlockPos pos, @NonNull BlockPos neighbor) {
        if(level instanceof ServerLevel serverLevel) {
            Patchwork.UNIVERSE.getGraphWorld(serverLevel).updateNodes(pos);
        }
    }

    @Override
    protected void updateIndirectNeighbourShapes(@NonNull BlockState state, @NonNull LevelAccessor level, @NonNull BlockPos pos, @UpdateFlags int updateFlags, int updateLimit) {
        if(level instanceof ServerLevel serverLevel) {
            Patchwork.UNIVERSE.getGraphWorld(serverLevel).updateNodes(pos);
        }
    }

    @Override
    protected @NonNull VoxelShape getOcclusionShape(BlockState state) {
        VoxelShape shape = Shapes.create(7.0 / 16.0, 7.0 / 16.0, 7.0 / 16.0, 9.0 / 16.0, 9.0 / 16.0, 9.0 / 16.0);
        VoxelShape part = Shapes.create(7.0 / 16.0, 7.0 / 16.0, 0.0, 9.0 / 16.0, 9.0 / 16.0, 7.0 / 16.0);

        var parts = Shapes.rotateAll(part);

        if(state.getValue(NORTH)) shape = Shapes.join(shape, parts.get(Direction.NORTH), BooleanOp.OR);
        if(state.getValue(EAST)) shape = Shapes.join(shape, parts.get(Direction.EAST), BooleanOp.OR);
        if(state.getValue(SOUTH)) shape = Shapes.join(shape, parts.get(Direction.SOUTH), BooleanOp.OR);
        if(state.getValue(WEST)) shape = Shapes.join(shape, parts.get(Direction.WEST), BooleanOp.OR);
        if(state.getValue(UP)) shape = Shapes.join(shape, parts.get(Direction.UP), BooleanOp.OR);
        if(state.getValue(DOWN)) shape = Shapes.join(shape, parts.get(Direction.DOWN), BooleanOp.OR);

        return shape;
    }

    @Override
    protected @NonNull VoxelShape getVisualShape(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return this.getOcclusionShape(state);
    }

    @Override
    protected @NonNull VoxelShape getCollisionShape(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return this.getOcclusionShape(state);
    }

    @Override
    protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return this.getOcclusionShape(state);
    }

    @Override
    protected @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return super.getRenderShape(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.findConnections(context.getLevel(), null, context.getClickedPos());
    }

    private BlockState findConnections(Level level, BlockState state, BlockPos pos) {
        BlockState newState = state == null ? defaultBlockState() : state;

        newState = newState.setValue(NORTH, level.getBlockState(pos.relative(Direction.NORTH)).getBlock() instanceof SFNetworkConnectable);
        newState = newState.setValue(EAST, level.getBlockState(pos.relative(Direction.EAST)).getBlock() instanceof SFNetworkConnectable);
        newState = newState.setValue(SOUTH, level.getBlockState(pos.relative(Direction.SOUTH)).getBlock() instanceof SFNetworkConnectable);
        newState = newState.setValue(WEST, level.getBlockState(pos.relative(Direction.WEST)).getBlock() instanceof SFNetworkConnectable);
        newState = newState.setValue(UP, level.getBlockState(pos.relative(Direction.UP)).getBlock() instanceof SFNetworkConnectable);
        newState = newState.setValue(DOWN, level.getBlockState(pos.relative(Direction.DOWN)).getBlock() instanceof SFNetworkConnectable);

        return newState;
    }

    @Override
    protected void neighborChanged(@NonNull BlockState state, Level level, @NonNull BlockPos pos, @NonNull Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        level.setBlockAndUpdate(pos, this.findConnections(level, state, pos));
    }

    @Override
    public List<BlockNode> createNodes() {
        return List.of(SFCableNode.INSTANCE);
    }

}
