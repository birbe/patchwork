package party.stoat.patchwork.compat;

import mekanism.api.IConfigCardAccess;
import mekanism.api.RelativeSide;
import mekanism.api.SerializationConstants;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.TileEntityChemicalTank;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.factory.TileEntityItemStackChemicalToItemStackFactory;
import mekanism.common.tile.factory.TileEntityItemStackToItemStackFactory;
import mekanism.common.tile.interfaces.ITileDirectional;
import mekanism.common.tile.machine.*;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import party.stoat.patchwork.patchgraph.NodeDescriptor;
import party.stoat.patchwork.patchgraph.StorageConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static party.stoat.patchwork.patchgraph.StorageConfiguration.configurators;
import static party.stoat.patchwork.patchgraph.StorageConfiguration.descriptorProvider;

public class MekanismConfigurator implements StorageConfiguration.BlockConfigurator {

    private HashMap<Integer, TypeConfig> configs = new HashMap<>();
    private Function<Function<String, String>, NodeDescriptor> descriptorSupplier;

    public static void init() {
        configurators.put(
                TileEntityChemicalTank.class,
                new MekanismConfigurator()
                        .config(TransmissionType.CHEMICAL)
                        .set(DataType.INPUT, Direction.UP)
                        .set(DataType.OUTPUT, Direction.DOWN)
                        .finish()
        );

        descriptorProvider.put(
                TileEntityChemicalTank.class,
                (config, block, formatter, _, i) -> new NodeDescriptor(
                        formatter.apply(block.getName().getString()),
                        List.of(
                                new NodeDescriptor.IO("In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.UP)
                        ),
                        List.of(
                                new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.DOWN)
                        ),
                        ARGB.color(255, 110, 100, 105),
                        i,
                        BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                        config
                )
        );

        configurators.put(
                TileEntityFluidTank.class,
                new MekanismConfigurator()
                        .config(TransmissionType.FLUID)
                        .set(DataType.INPUT, Direction.UP)
                        .set(DataType.OUTPUT, Direction.DOWN)
                        .finish()
        );

        descriptorProvider.put(
                TileEntityFluidTank.class,
                (config, block, formatter, _, i) -> new NodeDescriptor(
                        formatter.apply(block.getName().getString()),
                        List.of(
                                new NodeDescriptor.IO("In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Fluid, false), Direction.UP)
                        ),
                        List.of(
                                new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Fluid, false), Direction.DOWN)
                        ),
                        ARGB.color(255, 110, 100, 105),
                        i,
                        BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                        config
                )
        );


        configurators.put(
                TileEntityElectrolyticSeparator.class,
                new MekanismConfigurator()
                        .config(TransmissionType.CHEMICAL)
                        .set(DataType.OUTPUT_1, Direction.WEST)
                        .set(DataType.OUTPUT_2, Direction.EAST)
                        .finish()
                        .config(TransmissionType.FLUID)
                        .set(DataType.INPUT, Direction.NORTH)
                        .finish()
                        .config(TransmissionType.ENERGY)
                        .set(DataType.INPUT, Direction.UP)
                        .finish()
        );

        descriptorProvider.put(
                TileEntityElectrolyticSeparator.class,
                (config, block, formatter, _, i) -> new NodeDescriptor(
                        formatter.apply(block.getName().getString()),
                        List.of(
                                new NodeDescriptor.IO("In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Fluid, false), Direction.NORTH),
                                new NodeDescriptor.IO("Power", "powerin", new NodeDescriptor.Data(NodeDescriptor.DataType.Energy, false), Direction.UP)
                        ),
                        List.of(
                                new NodeDescriptor.IO("Left", "left", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.WEST),
                                new NodeDescriptor.IO("Right", "right", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.EAST)
                        ),
                        ARGB.color(255, 110, 100, 105),
                        i,
                        BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                        config
                )
        );

        configurators.put(
                TileEntityChemicalDissolutionChamber.class,
                new MekanismConfigurator()
                        .config(TransmissionType.CHEMICAL)
                        .set(DataType.INPUT, Direction.UP)
                        .set(DataType.OUTPUT, Direction.SOUTH)
                        .finish()
                        .config(TransmissionType.ITEM)
                        .set(DataType.INPUT, Direction.NORTH)
                        .finish()
        );

        descriptorProvider.put(
                TileEntityChemicalDissolutionChamber.class,
                (config, block, formatter, _, i) -> new NodeDescriptor(
                        formatter.apply(block.getName().getString()),
                        List.of(
                                new NodeDescriptor.IO("Item In", "itemin", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH),
                                new NodeDescriptor.IO("Chemical In", "chemin", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.UP),
                                new NodeDescriptor.IO("Power", "powerin", new NodeDescriptor.Data(NodeDescriptor.DataType.Energy, false), Direction.UP)
                        ),
                        List.of(
                                new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.SOUTH)
                        ),
                        ARGB.color(255, 110, 100, 105),
                        i,
                        BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                        config
                )
        );

