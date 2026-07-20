package party.stoat.patchwork;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.kneelawk.graphlib.api.graph.GraphUniverse;
import com.kneelawk.graphlib.api.util.NodePos;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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
import party.stoat.patchwork.block.ControllerConfiguration;
import party.stoat.patchwork.block.PatchInstance;
import party.stoat.patchwork.block.SFControllerMenu;
import party.stoat.patchwork.block.SFNetworkConnectable;
import party.stoat.patchwork.block.controller.SFControllerBlockEntity;
import party.stoat.patchwork.client.screen.EditorScreen;
import party.stoat.patchwork.graph.ContainerNode;
import party.stoat.patchwork.graph.NodeDescriptor;
import party.stoat.patchwork.graph.PatchGraph;
import party.stoat.patchwork.graphlib.SFCableNode;
import party.stoat.patchwork.graphlib.SFControllerNode;
import party.stoat.patchwork.graphlib.SFInterfaceNode;
import party.stoat.patchwork.network.*;
import party.stoat.patchwork.virtual.VirtualManager;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
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
            BlockCapability.create(
                    // Provide a name to uniquely identify the capability.
                    Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "sf_controller"),
                    // Provide the queried type. Here, we want to look up `ResourceHandler<ItemResource>` instances.
                    EnergyHandler.class,
                    // Provide the context type. We will allow the query to receive an extra `Direction side` parameter.
                    Void.class
            );

    public static final GraphUniverse UNIVERSE = GraphUniverse.builder().build(Identifier.fromNamespaceAndPath(MOD_ID, "graph_universe"));

    public static final VirtualManager VIRTUAL_MANAGER = new VirtualManager();

    // Creates a new food item with the id "patchwork:example_id", nutrition 1 and saturation 2
//    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
//            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Creates a creative tab with the id "patchwork:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.patchwork")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> MyBlocks.SF_CONTROLLER_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(MyBlocks.SF_CONTROLLER_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Patchwork(IEventBus modEventBus, ModContainer modContainer) {
        MyBlocks.initialize();

        UNIVERSE.register();

        UNIVERSE.addNodeType(SFCableNode.TYPE);
        UNIVERSE.addNodeType(SFControllerNode.TYPE);
        UNIVERSE.addNodeType(SFInterfaceNode.TYPE);

        UNIVERSE.addDiscoverer(
                (world, pos) -> world.getBlockState(pos).getBlock() instanceof SFNetworkConnectable connectable ? connectable.createNodes() : List.of()
        );

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::serverSetup);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerPayloads);

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

    static class Graphs {
        public HashMap<UUID, PatchGraph> graphs;
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(
                OpenPatchControllerScreenClientboundPayload.TYPE, OpenPatchControllerScreenClientboundPayload.CODEC, (payload, context) -> {

                }
        );
        registrar.playToClient(
                SFControllerSyncClientboundPayload.TYPE, SFControllerSyncClientboundPayload.CODEC, (payload, context) -> {
                    var mc = Minecraft.getInstance();

                    if(mc.gui.screen() instanceof EditorScreen editor) {
                        UUID currentGraphId = null;
                        HashMap<UUID, Vec2> oldPositions = null;

                        if(editor.state.getCurrentGraph() != null) {
                            currentGraphId = editor.state.getCurrentGraph().graphId;
                            oldPositions = editor.state.getCurrentGraph().nodePositions;
                        }

                        editor.state.controllerPos = payload.controllerPos();
                        Type type = new TypeToken<List<PatchGraph>>() {}.getType();

                        editor.state.patchGraphs = new Gson().fromJson(payload.patches(), type);
                        editor.state.serverProvidedDescriptors = new Gson().fromJson(payload.nodeDescriptors(), new com.google.gson.reflect.TypeToken<List<NodeDescriptor>>() {}.getType());

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
                    if(context.player().level().getBlockEntity(payload.pos()) instanceof SFControllerBlockEntity e) {
                        var node = e.config.instances.get(payload.patch()).nodes.get(payload.node());
                        if(node != null && node instanceof ContainerNode containerNode) {
                            var machineLevel = context.player().level().getServer().getLevel(MyBlocks.MACHINE_LEVEL);
                            context.player().closeContainer();

                            var blockState = machineLevel.getBlockState(containerNode.proxyPos);
                            blockState.useWithoutItem(machineLevel, context.player(), new BlockHitResult(
                                    new Vec3(containerNode.proxyPos.getX(), containerNode.proxyPos.getY(), containerNode.proxyPos.getZ()),
                                    Direction.NORTH,
                                    containerNode.proxyPos,
                                    false
                            ));
                        }
                    }
                }
        );
        registrar.playToServer(
                UpdatePatchServerboundPayload.TYPE, UpdatePatchServerboundPayload.CODEC, (payload, context) -> {
                    if(context.player().level().getBlockEntity(payload.pos()) instanceof SFControllerBlockEntity e) {
                        var newPatch = new Gson().fromJson(payload.graph(), PatchGraph.class);

                        e.config.graphs.removeIf(g -> g.graphId.equals(newPatch.graphId));

                        e.config.graphs.add(newPatch);
                        var instance = PatchInstance.build(newPatch);
                        e.config.instances.put(newPatch.graphId, instance);
                        instance.initialize(context.player().level().getServer());

                        var descriptors = ControllerConfiguration.getNodesFromNetworkResources(Patchwork.UNIVERSE.getGraphWorld((ServerLevel) context.player().level()).getGraphForNode(
                                new NodePos(payload.pos(), SFControllerNode.INSTANCE)
                        ));

                        PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new SFControllerSyncClientboundPayload(new Gson().toJson(e.config.graphs), new Gson().toJson(descriptors), payload.pos()));
                    }
                }
        );
        registrar.playToServer(
                CreatePatchServerboundPayload.TYPE, CreatePatchServerboundPayload.CODEC, (payload, context) -> {
                    if(context.player().level().getBlockEntity(payload.pos()) instanceof SFControllerBlockEntity e) {
                        var patch = new PatchGraph(UUID.randomUUID());
                        patch.name = "Patch #" + e.config.graphs.size();
                        e.config.graphs.add(patch);
                        e.setChanged();

                        var descriptors = ControllerConfiguration.getNodesFromNetworkResources(Patchwork.UNIVERSE.getGraphWorld((ServerLevel) context.player().level()).getGraphForNode(
                                new NodePos(payload.pos(), SFControllerNode.INSTANCE)
                        ));

                        PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new SFControllerSyncClientboundPayload(new Gson().toJson(e.config.graphs), new Gson().toJson(descriptors), payload.pos()));
                    }
                }
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
