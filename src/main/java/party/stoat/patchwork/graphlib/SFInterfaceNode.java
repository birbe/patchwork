package party.stoat.patchwork.graphlib;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.kneelawk.graphlib.api.util.NodePos;
import com.kneelawk.graphlib.api.wire.FullWireBlockNode;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DirectionalBlock;
import org.jetbrains.annotations.NotNull;
import party.stoat.patchwork.Patchwork;

import java.util.Collection;
import java.util.List;

public class SFInterfaceNode implements FullWireBlockNode {

    public static final SFInterfaceNode INSTANCE = new SFInterfaceNode();
    public static final BlockNodeType TYPE = BlockNodeType.of(Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "sf_interface"), () -> INSTANCE);

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {
        if(self.getBlockWorld() instanceof ServerLevel serverLevel) SFBehavior.networkUpdated(serverLevel, self.getPos());
    }
}
