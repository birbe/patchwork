package party.stoat.patchwork.block;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.NodePos;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Interaction;
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
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.NonNull;import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.sf_controller.SFControllerBlockEntity;
import party.stoat.patchwork.graphlib.SFControllerNode;
import party.stoat.patchwork.patchgraph.StorageConfiguration;
import party.stoat.patchwork.graphlib.SFCableNode;

import java.util.List;

public class SFTerminal extends DirectionalBlock implements SFNetworkConnectable, SFEnergyHandler {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public SimpleEnergyHandler storage = new SimpleEnergyHandler(5, 5, 0);

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
        return defaultBlockState().setValue(POWERED, false).setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        if (level instanceof ServerLevel serverLevel) {
            BlockGraph graph = Patchwork.UNIVERSE.getGraphWorld(serverLevel).getGraphForNode(new NodePos(pos, SFCableNode.INSTANCE));

            if(graph != null) if(graph.getNodes().filter(c -> c.getNode() == SFControllerNode.INSTANCE).count() > 1) return InteractionResult.FAIL;

            if(graph != null) for(var node : graph.getNodes().toList()) {
                if(node.getBlockEntity() instanceof SFControllerBlockEntity e) {
                    if(e.watcher != null) {
                        player.sendSystemMessage(Component.literal("This network is already in use by another player, please wait until they close their terminal."));
                        return InteractionResult.FAIL;
                    }
                    player.openMenu(e);
                    e.watcher = (ServerPlayer) player;

                    var configs = StorageConfiguration.getConfigurationsFromNetwork(graph);

                    StorageConfiguration.syncToPlayer(configs, graph, serverLevel, e.watcher, e.getBlockPos());

                    break;
                }
            }

        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public List<BlockNode> createNodes() {
        return List.of(SFCableNode.INSTANCE);
    }

    @Override
    public int desiredAmount() {
        return this.storage.getAmountAsLong() < this.storage.getCapacityAsLong() ? 1 : 0;
    }

    @Override
    public void checkPowered(NodeHolder<BlockNode> node) {
        BlockState state = node.getBlockState().setValue(POWERED, this.storage.getAmountAsLong() > 0);
        if(node.getBlockState() != state) {
            node.getBlockWorld().setBlockAndUpdate(node.getBlockPos(), state);
        }
    }

    @Override
    public long getAmountAsLong() {
        return this.storage.getAmountAsLong();
    }

    @Override
    public long getCapacityAsLong() {
        return this.storage.getCapacityAsLong();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        return this.storage.insert(amount, transaction);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return this.storage.extract(amount, transaction);
    }
}