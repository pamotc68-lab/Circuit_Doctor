package com.example.circuitdoctor;

import java.util.Collections;
import java.util.Set;

public class SimulationResult {

    public final boolean hasBattery;
    public final boolean pathFound;
    public final boolean hasResistor;
    public final boolean hasLed;
    public final double totalResistanceOhms;
    public final double currentAmps;
    public final double ledBrightnessFraction;
    public final boolean overCurrentDanger;
    public final Set<Long> litLedComponentIds;

    private SimulationResult(boolean hasBattery, boolean pathFound, boolean hasResistor, boolean hasLed,
                             double totalResistanceOhms, double currentAmps,
                             double ledBrightnessFraction, boolean overCurrentDanger,
                             Set<Long> litLedComponentIds) {
        this.hasBattery = hasBattery;
        this.pathFound = pathFound;
        this.hasResistor = hasResistor;
        this.hasLed = hasLed;
        this.totalResistanceOhms = totalResistanceOhms;
        this.currentAmps = currentAmps;
        this.ledBrightnessFraction = ledBrightnessFraction;
        this.overCurrentDanger = overCurrentDanger;
        this.litLedComponentIds = litLedComponentIds;
    }

    public static SimulationResult noBattery() {
        return new SimulationResult(false, false, false, false, 0, 0, 0, false, Collections.emptySet());
    }

    public static SimulationResult noPath() {
        return new SimulationResult(true, false, false, false, 0, 0, 0, false, Collections.emptySet());
    }

    public static SimulationResult build(boolean hasResistor, boolean hasLed,
                                         double totalResistanceOhms, double currentAmps,
                                         double brightness, boolean overCurrentDanger,
                                         Set<Long> litLedComponentIds) {
        return new SimulationResult(true, true, hasResistor, hasLed,
                totalResistanceOhms, currentAmps, brightness, overCurrentDanger, litLedComponentIds);
    }

    public boolean isLedLit() {
        return hasLed && ledBrightnessFraction > 0 && !overCurrentDanger;
    }
}