package com.example.sandevistan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Замедление времени вокруг игрока с эффектом Sandevistan.
 * Живые существа (не игроки) пропускают (SLOW_FACTOR-1) из SLOW_FACTOR тиков,
 * снаряды/предметы/падающие блоки откатываются на позицию/скорость начала тика,
 * остальные игроки получают Slowness + Mining Fatigue.
 */
@Mod.EventBusSubscriber(modid = SandevistanMod.MODID)
public class TimeSlowHandler {

    private static class Snap {
        final Entity e; final Vec3 pos; final Vec3 motion;
        Snap(Entity e) { this.e = e; this.pos = e.position(); this.motion = e.getDeltaMovement(); }
    }

    private static final Map<ResourceKey<Level>, Set<Integer>> SLOWED_LIVING = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<Snap>> SNAPS = new HashMap<>();

    private static boolean isSource(Entity e) {
        return e instanceof Player p && p.hasEffect(ModEffects.SANDEVISTAN.get());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.side.isClient() || !(event.level instanceof ServerLevel level)) return;
        ResourceKey<Level> key = level.dimension();

        if (event.phase == TickEvent.Phase.START) {
            Set<Integer> living = SLOWED_LIVING.computeIfAbsent(key, k -> new HashSet<>());
            living.clear();
            List<Snap> snaps = SNAPS.computeIfAbsent(key, k -> new ArrayList<>());
            snaps.clear();

            boolean frozenTick = level.getGameTime() % SandevistanMod.SLOW_FACTOR != 0;
            double r2 = SandevistanMod.RADIUS * SandevistanMod.RADIUS;

            for (ServerPlayer src : level.players()) {
                if (!isSource(src)) continue;
                AABB box = src.getBoundingBox().inflate(SandevistanMod.RADIUS);
                for (Entity e : level.getEntities(src, box)) {
                    if (e.distanceToSqr(src) > r2 || isSource(e)) continue;

                    if (e instanceof Player p) {
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 3, true, false, false));
                        p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10, 2, true, false, false));
                    } else if (e instanceof LivingEntity) {
                        if (frozenTick) living.add(e.getId());
                    } else if (frozenTick && (e instanceof Projectile || e instanceof ItemEntity
                            || e instanceof FallingBlockEntity)) {
                        snaps.add(new Snap(e));
                    }
                }
            }
        } else {
            List<Snap> snaps = SNAPS.get(key);
            if (snaps == null) return;
            for (Snap s : snaps) {
                if (!s.e.isRemoved()) {
                    s.e.setPos(s.pos.x, s.pos.y, s.pos.z);
                    s.e.setDeltaMovement(s.motion);
                }
            }
            snaps.clear();
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) return;
        Set<Integer> set = SLOWED_LIVING.get(e.level().dimension());
        if (set != null && set.contains(e.getId())) {
            event.setCanceled(true);
        }
    }
}
