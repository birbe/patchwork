package party.stoat.patchwork.client.screen.components;

import net.minecraft.core.Direction;
import party.stoat.patchwork.client.screen.EditorScreen;
import party.stoat.patchwork.patchgraph.NodeDescriptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NodeIOConfiguring extends Renderable {

    TextInput display;
    public NodeIOPort port;

    boolean rightAlign = false;
    boolean disabled = false;

    public NodeDescriptor.DataType type;

    public Button typeSelector;
    public Button directionSelector;
    public Button remove;

    public int directionOrdinal;
    public Direction direction;

    public NodeIOConfiguring(String display, String key, UUID owner, boolean rightAlign, boolean disabled, NodeDescriptor.DataType type, Optional<Direction> direction, AbstractButton.ButtonHandler handler) {
        this.port = new NodeIOPort(key, owner, type);
        this.display = new TextInput(display, 100, EditorScreen.FONT.lineHeight);
        this.rightAlign = rightAlign;
        this.disabled = disabled;
        this.type = type;
        this.direction = direction.orElse(null);
        this.directionOrdinal = direction.map(Direction::ordinal).orElse(6);

        this.typeSelector = new Button(this.type.name().substring(0, 1), 30, 10, (btn, state) -> {
            this.type = NodeDescriptor.DataType.values()[(this.type.ordinal() + 1) % NodeDescriptor.DataType.values().length];
            this.typeSelector.text.content = this.type.name().substring(0, 1);
        });

        this.remove = new Button("-", 30, 10, handler);
        this.remove.backgroundColor = 0xffff0000;

        this.directionSelector = new Button(this.direction != null ? this.direction.name().substring(0, 1) : "-", 20, 10, (btn, state) -> {
            this.directionOrdinal++;
            if(this.directionOrdinal > Direction.values().length + 1) {
                this.directionOrdinal = 0;
            }
            this.directionSelector.text.content = this.directionOrdinal < 6 ? Direction.values()[this.directionOrdinal].getName().substring(0, 1).toUpperCase() : "-";
            this.direction = this.directionOrdinal < 6 ? Direction.values()[this.directionOrdinal] : null;
        });
    }

    @Override
    protected Layout extractInnerLayout(int x, int y) {
        Layout displayLayout;
        Layout dirLayout;
        Layout removeLayout;

        int w = this.display.editBox.getInnerWidth() + 4 + this.typeSelector.width + this.directionSelector.width + this.remove.width;

        dirLayout = this.directionSelector.extractLayout(x + this.typeSelector.width, y);
        removeLayout = this.remove.extractLayout(x + this.typeSelector.width + this.directionSelector.width, y);
        displayLayout = display.extractLayout(x + this.typeSelector.width + this.directionSelector.width + this.remove.width, y);

        var l = this.typeSelector.extractLayout(x, y);

        return new Layout(x, y, 180, EditorScreen.FONT.lineHeight, this, List.of(displayLayout, l, dirLayout, removeLayout, removeLayout), this.disabled);
    }

}
