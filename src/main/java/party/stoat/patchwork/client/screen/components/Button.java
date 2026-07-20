package party.stoat.patchwork.client.screen.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import party.stoat.patchwork.client.screen.EditorScreen;

import java.util.List;

public class Button extends AbstractButton {

    public Text text;
    public int width;
    public int height;

    private final ButtonHandler onClick;

    public int backgroundColor = ARGB.color(255, 100, 100, 100);
    public int highlightedColor = ARGB.color(255, 150, 150, 150);

    public boolean highlight = false;

    public Button(String content, int width, int height, ButtonHandler onClick) {
        super(width, height, onClick);

        this.text = new Text(content, 0xffffffff);
        this.width = width;
        this.height = height;
        this.onClick = onClick;
    }

    @Override
    public void paint(GuiGraphicsExtractor g, Layout l) {
        g.fill(l.x(), l.y(), l.x() + l.width(), l.y() + l.height(), this.highlight ? this.highlightedColor : this.backgroundColor);
    }

    @Override
    protected Layout extractInnerLayout(int x, int y) {
        this.text.extractLayout(0, 0);
        var textLayout = this.text.extractLayout(x + (this.width - text.layoutCache.width()) / 2, y + (this.height - text.layoutCache.height()) / 2);

        return new Layout(x, y, this.width, this.height, this, List.of(textLayout), false);
    }
}
