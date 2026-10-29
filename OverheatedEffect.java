package com.example.sandevistan;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class OverheatedEffect extends MobEffect {
    public OverheatedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF3300);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "5b1e3f1c-6a52-4c53-9d0a-0d6f4f1a7a03",
                -0.25, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_SPEED, "5b1e3f1c-6a52-4c53-9d0a-0d6f4f1a7a04",
                -0.25, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
