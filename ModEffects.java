package com.example.sandevistan;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SandevistanMod.MODID);

    public static final RegistryObject<MobEffect> SANDEVISTAN =
            EFFECTS.register("sandevistan", SandevistanEffect::new);

    /** Кулдаун после перегрева: блокирует повторное включение. */
    public static final RegistryObject<MobEffect> OVERHEATED =
            EFFECTS.register("overheated", OverheatedEffect::new);
}
