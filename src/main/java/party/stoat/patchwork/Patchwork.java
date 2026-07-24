package party.stoat.patchwork;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.kneelawk.graphlib.api.graph.GraphUniverse;
import com.kneelawk.graphlib.api.util.NodePos;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.jfr.event.ChunkGenerationEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import party.stoat.patchwork.block.*;
import party.stoat.patchwork.block.sf_controller.SFControllerBlockEntity;
import party.stoat.patchwork.block.sf_controller.SFControllerMenu;
import party.stoat.patchwork.client.screen.EditorScreen;
import party.stoat.patchwork.item.VirtualStorageItem;
import party.stoat.patchwork.patchgraph.PatchInstance;
import party.stoat.patchwork.patchgraph.StorageConfiguration;
import party.stoat.patchwork.patchgraph.nodes.VirtualizedBlockNode;
import party.stoat.patchwork.patchgraph.PatchGraph;
import party.stoat.patchwork.graphlib.SFCableNode;
import party.stoat.patchwork.graphlib.SFControllerNode;
import party.stoat.patchwork.graphlib.SFDriveNode;
import party.stoat.patchwork.graphlib.SFInterfaceNode;
import party.stoat.patchwork.virtual.*;
import party.stoat.patchwork.network.*;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Patchwork.MOD_ID)
public class Patchwork {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "patchwork";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "patchwork" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    // Create a Deferred Register to hold Items which will all be registered under the "patchwork" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "patchwork" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Patchwork.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_REGISTER = DeferredRegister.create(Registries.MENU, Patchwork.MOD_ID);
    public static final Supplier<MenuType<SFControllerMenu>> CONTROLLER_MENU_TYPE = MENU_REGISTER.register("sf_controller",
            () -> new MenuType<>(SFControllerMenu::new, FeatureFlags.DEFAULT_FLAGS)
        );

    public static final BlockCapability<EnergyHandler, Void> SF_CONTROLLER_ENERGY_CAPABILITY =
            BlockCapability.createVoid(Identifier.fromNamespaceAndPath(MOD_ID, "energy_handler"), EnergyHandler.class);

    public static final GraphUniverse UNIVERSE = GraphUniverse.builder().build(Identifier.fromNamespaceAndPath(MOD_ID, "graph_universe"));

    public static final VirtualManager VIRTUAL_MANAGER = new VirtualManager();

    // Creates a new food item with the id "patchwork:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> SUPERCONDUCTING_INGOT = ITEMS.registerSimpleItem("superconducting_ingot", p -> p);
    public static final DeferredItem<Item> SUPERCONDUCTING_DUST = ITEMS.registerSimpleItem("superconducting_dust", p -> p);

    public static final DeferredItem<Item> MEDIATION_CORE = ITEMS.registerSimpleItem("mediation_core", p -> p);
    public static final DeferredItem<Item> NEGOTIATION_CORE = ITEMS.registerSimpleItem("negotiation_core", p -> p);

