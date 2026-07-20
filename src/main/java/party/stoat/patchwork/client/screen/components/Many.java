package party.stoat.patchwork.client.screen.components;

import java.util.ArrayList;
import java.util.List;

public class Many extends Renderable {

    public List<Renderable> elements;

    public boolean scissor = true;

    public Many(List<Renderable> elements) {
        this.elements = elements;
    }

    @Override
    protected Layout extractInnerLayout(int x, int y) {
        int w = 0;
        int h = 0;

        List<Layout> c = new ArrayList<>();

        for (var e : this.elements) {
            var l = e.extractLayout(x, y);

            w = Math.max(w, l.x() + l.width() - x);
            h = Math.max(h, l.y() + l.height() - y);

            c.add(l);
        }

        return new Layout(x, y, w, h, this, c, false, this.scissor);
    }
}
