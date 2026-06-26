**Author:** codersoft

1. Prerequisite Dependencies
   Ensure your build.gradle includes the Fabric API dependency, as we will use its lifestyle events and keybinding helpers.

2. Implementation Code
   Create a new client-side class (e.g., ClientAimMod.java) in your mod's client initialization package:

Java
package com.example.aimmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class ClientAimMod implements ClientModInitializer {

    private static KeyMapping aimKeyBind;
    
    // Configuration Settings
    private static final double MAX_RANGE = 45.0; // Maximum distance to look for targets (in blocks)
    private static final float AIM_SPEED = 0.15f;  // Smoothness factor (1.0f = instant snap, 0.05f = very slow/smooth)

    @Override
    public void onInitializeClient() {
        // Register a toggle/hold keybind (Default: V)
        aimKeyBind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aimmod.track",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.aimmod"
        ));

        // Listen to the end of every client tick (20 ticks per second)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            ClientLevel level = client.level;

            // Safeguard against running when the world or player isn't fully loaded
            if (player == null || level == null) return;

            // Only execute if the target key is held down
            if (aimKeyBind.isDown()) {
                Player closestTarget = findClosestPlayer(player, level);
                if (closestTarget != null) {
                    applySmoothAim(player, closestTarget);
                }
            }
        });
    }

    private Player findClosestPlayer(LocalPlayer player, ClientLevel level) {
        Player closest = null;
        double closestDistance = MAX_RANGE;

        // Iterate through all players loaded in the client's chunk rendering distance
        for (Player target : level.players()) {
            // Ignore self, dead players, and invisible players
            if (target == player || !target.isAlive() || target.isInvisible()) continue;

            double distance = player.distanceTo(target);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = target;
            }
        }
        return closest;
    }

    private void applySmoothAim(LocalPlayer player, Player target) {
        // Get positions based on eye-level rather than foot-level
        Vec3 playerEyePos = player.getEyePosition();
        Vec3 targetEyePos = target.getEyePosition();

        // Calculate delta vectors
        double dx = targetEyePos.x - playerEyePos.x;
        double dy = targetEyePos.y - playerEyePos.y;
        double dz = targetEyePos.z - playerEyePos.z;
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        // Convert delta coordinates to Minecraft Angular Values (Degrees)
        // Yaw needs a -90 offset because Minecraft's Z+ axis starts at a 90-degree rotational offset
        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) -(Math.atan2(dy, distanceXZ) * 180.0 / Math.PI);

        // Get the current player rotations
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        // Critical step: Wrap the delta angles to prevent 360-degree camera spin bugs 
        // (e.g., moving from 179° to -179° handles shifting across the border smoothly)
        float yawDifference = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDifference = targetPitch - currentPitch;

        // Interpolate over time for smooth tracking
        float newYaw = currentYaw + (yawDifference * AIM_SPEED);
        float newPitch = currentPitch + (pitchDifference * AIM_SPEED);

        // Clamp pitch to prevent the camera flipping upside down (-90 is straight up, 90 is straight down)
        newPitch = Mth.clamp(newPitch, -90.0f, 90.0f);

        // Set the client-side entity angles
        player.setYRot(newYaw);
        player.setXRot(newPitch);
    }
}
3. Deep Dive into the Mechanics
   Target Calculation (Math.atan2): Trigonometric functions return measurements relative to standard math grids. Because Minecraft treats South as 0
   ∘
   (running along positive Z) and moves clockwise, the math requires subtracting 90
   ∘
   to translate the horizontal trajectory to your viewport's yaw.

Angle Normalization (Mth.wrapDegrees): If a target steps across the absolute coordinate boundary (180
∘
to −180
∘
), a basic subtraction calculation forces your mouse to violently spin around a full cycle. Wrapping the degrees calculates the absolute shortest path to rotate.

Smoothing System (AIM_SPEED): Multiplying the positional difference by a fraction lets the camera approach the target incrementally each tick. Setting AIM_SPEED = 1.0f removes the dampening, causing an instantaneous mechanical snap.

Alternative Mapping Guide
If your modding workspace utilizes Yarn Mappings instead of the official Mojang mappings, modify your method targets and class descriptors using this quick translation map:

Mojang Mapping (Used Above)	Yarn Mapping Equivalent
LocalPlayer	ClientPlayerEntity
ClientLevel	ClientWorld
Player	PlayerEntity
Mth.wrapDegrees()	MathHelper.wrapDegrees()
player.getYRot() / setYRot()	player.getYaw() / setYaw()
player.getXRot() / setXRot()	player.getPitch() / setPitch()
KeyMapping	KeyBinding1. Prerequisite Dependencies
Ensure your build.gradle includes the Fabric API dependency, as we will use its lifestyle events and keybinding helpers.

