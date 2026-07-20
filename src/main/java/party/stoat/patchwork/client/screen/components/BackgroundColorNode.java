package party.stoat.patchwork.client.screen.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class BackgroundColorNode<T extends Renderable> extends Renderable {
    int color;
    T child;

    public BackgroundColorNode(int color, T child) {
        this.color = color;
        this.child = child;
    }

    @Override
    public void paint(GuiGraphicsExtractor g, Layout l) {
        g.fill(l.x(), l.y(), l.x() + l.width(), l.y() + l.height(), this.color);
    }

    @Override
    public Layout extractInnerLayout(int x, int y) {
        var l = child.extractLayout(x, y);

        return new Layout(l.x(), l.y(), l.width(), l.height(), this, List.of(l), false);
    }
}
