package com.example.sandevistan;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SandevistanEffect extends MobEffect {
    public SandevistanEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00E5FF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "5b1e3f1c-6a52-4c53-9d0a-0d6f4f1a7a01",
                0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_SPEED, "5b1e3f1c-6a52-4c53-9d0a-0d6f4f1a7a02",
                0.75, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
