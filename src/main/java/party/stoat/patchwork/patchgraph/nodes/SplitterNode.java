package party.stoat.patchwork.patchgraph.nodes;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.patchgraph.Node;
import party.stoat.patchwork.patchgraph.NodeDescriptor;
import party.stoat.patchwork.patchgraph.PatchGraph;
import party.stoat.patchwork.patchgraph.PatchInstance;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SplitterNode extends Node {

    public static final Identifier IDENTIFIER = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "splitter");

    public SplitterNode(UUID uuid, NodeDescriptor descriptor) {
        super(uuid, descriptor);
    }

    record SplitterResourceHandler<T extends Resource>(List<ResourceHandler<T>> handlers, T empty) implements ResourceHandler<T> {

        @Override
        public int size() {
            return handlers.stream().mapToInt(ResourceHandler::size).sum();
        }

        @Override
        public @NonNull T getResource(int index) {
            if(handlers.isEmpty()) return empty;
            var i = 0;

            for(var handler : handlers) {
                if(index - i < handler.size()) {
                    return handler.getResource(index - i);
                }
                i += handler.size();
            }

            return empty;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, @NonNull T resource) {
            return handlers.stream().mapToLong(h -> h.getCapacityAsLong(index, resource)).sum();
        }

        @Override
        public boolean isValid(int index, @NonNull T resource) {
            return getResource(index).equals(resource);
        }

        @Override
        public int insert(@NonNull T resource, int amount, @NonNull TransactionContext transaction) {
            if(handlers.isEmpty()) return 0;
            var per = amount / handlers.size();

            var inserted = 0;

            for(var handler : handlers) {
                inserted += handler.insert(resource, per, transaction);
            }

            return inserted;
        }

        @Override
        public int insert(int index, @NonNull T resource, int amount, @NonNull TransactionContext transaction) {
            if(handlers.isEmpty()) return 0;
            var i = 0;

            for(var handler : handlers) {
                if(index - i < handler.size()) {
                    return handler.insert(index - i, resource, amount, transaction);
                }
                i += handler.size();
            }

            return 0;
        }

        @Override
        public int extract(int index, @NonNull T resource, int amount, @NonNull TransactionContext transaction) {
            return 0;
        }
    }

    @Override
    public @Nullable ResourceHandler<ItemResource> getItemHandler(ServerLevel level, NodeDescriptor.IO p_, PatchInstance patch) {
        var outputs = this.getOutputConnections(patch.graph);

        return new SplitterResourceHandler<>(
                outputs.stream().map(output -> {
                    var foreignPort = patch.graph.nodeDescriptors.get(output.to()).getPort(output.keyTo());
                    var foreignNode = patch.nodes.get(output.to());

                    return foreignNode.getItemHandler(level, foreignPort, patch);
                }).filter(Objects::nonNull).toList(),
                ItemResource.EMPTY
        );
    }

    @Override
    public Identifier getIdentifier() {
        return null;
    }

    @Override
    public void acceptConfiguration(String string) {

    }
}
