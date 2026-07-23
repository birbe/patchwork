package party.stoat.patchwork.patchgraph;

import com.google.gson.Gson;
import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.TileEntityChemicalTank;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.tile.factory.TileEntityItemStackChemicalToItemStackFactory;
import mekanism.common.tile.factory.TileEntityItemStackToItemStackFactory;
import mekanism.common.tile.machine.*;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.compat.MekanismConfigurator;
import party.stoat.patchwork.block.SFStorageDriveData;
import party.stoat.patchwork.block.sf_drive.SFDriveBlockEntity;
import party.stoat.patchwork.block.sf_interface.SFInterface;
import party.stoat.patchwork.graphlib.SFDriveNode;
import party.stoat.patchwork.graphlib.SFInterfaceNode;
import party.stoat.patchwork.item.VirtualStorageItem;
import party.stoat.patchwork.network.SFControllerSyncClientboundPayload;
import party.stoat.patchwork.patchgraph.nodes.InterfaceNode;
import party.stoat.patchwork.virtual.ServerSavedData;

import java.util.*;
import java.util.function.Function;

public class StorageConfiguration {

    private static final Identifier INTERFACE_IDENTIFIER = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "interface");
    private static final Identifier VIRTUAL_IDENTIFIER = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "virtual");
    public transient HashMap<UUID, PatchInstance> instances;
    public transient boolean initialized;

    public List<PatchGraph> getGraphs() {
        return graphs;
    }

    public List<PatchGraph> graphs;
    public HashSet<BlockPos> virtualized;

    public int maxVirtualized;
    public int maxGraphs;
    public final UUID uuid;

    public static final Codec<StorageConfiguration> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.list(BlockPos.CODEC).fieldOf("virtualized").forGetter(StorageConfiguration::getVirtualized),
                    Codec.INT.fieldOf("maxVirtualized").forGetter(StorageConfiguration::getMaxVirtualized),
                    Codec.INT.fieldOf("maxGraphs").forGetter(StorageConfiguration::getMaxGraphs),
                    UUIDUtil.CODEC.fieldOf("uuid").forGetter(StorageConfiguration::getUuid),
                    Codec.list(PatchGraph.CODEC).fieldOf("graphs").forGetter(StorageConfiguration::getGraphs)
            ).apply(instance, StorageConfiguration::new)
    );

    public StorageConfiguration(List<BlockPos> virtualized, int maxVirtualized, int maxGraphs, UUID id, List<PatchGraph> graphs) {
        this.instances = new HashMap<>();
        this.virtualized = new HashSet<>(virtualized);
        this.maxVirtualized = maxVirtualized;
        this.maxGraphs = maxGraphs;
        this.uuid = id != null ? id : UUID.randomUUID();
        this.graphs = new ArrayList<>(graphs);
    }

    public StorageConfiguration(UUID uuid, int maxGraphs, int maxVirtualized) {
        this.maxGraphs = maxGraphs;
        this.maxVirtualized = maxVirtualized;
        this.uuid = uuid;
        this.graphs = new ArrayList<>();
        this.virtualized = new HashSet<>();
        this.instances = new HashMap<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getMaxVirtualized() {
        return maxVirtualized;
    }

    public int getMaxGraphs() {
        return maxGraphs;
    }

    public List<BlockPos> getVirtualized() {
        return virtualized.stream().toList();
    }

    public static HashMap<Class<? extends BlockEntity>, BlockConfigurator> configurators = new HashMap<>();
    public static HashMap<Class<? extends BlockEntity>, NodeDescriptorProvider> descriptorProvider = new HashMap<>();
    static HashMap<Class<?>, NodeDescriptorProvider> genericDescriptorProvider = new HashMap<>();

    static {
        if(ModList.get().isLoaded("mekanism")) MekanismConfigurator.init();

        descriptorProvider.put(
                AbstractFurnaceBlockEntity.class,
                (config, block, formatter, _, i) ->
                        new NodeDescriptor(
                                formatter.apply(block.getName().getString()),
                                List.of(
                                        new NodeDescriptor.IO("In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.UP),
                                        new NodeDescriptor.IO("Fuel", "fuelin", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH)
                                ), List.of(
                                new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.DOWN)
                        ),
                                ARGB.color(255, 40, 40, 40),
                                i,
                                BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                                config
                        )
        );

        descriptorProvider.put(
                ChestBlockEntity.class,
                (config, block, formatter, _, i) -> new NodeDescriptor(
                        formatter.apply(block.getName().getString()),
                        List.of(
                                new NodeDescriptor.IO("In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.UP)
                        ),
                        List.of(
                                new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.DOWN)
                        ),
                        ARGB.color(255, 110, 100, 105),
                        i,
                        BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                        config
                )
        );

    }

    public interface BlockConfigurator {

        void apply(BlockPos pos, BlockState state, BlockEntity entity, ServerLevel level, ServerPlayer player);

    }

    public interface NodeDescriptorProvider {

        NodeDescriptor apply(String config, Block state, Function<String, String> formatter, BlockEntity entity, Identifier identifier);

    }

    public void save(ValueOutput output) {
        var virts = output.childrenList("virtualized");

        for (var pos : virtualized) {
            virts.addChild().putLong("pos", pos.asLong());
        }

        output.putString("graphs", new Gson().toJson(this.graphs));
    }

    public NodeDescriptor getDescriptorForBlock(ServerLevel level, BlockPos pos, Function<String, String> formatter, ServerPlayer player, Identifier i, String config) {
        BlockState state = level.getBlockState(pos);
        BlockEntity entity = level.getBlockEntity(pos);

        for (var nodeDescriptorClass : descriptorProvider.keySet()) {
            if (nodeDescriptorClass.isInstance(entity)) {
                return descriptorProvider.get(nodeDescriptorClass).apply(config, state.getBlock(), formatter, entity, i);
            }
        }

        var itemCap = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.NORTH);

        List<NodeDescriptor.IO> inputs = new ArrayList<>();
        List<NodeDescriptor.IO> outputs = new ArrayList<>();
        var energy = level.getCapability(Capabilities.Energy.BLOCK, pos, null);

        if(itemCap != null) {
            inputs.add(new NodeDescriptor.IO("In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH));
            outputs.add(new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH));
        }

        if(energy != null) {
            inputs.add(
                    new NodeDescriptor.IO("Power", "powerin", new NodeDescriptor.Data(NodeDescriptor.DataType.Energy, false), Optional.empty())
            );
        }

        return new NodeDescriptor(
                formatter.apply(state.getBlock().getName().getString()),
                inputs,
                outputs,
                ARGB.color(255, 110, 100, 105),
                i,
                BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(state.getBlock())),
                config
        );
    }

    public record NodeCategory(String name, ArrayList<NodeDescriptor> nodes) {

        public NodeCategory(String name, List<NodeDescriptor> nodes) {
            this(name, new ArrayList<>(nodes));
        }

        public static final Codec<NodeCategory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(NodeCategory::name),
                Codec.list(NodeDescriptor.CODEC).fieldOf("nodes").forGetter(NodeCategory::nodes)
        ).apply(instance, NodeCategory::new));

    }

    public static List<StorageConfiguration> getConfigurationsFromNetwork(BlockGraph graph) {
        List<StorageConfiguration> configs = new ArrayList<>();

        for (var node : graph.getNodes().toList()) {
            if (node.getNode() instanceof SFDriveNode && node.getBlockEntity() instanceof SFDriveBlockEntity drive) {
                for(var item : drive.slots) {
                    if(item.getItem() instanceof VirtualStorageItem vi) {
                        SFStorageDriveData config = item.get(Patchwork.STORAGE_MODULE_DATA_COMPONENT.get());

                        ServerSavedData data = graph.getGraphView().getWorld().getServer().getDataStorage().computeIfAbsent(ServerSavedData.ID);

                        if(config == null || !data.configs.containsKey(config.id())) {
                            config = new SFStorageDriveData(UUID.randomUUID());

                            item.set(
                                    Patchwork.STORAGE_MODULE_DATA_COMPONENT,
                                    config
                            );

                            data.configs.put(config.id(), new StorageConfiguration(config.id(), vi.maxGraphs, vi.maxVirtualized));
                            data.setDirty();
                        }

                        configs.add(data.configs.get(config.id()));
                    }
                }
            }
        }

        return configs;
    }

    public static void syncToPlayer(List<StorageConfiguration> configs, BlockGraph graph, ServerLevel level, ServerPlayer player, BlockPos controllerPos) {
        var descriptors = StorageConfiguration.getNodesFromNetworkResources(configs, graph, level, player);

        PacketDistributor.sendToPlayer(player, new SFControllerSyncClientboundPayload(configs.stream().filter(c -> c.graphs != null).flatMap(c -> c.graphs.stream()).toList(), descriptors, controllerPos));
    }

    public static List<NodeCategory> getNodesFromNetworkResources(List<StorageConfiguration> configs, BlockGraph graph, ServerLevel level, ServerPlayer player) {
        ArrayList<StorageConfiguration.NodeCategory> categories = new ArrayList<>();

        ArrayList<NodeDescriptor> virtualizedCategory = new ArrayList<>();
        ArrayList<NodeDescriptor> interfaceCategory = new ArrayList<>();

        categories.add(new StorageConfiguration.NodeCategory("Interfaces", interfaceCategory));
        categories.add(new StorageConfiguration.NodeCategory("Virtual", virtualizedCategory));

        for(var config : configs) {
            for (var virtualizedPos : config.virtualized) {
                var blockPosConfig = new Gson().toJson(virtualizedPos);
                virtualizedCategory.add(config.configureBlockAndGetDescriptor(level, player, virtualizedPos, s -> s, VIRTUAL_IDENTIFIER, blockPosConfig));
            }

            for (var node : graph.getNodes().toList()) {
                if (node.getNode() instanceof SFInterfaceNode) {
                    var facing = node.getBlockState().getValue(SFInterface.FACING);
                    var proxiedPos = node.getBlockPos().relative(facing);

                    var interfaceConfig = new Gson().toJson(new InterfaceNode.Configuration(node.getBlockPos(), facing));

                    interfaceCategory.add(config.configureBlockAndGetDescriptor(level, player, proxiedPos, s -> "Interface (" + s + ")", INTERFACE_IDENTIFIER, interfaceConfig));
                }
            }
        }

        return categories;
    }

    private NodeDescriptor configureBlockAndGetDescriptor(ServerLevel level, ServerPlayer player, BlockPos proxiedPos, Function<String, String> formatter, Identifier identifier, String config) {
        BlockState state = level.getBlockState(proxiedPos);
        BlockEntity entity = level.getBlockEntity(proxiedPos);

        for (var configuratorClass : configurators.keySet()) {
            if (configuratorClass.isInstance(entity)) {
                configurators.get(configuratorClass).apply(proxiedPos, state, entity, level, player);
                break;
            }
        }

        return getDescriptorForBlock(level, proxiedPos, formatter, player, identifier, config);
    }

    public void initializeIfNeeded(MinecraftServer server) {
        if (this.initialized) return;

        for (var graph : this.graphs) {
            var instance = PatchInstance.build(graph);
            instance.initialize(server);
            this.instances.put(graph.graphId, instance);
        }

        this.initialized = true;
    }

}
