package party.stoat.patchwork.graphlib;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.util.NodePos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.controller.SFControllerBlockEntity;
import party.stoat.patchwork.block.sf_interface.SFInterface;
import party.stoat.patchwork.block.sf_interface.SFInterfaceBlockEntity;

import java.util.ArrayList;

public class SFBehavior {

    public static void getPatchGraphs(BlockGraph graph) {

    }

    public static void networkUpdated(ServerLevel level, NodePos nodePos) {
        var graph = Patchwork.UNIVERSE.getGraphWorld(level).getGraphForNode(nodePos);
    }

}
