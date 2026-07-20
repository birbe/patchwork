package party.stoat.patchwork.graph;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

public class StaticContainerNode extends ContainerNode {

    public StaticContainerNode(UUID uuid, NodeDescriptor descriptor) {
        super(uuid, descriptor);
    }

}
