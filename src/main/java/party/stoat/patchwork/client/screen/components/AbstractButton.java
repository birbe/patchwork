package party.stoat.patchwork.client.screen.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import party.stoat.patchwork.client.screen.EditorScreen;

import java.util.List;

public abstract class AbstractButton extends Renderable {
    public int width;
    public int height;

    private final ButtonHandler onClick;

    public boolean highlight = false;

    @FunctionalInterface
    public static interface ButtonHandler {

        public void handle(AbstractButton button, EditorScreen.EditorState state);

    }

    public AbstractButton(int width, int height, ButtonHandler onClick) {
        this.width = width;
        this.height = height;
        this.onClick = onClick;
    }

    @Override
    public boolean onMouseDown(int x, int y, EditorScreen.EditorState state) {
        this.onClick.handle(this, state);

        return true;
    }

    @Override
    public void paint(GuiGraphicsExtractor g, Renderable.Layout l) { }

    @Override
    public Renderable.Layout extractInnerLayout(int x, int y) {
        return new Renderable.Layout(x, y, this.width, this.height, this, List.of(), false);
    }

}
