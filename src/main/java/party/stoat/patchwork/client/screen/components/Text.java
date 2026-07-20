package party.stoat.patchwork.client.screen.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import party.stoat.patchwork.client.screen.EditorScreen;

import java.util.List;

public class Text extends Renderable {

    String content;
    int color;

    public Text(String content, int color) {
        this.content = content;
        this.color = color;
    }

    @Override
    protected Layout extractInnerLayout(int x, int y) {
        return new Layout(x, y, EditorScreen.FONT.width(this.content), EditorScreen.FONT.lineHeight, this, List.of(), false);
    }

    @Override
    public void paint(GuiGraphicsExtractor g, Layout l) {
        super.paint(g, l);
        
        g.text(EditorScreen.FONT, this.content, l.x(), l.y(), this.color);
    }
}
