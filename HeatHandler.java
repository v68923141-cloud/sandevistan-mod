package com.example.sandevistan;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Перегрев, кулдаун, звуки и полоска нагрева в action bar. */
@Mod.EventBusSubscriber(modid = SandevistanMod.MODID)
public class HeatHandler {

    private static final Map<UUID, Float> HEAT = new HashMap<>();

    private static void play(LivingEntity e, SoundEvent s, float vol, float pitch) {
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }

    // ---------- звуки включения/выключения ----------
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) return;
        if (event.getEffectInstance().getEffect() == ModEffects.SANDEVISTAN.get()
                && event.getOldEffectInstance() == null) {
            play(e, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.9f);
            play(e, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.8f, 1.6f);
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() == ModEffects.SANDEVISTAN.get()) {
            play(e, SoundEvents.BEACON_DEACTIVATE, 1.0f, 1.7f);
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) return;
        MobEffect m = event.getEffect();
        if (m == ModEffects.SANDEVISTAN.get()) {
            play(e, SoundEvents.BEACON_DEACTIVATE, 1.0f, 1.7f);
        }
    }

    // ---------- блокировка включения во время кулдауна ----------
    @SubscribeEvent
    public static void onApplicable(MobEffectEvent.Applicable event) {
        LivingEntity e = event.getEntity();
        if (event.getEffectInstance().getEffect() == ModEffects.SANDEVISTAN.get()
                && e.hasEffect(ModEffects.OVERHEATED.get())) {
            event.setResult(Event.Result.DENY);
        }
    }

    // ---------- нагрев ----------
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer p)) return;

        float heat = HEAT.getOrDefault(p.getUUID(), 0f);
        boolean active = p.hasEffect(ModEffects.SANDEVISTAN.get());

        if (active) heat += 1f;
        else heat = Math.max(0f, heat - SandevistanMod.COOL_RATE);

        if (heat >= SandevistanMod.HEAT_MAX) {
            overheat(p);
            heat = 0f;
        } else if (heat <= 0f) {
            HEAT.remove(p.getUUID());
        } else {
            HEAT.put(p.getUUID(), heat);
        }

        if (p.tickCount % 5 == 0 && (heat > 0f || p.hasEffect(ModEffects.OVERHEATED.get()))) {
            showBar(p, heat / SandevistanMod.HEAT_MAX);
        }
        // предупреждение
        if (active && heat > SandevistanMod.HEAT_MAX * 0.8f && p.tickCount % 20 == 0) {
            play(p, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 0.5f);
        }
    }

    private static void showBar(ServerPlayer p, float pct) {
        boolean cooldown = p.hasEffect(ModEffects.OVERHEATED.get());
        int total = 20;
        int filled = Math.round(Math.min(1f, pct) * total);
        ChatFormatting color = cooldown ? ChatFormatting.DARK_RED
                : pct < 0.5f ? ChatFormatting.AQUA
                : pct < 0.8f ? ChatFormatting.YELLOW : ChatFormatting.RED;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) sb.append(i < filled ? '|' : '.');
        Component msg = Component.translatable(cooldown
                        ? "sandevistan_origin.cooldown" : "sandevistan_origin.heat")
                .withStyle(color)
                .append(Component.literal(" [" + sb + "]").withStyle(color));
        p.displayClientMessage(msg, true);
    }

    private static void overheat(ServerPlayer p) {
        p.removeEffect(ModEffects.SANDEVISTAN.get());
        p.addEffect(new MobEffectInstance(ModEffects.OVERHEATED.get(),
                SandevistanMod.OVERHEAT_COOLDOWN, 0, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, false));

        float dmg = Math.min(SandevistanMod.OVERHEAT_DAMAGE, p.getHealth() - 1f);
        if (dmg > 0f) p.hurt(p.damageSources().magic(), dmg);

        play(p, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.8f);
        play(p, SoundEvents.LAVA_EXTINGUISH, 1.0f, 0.8f);
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1.0, p.getZ(),
                    25, 0.3, 0.6, 0.3, 0.02);
            sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.0, p.getZ(),
                    15, 0.3, 0.6, 0.3, 0.03);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HEAT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        HEAT.remove(event.getEntity().getUUID());
    }
}