2. Implementation Code
   Create a new client-side class (e.g., ClientAimMod.java) in your mod's client initialization package:

Java
package com.example.aimmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class ClientAimMod implements ClientModInitializer {

    private static KeyMapping aimKeyBind;
    
    // Configuration Settings
    private static final double MAX_RANGE = 45.0; // Maximum distance to look for targets (in blocks)
    private static final float AIM_SPEED = 0.15f;  // Smoothness factor (1.0f = instant snap, 0.05f = very slow/smooth)

    @Override
    public void onInitializeClient() {
        // Register a toggle/hold keybind (Default: V)
        aimKeyBind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aimmod.track",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.aimmod"
        ));

        // Listen to the end of every client tick (20 ticks per second)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            ClientLevel level = client.level;

            // Safeguard against running when the world or player isn't fully loaded
            if (player == null || level == null) return;

            // Only execute if the target key is held down
            if (aimKeyBind.isDown()) {
                Player closestTarget = findClosestPlayer(player, level);
                if (closestTarget != null) {
                    applySmoothAim(player, closestTarget);
                }
            }
        });
    }

    private Player findClosestPlayer(LocalPlayer player, ClientLevel level) {
        Player closest = null;
        double closestDistance = MAX_RANGE;

        // Iterate through all players loaded in the client's chunk rendering distance
        for (Player target : level.players()) {
            // Ignore self, dead players, and invisible players
            if (target == player || !target.isAlive() || target.isInvisible()) continue;

            double distance = player.distanceTo(target);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = target;
            }
        }
        return closest;
    }

    private void applySmoothAim(LocalPlayer player, Player target) {
        // Get positions based on eye-level rather than foot-level
        Vec3 playerEyePos = player.getEyePosition();
        Vec3 targetEyePos = target.getEyePosition();

        // Calculate delta vectors
        double dx = targetEyePos.x - playerEyePos.x;
        double dy = targetEyePos.y - playerEyePos.y;
        double dz = targetEyePos.z - playerEyePos.z;
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        // Convert delta coordinates to Minecraft Angular Values (Degrees)
        // Yaw needs a -90 offset because Minecraft's Z+ axis starts at a 90-degree rotational offset
        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) -(Math.atan2(dy, distanceXZ) * 180.0 / Math.PI);

        // Get the current player rotations
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        // Critical step: Wrap the delta angles to prevent 360-degree camera spin bugs 
        // (e.g., moving from 179° to -179° handles shifting across the border smoothly)
        float yawDifference = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDifference = targetPitch - currentPitch;

        // Interpolate over time for smooth tracking
        float newYaw = currentYaw + (yawDifference * AIM_SPEED);
        float newPitch = currentPitch + (pitchDifference * AIM_SPEED);

        // Clamp pitch to prevent the camera flipping upside down (-90 is straight up, 90 is straight down)
        newPitch = Mth.clamp(newPitch, -90.0f, 90.0f);

        // Set the client-side entity angles
        player.setYRot(newYaw);
        player.setXRot(newPitch);
    }
}
3. Deep Dive into the Mechanics
   Target Calculation (Math.atan2): Trigonometric functions return measurements relative to standard math grids. Because Minecraft treats South as 0
   ∘
   (running along positive Z) and moves clockwise, the math requires subtracting 90
   ∘
   to translate the horizontal trajectory to your viewport's yaw.

Angle Normalization (Mth.wrapDegrees): If a target steps across the absolute coordinate boundary (180
∘
to −180
∘
), a basic subtraction calculation forces your mouse to violently spin around a full cycle. Wrapping the degrees calculates the absolute shortest path to rotate.

Smoothing System (AIM_SPEED): Multiplying the positional difference by a fraction lets the camera approach the target incrementally each tick. Setting AIM_SPEED = 1.0f removes the dampening, causing an instantaneous mechanical snap.

Alternative Mapping Guide
If your modding workspace utilizes Yarn Mappings instead of the official Mojang mappings, modify your method targets and class descriptors using this quick translation map:

Mojang Mapping (Used Above)	Yarn Mapping Equivalent
LocalPlayer	ClientPlayerEntity
ClientLevel	ClientWorld
Player	PlayerEntity
Mth.wrapDegrees()	MathHelper.wrapDegrees()
player.getYRot() / setYRot()	player.getYaw() / setYaw()
player.getXRot() / setXRot()	player.getPitch() / setPitch()
KeyMapping	KeyBinding