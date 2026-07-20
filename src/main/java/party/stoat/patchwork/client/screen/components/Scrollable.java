package party.stoat.patchwork.client.screen.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import party.stoat.patchwork.client.screen.EditorScreen;

import java.util.List;

import static party.stoat.patchwork.client.screen.components.Dropdown.ease;

public class Scrollable<T extends Renderable> extends Renderable {

    T child;
    ScissorNode scissor;

    int width;
    int height;

    long lastScroll;

    public Scrollable(T child, int width, int height) {
        this.child = child;
        this.scissor = new ScissorNode(this.child, false);

        this.width = width;
        this.height = height;

        this.scissor.width = width - 1;
        this.scissor.height = height;
    }

    @Override
    public void onScroll(double x, double y, double scrollX, double scrollY) {
        this.scissor.innerOffsetY += scrollY * EditorScreen.FONT.lineHeight * 3;
        this.lastScroll = System.currentTimeMillis();
        if(this.child.layoutCache != null) {
            this.scissor.innerOffsetY = Math.clamp(this.scissor.innerOffsetY, -Math.max(0, this.child.layoutCache.height() - this.height), 0);
        }
    }

    @Override
    public void paint(GuiGraphicsExtractor g, Layout l) {
        int maxScroll = -Math.max(0, this.child.layoutCache.height() - this.height);

        float scrollbarValue = (float) ease(((double) Math.clamp(System.currentTimeMillis() - this.lastScroll - 1000, 0, 500)) / 500.0d);
        int col = 255 - (int) (scrollbarValue * 80);

        float scrollProgress = (float) this.scissor.innerOffsetY / (float) maxScroll;
        float visible = (float) this.height / (float) Math.max(this.child.layoutCache.height(), this.height);

        int scrollbarHeight = (int) (visible * this.height);

        int y0 = (int) ((this.height - scrollbarHeight) * scrollProgress);
        int y1 = y0 + scrollbarHeight;

//        int y0 = l.y();
//        int y1 = l.y() + 100;

        g.fill(l.x() + this.width - 1, l.y() + y0, l.x() + this.width, l.y() + y1, ARGB.color(255, col, col, col));
    }

    @Override
    protected Layout extractInnerLayout(int x, int y) {
        var l = this.scissor.extractLayout(x, y);
        return new Layout(l.x(), l.y(), l.x() + this.width, l.y() + this.height, this, List.of(l), false);
    }

}