        StorageConfiguration.BlockConfigurator electricMachineConfigurator = new MekanismConfigurator()
                .config(TransmissionType.ITEM)
                .set(DataType.INPUT, Direction.NORTH)
                .set(DataType.OUTPUT, Direction.SOUTH)
                .finish()
                .config(TransmissionType.ENERGY)
                .set(DataType.INPUT, Direction.UP)
                .finish();

        StorageConfiguration.NodeDescriptorProvider electricMachineDescriptor = (config, block, formatter, _, i) -> new NodeDescriptor(
                formatter.apply(block.getName().getString()),
                List.of(
                        new NodeDescriptor.IO("In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH),
                        new NodeDescriptor.IO("Power", "powerin", new NodeDescriptor.Data(NodeDescriptor.DataType.Energy, false), Direction.UP)
                ),
                List.of(
                        new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.SOUTH)
                ),
                ARGB.color(255, 110, 100, 105),
                i,
                BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                config
        );

        configurators.put(
                TileEntityElectricMachine.class,
                electricMachineConfigurator
        );

        configurators.put(
                TileEntityItemStackToItemStackFactory.class,
                electricMachineConfigurator
        );

        descriptorProvider.put(
                TileEntityElectricMachine.class,
                electricMachineDescriptor
        );

        descriptorProvider.put(
                TileEntityItemStackToItemStackFactory.class,
                electricMachineDescriptor
        );

        configurators.put(
                TileEntityFormulaicAssemblicator.class,
                new MekanismConfigurator()
                        .config(TransmissionType.ITEM)
                        .set(DataType.INPUT, Direction.NORTH)
                        .set(DataType.OUTPUT, Direction.SOUTH)
                        .finish()
                        .config(TransmissionType.ENERGY)
                        .set(DataType.INPUT, Direction.UP)
                        .finish()
        );

        descriptorProvider.put(
                TileEntityFormulaicAssemblicator.class,
                (config, block, formatter, _, i) -> new NodeDescriptor(
                        formatter.apply(block.getName().getString()),
                        List.of(
                                new NodeDescriptor.IO("Ingredients", "ingredients", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.NORTH),
                                new NodeDescriptor.IO("Power", "powerin", new NodeDescriptor.Data(NodeDescriptor.DataType.Energy, false), Direction.UP)
                        ),
                        List.of(
                                new NodeDescriptor.IO("Result", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.SOUTH)
                        ),
                        ARGB.color(255, 110, 100, 105),
                        i,
                        BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                        config
                )
        );

        configurators.put(
                TileEntityChemicalWasher.class,
                new MekanismConfigurator()
                        .config(TransmissionType.CHEMICAL)
                        .set(DataType.INPUT, Direction.EAST)
                        .set(DataType.OUTPUT, Direction.SOUTH)
                        .finish()
                        .config(TransmissionType.FLUID)
                        .set(DataType.INPUT, Direction.WEST)
                        .finish()
                        .config(TransmissionType.ENERGY)
                        .set(DataType.INPUT, Direction.UP)
                        .finish()
        );

