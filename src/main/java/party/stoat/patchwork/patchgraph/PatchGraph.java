package party.stoat.patchwork.patchgraph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PatchGraph {

    public static final Codec<PatchGraph> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("name", "").forGetter(graph -> graph.name),
                    UUIDUtil.CODEC.fieldOf("graphId").forGetter(graph -> graph.graphId),
                    Connection.CODEC.listOf()
                            .fieldOf("connections")
                            .forGetter(graph -> graph.connections),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, NodeDescriptor.CODEC)
                            .fieldOf("nodeDescriptors")
                            .forGetter(graph -> graph.nodeDescriptors),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Vec2.CODEC)
                            .fieldOf("nodePositions")
                            .forGetter(graph -> graph.nodePositions)
            ).apply(instance, (name, graphId, connections, nodeDescriptors, nodePositions) -> {
                PatchGraph graph = new PatchGraph(graphId);
                graph.name = name;
                graph.connections = new ArrayList<>(connections);
                graph.nodeDescriptors = new HashMap<>(nodeDescriptors);
                graph.nodePositions = new HashMap<>(nodePositions);
                return graph;
            })
    );

    public record Connection(UUID from, UUID to, String keyFrom, String keyTo) {

        public static final Codec<Connection> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        UUIDUtil.CODEC.fieldOf("from").forGetter(Connection::from),
                        UUIDUtil.CODEC.fieldOf("to").forGetter(Connection::to),
                        Codec.STRING.fieldOf("keyFrom").forGetter(Connection::keyFrom),
                        Codec.STRING.fieldOf("keyTo").forGetter(Connection::keyTo)
                ).apply(instance, Connection::new)
        );

    }

    public String name;
    public UUID graphId;
    public List<Connection> connections = new ArrayList<>();
    public HashMap<UUID, NodeDescriptor> nodeDescriptors = new HashMap<>();
    public HashMap<UUID, Vec2> nodePositions = new HashMap<>();

    public PatchGraph(UUID id) {
        this.graphId = id;
    }

    public void fixConnections() {
        this.connections.removeIf(
                c -> {
                    if(!this.nodeDescriptors.containsKey(c.from)) return true;
                    if(!this.nodeDescriptors.containsKey(c.to)) return true;

                    if(this.nodeDescriptors.get(c.from).getPort(c.keyFrom) == null) return true;
                    return this.nodeDescriptors.get(c.to).getPort(c.keyTo) == null;
                }
        );
    }

    public void removeConnection(UUID from, UUID to, String keyFrom, String keyTo) {
        this.connections = this.connections.stream().filter(c -> !(c.from.equals(from) && c.to.equals(to) && c.keyFrom.equals(keyFrom) && c.keyTo.equals(keyTo))).collect(Collectors.toCollection(ArrayList::new));
    }

}
