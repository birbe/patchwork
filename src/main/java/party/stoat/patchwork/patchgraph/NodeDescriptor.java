package party.stoat.patchwork.patchgraph;

import com.google.gson.Gson;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.List;
import java.util.Optional;

public record NodeDescriptor(String title, List<IO> inputs, List<IO> outputs, int color, Identifier identifier,
                             Identifier icon, String configuration) {

    public static NodeDescriptor ofName(String newName, NodeDescriptor other) {
        return new NodeDescriptor(newName, other.inputs, other.outputs, other.color, other.identifier, other.icon, other.configuration);
    }

    public static final Codec<NodeDescriptor> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("title").forGetter(NodeDescriptor::title),
                    IO.CODEC.listOf().fieldOf("inputs").forGetter(NodeDescriptor::inputs),
                    IO.CODEC.listOf().fieldOf("outputs").forGetter(NodeDescriptor::outputs),
                    Codec.INT.fieldOf("color").forGetter(NodeDescriptor::color),
                    Identifier.CODEC.fieldOf("identifier").forGetter(NodeDescriptor::identifier),
                    Identifier.CODEC.optionalFieldOf("icon")
                            .forGetter(node -> Optional.ofNullable(node.icon())),
                    Codec.STRING.fieldOf("configuration").forGetter(NodeDescriptor::configuration)
            ).apply(instance, (title, inputs, outputs, color, identifier, icon, configuration) ->
                    new NodeDescriptor(
                            title,
                            inputs,
                            outputs,
                            color,
                            identifier,
                            icon.orElse(null),
                            configuration
                    )
            )
    );

    public NodeDescriptor(String title, List<IO> inputs, List<IO> outputs, int color, Identifier identifier, String configuration) {
        this(title, inputs, outputs, color, identifier, null, configuration);
    }

    public <D extends NodeConfiguration> NodeDescriptor(String title, List<IO> inputs, List<IO> outputs, int color, Identifier identifier, D configuration) {
        this(title, inputs, outputs, color, identifier, null, new Gson().toJson(configuration));
    }

    public record Data(DataType d, boolean array) {

        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        DataType.CODEC.fieldOf("type").forGetter(Data::d),
                        Codec.BOOL.fieldOf("array").forGetter(Data::array)
                ).apply(instance, Data::new)
        );

    }

    public record IO(String name, String key, Data d, Optional<Direction> direction) {

        public IO(String name, String key, Data d, Direction direction) {
            this(name, key, d, Optional.of(direction));
        }

        public static final Codec<IO> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("name").forGetter(IO::name),
                        Codec.STRING.fieldOf("key").forGetter(IO::key),
                        Data.CODEC.fieldOf("data").forGetter(IO::d),
                        Direction.CODEC.optionalFieldOf("direction").forGetter(IO::direction)
                ).apply(instance, IO::new)
        );

    }

    public IO getPort(String key) {
        for (var output : this.outputs) {
            if (output.key.equals(key)) return output;
        }

        for (var input : this.inputs) {
            if (input.key.equals(key)) return input;
        }

        return null;
    }

    public enum DataType {

        Item(ARGB.color(252, 186, 3)),
        Inventory(ARGB.color(94, 3, 252)),
        Fluid(ARGB.color(187, 242, 250)),
        Energy(ARGB.color(91, 143, 66)),
        Chemical(ARGB.color(175, 113, 76)),
        Any(ARGB.color(255, 255, 255));

        public final int color;

        DataType(int color) {
            this.color = color;
        }

        public static final Codec<DataType> CODEC = Codec.STRING.xmap(
                DataType::valueOf,
                DataType::name
        );
    }

    public boolean isOutput(String key) {
        return this.outputs().stream().anyMatch(io -> io.key.equals(key));
    }

}
