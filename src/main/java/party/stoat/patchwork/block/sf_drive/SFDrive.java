package party.stoat.patchwork.block.sf_drive;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.SFNetworkConnectable;
import party.stoat.patchwork.graphlib.SFDriveNode;

import java.util.List;

public class SFDrive extends BaseEntityBlock implements SFNetworkConnectable {

    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    public SFDrive(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SFDrive::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, @UpdateFlags int updateFlags, int updateLimit) {
        if(level instanceof ServerLevel serverLevel) {
            Patchwork.UNIVERSE.getGraphWorld(serverLevel).updateNodes(pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if(level instanceof ServerLevel serverLevel) {
            Patchwork.UNIVERSE.getGraphWorld(serverLevel).updateNodes(pos);
        }
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, Level level, @NonNull BlockPos pos, @NonNull Player player, @NonNull BlockHitResult hitResult) {
        BlockEntity entity = level.getBlockEntity(pos);

        if(entity instanceof SFDriveBlockEntity drive) {
            player.openMenu(drive);
        }

        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return new SFDriveBlockEntity(blockPos, blockState);
    }

    @Override
    public List<BlockNode> createNodes() {
        return List.of(SFDriveNode.INSTANCE);
    }
}
