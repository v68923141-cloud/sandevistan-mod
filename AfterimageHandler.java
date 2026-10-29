package com.example.sandevistan.client;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.sandevistan.ModEffects;
import com.example.sandevistan.SandevistanMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Радужные послеобразы (afterimage) для игроков с эффектом Sandevistan. */
@Mod.EventBusSubscriber(modid = SandevistanMod.MODID, value = Dist.CLIENT)
public class AfterimageHandler {

    private static final ResourceLocation WHITE =
            new ResourceLocation(SandevistanMod.MODID, "textures/entity/afterimage.png");
    private static final int LIFETIME = 24;     // тиков жизни послеобраза
    private static final int SPAWN_EVERY = 1;   // тиков между снимками
    private static final float MAX_ALPHA = 0.65f;
    private static final int FULL_BRIGHT = 15728880;

    private static class Ghost {
        double x, y, z;
        float bodyYaw, netHeadYaw, pitch, limbSwing, limbAmount, age;
        boolean slim, crouch;
        long birth;
        float hue;
    }

    private static final List<Ghost> GHOSTS = new ArrayList<>();
    private static final Map<UUID, Vec3> LAST = new HashMap<>();
    private static float hueCounter = 0f;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { GHOSTS.clear(); LAST.clear(); return; }
        if (mc.isPaused()) return;

        long now = mc.level.getGameTime();
        GHOSTS.removeIf(g -> now - g.birth > LIFETIME);

        if (now % SPAWN_EVERY != 0) return;
        for (AbstractClientPlayer p : mc.level.players()) {
            if (!p.hasEffect(ModEffects.SANDEVISTAN.get())) { LAST.remove(p.getUUID()); continue; }
            Vec3 pos = p.position();
            Vec3 last = LAST.get(p.getUUID());
            double moved = last == null ? 1 : pos.distanceTo(last);
            if (moved < 0.05) continue;
            LAST.put(p.getUUID(), pos);

            Ghost g = new Ghost();
            g.x = pos.x; g.y = pos.y; g.z = pos.z;
            g.bodyYaw = p.yBodyRot;
            g.netHeadYaw = p.yHeadRot - p.yBodyRot;
            g.pitch = p.getXRot();
            g.limbSwing = p.tickCount * 0.8f;
            g.limbAmount = (float) Math.min(1.0, moved * 4.0);
            g.age = p.tickCount;
            g.slim = "slim".equals(p.getModelName());
            g.crouch = p.isCrouching();
            g.birth = now;
            hueCounter = (hueCounter + 0.06f) % 1f;
            g.hue = hueCounter;
            GHOSTS.add(g);
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (GHOSTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack ps = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        long now = mc.level.getGameTime();

        for (Ghost g : GHOSTS) {
            float age = (now - g.birth) + partial;
            float alpha = MAX_ALPHA * Math.max(0f, 1f - age / LIFETIME);
            if (alpha <= 0.01f) continue;

            int rgb = Color.HSBtoRGB(g.hue, 0.85f, 1.0f);
            float r = ((rgb >> 16) & 255) / 255f;
            float gr = ((rgb >> 8) & 255) / 255f;
            float b = (rgb & 255) / 255f;

            PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher()
                    .getSkinMap().get(g.slim ? "slim" : "default");
            if (renderer == null) continue;
            PlayerModel<AbstractClientPlayer> model = renderer.getModel();

            ps.pushPose();
            ps.translate(g.x - cam.x, g.y - cam.y, g.z - cam.z);
            ps.mulPose(Axis.YP.rotationDegrees(180.0f - g.bodyYaw));
            ps.scale(-1.0f, -1.0f, 1.0f);
            ps.scale(0.9375f, 0.9375f, 0.9375f);
            ps.translate(0.0f, -1.501f, 0.0f);

            model.young = false;
            model.riding = false;
            model.attackTime = 0;
            model.crouching = g.crouch;
            model.setAllVisible(true);
            model.setupAnim(mc.player, g.limbSwing, g.limbAmount, g.age, g.netHeadYaw, g.pitch);

            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
            model.renderToBuffer(ps, vc, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, gr, b, alpha);
            ps.popPose();
        }
        buffers.endBatch();
    }
}
