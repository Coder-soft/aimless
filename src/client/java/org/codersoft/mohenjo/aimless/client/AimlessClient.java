package org.codersoft.mohenjo.aimless.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import org.codersoft.mohenjo.aimless.util.PlayerEntityVerifier;
import org.lwjgl.glfw.GLFW;

public class AimlessClient implements ClientModInitializer {

    private static KeyBinding aimKeyBind;

    private static final double MAX_RANGE = 3.0;
    private static final AimlessConfig CONFIG = AimlessConfig.load();
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

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("aimless")
                .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "ticks");
                        CONFIG.setReactionTicks(value);
                        ctx.getSource().sendFeedback(Text.literal("§aReaction ticks set to " + value));
                        return 1;
                    })
                )
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Text.literal("§eReaction ticks: " + CONFIG.getReactionTicks()));
                    return 1;
                })
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            ClientWorld level = client.world;

            if (player == null || level == null) return;

            if (aimKeyBind.wasPressed()) {
                aiming = !aiming;
                player.sendMessage(Text.literal(aiming ? "Aimless §aEnabled §7(rt: " + CONFIG.getReactionTicks() + ")" : "Aimless §cDisabled"), true);
            }

            if (!aiming) {
                tickCounter = 0;
                return;
            }

            tickCounter++;
            if (tickCounter < CONFIG.getReactionTicks()) return;
            tickCounter = 0;

            PlayerEntity closestTarget = findClosestPlayer(player, level);
            if (closestTarget != null) {
                applyAim(player, closestTarget);
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

    private void applyAim(ClientPlayerEntity player, PlayerEntity target) {
        Vec3d playerEyePos = player.getEyePos();
        Vec3d targetEyePos = target.getEyePos();

        double dx = targetEyePos.x - playerEyePos.x;
        double dy = targetEyePos.y - playerEyePos.y;
        double dz = targetEyePos.z - playerEyePos.z;
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) -(Math.atan2(dy, distanceXZ) * 180.0 / Math.PI);
        targetPitch = Math.clamp(targetPitch, -90.0f, 90.0f);

        player.setYaw(targetYaw);
        player.setPitch(targetPitch);
    }
}
