package party.stoat.patchwork.graph;

import com.google.gson.Gson;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record NodeDescriptor(String title, List<IO> inputs, List<IO> outputs, int color, Identifier identifier, Identifier icon, String configuration) {

    public NodeDescriptor(String title, List<IO> inputs, List<IO> outputs, int color, Identifier identifier, String configuration) {
        this(title, inputs, outputs, color, identifier, null, configuration);
    }

    public <D extends NodeConfiguration> NodeDescriptor(String title, List<IO> inputs, List<IO> outputs, int color, Identifier identifier, D configuration) {
        this(title, inputs, outputs, color, identifier, null, new Gson().toJson(configuration));
    }

    public record Data(DataType d, boolean array) {}

    public record IO(String name, String key, Data d, Direction direction) {}

    public IO getPort(String key) {
        for(var output : this.outputs) {
            if(output.key.equals(key)) return output;
        }

        for(var input : this.inputs) {
            if(input.key.equals(key)) return input;
        }

        return null;
    }

    public enum DataType {

        Item(ARGB.color(252, 186, 3)),
        Inventory(ARGB.color(94, 3, 252)),
        Fluid(ARGB.color(187, 242, 250)),
        Energy(ARGB.color(0, 252, 10)),
        Any(ARGB.color(255, 255, 255));

        public final int color;

        DataType(int color) {
            this.color = color;
        }
    }

    public boolean isOutput(String key) {
        return this.outputs().stream().anyMatch(io -> io.key.equals(key));
    }

}
