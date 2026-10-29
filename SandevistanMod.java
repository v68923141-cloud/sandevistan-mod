package com.example.sandevistan;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SandevistanMod.MODID)
public class SandevistanMod {
    public static final String MODID = "sandevistan_origin";
    /** Радиус замедления времени, блоков. */
    public static final double RADIUS = 15.0;
    /** Во сколько раз замедлено время (1 тик из N). 4 = 25% скорости. */
    public static final int SLOW_FACTOR = 4;
    /** Сколько тиков можно держать имплант включённым до перегрева (20 тиков = 1 сек). */
    public static final float HEAT_MAX = 600f;
    /** Скорость остывания, когда имплант выключен (тиков нагрева в тик). */
    public static final float COOL_RATE = 1.5f;
    /** Длительность кулдауна после перегрева, тиков. */
    public static final int OVERHEAT_COOLDOWN = 300;
    /** Урон при перегреве (в половинках сердец), не убивает. */
    public static final float OVERHEAT_DAMAGE = 4.0f;

    public SandevistanMod() {
        ModEffects.EFFECTS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
