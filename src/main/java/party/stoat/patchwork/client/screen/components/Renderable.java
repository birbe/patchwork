package party.stoat.patchwork.client.screen.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import party.stoat.patchwork.client.screen.EditorScreen;

import java.util.List;

public abstract class Renderable {
    public int offsetX;
    public int offsetY;

    public Layout layoutCache;

    public record Layout(int x, int y, int width, int height, Renderable r, List<Layout> children, boolean disabled, boolean scissor) {

        public Layout(int x, int y, int width, int height, Renderable r, List<Layout> children, boolean disabled) {
            this(x, y, width, height, r, children, disabled, true);
        }

        public void paint(GuiGraphicsExtractor g) {
            if(this.scissor) g.enableScissor(this.x, this.y, this.x + this.width, this.y + this.height);
            this.r.paint(g, this);
            this.children.forEach(c -> c.paint(g));
            if(this.scissor) g.disableScissor();
        }

        public void onKeyDown(int key) {
            this.children.forEach(c -> c.onKeyDown(key));
            this.r.onKeyDown(key);
        }

        public boolean contains(int x, int y) {
            return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height;
        }

        public boolean onMouseDown(int x, int y, EditorScreen.EditorState state) {
            if(this.disabled) return false;

            if (this.contains(x, y) || !this.scissor) {
                for (var c : this.children) {
                    if (c.onMouseDown(x, y, state)) return true;
                }

                return this.r.onMouseDown(x - this.x, y - this.y, state);
            }

            return false;
        }

        public boolean onMouseUp(int x, int y, EditorScreen.EditorState state) {
            if(this.disabled) return false;

            for (var c : this.children) {
                if (c.onMouseUp(x, y, state)) return true;
            }

            return this.r.onMouseUp(x - this.x, y - this.y, state);
        }

        public void onMouseMove(int x, int y, EditorScreen.EditorState state) {
            if(this.disabled) return;

            for (var c : this.children) {
                c.onMouseMove(x, y, state);
            }

            this.r.onMouseMove(x - this.x, y - this.y, state);
        }

        public void onScroll(double x, double y, double scrollX, double scrollY) {
            if(this.disabled) return;

            if (this.contains((int) x, (int) y) || !this.scissor) {
                for (var c : this.children) {
                    c.onScroll(x, y, scrollX, scrollY);
                }

                this.r.onScroll(x - this.x, y - this.y, scrollX, scrollY);
            }
        }
    }

    public boolean onMouseDown(int x, int y, EditorScreen.EditorState state) {
        return false;
    }

    public void onKeyDown(int key) {}

    public void onScroll(double x, double y, double scrollX, double scrollY) {}

    public void onMouseMove(int x, int y, EditorScreen.EditorState state) {}

    public boolean onMouseUp(int x, int y, EditorScreen.EditorState state) {
        return false;
    }

    public void paint(GuiGraphicsExtractor g, Layout l) {
    }

    public Layout extractLayout(int x, int y) {
        this.layoutCache = this.extractInnerLayout(x + this.offsetX, y + this.offsetY);
        return layoutCache;
    }

    protected abstract Layout extractInnerLayout(int x, int y);

}
