package com.example.sandevistan.client;

import java.awt.Color;

import com.example.sandevistan.ModEffects;
import com.example.sandevistan.SandevistanMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Экранные эффекты:
 *  - анимация активации (кадры из activation.png: глитч -> голубой позвоночник -> вспышка);
 *  - радужная виньетка и расширенный FOV, пока имплант работает;
 *  - красная пульсация при перегреве.
 */
@Mod.EventBusSubscriber(modid = SandevistanMod.MODID, value = Dist.CLIENT)
public class ScreenEffectHandler {

    // --- анимация активации: спрайт-лист 6 x 6, кадр 384 x 216, 34 кадра, 25 к/с ---
    private static final ResourceLocation ACTIVATION =
            new ResourceLocation(SandevistanMod.MODID, "textures/gui/activation.png");
    private static final int COLS = 6, FRAME_W = 384, FRAME_H = 216, FRAMES = 34;
    private static final int SHEET_W = COLS * FRAME_W, SHEET_H = 6 * FRAME_H;
    private static final float FRAMES_PER_TICK = 1.25f;                       // 25 к/с при 20 тиках/с
    private static final int ANIM_TICKS = (int) Math.ceil(FRAMES / FRAMES_PER_TICK) + 1;

    private static int animTicks = -1;
    private static boolean wasActive = false;

    private static float intensity = 0f, prevIntensity = 0f;
    private static float heatFlash = 0f, prevHeatFlash = 0f;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;

        boolean active = mc.player != null && mc.player.hasEffect(ModEffects.SANDEVISTAN.get());
        boolean hot = mc.player != null && mc.player.hasEffect(ModEffects.OVERHEATED.get());

        // запуск анимации в момент включения
        if (active && !wasActive) animTicks = 0;
        else if (animTicks >= 0 && ++animTicks > ANIM_TICKS) animTicks = -1;
        wasActive = active;

        prevIntensity = intensity;
        prevHeatFlash = heatFlash;
        intensity += active ? 0.12f : -0.08f;
        heatFlash += hot ? 0.1f : -0.06f;
        intensity = Math.max(0f, Math.min(1f, intensity));
        heatFlash = Math.max(0f, Math.min(1f, heatFlash));
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        float k = prevIntensity + (intensity - prevIntensity) * Minecraft.getInstance().getFrameTime();
        if (k > 0f) event.setNewFovModifier(event.getNewFovModifier() * (1f + 0.18f * k));
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        float pt = mc.getFrameTime();
        float k = prevIntensity + (intensity - prevIntensity) * pt;
        float h = prevHeatFlash + (heatFlash - prevHeatFlash) * pt;
        if (k <= 0.01f && h <= 0.01f && animTicks < 0) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), ht = g.guiHeight();
        float t = (mc.level == null ? 0 : mc.level.getGameTime()) + pt;

        // ----- анимация активации -----
        if (animTicks >= 0) {
            float pos = (animTicks + pt) * FRAMES_PER_TICK;   // позиция в кадрах
            int frame = Math.min(FRAMES - 1, (int) pos);
            float progress = Math.min(1f, pos / FRAMES);

            // затемнение мира на время анимации
            int dim = (int) (150 * (1f - progress * progress * progress));
            if (dim > 0) g.fill(0, 0, w, ht, dim << 24);

            // кадр поверх мира аддитивно: чёрное прозрачно, яркое светится
            int u = (frame % COLS) * FRAME_W;
            int v = (frame / COLS) * FRAME_H;
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            g.blit(ACTIVATION, 0, 0, w, ht, (float) u, (float) v, FRAME_W, FRAME_H, SHEET_W, SHEET_H);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        // ----- радужная виньетка, пока имплант работает -----
        if (k > 0.01f) {
            int steps = 36;
            int depth = Math.max(w, ht) / 7;
            for (int i = 0; i < steps; i++) {
                float f = 1f - i / (float) steps;
                int a = (int) (110 * k * f * f);
                if (a <= 0) continue;
                int off = depth * i / steps;
                int top    = argb(a, (t * 0.01f) % 1f);
                int right  = argb(a, (t * 0.01f + 0.25f) % 1f);
                int bottom = argb(a, (t * 0.01f + 0.5f) % 1f);
                int left   = argb(a, (t * 0.01f + 0.75f) % 1f);
                int thick = Math.max(1, depth / steps);
                g.fill(0, off, w, off + thick, top);
                g.fill(0, ht - off - thick, w, ht - off, bottom);
                g.fill(off, 0, off + thick, ht, left);
                g.fill(w - off - thick, 0, w - off, ht, right);
            }
        }

        // ----- перегрев -----
        if (h > 0.01f) {
            float pulse = 0.6f + 0.4f * (float) Math.sin(t * 0.4f);
            int a = (int) (90 * h * pulse);
            g.fill(0, 0, w, ht, (a << 24) | 0xFF2000);
        }
    }

    private static int argb(int alpha, float hue) {
        return (alpha << 24) | (Color.HSBtoRGB(hue, 0.9f, 1f) & 0x00FFFFFF);
    }
}
