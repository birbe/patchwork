package party.stoat.patchwork;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.Optional;

import static net.minecraft.client.renderer.RenderPipelines.LINES_SNIPPET;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Patchwork.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Patchwork.MOD_ID, value = Dist.CLIENT)
public class PatchworkClient {

    public static RenderPipeline LINE;
    public static VertexFormat POS_COL_FLOAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .addAttribute("t", GpuFormat.R32_FLOAT)
            .build();

    public PatchworkClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        LINE = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("patchwork", "pipelines/line"))
                .withVertexShader(Identifier.fromNamespaceAndPath("patchwork", "line"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("patchwork", "line"))
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withVertexBinding(0, POS_COL_FLOAT)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withDepthStencilState(Optional.empty())
                .withoutStencilTest()
                .build();
    }
}
