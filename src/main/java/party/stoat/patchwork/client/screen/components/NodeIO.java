package party.stoat.patchwork.client.screen.components;

import party.stoat.patchwork.client.screen.EditorScreen;
import party.stoat.patchwork.graph.NodeDescriptor;

import java.util.List;
import java.util.UUID;

public class NodeIO extends Renderable {

    Text display;
    public NodeIOPort port;

    boolean rightAlign = false;
    boolean disabled = false;

    public NodeDescriptor.DataType type;

    public NodeIO(String display, String key, UUID owner, boolean rightAlign, boolean disabled, NodeDescriptor.DataType type) {
        this.port = new NodeIOPort(key, owner, type);
        this.display = new Text(display, 0xffffffff);
        this.rightAlign = rightAlign;
        this.disabled = disabled;
    }

    @Override
    public Layout extractInnerLayout(int x, int y) {
        Layout displayLayout;
        Layout portLayout;

        int w = EditorScreen.FONT.width(this.display.content) + 4;

        if (this.rightAlign) {
            displayLayout = display.extractLayout(x, y);
            portLayout = port.extractLayout(x + w, y + 2);
        } else {
            displayLayout = display.extractLayout(x + 8, y);
            portLayout = port.extractLayout(x, y + 2);
        }

        return new Layout(x, y, w + 4, EditorScreen.FONT.lineHeight, this, List.of(displayLayout, portLayout), this.disabled);
    }

}
