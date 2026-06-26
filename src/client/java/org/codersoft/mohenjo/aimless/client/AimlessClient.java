package org.codersoft.mohenjo.aimless.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import org.codersoft.mohenjo.aimless.util.PlayerEntityVerifier;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class AimlessClient implements ClientModInitializer {

    private static KeyBinding aimKeyBind;

    private static final double MAX_RANGE = 45.0;
    private static final int REACTION_TICKS = 3;
    private static final int TRACKING_TICKS = 2;
    private static final float MAX_DEGREES_PER_TICK = 15.0f;
    private static final float JITTER_STRENGTH = 0.15f;

    private final Random random = new Random();
    private float currentYawNoise = 0.0f;
    private float currentPitchNoise = 0.0f;
    private int tickCounter = 0;
    private boolean aiming = false;

    @Override
    public void onInitializeClient() {
        aimKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aimless.track",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyBinding.Category.create(Identifier.of("aimless", "category"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            ClientWorld level = client.world;

            if (player == null || level == null) return;

            if (aimKeyBind.wasPressed()) {
                aiming = !aiming;
                player.sendMessage(Text.literal(aiming ? "Aimless §aEnabled" : "Aimless §cDisabled"), true);
            }

            if (!aiming) {
                tickCounter = 0;
                return;
            }

            tickCounter++;
            if (tickCounter < REACTION_TICKS) return;
            tickCounter = 0;

            PlayerEntity closestTarget = findClosestPlayer(player, level);
            if (closestTarget != null) {
                applySmoothAim(player, closestTarget);
            }
        });
    }

    private PlayerEntity findClosestPlayer(ClientPlayerEntity player, ClientWorld level) {
        PlayerEntity closest = null;
        double closestDistance = MAX_RANGE;

        for (PlayerEntity target : level.getPlayers()) {
            if (target == player || !target.isAlive() || !PlayerEntityVerifier.isLegitimateHumanPlayer(target)) continue;

            double distance = player.distanceTo(target);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = target;
            }
        }
        return closest;
    }

    private void applySmoothAim(ClientPlayerEntity player, PlayerEntity target) {
        Vec3d playerEyePos = player.getEyePos();
        Vec3d targetChestPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.55f, target.getZ());

        double dx = targetChestPos.x - playerEyePos.x;
        double dy = targetChestPos.y - playerEyePos.y;
        double dz = targetChestPos.z - playerEyePos.z;
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) -(Math.atan2(dy, distanceXZ) * 180.0 / Math.PI);

        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();

        float yawDifference = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDifference = targetPitch - currentPitch;

        if (player.age % 2 == 0) {
            currentYawNoise = (float) (random.nextGaussian() * JITTER_STRENGTH);
            currentPitchNoise = (float) (random.nextGaussian() * JITTER_STRENGTH);
        }

        float yawStep = yawDifference / TRACKING_TICKS;
        float pitchStep = pitchDifference / TRACKING_TICKS;

        yawStep = MathHelper.clamp(yawStep, -MAX_DEGREES_PER_TICK, MAX_DEGREES_PER_TICK);
        pitchStep = MathHelper.clamp(pitchStep, -MAX_DEGREES_PER_TICK, MAX_DEGREES_PER_TICK);

        float newYaw = currentYaw + yawStep + currentYawNoise;
        float newPitch = currentPitch + pitchStep + currentPitchNoise;

        newPitch = MathHelper.clamp(newPitch, -90.0f, 90.0f);

        player.setYaw(newYaw);
        player.setPitch(newPitch);
    }
}
