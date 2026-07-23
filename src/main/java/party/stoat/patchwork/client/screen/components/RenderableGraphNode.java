package party.stoat.patchwork.client.screen.components;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.client.screen.EditorScreen;
import party.stoat.patchwork.network.EjectVirtualizedMachineServerboundPayload;
import party.stoat.patchwork.patchgraph.NodeDescriptor;
import party.stoat.patchwork.network.OpenRemoteMachineServerboundPayload;
import party.stoat.patchwork.patchgraph.PatchGraph;

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
    private ImageButton ejectRemote;

    private static final int HEADER_HEIGHT = 20;

    public boolean highlighted = false;

    public HashMap<String, NodeIO> ports = new HashMap<>();

    public boolean hovering;
    public boolean configuringName;

    private Text headerAsText;
    private Many header;
    private TextInput nameInput;
    private PatchGraph graph;

    public boolean hasClicked;
    public long lastClick;

    boolean preview;

    public static final Identifier VIRTUAL_NODE_IDENTIFIER = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "virtual");

    public RenderableGraphNode(NodeDescriptor d, UUID u, boolean preview) {
        this.uuid = u;
        this.descriptor = d;
        this.preview = preview;

        this.headerAsText = new Text(d.title(), 0xffffffff);

        this.nameInput = new TextInput(d.title(), 100, 15);
        this.nameInput.offsetY = -2;

        this.header = new Many(new ArrayList<>(List.of(this.headerAsText)));
        header.offsetX = 5;
        header.offsetY = 5;

        children.add(header);

        var inputs = new VerticalList<NodeIO>(new ArrayList<>(), 4, false, false);
        var outputs = new VerticalList<>(new ArrayList<>(), 4, true, false);

        this.openRemote = new ImageButton(EditorScreen.MAGNIFYING_GLASS_TEXTURE, 16, 16, (btn, state) -> {
            ClientPacketDistributor.sendToServer(new OpenRemoteMachineServerboundPayload(uuid, state.controllerPos));
        });

        this.openRemote.paddingX = 3;
        this.openRemote.paddingY = 3;

        inputs.width = d.inputs().stream().mapToInt(input -> EditorScreen.FONT.width(input.name())).max().orElse(0);
        outputs.width = d.outputs().stream().mapToInt(output -> EditorScreen.FONT.width(output.name())).max().orElse(0);

        inputs.offsetY = HEADER_HEIGHT + 10;
        outputs.offsetY = HEADER_HEIGHT + 10;

        outputs.offsetX = Math.max(EditorScreen.FONT.width(d.title()), inputs.width + outputs.width) + 5;
        outputs.offsetX = Math.max(outputs.offsetX, 80);

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

        if(this.descriptor.identifier().equals(VIRTUAL_NODE_IDENTIFIER) || this.descriptor.identifier().equals(Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "interface"))) {
            outputs.elements.add(this.openRemote);
        }

        this.header.scissor = false;

        if(this.descriptor.identifier().equals(VIRTUAL_NODE_IDENTIFIER)) {
            this.ejectRemote = new ImageButton(EditorScreen.EJECT_TEXTURE, 16, 16, (btn, state) -> {
                BlockPos pos = new Gson().fromJson(this.descriptor.configuration(), new TypeToken<BlockPos>() {}.getType());
                if(pos != null) {
                    ClientPacketDistributor.sendToServer(new EjectVirtualizedMachineServerboundPayload(state.controllerPos, pos));
                }

                state.selectedNodes.removeIf(
                        n -> n.descriptor.configuration().equals(this.descriptor.configuration())
                );

                state.serverProvidedDescriptors.forEach(cat -> cat.nodes().removeIf(
                        node -> node.configuration().equals(this.descriptor.configuration())
                ));

                if(state.getCurrentGraph() != null) {
                    var similarNodes = state.getCurrentGraph().nodeDescriptors.entrySet().stream().filter(entry -> entry.getValue().configuration().equals(this.descriptor.configuration())).toList();

                    for (var similarNode : similarNodes) {
                        state.getCurrentGraph().nodeDescriptors.remove(similarNode.getKey());
                        state.getCurrentGraph().nodePositions.remove(similarNode.getKey());

                        state.graphNodes.elements.removeIf(
                                renderable -> {
                                    var desc = ((RenderableGraphNode) renderable).descriptor;
                                    if(desc == null || desc.configuration() == null) return false;
                                    return desc.configuration().equals(this.descriptor.configuration());
                                }
                        );
                    }
                }

                if(state.getCurrentGraph() != null) state.getCurrentGraph().connections.removeIf(
                        c -> state.getCurrentGraph().nodeDescriptors.get(c.to()).configuration().equals(this.descriptor.configuration()) || state.getCurrentGraph().nodeDescriptors.get(c.from()).configuration().equals(this.descriptor.configuration())
                );
            });
            this.ejectRemote.paddingX = 3;
            this.ejectRemote.paddingY = 3;
            outputs.elements.add(this.ejectRemote);
        }

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

        if(hasClicked) {
            if(System.currentTimeMillis() - lastClick < 500) {
                this.configuringName = true;
            }
            lastClick = System.currentTimeMillis();
        } else hasClicked = true;

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
    public void onMouseDownGlobal(int x, int y, EditorScreen.EditorState state) {
        if(this.layoutCache != null) {
            if(!this.layoutCache.contains(x, y)) {
                this.configuringName = false;
            }
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event, EditorScreen.EditorState state) {
        if(this.configuringName) {
            this.headerAsText.content = this.nameInput.editBox.getValue();
            this.descriptor = NodeDescriptor.ofName(this.nameInput.editBox.getValue(), this.descriptor);
            state.getCurrentGraph().nodeDescriptors.put(this.uuid, this.descriptor);
        }

        return super.charTyped(event, state);
    }

    @Override
    public void onMouseMove(int x, int y, EditorScreen.EditorState state) {
        dragging = mouseDown;

        if (dragging) {
            this.hovering = false;
            if(!state.selectedNodes.contains(this)) {
                if(state.shiftPressed) {
                    state.selectedNodes.add(this);
                    this.highlighted = true;
                } else {
                    state.selectedNodes.forEach(node -> node.highlighted = false);
                    state.selectedNodes.clear();
                    state.selectedNodes.add(this);
                    this.highlighted = true;

                }
            }

            state.selectedNodes.forEach(node -> {
                node.offsetX += x - mX;
                node.offsetY += y - mY;
                state.getCurrentGraph().nodePositions.put(node.uuid, new Vec2(node.offsetX, node.offsetY));
            });

            state.markDirty();
        } else {
            if(this.layoutCache != null) {
                if(!this.configuringName && !this.preview) this.hovering = this.layoutCache.contains(x + this.layoutCache.x(), y + this.layoutCache.y());
            }
        }
    }

    @Override
    public boolean onMouseUp(int x, int y, EditorScreen.EditorState state) {
        if(this.layoutCache.contains(x + this.layoutCache.x(), y + this.layoutCache.y()) && !this.preview) {
            if(!dragging) {
                if (!state.shiftPressed) {
                    state.selectedNodes.forEach(node -> node.highlighted = false);
                    state.selectedNodes.clear();

                    state.selectedNodes.add(this);
                    this.highlighted = true;
                } else {
                    if(!state.selectedNodes.contains(this)) state.selectedNodes.add(this);
                    this.highlighted = true;
                }
            }
        }

        mouseDown = false;
        dragging = false;

        return false;
    }

    @Override
    protected Layout extractInnerLayout(int dX, int dY) {
        int w = 0;
        int h = 0;

        if(!this.preview) {
            if(!this.hovering) this.configuringName = false;
            this.header.elements.set(0, this.configuringName ? this.nameInput : this.headerAsText);
        }

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