        descriptorProvider.put(
                TileEntityChemicalWasher.class,
                (config, block, formatter, _, i) -> new NodeDescriptor(
                        formatter.apply(block.getName().getString()),
                        List.of(
                                new NodeDescriptor.IO("Fluid In", "fluidin", new NodeDescriptor.Data(NodeDescriptor.DataType.Fluid, false), Direction.WEST),
                                new NodeDescriptor.IO("Chemical In", "chemicalin", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.EAST),
                                new NodeDescriptor.IO("Power", "powerin", new NodeDescriptor.Data(NodeDescriptor.DataType.Energy, false), Direction.UP)
                        ),
                        List.of(
                                new NodeDescriptor.IO("Chemical Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.SOUTH)
                        ),
                        ARGB.color(255, 110, 100, 105),
                        i,
                        BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                        config
                )
        );

        configurators.put(
                TileEntityChemicalCrystallizer.class,
                new MekanismConfigurator()
                        .config(TransmissionType.CHEMICAL)
                        .set(DataType.INPUT, Direction.WEST)
                        .finish()
                        .config(TransmissionType.ITEM)
                        .set(DataType.OUTPUT, Direction.EAST)
                        .finish()
                        .config(TransmissionType.ENERGY)
                        .set(DataType.INPUT, Direction.UP)
                        .finish()
        );

        descriptorProvider.put(
                TileEntityChemicalCrystallizer.class,
                (config, block, formatter, _, i) -> new NodeDescriptor(
                        formatter.apply(block.getName().getString()),
                        List.of(
                                new NodeDescriptor.IO("In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.WEST),
                                new NodeDescriptor.IO("Power", "powerin", new NodeDescriptor.Data(NodeDescriptor.DataType.Energy, false), Direction.UP)
                        ),
                        List.of(
                                new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.EAST)
                        ),
                        ARGB.color(255, 110, 100, 105),
                        i,
                        BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                        config
                )
        );

        StorageConfiguration.BlockConfigurator advancedElectricMachineConfigurator = new MekanismConfigurator()
                .config(TransmissionType.CHEMICAL)
                .set(DataType.INPUT, Direction.EAST)
                .finish()
                .config(TransmissionType.ITEM)
                .set(DataType.INPUT, Direction.WEST)
                .set(DataType.OUTPUT, Direction.SOUTH)
                .finish()
                .config(TransmissionType.ENERGY)
                .set(DataType.INPUT, Direction.UP)
                .finish();

        StorageConfiguration.NodeDescriptorProvider advancedElectricMachineDescriptor = (config, block, formatter, _, i) -> new NodeDescriptor(
                formatter.apply(block.getName().getString()),
                List.of(
                        new NodeDescriptor.IO("Item In", "in", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.WEST),
                        new NodeDescriptor.IO("Chemical In", "chemin", new NodeDescriptor.Data(NodeDescriptor.DataType.Chemical, false), Direction.EAST),
                        new NodeDescriptor.IO("Power", "powerin", new NodeDescriptor.Data(NodeDescriptor.DataType.Energy, false), Direction.UP)
                ),
                List.of(
                        new NodeDescriptor.IO("Out", "out", new NodeDescriptor.Data(NodeDescriptor.DataType.Item, false), Direction.SOUTH)
                ),
                ARGB.color(255, 110, 100, 105),
                i,
                BuiltInRegistries.ITEM.getKey(BlockItem.BY_BLOCK.get(block)),
                config
        );

        configurators.put(
                TileEntityAdvancedElectricMachine.class,
                advancedElectricMachineConfigurator
        );

        configurators.put(
                TileEntityItemStackChemicalToItemStackFactory.class,
                advancedElectricMachineConfigurator
        );

        descriptorProvider.put(
                TileEntityAdvancedElectricMachine.class,
                advancedElectricMachineDescriptor
        );

        descriptorProvider.put(
                TileEntityItemStackChemicalToItemStackFactory.class,
                advancedElectricMachineDescriptor
        );
    }

    public static class TypeConfig {

        HashMap<Integer, Direction[]> sets = new HashMap<>();

        private final MekanismConfigurator parent;
        final int ordinal;

        TypeConfig(MekanismConfigurator parent, int ordinal) {
            this.parent = parent;
            this.ordinal = ordinal;
        }

        public TypeConfig set(DataType dataType, Direction... dirs) {
            this.sets.put(dataType.ordinal(), dirs);
            return this;
        }

        public MekanismConfigurator finish() {
            this.parent.configs.put(this.ordinal, this);
            return this.parent;
        }

    }

    public TypeConfig config(TransmissionType type) {
        return new TypeConfig(this, type.ordinal());
    }

    @Override
    public void apply(BlockPos pos, BlockState state, BlockEntity entity, ServerLevel level, ServerPlayer player) {
        IConfigCardAccess access = level.getCapability(Capabilities.CONFIG_CARD, pos, null);

        if (access != null && entity instanceof ITileDirectional directional) {
            // Get current configuration data
            TagValueOutput valueOutput = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);

            Direction machineFacing = directional.getDirection();

            access.writeConfigurationData(valueOutput, player);

            var currentConfig = valueOutput.buildResult();
            var component_config = currentConfig.getCompoundOrEmpty("component_config");

            for(var configOrdinal : this.configs.keySet()) {
                var sfConfig = this.configs.get(configOrdinal);
                var mekConfig = component_config.getIntArray(SerializationConstants.CONFIG + configOrdinal).orElse(new int[6]);

                for(var transmissionTypeOrdinal : sfConfig.sets.keySet()) {
                    var sides = sfConfig.sets.get(transmissionTypeOrdinal);

                    for(Direction d : sides) {
                        RelativeSide side = RelativeSide.fromDirections(machineFacing, d);
                        mekConfig[side.ordinal()] = transmissionTypeOrdinal;
                    }
                }
            }

            ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), currentConfig);
            access.setConfigurationData(input, player);
        }
    }
}
