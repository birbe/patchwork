package party.stoat.patchwork.client.screen.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import party.stoat.patchwork.client.screen.EditorScreen;
import party.stoat.patchwork.graph.NodeDescriptor;
import party.stoat.patchwork.network.OpenRemoteMachineServerboundPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RenderableGraphNode extends Renderable {

    int mX = 0;
    int mY = 0;

    boolean mouseDown = false;
    boolean dragging = false;

    public UUID uuid;
    public NodeDescriptor descriptor;
    List<Renderable> children = new ArrayList<>();

    private ImageButton openRemote;

    private static final int HEADER_HEIGHT = 20;

    public boolean highlighted = false;

    public HashMap<String, NodeIO> ports = new HashMap<>();

    boolean preview;

    public RenderableGraphNode(NodeDescriptor d, UUID u, boolean preview) {
        this.uuid = u;
        this.descriptor = d;
        this.preview = preview;

        var header = new Text(d.title(), 0xffffffff);
        header.offsetX = 5;
        header.offsetY = 5;
        children.add(header);

        this.openRemote = new ImageButton(EditorScreen.MAGNIFYING_GLASS_TEXTURE, 16, 16, (btn, state) -> {
            ClientPacketDistributor.sendToServer(new OpenRemoteMachineServerboundPayload(uuid, state.getCurrentGraph().graphId, state.controllerPos));
        });

        this.openRemote.paddingX = 3;
        this.openRemote.paddingY = 3;

        var inputs = new VerticalList<NodeIO>(new ArrayList<>(), 4, false, false);
        var outputs = new VerticalList<>(new ArrayList<>(), 4, true, false);

        inputs.width = 40;
        outputs.width = 40;

        inputs.offsetY = HEADER_HEIGHT + 10;
        outputs.offsetY = HEADER_HEIGHT + 10;

        outputs.offsetX = Math.max(EditorScreen.FONT.width(d.title()), 130);

        for (var i : d.inputs()) {
            var io = new NodeIO(i.name(), i.key(), u, false, this.preview, i.d().d());
            inputs.elements.add(io);
            ports.put(i.key(), io);
        }

        for (var o : d.outputs()) {
            var io = new NodeIO(o.name(), o.key(), u, true, this.preview, o.d().d());
            outputs.elements.add(io);
            ports.put(o.key(), io);
        }

        outputs.elements.add(this.openRemote);

        children.add(inputs);
        children.add(outputs);
    }

    @Override
    public void paint(GuiGraphicsExtractor g, Layout l) {
        super.paint(g, l);

        var borderFill = this.highlighted ? ARGB.color(255, 200, 200, 200) : ARGB.color(255, 60, 60, 60);

        g.fill(l.x(), l.y(), l.x() + l.width(), l.y() + l.height(), borderFill);
        g.fill(l.x() + 1, l.y() + 1, l.x() + l.width() - 1, l.y() + l.height() - 1, ARGB.color(255, 35, 35, 35));
        g.fill(l.x() + 1, l.y() + 1, l.x() + l.width() - 1, l.y() + HEADER_HEIGHT, this.descriptor.color());

        if(descriptor.icon() != null) {
            Item item = BuiltInRegistries.ITEM.get(descriptor.icon()).get().value();
            g.pose().pushMatrix();

            g.pose().scale(1.25f);
            int scaledX = (int) (((float) l.x() + l.width() - 20) / 1.25f);
            int scaledY = (int) (((float) l.y()) / 1.25f);

            g.item(new ItemStack(item, 1), scaledX, scaledY);
            g.pose().popMatrix();
        }
    }

    @Override
    public boolean onMouseDown(int x, int y, EditorScreen.EditorState state) {
        if(state.getCurrentGraph() == null) return false;

        if(!this.preview) {
            if(!state.selectedNodes.contains(this)) state.selectedNodes.add(this);
            this.highlighted = true;
        }
        
        if(this.preview) {
            var newNode = new RenderableGraphNode(this.descriptor, UUID.randomUUID(), false);

            newNode.highlighted = true;

            newNode.mouseDown = true;
            mX = 50;
            mY = 4;
            state.getCurrentGraph().nodeDescriptors.put(newNode.uuid, newNode.descriptor);

            state.graphNodeToRenderableMap.put(newNode.uuid, newNode);
            state.graphNodes.elements.add(newNode);

            state.selectedNodes.forEach(node -> node.highlighted = false);
            state.selectedNodes.clear();
            state.selectedNodes.add(newNode);

            return false;
        }

        if (y < EditorScreen.FONT.lineHeight * 2) mouseDown = true;
        mX = x;
        mY = y;

        return mouseDown;
    }

    @Override
    public void onMouseMove(int x, int y, EditorScreen.EditorState state) {
        dragging = mouseDown;

        if (dragging) {
            state.selectedNodes.forEach(node -> {
                node.offsetX += x - mX;
                node.offsetY += y - mY;
                state.getCurrentGraph().nodePositions.put(node.uuid, new Vec2(node.offsetX, node.offsetY));
            });

            state.markDirty();
        }
    }

    @Override
    public boolean onMouseUp(int x, int y, EditorScreen.EditorState state) {
        mouseDown = false;
        dragging = false;

        if (!state.shiftPressed) {
            state.selectedNodes.forEach(node -> node.highlighted = node == this);
            state.selectedNodes.removeIf(node -> node != this);
        }

        return false;
    }

    @Override
    protected Layout extractInnerLayout(int dX, int dY) {
        int w = 0;
        int h = 0;

        var childLayouts = new ArrayList<Layout>();

        for (var c : this.children) {
            var childLayout = c.extractLayout(dX, dY);
            childLayouts.add(childLayout);
            w = Math.max(w, childLayout.x() + childLayout.width() - dX);
            h = Math.max(h, childLayout.y() + childLayout.height() - dY);
        }

        return new Layout(dX, dY, w, h, this, childLayouts, false);
    }
}
