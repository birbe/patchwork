package party.stoat.patchwork.client.screen.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import party.stoat.patchwork.Patchwork;
import party.stoat.patchwork.client.screen.EditorScreen;

import java.util.List;

public class ImageButton extends AbstractButton {

    public Identifier image;
    public Text text;
    public int width;
    public int height;

    public static final Identifier SCHEMATIC_BUTTON = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "textures/gui/schematic_button.png");
    public static final Identifier SCHEMATIC_BUTTON_ACTIVE = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "textures/gui/schematic_button_active.png");
    public static final Identifier SAVE = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "textures/gui/save.png");
    public static final Identifier PLUS = Identifier.fromNamespaceAndPath(Patchwork.MOD_ID, "textures/gui/plus.png");

    public int paddingX = 0;
    public int paddingY = 0;

    public boolean highlight = false;

    public ImageButton(Identifier image, int width, int height, ButtonHandler onClick) {
        super(width, height, onClick);

        this.image = image;
        this.width = width;
        this.height = height;
    }

    @Override
    public void paint(GuiGraphicsExtractor g, Layout l) {
        g.blit(this.image, l.x() + this.paddingX, l.y() + this.paddingY, l.x() + this.width + this.paddingX, l.y() + this.height + this.paddingY, 0.0f, 1.0f, 0.0f, 1.0f);

        if(this.highlight) {
            g.fill(l.x() + this.paddingX + 1, l.y() + this.paddingY + 1, l.x() + this.width + this.paddingX - 1, l.y() + this.height + this.paddingY - 1, 0x22ffffff);
        }
    }

    @Override
    public void onMouseMove(int x, int y, EditorScreen.EditorState state) {
        if(this.layoutCache == null) return;
        this.highlight = x >= 0 && x <= this.layoutCache.width() && y >= 0 && y <= this.layoutCache.height();
    }

    @Override
    protected Layout extractInnerLayout(int x, int y) {
        if(this.text != null) {
            this.text.extractLayout(0, 0);
            var textLayout = this.text.extractLayout(x + (this.width - text.layoutCache.width()) / 2, y + (this.height - text.layoutCache.height()) / 2);
            return new Layout(x, y, this.width + (this.paddingX * 2), this.height + (this.paddingY * 2), this, List.of(textLayout), false);
        }

        return new Layout(x, y, this.width + (this.paddingX * 2), this.height + (this.paddingY * 2), this, List.of(), false);
    }
}