    public static final DeferredItem<Item> T1_VIRTUAL_STORAGE = ITEMS.registerItem("t1_virtual_storage", props -> new VirtualStorageItem(props, 1, 4));
    public static final DeferredItem<Item> T2_VIRTUAL_STORAGE = ITEMS.registerItem("t2_virtual_storage", props -> new VirtualStorageItem(props, 2, 16));
    public static final DeferredItem<Item> T3_VIRTUAL_STORAGE = ITEMS.registerItem("t3_virtual_storage", p -> new VirtualStorageItem(p.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true), 8, 64));

    public static final DeferredItem<Item> T1_STORAGE_CELL = ITEMS.registerSimpleItem("t1_storage_cell", p -> p);
    public static final DeferredItem<Item> T2_STORAGE_CELL = ITEMS.registerSimpleItem("t2_storage_cell", p -> p);
    public static final DeferredItem<Item> T3_STORAGE_CELL = ITEMS.registerSimpleItem("t3_storage_cell", p -> p);

    public static final DeferredItem<Item> STORAGE_HOUSING = ITEMS.registerSimpleItem("storage_housing", p -> p);

    // Creates a creative tab with the id "patchwork:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PATCHWORK_TAB = CREATIVE_MODE_TABS.register("patchwork", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.patchwork")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> MyBlocks.SF_CONTROLLER_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(MyBlocks.SF_CONTROLLER_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(MyBlocks.SF_CABLE_ITEM.get());
                output.accept(MyBlocks.SF_TERMINAL_ITEM.get());
                output.accept(MyBlocks.SF_INTERFACE_ITEM.get());
                output.accept(SUPERCONDUCTING_INGOT.get());
                output.accept(SUPERCONDUCTING_DUST.get());
                output.accept(NEGOTIATION_CORE.get());
                output.accept(MEDIATION_CORE.get());
                output.accept(T1_VIRTUAL_STORAGE);
                output.accept(T2_VIRTUAL_STORAGE);
                output.accept(T3_VIRTUAL_STORAGE);
                output.accept(T1_STORAGE_CELL);
                output.accept(T2_STORAGE_CELL);
                output.accept(T3_STORAGE_CELL);
                output.accept(STORAGE_HOUSING);
                output.accept(MyBlocks.SF_DRIVE_ITEM);
            }).build());

    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);



    // No data will be synced across the network
    public static final Supplier<DataComponentType<SFStorageDriveData>> STORAGE_MODULE_DATA_COMPONENT = DATA_COMPONENTS.registerComponentType(
            "no_network",
            builder -> builder
                    .persistent(SFStorageDriveData.CODEC)
    );

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Patchwork(IEventBus modEventBus, ModContainer modContainer) {
        MyBlocks.initialize();

        UNIVERSE.register();

        UNIVERSE.addNodeType(SFCableNode.TYPE);
        UNIVERSE.addNodeType(SFControllerNode.TYPE);
        UNIVERSE.addNodeType(SFInterfaceNode.TYPE);
        UNIVERSE.addNodeType(SFDriveNode.TYPE);

        UNIVERSE.addDiscoverer(
                (world, pos) -> world.getBlockState(pos).getBlock() instanceof SFNetworkConnectable connectable ? connectable.createNodes() : List.of()
        );

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::serverSetup);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::registerCapabilities);

        DATA_COMPONENTS.register(modEventBus);
        MENU_REGISTER.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Patchwork) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static boolean isPosTotallyEffedTheFuckUp(double x, double z) {
        return Math.abs(x) > 29999980.0D || Math.abs(z) > 29999980.0D;
    }

    public static boolean isPosTotallyEffedTheFuckUp(int x, int z) {
        return Math.abs(x) > 29999980 || Math.abs(z) > 29999980;
    }

    static class Graphs {
        public HashMap<UUID, PatchGraph> graphs;
    }

    @SubscribeEvent
    private void handleVirtualDroppedItem(EntityJoinLevelEvent event) {
        if(event.getLevel() instanceof ServerLevel serverLevel && ((LevelVirtualDrops) serverLevel).patchwork$get() && event.getEntity() instanceof ItemEntity ie) {
            ((LevelVirtualDrops) serverLevel).patchwork$addDrop(ie);
            event.setCanceled(true);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToServer(
                EjectVirtualizedMachineServerboundPayload.TYPE,
                EjectVirtualizedMachineServerboundPayload.CODEC,
                (payload, context) -> {
                    if(context.player().level().getBlockEntity(payload.controllerPos()) instanceof SFControllerBlockEntity controller && context.player() instanceof ServerPlayer serverPlayer) {
                        ServerLevel level = serverPlayer.level();
                        var graph = Patchwork.UNIVERSE.getGraphWorld(serverPlayer.level()).getGraphForNode(new NodePos(payload.controllerPos(), SFControllerNode.INSTANCE));
                        var configs = StorageConfiguration.getConfigurationsFromNetwork(graph);

                        for(var config : configs) {
                            if(config.virtualized.contains(payload.virtualPos())) {
                                config.virtualized.remove(payload.virtualPos());

                                ((LevelVirtualDrops) serverPlayer.level()).patchwork$set(true);

                                BlockState bs = level.getBlockState(payload.virtualPos());
                                BlockEntity be = level.getBlockEntity(payload.virtualPos());

                                if(be != null) be.preRemoveSideEffects(payload.virtualPos(), bs);
                                Block.dropResources(bs, (ServerLevel) level, payload.virtualPos(), be);

                                level.setBlockAndUpdate(payload.virtualPos(), Blocks.AIR.defaultBlockState());

//                                level.setBlock(payload.virtualPos(), Blocks.AIR.defaultBlockState(), 0);

                                ((LevelVirtualDrops) serverPlayer.level()).patchwork$set(false);
                                var drops = ((LevelVirtualDrops) serverPlayer.level()).patchwork$getDrops();


                                for(var itemEntity : drops) {
                                    level.addFreshEntity(new ItemEntity(level, context.player().getX(), context.player().getY(), context.player().getZ(), itemEntity.getItem()));
                                }

                                level.getServer().getDataStorage().computeIfAbsent(ServerSavedData.ID).setDirty();

                                break;
                            }
                        }


                    }
                }
        );

        registrar.playToClient(
                CacheBlockStateClientboundPayload.TYPE,
                CacheBlockStateClientboundPayload.CODEC,
                (payload, context) -> {
                    var cache = ((LevelVirtualBlockCache) context.player().level());

                    cache.patchwork$cacheBlock(payload.pos(), payload.state(), payload.tag());
                }
        );
        registrar.playToClient(
                OpenPatchControllerScreenClientboundPayload.TYPE, OpenPatchControllerScreenClientboundPayload.CODEC, (payload, context) -> {

                }
        );
        registrar.playToClient(
                SFControllerSyncClientboundPayload.TYPE, SFControllerSyncClientboundPayload.CODEC, (payload, context) -> {
                    var mc = Minecraft.getInstance();

                    if(mc.screen instanceof EditorScreen editor) {
                        UUID currentGraphId = null;
                        HashMap<UUID, Vec2> oldPositions = null;

                        if(editor.state.getCurrentGraph() != null) {
                            currentGraphId = editor.state.getCurrentGraph().graphId;
                            oldPositions = editor.state.getCurrentGraph().nodePositions;
                        }

                        editor.state.controllerPos = payload.controllerPos();
                        editor.state.patchGraphs = new ArrayList<>(payload.patches());
                        editor.state.serverProvidedDescriptors = new ArrayList<>(payload.nodeDescriptors());

                        if(editor.state.getCurrentGraph() == null && !editor.state.patchGraphs.isEmpty()) editor.setGraph(editor.state.patchGraphs.get(0));

                        if(currentGraphId != null) {
                            for(var graph : editor.state.patchGraphs) if(graph.graphId.equals(currentGraphId)) editor.setGraph(graph);
                        }

                        if(oldPositions != null && editor.state.getCurrentGraph() != null) {
                            editor.state.getCurrentGraph().nodePositions.putAll(oldPositions);
                        }

                        editor.state.editorDirty = false;
                        editor.refresh(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                    }
                }
        );
        registrar.playToServer(
                OpenRemoteMachineServerboundPayload.TYPE, OpenRemoteMachineServerboundPayload.CODEC, (payload, context) -> {
                    if(context.player().level().getBlockEntity(payload.pos()) instanceof SFControllerBlockEntity e && context.player() instanceof ServerPlayer serverPlayer) {
                        ServerLevel level = serverPlayer.level();
                        var graph = Patchwork.UNIVERSE.getGraphWorld(serverPlayer.level()).getGraphForNode(new NodePos(payload.pos(), SFControllerNode.INSTANCE));
                        var configs = StorageConfiguration.getConfigurationsFromNetwork(graph);

                        outer: for(var config : configs) for(var patch : config.instances.values()) {
                            if(patch.nodes.containsKey(payload.node())) {
                                var node = patch.nodes.get(payload.node());

                                if(node instanceof VirtualizedBlockNode containerNode) {
                                    context.player().closeContainer();

                                    var blockState = level.getBlockState(containerNode.proxyPos);
                                    var chunk = level.getChunk(containerNode.proxyPos);

                                    ((PlayerVirtualTrackedChunk) serverPlayer).patchwork$setChunk(chunk.getPos());

                                    var blockEntity = level.getBlockEntity(containerNode.proxyPos);

                                    Optional<CompoundTag> nbt = Optional.empty();

                                    if(blockEntity != null) {
                                        nbt = Optional.of(blockEntity.saveWithFullMetadata(context.player().level().registryAccess()));
                                    }

                                    PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new CacheBlockStateClientboundPayload(
                                            blockState,
                                            nbt,
                                            containerNode.proxyPos
                                    ));

                                    blockState.useWithoutItem(level, context.player(), new BlockHitResult(
                                            new Vec3(containerNode.proxyPos.getX(), containerNode.proxyPos.getY(), containerNode.proxyPos.getZ()),
                                            Direction.NORTH,
                                            containerNode.proxyPos,
                                            false
                                    ));

                                    break outer;
                                }
                            }
                        }
                    }
                }
        );
        registrar.playToServer(
                UpdatePatchServerboundPayload.TYPE, UpdatePatchServerboundPayload.CODEC, (payload, context) -> {
                    if(context.player().level().getBlockEntity(payload.pos()) instanceof SFControllerBlockEntity e && context.player().level() instanceof ServerLevel serverLevel) {
                        var newPatch = payload.graph();

                        var graph = Patchwork.UNIVERSE.getGraphWorld(serverLevel).getGraphForNode(new NodePos(payload.pos(), SFControllerNode.INSTANCE));

                        var configs = StorageConfiguration.getConfigurationsFromNetwork(graph);

                        for(var config : configs) {
                            if(config.graphs.stream().anyMatch(g -> g.graphId.equals(payload.id()))) {
                                config.graphs.removeIf(g -> g.graphId.equals(newPatch.graphId));

                                config.graphs.add(newPatch);
                                var instance = PatchInstance.build(newPatch);
                                config.instances.put(newPatch.graphId, instance);
                                instance.initialize(context.player().level().getServer());

                                serverLevel.getServer().getDataStorage().computeIfAbsent(ServerSavedData.ID).setDirty();

                                StorageConfiguration.syncToPlayer(configs, graph, serverLevel, (ServerPlayer) context.player(), payload.pos());
                            }
                        }
                    }
                }
        );
        registrar.playToServer(
                CreatePatchServerboundPayload.TYPE, CreatePatchServerboundPayload.CODEC, (payload, context) -> {
                    if(context.player().level().getBlockEntity(payload.pos()) instanceof SFControllerBlockEntity e) {
                        var graph = Patchwork.UNIVERSE.getGraphWorld((ServerLevel) context.player().level()).getGraphForNode(new NodePos(payload.pos(), SFControllerNode.INSTANCE));
                        var configs = StorageConfiguration.getConfigurationsFromNetwork(graph);

                        var total = configs.stream().mapToInt(c -> c.graphs.size()).sum();

                        for(var config : configs) {
                            if(config.graphs.size() >= config.maxGraphs) continue;

                            var patch = new PatchGraph(UUID.randomUUID());
                            patch.name = "Patch #" + (total + 1);
                            config.graphs.add(patch);
                            e.setChanged();

                            StorageConfiguration.syncToPlayer(configs, graph, (ServerLevel) context.player().level(), (ServerPlayer) context.player(), payload.pos());

                            ((ServerLevel) context.player().level()).getServer().getDataStorage().computeIfAbsent(ServerSavedData.ID).setDirty();

                            break;
                        }
                    }
                }
        );
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                MyBlocks.SF_CONTROLLER_BLOCK_ENTITY.get(),
                (entity, side) -> entity.storage
        );
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(CONTROLLER_MENU_TYPE.get(), EditorScreen::new);
    }

    private void serverSetup(FMLDedicatedServerSetupEvent event) {
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(MyBlocks.SF_CONTROLLER_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
