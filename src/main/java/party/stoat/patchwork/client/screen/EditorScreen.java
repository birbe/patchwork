package party.stoat.patchwork.client.screen;

import com.google.gson.Gson;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.PatchworkClient;
import party.stoat.patchwork.block.SFControllerMenu;
import party.stoat.patchwork.client.Bezier4;
import party.stoat.patchwork.client.screen.components.*;
import party.stoat.patchwork.graph.NodeDescriptor;
import party.stoat.patchwork.graph.PatchGraph;
import party.stoat.patchwork.network.CreatePatchServerboundPayload;
import party.stoat.patchwork.network.UpdatePatchServerboundPayload;

import java.util.*;

public class EditorScreen extends AbstractContainerScreen<SFControllerMenu> {

    public static Font FONT = Minecraft.getInstance().font;

    private static final Identifier CONTAINER_TEXTURE = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "textures/gui/container/inventory.png");
    public static final Identifier MAGNIFYING_GLASS_TEXTURE = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "textures/gui/magnifying_glass.png");

    private static final int CONTAINER_WIDTH = 175;
    private static final int CONTAINER_HEIGHT = 90;

    private Renderable.Layout lastLayout;
    private Renderable root;
    public EditorState state;

    private GpuTexture toCloseTex;
    private GpuTextureView toCloseView;

    private Renderable rightSidebar;
    private Renderable leftSidebar;
    private ScissorNode canvas;
    private Renderable saveButton = new ImageButton(ImageButton.SAVE, 28, 28, (_, _) -> this.save());

    public EditorScreen(SFControllerMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, CONTAINER_WIDTH, CONTAINER_HEIGHT);
        this.state = new EditorState();
        this.state.menu = menu;

        var window = Minecraft.getInstance().getWindow();
    }

    @Override
    protected void init() {
        super.init();

        this.topPos = this.height - this.imageHeight;
    }

    public static class EditorState {

        @Nullable
        public NodeIOPort draggingFrom;
        public SFControllerMenu menu;

        public BlockPos controllerPos;

        public int currentGraph;

        public boolean editorDirty = false;

        public boolean shiftPressed = false;
        public Many graphNodes = new Many(new ArrayList<>());
        public HashMap<UUID, RenderableGraphNode> graphNodeToRenderableMap = new HashMap<>();

        public List<PatchGraph> patchGraphs = new ArrayList<>();
        public List<RenderableGraphNode> selectedNodes = new ArrayList<>();

        public List<NodeDescriptor> serverProvidedDescriptors = new ArrayList<>();

        public void markDirty() {
            this.editorDirty = true;
        }

        public @Nullable PatchGraph getCurrentGraph() {
            if(this.currentGraph >= this.patchGraphs.size()) this.currentGraph = -1;
            return this.currentGraph != -1 ? this.patchGraphs.get(this.currentGraph) : null;
        }

    }

    abstract static class NodeIcon extends Renderable {
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (this.lastLayout != null) {
            var result = this.lastLayout.onMouseDown((int) event.x(), (int) event.y(), this.state);

            if(!result) {
                state.selectedNodes.forEach(node -> node.highlighted = false);
                state.selectedNodes.clear();
            }
        }

        super.mouseClicked(event, doubleClick);

        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.lastLayout != null) {
            this.lastLayout.onMouseUp((int) event.x(), (int) event.y(), this.state);
        }

        super.mouseReleased(event);

        state.draggingFrom = null;

        return true;
    }

    @Override
    public void mouseMoved(double x, double y) {
        if (this.lastLayout != null) {
            this.lastLayout.onMouseMove((int) x, (int) y, this.state);
        }

        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if(this.lastLayout != null) this.lastLayout.onScroll(x, y, scrollX, scrollY);

        super.mouseScrolled(x, y, scrollX, scrollY);
        return true;
    }

    public void save() {
        if(state.getCurrentGraph() == null) return;
        this.state.editorDirty = false;
        ClientPacketDistributor.sendToServer(new UpdatePatchServerboundPayload(
                state.getCurrentGraph().graphId,
                new Gson().toJson(
                        state.getCurrentGraph()
                ),
                this.state.controllerPos
        ));
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if(event.key() == GLFW.GLFW_KEY_LEFT_SHIFT) state.shiftPressed = false;

        return super.keyReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if(event.hasControlDown() && event.key() == GLFW.GLFW_KEY_S) {
            this.save();
            return true;
        }

        if(event.key() == GLFW.GLFW_KEY_LEFT_SHIFT) state.shiftPressed = true;
        
        if(event.key() == GLFW.GLFW_KEY_DELETE) {
            for(var node : state.selectedNodes) {
                if(state.getCurrentGraph() == null) break;
                this.state.graphNodes.elements.removeIf(renderable -> renderable == node);
                state.getCurrentGraph().nodeDescriptors.remove(node.uuid);
                state.getCurrentGraph().connections.removeIf(
                        conn -> conn.from() == node.uuid || conn.to() == node.uuid
                );
            }
            state.selectedNodes.clear();
        }

        if(this.lastLayout != null) this.lastLayout.onKeyDown(event.key());

        super.keyPressed(event);

        return super.keyPressed(event);
    }

    static class ItemStackNodeIcon extends NodeIcon {

        ItemStack stack;

        public ItemStackNodeIcon(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public void paint(GuiGraphicsExtractor g, Layout l) {
            g.item(stack, l.x(), l.y());
        }

        @Override
        protected Layout extractInnerLayout(int x, int y) {
            return new Layout(x, y, 16, 16, this, List.of(), false);
        }

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void refresh(int width, int height) {
        this.leftSidebar = this.createLeftSidebar();

        if(this.canvas == null || this.rightSidebar == null) this.resize(width, height);

        this.root = new Many(List.of(
            this.canvas,
            this.leftSidebar,
            this.rightSidebar
        ));
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        state.graphNodes.scissor = false;

        var externalResources = new Dropdown("Resources", this.state.serverProvidedDescriptors.stream().map(
                descriptor ->
                        (Renderable) new RenderableGraphNode(
                                descriptor,
                                UUID.randomUUID(),
                                true
                        )
        ).toList());

        var list = new VerticalList<>(List.of(new Text("Nodes", 0xffffffff), externalResources), 4, false, false);
        list.width = 200;

        this.canvas = new ScissorNode(state.graphNodes, true);

        this.canvas.width = minecraft.getWindow().getGuiScaledWidth() - externalResources.width - 200;
        this.canvas.height = minecraft.getWindow().getGuiScaledHeight();
        this.canvas.offsetX = 200;

        var scrollable = new Scrollable<>(list, externalResources.width, minecraft.getWindow().getGuiScaledHeight());

        this.rightSidebar = new BackgroundColorNode<>(ARGB.color(200, 25, 25, 35), scrollable);

        scrollable.offsetX = minecraft.getWindow().getGuiScaledWidth() - list.width;
        scrollable.offsetY = 5;

        saveButton.offsetX = 204;
        saveButton.offsetY = 4;

        this.refresh(width, height);

        this.lastLayout = root.extractLayout(0, 0);
    }

    private Renderable createLeftSidebar() {
        List<Renderable> patchSelectButtons = new ArrayList<>();
        List<Renderable> otherButtons = new ArrayList<>();

        for(var graph : this.state.patchGraphs) {
            var button = new ImageButton(this.state.getCurrentGraph() == graph ? ImageButton.SCHEMATIC_BUTTON_ACTIVE : ImageButton.SCHEMATIC_BUTTON, 150, 25, (btn, _) -> {
                for(var el : patchSelectButtons) {
                    if(el instanceof ImageButton b) b.image = ImageButton.SCHEMATIC_BUTTON;
                }

                ((ImageButton) btn).image = ImageButton.SCHEMATIC_BUTTON_ACTIVE;

                this.setGraph(graph);
            });

            button.text = new Text(graph.name, 0xffffffff);

            patchSelectButtons.add(button);
        }

        otherButtons.add(new ImageButton(ImageButton.PLUS, 24, 24, (_, _) -> {
            ClientPacketDistributor.sendToServer(new CreatePatchServerboundPayload(
                    this.state.controllerPos
            ));
        }));

        var list = new VerticalList<>(patchSelectButtons, 6, false, true);
        list.offsetY = 16;
        list.width = 200;

        var otherButtonsList = new VerticalList<>(otherButtons, 2, false, true);
        list.elements.add(otherButtonsList);

        var scrollable = new Scrollable<>(list, 200, Minecraft.getInstance().getWindow().getGuiScaledHeight());
        return new BackgroundColorNode<>(ARGB.color(200, 25, 25, 35), scrollable);
    }

    public void setGraph(PatchGraph graph) {
        this.state.currentGraph = this.state.patchGraphs.indexOf(graph);
        if(this.state.getCurrentGraph() == null) return;
        this.state.graphNodes.elements.clear();
        this.state.graphNodeToRenderableMap.clear();
        this.state.draggingFrom = null;

        for(var id : graph.nodeDescriptors.keySet()) {
            var descriptor = graph.nodeDescriptors.get(id);
            var node = new RenderableGraphNode(descriptor, id, false);
            var position = graph.nodePositions.get(id);
            if(position == null) {
                graph.nodePositions.put(id, Vec2.ZERO);
                position = Vec2.ZERO;
            }
            node.offsetX = (int) position.x;
            node.offsetY = (int) position.y;
            this.state.graphNodes.elements.add(node);
            this.state.graphNodeToRenderableMap.put(id, node);
        }
    }

    record Line(List<Vec3> points, int color) {}

    private List<Line> getLines(int mouseX, int mouseY) {
        List<Line> out = new ArrayList<>();

        List<Vec2[]> connections = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        if(this.state.draggingFrom != null) {
            var from = new Vec2(this.state.draggingFrom.layoutCache.x(), this.state.draggingFrom.layoutCache.y());
            var to = new Vec2(mouseX - 2, mouseY - 2);

            connections.add(new Vec2[] { from, to });
            colors.add(this.state.draggingFrom.type.color);
        }

        if(state.getCurrentGraph() != null) for(var conn : state.getCurrentGraph().connections) {
            var fromNode = state.graphNodeToRenderableMap.get(conn.from());
            var toNode = state.graphNodeToRenderableMap.get(conn.to());

            var fromPort = fromNode.ports.get(conn.keyFrom()).port;
            var toPort = toNode.ports.get(conn.keyTo()).port;

            if(fromPort.layoutCache == null || toPort.layoutCache == null) continue;

            connections.add(new Vec2[] {
                    new Vec2(fromPort.layoutCache.x(), fromPort.layoutCache.y() + 2),
                    new Vec2(toPort.layoutCache.x(), toPort.layoutCache.y() + 2),
            });
            colors.add(fromPort.type.color);
        }


        for(int c = 0; c < connections.size(); c++) {
            var connection = connections.get(c);
            var color = colors.get(c);

            var c1 = new Vec2(connection[0].x + 2, connection[0].y + 2);
            var c4 = new Vec2(connection[1].x + 2, connection[1].y + 2);

            var h = (c1.x + c4.x) * 0.5f;

            var c2 = c1.add(new Vec2(h, c1.y)).scale(0.5f);
            var c3 = c4.add(new Vec2(h, c4.y)).scale(0.5f);

//            c2 = new Vec2(0.0f, 0.0f);
//            c3 = new Vec2(0.0f, 0.0f);

            var b = new Bezier4(c1, c2, c3, c4);

            var evaluated = b.eval(0.02f);

            var line = new ArrayList<Vec3>();

            float dist = 0.0f;

            for(int i = 0; i < evaluated.size() - 1; i++) {
                var p1 = evaluated.get(i);
                var p2 = evaluated.get(i + 1);

                line.add(new Vec3(p1.x, p1.y, dist));
                dist += (float) Math.sqrt(p1.distanceToSqr(p2));
                line.add(new Vec3(p2.x, p2.y, dist));
            }

            out.add(new Line(line, color));
        }
        return out;
    }

    private void drawEffects(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        var lines = this.getLines(mouseX, mouseY);

        if(this.canvas == null) return;
        if(this.canvas.layoutCache == null) return;

        if(lines.isEmpty()) return;

        var device = RenderSystem.getDevice();
        var encoder = RenderSystem.getDevice().createCommandEncoder();

        var mc = Minecraft.getInstance();
        var target = mc.gameRenderer.mainRenderTarget();

        var tex = device.createTexture("", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING, GpuFormat.RGBA8_UNORM, mc.getWindow().getWidth() * 2, mc.getWindow().getHeight() * 2, 1, 1);
        var view = device.createTextureView(tex);

        var pass = encoder.createRenderPass(() -> "patchwork lines", view, Optional.empty());

        var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        pass.setPipeline(PatchworkClient.LINE);

        var builder = new BufferBuilder(new ByteBufferBuilder(128), PrimitiveTopology.TRIANGLES, PatchworkClient.POS_COL_FLOAT);

        int vertCount = 0;

        for(var points : lines) {
            float hwidth = 0.7f;

            for(int i = 0; i < points.points().size() - 1; i++) {
                Vec3 a3 = points.points().get(i);
                Vec3 b3 = points.points().get(i + 1);

                Vec2 a = new Vec2((float) a3.x, (float) a3.y);
                Vec2 b = new Vec2((float) b3.x, (float) b3.y);

                var dir = b.add(a.negated()).normalized();

                var ortho1 = new Vec2(-dir.y, dir.x).scale(hwidth * 2.0f);
                var ortho2 = new Vec2(dir.y, -dir.x).scale(hwidth * 2.0f);

                var x1 = ortho1.add(a).x;
                var x2 = ortho2.add(a).x;
                var x3 = ortho1.add(b).x;
                var x4 = ortho2.add(b).x;

                var y1 = ortho1.add(a).y;
                var y2 = ortho2.add(a).y;
                var y3 = ortho1.add(b).y;
                var y4 = ortho2.add(b).y;

                int col = (points.color() & 0x00ffffff) | 0xcc000000;

                builder.addVertex(x1, y1, 0.0f).setColor(col).setLineWidth((float) a3.z);
                builder.addVertex(x2, y2, 0.0f).setColor(col).setLineWidth((float) a3.z);
                builder.addVertex(x3, y3, 0.0f).setColor(col).setLineWidth((float) a3.z);
                builder.addVertex(x2, y2, 0.0f).setColor(col).setLineWidth((float) b3.z);
                builder.addVertex(x4, y4, 0.0f).setColor(col).setLineWidth((float) b3.z);
                builder.addVertex(x3, y3, 0.0f).setColor(col).setLineWidth((float) b3.z);

                vertCount += 6;
            }

            for(int i = 0; i < points.points().size() - 1; i++) {
                Vec3 a3 = points.points().get(i);
                Vec3 b3 = points.points().get(i + 1);

                Vec2 a = new Vec2((float) a3.x, (float) a3.y);
                Vec2 b = new Vec2((float) b3.x, (float) b3.y);

                var dir = b.add(a.negated()).normalized();

                var ortho1 = new Vec2(-dir.y, dir.x).scale(hwidth);
                var ortho2 = new Vec2(dir.y, -dir.x).scale(hwidth);

                var x1 = ortho1.add(a).x;
                var x2 = ortho2.add(a).x;
                var x3 = ortho1.add(b).x;
                var x4 = ortho2.add(b).x;

                var y1 = ortho1.add(a).y;
                var y2 = ortho2.add(a).y;
                var y3 = ortho1.add(b).y;
                var y4 = ortho2.add(b).y;

                builder.addVertex(x1, y1, 0.0f).setColor(points.color()).setLineWidth((float) a3.z);
                builder.addVertex(x2, y2, 0.0f).setColor(points.color()).setLineWidth((float) a3.z);
                builder.addVertex(x3, y3, 0.0f).setColor(points.color()).setLineWidth((float) a3.z);
                builder.addVertex(x2, y2, 0.0f).setColor(points.color()).setLineWidth((float) b3.z);
                builder.addVertex(x4, y4, 0.0f).setColor(points.color()).setLineWidth((float) b3.z);
                builder.addVertex(x3, y3, 0.0f).setColor(points.color()).setLineWidth((float) b3.z);

                vertCount += 6;
            }

        }

        try(var m = builder.build()) {
            GpuBuffer buf = device.createBuffer(null, GpuBuffer.USAGE_VERTEX, m.vertexBuffer());

            RenderSystem.bindDefaultUniforms(pass);
            pass.setVertexBuffer(0, buf.slice());
            pass.draw(vertCount, 1, 0, 0);
        }

        pass.close();

        graphics.enableScissor(this.canvas.layoutCache.x(), this.canvas.layoutCache.y(), this.canvas.layoutCache.x() + this.canvas.layoutCache.width(), this.canvas.layoutCache.y() + this.canvas.layoutCache.height());
        graphics.blit(view, sampler, 0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0.0F, 1.0F, 1.0F, 0.0F);
        graphics.disableScissor();

        if(toCloseView != null) toCloseView.close();
        if(toCloseTex != null) toCloseTex.close();

        toCloseTex = tex;
        toCloseView = view;

    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.drawEffects(graphics, mouseX, mouseY);

        int gridSize = 15;

        if(this.canvas != null && this.canvas.layoutCache != null) {
            var cache = this.canvas.layoutCache;

            graphics.enableScissor(cache.x(), cache.y(), cache.x() + cache.width(), cache.y() + cache.height());

            for(int x=0;x<(cache.width() / gridSize) + 1;x++) {
                int renderX = (x * gridSize) + cache.x() + (this.canvas.innerOffsetX % gridSize);
                graphics.verticalLine(renderX, 0, cache.height(), 0x11555555);
            }

            for(int y=0;y<(cache.height() / gridSize) + 1;y++) {
                int renderY = (y * gridSize) + cache.y() + (this.canvas.innerOffsetY % gridSize);
                graphics.horizontalLine(cache.x(), cache.x() + cache.width(), renderY, 0x11555555);
            }

            graphics.disableScissor();
        }

        if(this.lastLayout != null) this.lastLayout.paint(graphics);

        if(this.root != null) {
            if(state.editorDirty) {
                this.lastLayout = new Many(List.of(this.root, saveButton)).extractLayout(0, 0);
            } else this.lastLayout = this.root.extractLayout(0, 0);
        }

        int tl = this.leftPos - 8;
        int tp = this.topPos - 9;
        graphics.blit(CONTAINER_TEXTURE, tl, tp, tl + 256, tp + 256, 0.0F, 1.0F, 0.0F, 1.0F);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        //dont do that
    }
}