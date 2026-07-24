package party.stoat.patchwork.block.sf_controller;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.NonNull;
import party.stoat.patchwork.patchgraph.StorageConfiguration;
import party.stoat.patchwork.patchgraph.nodes.SFSystemPowerNode;

import java.util.ArrayList;
import java.util.List;

public class MultiEnergyHandler implements EnergyHandler {

    public List<EnergyHandler> handlers;

    public MultiEnergyHandler(List<EnergyHandler> handlers) {
        this.handlers = handlers;
    }

    public List<EnergyHandler> getHandlers() {
        return this.handlers;
    }

    @Override
    public long getAmountAsLong() {
        return getHandlers().stream().mapToLong(EnergyHandler::getAmountAsLong).sum();
    }

    @Override
    public long getCapacityAsLong() {
        return getHandlers().stream().mapToLong(EnergyHandler::getCapacityAsLong).sum();
    }

    @Override
    public int insert(int amount, @NonNull TransactionContext transaction) {
        if(getHandlers().isEmpty()) return 0;

        var surplus = 0;

        var per = amount / getHandlers().size();

        var totalInserted = 0;

        for(var handler : getHandlers()) {
            var inserted = handler.insert(per, transaction);
            totalInserted += inserted;
            surplus += per - inserted;
        }

        surplus += amount - totalInserted;

        for(var handler : getHandlers()) {
            var surplusInserted = handler.insert(surplus, transaction);
            surplus -= surplusInserted;
            totalInserted += surplusInserted;
            if(surplus == 0) break;
        }

        return totalInserted;
    }

    @Override
    public int extract(int amount, @NonNull TransactionContext transaction) {
        return 0;
    }
}
