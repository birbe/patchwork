package party.stoat.patchwork.client;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Bezier4 {

    Vec2 c1;
    Vec2 c2;
    Vec2 c3;
    Vec2 c4;

    public Bezier4(Vec2 c1, Vec2 c2, Vec2 c3, Vec2 c4) {
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
        this.c4 = c4;
    }

    static Vec2 lerp(Vec2 a, Vec2 b, float t) {
        return a.scale(t).add(b.scale(1.0f - t));
    }

    public List<Vec2> eval(float delta) {
        List<Vec2> out = new ArrayList<>();

        float t = 0.0f;

        while(t <= 1.0) {
            Vec2 a = lerp(c1, c2, t);
            Vec2 b = lerp(c3, c4, t);
            Vec2 c = lerp(c2, c3, t);
            Vec2 d = lerp(a, c, t);
            Vec2 e = lerp(c, b, t);
            Vec2 f = lerp(d, e, t);

            out.add(f);

            t += delta;
        }

        return out;
    }

}
