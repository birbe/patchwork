package party.stoat.patchwork.graphlib;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.kneelawk.graphlib.api.wire.FullWireBlockNode;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import party.stoat.patchwork.Patchwork;

import java.util.Collection;
import java.util.List;

public class SFCableNode implements FullWireBlockNode {

    public static final SFCableNode INSTANCE = new SFCableNode();
    public static final BlockNodeType TYPE = BlockNodeType.of(Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "sf_cable"), () -> INSTANCE);

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }

}
