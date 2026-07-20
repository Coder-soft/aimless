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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.codersoft.mohenjo.aimless.util.PlayerEntityVerifier;
import org.lwjgl.glfw.GLFW;

public class AimlessClient implements ClientModInitializer {

    private static KeyMapping aimKeyBind;

    private static final double MAX_RANGE = 4.0;
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

        SuggestionProvider<FabricClientCommandSource> PLAYER_SUGGESTIONS = (ctx, builder) -> {
            Minecraft client = Minecraft.getInstance();
            ClientLevel level = client.level;
            if (level != null) {
                for (Player p : level.players()) {
                    if (p != client.player) {
                        builder.suggest(p.getGameProfile().name());
                    }
                }
            }
            return builder.buildFuture();
        };

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("aimless")
                .then(ClientCommands.literal("config")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(() ->
                            Minecraft.getInstance().setScreenAndShow(AimlessConfigScreen.build(null))
                        );
                        return 1;
                    })
                )
                .then(ClientCommands.literal("exception")
                    .then(ClientCommands.literal("add")
                        .then(ClientCommands.argument("player", StringArgumentType.word())
                            .suggests(PLAYER_SUGGESTIONS)
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "player");
                                if (AimlessConfig.CONFIG.isExcepted(name)) {
                                    ctx.getSource().sendFeedback(Component.literal("§e" + name + " is already excepted"));
                                } else {
                                    AimlessConfig.CONFIG.addException(name);
                                    ctx.getSource().sendFeedback(Component.literal("§aAdded " + name + " to exception list"));
                                }
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommands.literal("remove")
                        .then(ClientCommands.argument("player", StringArgumentType.word())
                            .suggests(PLAYER_SUGGESTIONS)
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "player");
                                if (AimlessConfig.CONFIG.isExcepted(name)) {
                                    AimlessConfig.CONFIG.removeException(name);
                                    ctx.getSource().sendFeedback(Component.literal("§aRemoved " + name + " from exception list"));
                                } else {
                                    ctx.getSource().sendFeedback(Component.literal("§e" + name + " is not in the exception list"));
                                }
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommands.literal("list")
                        .executes(ctx -> {
                            java.util.List<String> ex = AimlessConfig.CONFIG.getExceptions();
                            if (ex.isEmpty()) {
                                ctx.getSource().sendFeedback(Component.literal("§eNo exceptions configured"));
                            } else {
                                ctx.getSource().sendFeedback(Component.literal("§eExceptions: " + String.join(", ", ex)));
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommands.literal("clear")
                        .executes(ctx -> {
                            AimlessConfig.CONFIG.clearExceptions();
                            ctx.getSource().sendFeedback(Component.literal("§aCleared all exceptions"));
                            return 1;
                        })
                    )
                )
                .then(ClientCommands.argument("ticks", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "ticks");
                        AimlessConfig.CONFIG.setReactionTicks(value);
                        ctx.getSource().sendFeedback(Component.literal("§aReaction ticks set to " + value));
                        return 1;
                    })
                )
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("§eReaction ticks: " + AimlessConfig.CONFIG.getReactionTicks()));
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
                player.sendOverlayMessage(Component.literal(aiming
                        ? String.format("Aimless §aEnabled §7(h: %.0f%% x: %+.1f z: %+.1f rt: %d)",
                        AimlessConfig.CONFIG.getBodyHeight() * 100, AimlessConfig.CONFIG.getOffsetX(), AimlessConfig.CONFIG.getOffsetZ(), AimlessConfig.CONFIG.getReactionTicks())
                        : "Aimless §cDisabled"));
            }

            if (!aiming) {
                tickCounter = 0;
                return;
            }

            tickCounter++;
            if (tickCounter < AimlessConfig.CONFIG.getReactionTicks()) return;
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
            if (target == player || !target.isAlive() || !PlayerEntityVerifier.isLegitimateHumanPlayer(target)
                || AimlessConfig.CONFIG.isExcepted(target.getGameProfile().name())) continue;

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

        double targetX = target.getX() + AimlessConfig.CONFIG.getOffsetX();
        double targetY = target.getY() + (AimlessConfig.CONFIG.getBodyHeight() * target.getBbHeight());
        double targetZ = target.getZ() + AimlessConfig.CONFIG.getOffsetZ();

        double dx = targetX - playerEyePos.x;
        double dy = targetY - playerEyePos.y;
        double dz = targetZ - playerEyePos.z;
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) -(Math.atan2(dy, distanceXZ) * 180.0 / Math.PI);
        targetPitch = Math.clamp(targetPitch, -90.0f, 90.0f);

        player.setYRot(targetYaw);
        player.setXRot(targetPitch);
    }
}
