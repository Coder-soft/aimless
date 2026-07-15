package org.codersoft.mohenjo.aimless.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import org.codersoft.mohenjo.aimless.util.PlayerEntityVerifier;
import org.lwjgl.glfw.GLFW;

public class AimlessClient implements ClientModInitializer {

    private static KeyMapping aimKeyBind;

    private static final double MAX_RANGE = 3.0;
    private static final AimlessConfig CONFIG = AimlessConfig.load();
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

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("aimless")
                .then(ClientCommands.argument("ticks", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "ticks");
                        CONFIG.setReactionTicks(value);
                        ctx.getSource().sendFeedback(Component.literal("§aReaction ticks set to " + value));
                        return 1;
                    })
                )
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("§eReaction ticks: " + CONFIG.getReactionTicks()));
                    return 1;
                })
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            ClientLevel level = client.level;

            if (player == null || level == null) return;

            if (aimKeyBind.consumeClick()) {
                aiming = !aiming;
                player.sendOverlayMessage(Component.literal(aiming ? "Aimless §aEnabled §7(rt: " + CONFIG.getReactionTicks() + ")" : "Aimless §cDisabled"));
            }

            if (!aiming) {
                tickCounter = 0;
                return;
            }

            tickCounter++;
            if (tickCounter < CONFIG.getReactionTicks()) return;
            tickCounter = 0;

            Player closestTarget = findClosestPlayer(player, level);
            if (closestTarget != null) {
                applyAim(player, closestTarget);
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

    private void applyAim(LocalPlayer player, Player target) {
        Vec3 playerEyePos = player.getEyePosition();
        Vec3 targetEyePos = target.getEyePosition();

        double dx = targetEyePos.x - playerEyePos.x;
        double dy = targetEyePos.y - playerEyePos.y;
        double dz = targetEyePos.z - playerEyePos.z;
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) -(Math.atan2(dy, distanceXZ) * 180.0 / Math.PI);
        targetPitch = Math.clamp(targetPitch, -90.0f, 90.0f);

        player.setYRot(targetYaw);
        player.setXRot(targetPitch);
    }
}
