package org.codersoft.mohenjo.aimless.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;
import org.codersoft.mohenjo.aimless.util.PlayerEntityVerifier;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class AimlessClient implements ClientModInitializer {

    private static KeyMapping aimKeyBind;

    private static final double MAX_RANGE = 45.0;
    private static final int REACTION_TICKS = 3;
    private static final float AIM_SPEED = 0.12f;
    private static final float MAX_DEGREES_PER_TICK = 15.0f;
    private static final float JITTER_STRENGTH = 0.15f;

    private final Random random = new Random();
    private float currentYawNoise = 0.0f;
    private float currentPitchNoise = 0.0f;
    private int tickCounter = 0;
    private boolean aiming = false;

    @Override
    public void onInitializeClient() {
        aimKeyBind = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.aimless.track",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("aimless", "category"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            ClientLevel level = client.level;

            if (player == null || level == null) return;

            if (aimKeyBind.consumeClick()) {
                aiming = !aiming;
                player.sendOverlayMessage(Component.literal(aiming ? "Aimless §aEnabled" : "Aimless §cDisabled"));
            }

            if (!aiming) {
                tickCounter = 0;
                return;
            }

            tickCounter++;
            if (tickCounter < REACTION_TICKS) return;
            tickCounter = 0;

            Player closestTarget = findClosestPlayer(player, level);
            if (closestTarget != null) {
                applySmoothAim(player, closestTarget);
            }
        });
    }

    private Player findClosestPlayer(LocalPlayer player, ClientLevel level) {
        Player closest = null;
        double closestDistance = MAX_RANGE;

        for (Player target : level.players()) {
            if (target == player || !target.isAlive() || !PlayerEntityVerifier.isLegitimateHumanPlayer(target)) continue;

            double distance = player.distanceTo(target);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = target;
            }
        }
        return closest;
    }

    private void applySmoothAim(LocalPlayer player, Player target) {
        Vec3 playerEyePos = player.getEyePosition();
        Vec3 targetChestPos = target.position().add(0, target.getBbHeight() * 0.55f, 0);

        double dx = targetChestPos.x - playerEyePos.x;
        double dy = targetChestPos.y - playerEyePos.y;
        double dz = targetChestPos.z - playerEyePos.z;
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) -(Math.atan2(dy, distanceXZ) * 180.0 / Math.PI);

        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        float yawDifference = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDifference = targetPitch - currentPitch;

        if (player.tickCount % 2 == 0) {
            currentYawNoise = (float) (random.nextGaussian() * JITTER_STRENGTH);
            currentPitchNoise = (float) (random.nextGaussian() * JITTER_STRENGTH);
        }

        float yawStep = yawDifference * AIM_SPEED;
        float pitchStep = pitchDifference * AIM_SPEED;

        yawStep = Mth.clamp(yawStep, -MAX_DEGREES_PER_TICK, MAX_DEGREES_PER_TICK);
        pitchStep = Mth.clamp(pitchStep, -MAX_DEGREES_PER_TICK, MAX_DEGREES_PER_TICK);

        float newYaw = currentYaw + yawStep + currentYawNoise;
        float newPitch = currentPitch + pitchStep + currentPitchNoise;

        newPitch = Mth.clamp(newPitch, -90.0f, 90.0f);

        player.setYRot(newYaw);
        player.setXRot(newPitch);
    }
}
