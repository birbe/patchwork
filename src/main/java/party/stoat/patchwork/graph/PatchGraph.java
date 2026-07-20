package party.stoat.patchwork.graph;

import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PatchGraph {

    public record Connection(UUID from, UUID to, String keyFrom, String keyTo) {}

    public HashMap<UUID, NodeDescriptor> nodeDescriptors = new HashMap<>();
    public HashMap<UUID, Vec2> nodePositions = new HashMap<>();

    public List<Connection> connections = new ArrayList<>();
    public String name;
    public UUID graphId;

    public PatchGraph(UUID id) {
        this.graphId = id;
    }

    public void removeConnection(UUID from, UUID to, String keyFrom, String keyTo) {
        this.connections = this.connections.stream().filter(c -> !(c.from == from && c.to == to && c.keyFrom.equals(keyFrom) && c.keyTo.equals(keyTo))).collect(Collectors.toCollection(ArrayList::new));
    }

}
