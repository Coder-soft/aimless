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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.codersoft.mohenjo.aimless.util.PlayerEntityVerifier;
import org.lwjgl.glfw.GLFW;

public class AimlessClient implements ClientModInitializer {

    private static KeyBinding aimKeyBind;

    private static final double MAX_RANGE = 4.0;
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

        SuggestionProvider<FabricClientCommandSource> PLAYER_SUGGESTIONS = (ctx, builder) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld level = client.world;
            if (level != null) {
                for (PlayerEntity p : level.getPlayers()) {
                    if (p != client.player) {
                        builder.suggest(p.getGameProfile().name());
                    }
                }
            }
            return builder.buildFuture();
        };

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("aimless")
                .then(ClientCommandManager.literal("config")
                    .executes(ctx -> {
                        MinecraftClient.getInstance().execute(() ->
                            MinecraftClient.getInstance().setScreen(AimlessConfigScreen.build(null))
                        );
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("exception")
                    .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("player", StringArgumentType.word())
                            .suggests(PLAYER_SUGGESTIONS)
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "player");
                                if (AimlessConfig.CONFIG.isExcepted(name)) {
                                    ctx.getSource().sendFeedback(Text.literal("§e" + name + " is already excepted"));
                                } else {
                                    AimlessConfig.CONFIG.addException(name);
                                    ctx.getSource().sendFeedback(Text.literal("§aAdded " + name + " to exception list"));
                                }
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("player", StringArgumentType.word())
                            .suggests(PLAYER_SUGGESTIONS)
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "player");
                                if (AimlessConfig.CONFIG.isExcepted(name)) {
                                    AimlessConfig.CONFIG.removeException(name);
                                    ctx.getSource().sendFeedback(Text.literal("§aRemoved " + name + " from exception list"));
                                } else {
                                    ctx.getSource().sendFeedback(Text.literal("§e" + name + " is not in the exception list"));
                                }
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("list")
                        .executes(ctx -> {
                            java.util.List<String> ex = AimlessConfig.CONFIG.getExceptions();
                            if (ex.isEmpty()) {
                                ctx.getSource().sendFeedback(Text.literal("§eNo exceptions configured"));
                            } else {
                                ctx.getSource().sendFeedback(Text.literal("§eExceptions: " + String.join(", ", ex)));
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("clear")
                        .executes(ctx -> {
                            AimlessConfig.CONFIG.clearExceptions();
                            ctx.getSource().sendFeedback(Text.literal("§aCleared all exceptions"));
                            return 1;
                        })
                    )
                )
                .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "ticks");
                        AimlessConfig.CONFIG.setReactionTicks(value);
                        ctx.getSource().sendFeedback(Text.literal("§aReaction ticks set to " + value));
                        return 1;
                    })
                )
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Text.literal("§eReaction ticks: " + AimlessConfig.CONFIG.getReactionTicks()));
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
                player.sendMessage(Text.literal(aiming
                        ? String.format("Aimless §aEnabled §7(h: %.0f%% x: %+.1f z: %+.1f rt: %d)",
                        AimlessConfig.CONFIG.getBodyHeight() * 100, AimlessConfig.CONFIG.getOffsetX(), AimlessConfig.CONFIG.getOffsetZ(), AimlessConfig.CONFIG.getReactionTicks())
                        : "Aimless §cDisabled"), true);
            }

            if (!aiming) {
                tickCounter = 0;
                return;
            }

            tickCounter++;
            if (tickCounter < AimlessConfig.CONFIG.getReactionTicks()) return;
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

    private void applyAim(ClientPlayerEntity player, PlayerEntity target) {
        Vec3d playerEyePos = player.getEyePos();

        double targetX = target.getX() + AimlessConfig.CONFIG.getOffsetX();
        double targetY = target.getY() + (AimlessConfig.CONFIG.getBodyHeight() * target.getHeight());
        double targetZ = target.getZ() + AimlessConfig.CONFIG.getOffsetZ();

        double dx = targetX - playerEyePos.x;
        double dy = targetY - playerEyePos.y;
        double dz = targetZ - playerEyePos.z;
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) -(Math.atan2(dy, distanceXZ) * 180.0 / Math.PI);
        targetPitch = Math.clamp(targetPitch, -90.0f, 90.0f);

        player.setYaw(targetYaw);
        player.setPitch(targetPitch);
    }
}
