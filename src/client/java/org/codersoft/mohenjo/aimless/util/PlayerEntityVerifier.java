package org.codersoft.mohenjo.aimless.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

import java.util.regex.Pattern;

public final class PlayerEntityVerifier {

    private static final Pattern MOJANG_USERNAME_RULE = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");

    public static boolean isLegitimateHumanPlayer(Player target) {
        if (target == null || target.isRemoved()) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (target == client.player) {
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

        if (client.getConnection() == null) {
            return false;
        }

        PlayerInfo networkRegistryEntry = client.getConnection().getPlayerInfo(target.getUUID());
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

        if (target.tickCount < 20) {
            double rangeSquared = target.distanceToSqr(client.player);
            if (rangeSquared <= 36.0) {
                return false;
            }
        }

        if (target.isInvisible()) {
            if (target.getActiveEffects().isEmpty()) {
                return false;
            }
        }

        if (!target.onGround() && !target.getAbilities().flying && !target.isFallFlying()) {
            if (target.getDeltaMovement().y == 0.0 && target.getDeltaMovement().horizontalDistance() > 0.0) {
                return false;
            }
        }

        return true;
    }
}
