package party.stoat.patchwork.client.screen.components;

import java.util.ArrayList;
import java.util.List;

public class VerticalList<T extends Renderable> extends Renderable {

    public List<T> elements;
    public int padding;

    public int width = 0;
    public int height = 0;

    boolean rightAlign;
    boolean centerContents;

    public VerticalList(List<T> elements, int padding, boolean rightAlign, boolean centerContents) {
        this.elements = elements;
        this.padding = padding;
        this.rightAlign = rightAlign;
        this.centerContents = centerContents;
    }

    @Override
    public Layout extractInnerLayout(int dX, int dY) {

        int maxWidth = this.width;
        int listY = 0;

        var c = new ArrayList<Layout>();

        for (var e : this.elements) {
            var childLayout = e.extractLayout(dX, dY + listY);
            listY += childLayout.height() + this.padding;

            maxWidth = Math.max(maxWidth, childLayout.width());

            c.add(childLayout);
        }

        if (this.rightAlign) {
            listY = 0;

            var newLayouts = new ArrayList<Layout>();

            for (int i = 0; i < c.size(); i++) {
                var layout = c.get(i);
                var element = this.elements.get(i);

                var childLayout = element.extractLayout(dX + (maxWidth - layout.width()), dY + listY);
                listY += childLayout.height() + this.padding;

                newLayouts.add(childLayout);
            }

            c = newLayouts;
        } else if(this.centerContents) {
            listY = 0;

            var newLayouts = new ArrayList<Layout>();

            for (int i = 0; i < c.size(); i++) {
                var layout = c.get(i);
                var element = this.elements.get(i);

                var childLayout = element.extractLayout(dX + (maxWidth - layout.width()) / 2, dY + listY);
                listY += childLayout.height() + this.padding;

                newLayouts.add(childLayout);
            }

            c = newLayouts;
        }

        return new Layout(dX, dY, Math.max(maxWidth, this.width), Math.max(listY - padding, this.height), this, c, false);
    }

}
