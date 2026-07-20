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

public class SFControllerNode implements FullWireBlockNode {

    public static final SFControllerNode INSTANCE = new SFControllerNode();
    public static final BlockNodeType TYPE = BlockNodeType.of(Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "sf_controller"), () -> INSTANCE);

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {
        if(self.getGraph() != null) self.getGraph().getNodes().forEach(
                n -> System.out.println("got dang " + n.getBlockPos())
        );
    }

}
