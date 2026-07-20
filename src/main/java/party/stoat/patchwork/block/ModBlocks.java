package party.stoat.patchwork.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.block.controller.SFController;
import party.stoat.patchwork.block.sf_interface.SFInterface;

import java.util.function.Function;

public class ModBlocks {

    public static final Block SF_CONTROLLER = register(
            "sf_controller",
            SFController::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).lightLevel(
                    state -> state.getValue(SFController.POWERED) ? 12 : 0
            ),
            true
    );

    public static final Block SF_TERMINAL = register(
            "sf_terminal",
            SFTerminal::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).lightLevel(
                    state -> state.getValue(SFTerminal.POWERED) ? 12 : 0
            ),
            true
    );

    public static final Block SF_INTERFACE = register(
            "sf_interface",
            SFInterface::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).sound(SoundType.STONE).dynamicShape(),
            true
    );

    public static final Block SF_CABLE = register(
            "sf_cable",
            SFCable::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).sound(SoundType.STONE).dynamicShape(),
            true
    );

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties, boolean shouldRegisterItem) {
        // Create a registry key for the block
        ResourceKey<Block> blockKey = keyOfBlock(name);
        // Create the block instance
        Block block = blockFactory.apply(properties.setId(blockKey));

        // Sometimes, you may not want to register an item for the block.
        // Eg: if it's a technical block like `minecraft:moving_piston` or `minecraft:end_gateway`
        if (shouldRegisterItem) {
            // Items need to be registered with a different type of registry key, but the ID
            // can be the same.
            ResourceKey<Item> itemKey = keyOfItem(name);

            BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
            Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        }

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static ResourceKey<Block> keyOfBlock(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, name));
    }

    private static ResourceKey<Item> keyOfItem(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, name));
    }

    public static void initialize() {

    }
}