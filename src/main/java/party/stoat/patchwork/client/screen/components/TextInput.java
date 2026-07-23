package party.stoat.patchwork.client.screen.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;
import party.stoat.patchwork.client.screen.EditorScreen;

import java.util.List;

public class TextInput extends Renderable {

    public Text text;
    public int width;
    public int height;

    public int backgroundColor = ARGB.color(255, 100, 100, 100);
    public int highlightedColor = ARGB.color(255, 150, 150, 150);

    public final EditBox editBox;

    public boolean highlight = false;

    public TextInput(String content, int width, int height) {
        this.editBox = new EditBox(EditorScreen.FONT, width, height, Component.empty());
        this.text = new Text(content, 0xffffffff);
        this.width = width;
        this.height = height;
    }

    @Override
    public void paint(GuiGraphicsExtractor g, Layout l) {
        this.editBox.setX(l.x());
        this.editBox.setY(l.y());


        g.fill(l.x(), l.y(), l.x() + l.width(), l.y() + l.height(), this.highlight ? this.highlightedColor : this.backgroundColor);
//        this.editBox.setValue("yuuup");
        this.editBox.extractWidgetRenderState(g, 0, 0, 1.0f);
    }

    @Override
    public boolean onMouseDown(int x, int y, EditorScreen.EditorState state) {
        this.editBox.setFocused(true);

        return true;
    }

    @Override
    public void onMouseDownGlobal(int x, int y, EditorScreen.EditorState state) {
        if(this.layoutCache != null) {
            this.editBox.setFocused(this.layoutCache.contains(x, y));
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event, EditorScreen.EditorState state) {
        return this.editBox.charTyped(event);
    }

    @Override
    public void onKeyDown(KeyEvent event) {
//        if(this.highlight) this.editBox.keyPressed(event);
        if(event.key() == GLFW.GLFW_KEY_ENTER) {
            this.editBox.setFocused(false);
        }

        if(this.editBox.isFocused()) {
            this.editBox.keyPressed(event);
        }
    }

    @Override
    public void onKeyUp(KeyEvent event) {
        this.editBox.keyPressed(event);
    }

    @Override
    protected Layout extractInnerLayout(int x, int y) {
        return new Layout(x, y, this.width, this.height, this, List.of(), false);
    }
}
