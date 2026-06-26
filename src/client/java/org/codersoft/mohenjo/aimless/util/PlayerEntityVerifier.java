package org.codersoft.mohenjo.aimless.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;

import java.util.regex.Pattern;

public final class PlayerEntityVerifier {

    private static final Pattern MOJANG_USERNAME_RULE = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");

    public static boolean isLegitimateHumanPlayer(PlayerEntity target) {
        if (target == null || target.isMainPlayer() || target.isRemoved()) {
            return false;
        }

        String profileName = target.getGameProfile().name();
        String displayName = target.getName().getString();

        if (profileName == null || profileName.isEmpty()) return false;

        if (profileName.startsWith("CIT-") || displayName.startsWith("CIT-") ||
            profileName.toLowerCase().contains("npc") || profileName.equalsIgnoreCase("dummy")) {
            return false;
        }

        if (!MOJANG_USERNAME_RULE.matcher(profileName).matches()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return false;
        }

        PlayerListEntry networkRegistryEntry = client.getNetworkHandler().getPlayerListEntry(target.getUuid());
        if (networkRegistryEntry == null) {
            return false;
        }

        int simulatedPing = networkRegistryEntry.getLatency();
        if (simulatedPing <= 0) {
            return false;
        }

        if (target.getGameProfile().properties().isEmpty()) {
            return false;
        }

        if (target.age < 20) {
            double rangeSquared = target.squaredDistanceTo(client.player);
            if (rangeSquared <= 36.0) {
                return false;
            }
        }

        if (target.isInvisible()) {
            if (target.getStatusEffects().isEmpty()) {
                return false;
            }
        }

        if (!target.isOnGround() && !target.getAbilities().flying && !target.isGliding()) {
            if (target.getVelocity().y == 0.0 && target.getVelocity().horizontalLength() > 0.0) {
                return false;
            }
        }

        return true;
    }
}
