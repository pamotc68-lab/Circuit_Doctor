package com.example.circuitdoctor;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class ComponentCatalog {

    public List<ComponentType> getComponents(Context c) {
        List<ComponentType> list = new ArrayList<>();

        list.add(new ComponentType(ComponentType.RESISTOR,
                c.getString(R.string.component_resistor), R.drawable.ic_resistor, "R",
                c.getColor(R.color.accent_orange),
                220.0, 0, 0, 0));

        list.add(new ComponentType(ComponentType.LED,
                c.getString(R.string.component_led), R.drawable.ic_led, "LED",
                c.getColor(R.color.accent_pink),
                0, 0, 2.0, 20.0));

        list.add(new ComponentType(ComponentType.PUSHBUTTON,
                c.getString(R.string.component_pushbutton), R.drawable.ic_button, "SW",
                c.getColor(R.color.accent_purple),
                0, 0, 0, 0));

        list.add(new ComponentType(ComponentType.BATTERY_9V,
                c.getString(R.string.component_battery), R.drawable.ic_battery, "BAT",
                c.getColor(R.color.accent_green),
                0, 9.0, 0, 0));

        list.add(new ComponentType(ComponentType.BREADBOARD,
                c.getString(R.string.component_breadboard), R.drawable.ic_breadboard, "BB",
                c.getColor(R.color.accent_blue),
                0, 0, 0, 0));

        list.add(new ComponentType(ComponentType.SLIDESWITCH,
                c.getString(R.string.component_slideswitch), R.drawable.ic_button, "SSW",
                c.getColor(R.color.accent_purple),
                0, 0, 0, 0));

        list.add(new ComponentType(ComponentType.CAPACITOR,
                c.getString(R.string.component_capacitor), R.drawable.ic_resistor, "C",
                c.getColor(R.color.accent_blue),
                0, 0, 0, 0));

        return list;
    }

    public ComponentType findById(Context c, int typeId) {
        for (ComponentType t : getComponents(c)) {
            if (t.id == typeId) return t;
        }
        return null;
    }
}