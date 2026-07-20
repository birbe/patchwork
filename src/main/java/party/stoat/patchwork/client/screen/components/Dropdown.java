package party.stoat.patchwork.client.screen.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import party.stoat.patchwork.client.screen.EditorScreen;

import java.util.List;

public class Dropdown extends Renderable {

    String titleStr;
    Text title;

    VerticalList<Renderable> list;
    ScissorNode scissor;

    public int width = 150;
    long openedAt = 0;

    DropdownHeader header = this.new DropdownHeader();

    boolean open;

    class DropdownHeader extends Renderable {

        int height = EditorScreen.FONT.lineHeight + 4;

        @Override
        protected Layout extractInnerLayout(int x, int y) {
            return new Layout(x, y, Dropdown.this.width, EditorScreen.FONT.lineHeight + 4, this, List.of(Dropdown.this.title.extractInnerLayout(x + 2, y + 2)), false);
        }

        @Override
        public boolean onMouseDown(int x, int y, EditorScreen.EditorState state) {
            Dropdown.this.open = !Dropdown.this.open;

            if(Dropdown.this.open) {
                Dropdown.this.title.content = "| " + Dropdown.this.titleStr;
            } else {
                Dropdown.this.title.content = "> " + Dropdown.this.titleStr;
            }

            Dropdown.this.openedAt = System.nanoTime() / 1000000;
            return true;
        }

        @Override
        public void paint(GuiGraphicsExtractor g, Layout l) {
            g.fill(l.x(), l.y(), l.x() + l.width(), l.y() + l.height(), ARGB.color(100, 100, 100, 100));
        }
    }

    @Override
    public boolean onMouseDown(int x, int y, EditorScreen.EditorState state) {
        return false;
    }

    public Dropdown(String title, List<Renderable> elements) {
        this.width = 200;

        this.titleStr = title;
        this.title = new Text("> " + title, 0xffffffff);
        this.list = new VerticalList<>(elements, 4, false, true);
        this.list.width = this.width;
        this.scissor = new ScissorNode(this.list, false);
        this.scissor.offsetY = this.header.height + 2;
    }

    public static double ease(double x) {
        return x < 0.5 ? 4.0d * x * x * x : (1.0d - Math.pow(-2.0d * x + 2.0d, 3.0d) / 2.0d);
    }

    @Override
    protected Layout extractInnerLayout(int x, int y) {
        if(!this.open) {
            return this.header.extractLayout(x, y);
        } else {
            var scissorLayout = this.scissor.extractLayout(x, y + this.header.height + 2);
            var listHeight = scissorLayout.height();

            long currentTime = System.nanoTime() / 1000000;
            double d = Math.clamp(((double) (currentTime - this.openedAt)) * 0.005d, 0.0d, 1.0d);
            d = 1.0f - (float) ease(d);

            this.scissor.innerOffsetY = -(int) (d * ((float) listHeight));
            scissorLayout = this.scissor.extractLayout(x, y);

            int partialHeight = (int) (((float) scissorLayout.height()) * (1.0 - d)) + 2;

            return new Layout(x, y, this.width, this.header.height + partialHeight, this, List.of(this.header.extractLayout(x, y), scissorLayout), false);
        }
    }
}
