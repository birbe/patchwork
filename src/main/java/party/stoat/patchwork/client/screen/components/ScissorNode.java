package party.stoat.patchwork.client.screen.components;

import party.stoat.patchwork.client.screen.EditorScreen;

import java.util.List;

public class ScissorNode extends Renderable {

    public int innerOffsetX = 0;
    public int innerOffsetY = 0;

    public int width = 0;
    public int height = 0;

    public boolean draggable = false;
    private int mX = -1;
    private int mY = -1;
    private boolean dragging = false;

    public Renderable child;

    public ScissorNode(Renderable child, boolean draggable) {
        this.child = child;
        this.draggable = draggable;
    }

    @Override
    public boolean onMouseDown(int x, int y, EditorScreen.EditorState state) {
        dragging = this.draggable;

        mX = x - this.innerOffsetX;
        mY = y - this.innerOffsetY;

        return false;
    }

    @Override
    public void onMouseMove(int x, int y, EditorScreen.EditorState state) {
        if(this.dragging) {
            this.innerOffsetX = x - mX;
            this.innerOffsetY = y - mY;
        }
    }

    @Override
    public boolean onMouseUp(int x, int y, EditorScreen.EditorState state) {
        this.dragging = false;
        return false;
    }

    @Override
    protected Layout extractInnerLayout(int x, int y) {
        var childLayout = child.extractLayout(x + innerOffsetX, y + innerOffsetY);

        return new Layout(x, y, width != 0 ? width : childLayout.width(), height != 0 ? height : childLayout.height(), this, List.of(childLayout), false);
    }
}
