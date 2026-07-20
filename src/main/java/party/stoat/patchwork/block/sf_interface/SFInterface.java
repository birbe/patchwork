package party.stoat.patchwork.block.sf_interface;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.NodePos;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.SFNetworkConnectable;
import party.stoat.patchwork.graphlib.SFBehavior;
import party.stoat.patchwork.graphlib.SFInterfaceNode;

import java.util.List;

public class SFInterface extends DirectionalBlock implements SFNetworkConnectable {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public SFInterface(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, FACING);
    }

    @Override
    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, @UpdateFlags int updateFlags, int updateLimit) {
        if(level instanceof ServerLevel serverLevel) {
            Patchwork.UNIVERSE.getGraphWorld(serverLevel).updateNodes(pos);
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();

        state = this.findConnections(context.getLevel(), state, context.getClickedPos());

        return state.setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.join(
                Shapes.join(
                Shapes.create(2.0 / 16.0, 2.0 / 16.0, 2.0 / 16.0, 2 / 16.0, 14.0 / 16.0, 14.0 / 16.0),
                Shapes.create(5.0 / 16.0, 2.0 / 16.0, 5.0 / 16.0, 11.0 / 16.0, 3.0 / 16.0, 11.0 / 16.0),
                BooleanOp.OR
        ),
                Shapes.create(7.0 / 16.0, 3.0 / 16.0, 7.0 / 16.0, 9.0 / 16.0, 9.0 / 16.0, 9.0 / 16.0),
                BooleanOp.OR
        );

        shape = Shapes.rotateAll(shape).get(state.getValue(FACING));

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

    private BlockState findConnections(Level level, BlockState state, BlockPos pos) {
        state = state.setValue(NORTH, level.getBlockState(pos.relative(Direction.NORTH)).getBlock() instanceof SFNetworkConnectable && state.getValue(FACING) != Direction.NORTH);
        state = state.setValue(EAST, level.getBlockState(pos.relative(Direction.EAST)).getBlock() instanceof SFNetworkConnectable && state.getValue(FACING) != Direction.EAST);
        state = state.setValue(SOUTH, level.getBlockState(pos.relative(Direction.SOUTH)).getBlock() instanceof SFNetworkConnectable && state.getValue(FACING) != Direction.SOUTH);
        state = state.setValue(WEST, level.getBlockState(pos.relative(Direction.WEST)).getBlock() instanceof SFNetworkConnectable && state.getValue(FACING) != Direction.WEST);
        state = state.setValue(UP, level.getBlockState(pos.relative(Direction.UP)).getBlock() instanceof SFNetworkConnectable && state.getValue(FACING) != Direction.UP);
        state = state.setValue(DOWN, level.getBlockState(pos.relative(Direction.DOWN)).getBlock() instanceof SFNetworkConnectable && state.getValue(FACING) != Direction.DOWN);

        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        level.setBlockAndUpdate(pos, this.findConnections(level, state, pos));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(SFInterface::new);
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, @NonNull Player player, @NonNull BlockHitResult hitResult) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public List<BlockNode> createNodes() {
        return List.of(SFInterfaceNode.INSTANCE);
    }
}
