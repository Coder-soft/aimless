Master Architecture Plan: Client-Side Entity Verification & Rendering Isolation PipelineThis document provides the complete, unified architectural design plan for isolating genuine human players from simulated NPCs (e.g., Citizens) and defensive anti-cheat combat-trap bots (e.g., Hypixel Watchdog, GrimAC, Vulcan) within a client-side Fabric mod environment.1. The Consolidated 5-Layer Detection PipelineTo filter entities seamlessly without causing frame drops or game-crashing NullPointerExceptions, entities must pass through a strict short-circuit evaluation pipeline ordered from lowest computational cost to highest.1.Layer 1: Structural Integrity & Identity Gate:Cost: O(1) - Reference Matching.Verifies that the target entity is a valid PlayerEntity instance, is fully initialized in the client world, and is not the local player client.2.Layer 2: Username Lexical Restrictions:Cost: O(1) - Lexical Rule Verification.Enforces official Mojang/Microsoft account authentication naming rules to quickly drop programmatic placeholder strings (like CIT- or names containing illegal spaces/hyphens).3.Layer 3: Network Player-List Synchronization:Cost: O(1) - Memory Hash-Map Lookup.Cross-references the entity’s unique cryptographic UUID against the active network connection registry cached in memory by the game client.4.Layer 4: Latency Telemetry Matrix:Cost: O(1) - Telemetry Variable Inspection.Inspects server-synchronized connection latency variables to weed out faked, dead-static connection states.5.Layer 5: Behavioral & Anti-Cheat Heuristics:Cost: O(N) - Kinematic & Spatiotemporal Analysis.Tracks the tracking lifecycle age, visibility flags, and physical movement vectors to intercept dynamically spawned anti-cheat combat loops.2. The Complete Technical Lifecycle BlueprintThis execution chart demonstrates how a packet-spawned entity flows through your utility filter via the intercepted rendering engine pipeline before being drawn or discarded: [ Server Packet Spawns Entity ]
               │
               ▼
 ┌─────────────────────────────────────────────────────────┐
 │ EntityRenderDispatcher#render() Intercepted via Mixin  │
 └─────────────────────────┬───────────────────────────────┘
                           │
                           ▼
               [ Is Entity a PlayerEntity? ]
                           │
             ┌─────────────┴─────────────┐
             ▼ (YES)                     ▼ (NO)
 ┌───────────────────────────┐     ┌───────────────────────┐
 │ Execute Filter Pipeline   │     │ Render Normallly      │
 └───────────┬───────────────┘     └───────────────────────┘
             │
             ├──► [ Layer 1: Is Self/Null/Removed? ] ─────(YES)──► [ DROP / RENDER NORMAL ]
             ├──► [ Layer 2: Violates Name Rules? ]  ─────(YES)──► [ DROP: FAKE NPC ]
             ├──► [ Layer 3: Missing from Tab List? ] ────(YES)──► [ DROP: FAKE ENTITY ]
             ├──► [ Layer 4: Ping is Spoofed <= 0? ] ─────(YES)──► [ DROP: BOT DETECTED ]
             └──► [ Layer 5: Defies Physics/Age? ]   ─────(YES)──► [ DROP: ANTI-CHEAT TRAP ]
                           │
                           ▼ (Passes All Layers)
 ┌─────────────────────────────────────────────────────────┐
 │                 APPROVED REAL HUMAN PLAYER              │
 │  • Allow Normal Rendering                               │
 │  • Inject Mod Visuals (ESP, Chams, Target HUD, etc.)     │
 └─────────────────────────────────────────────────────────┘
3. Comparative Metrics MatrixMetric VectorAuthentic Human PlayerStandard NPC (Citizens)Anti-Cheat Combat BotPlayerListEntry MapPresent in Client MemoryAbsent (Typically)Absent / Forced Fake EntryPing TelemetryDynamic/Variable ($>0\text{ ms}$)Fixed ($0\text{ ms}$ or $-1\text{ ms}$)Fixed $0\text{ ms}$ or Spoofed StaticProfile SignaturesCryptographically SignedBlank / UnsignedBlank / Stripped MetadataLifecycle ProfilePersistent / Normal TransitionPersistentEphemeral ($\le 20\text{ Ticks}$)Lexical Formatting^[a-zA-Z0-9_]{2,16}$Often contains -,  , [Random Alphanumeric Arrays4. Production Source Code ImplementationBelow is the complete, modular source implementation for your Fabric mod. It includes both the high-performance filter utility and the rendering injection mixin.Part A: The Filter Utility Layer (PlayerEntityVerifier.java)Javapackage net.yourmod.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import java.util.regex.Pattern;

/**
 * High-fidelity verification filter to isolate legitimate players from NPCs and Anti-Cheat entities.
 * Optimization Note: Cheap operations run first to short-circuit and preserve frame-rates.
 */
public final class PlayerEntityVerifier {

    // Matches standard Mojang username constraints: 2-16 characters, alphanumeric + underscores
    private static final Pattern MOJANG_USERNAME_RULE = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");

    public static boolean isLegitimateHumanPlayer(PlayerEntity target) {
        // LAYER 1: Structural Integrity Gate
        if (target == null || target.isMainPlayer() || target.isRemoved()) {
            return false;
        }

        // LAYER 2: Username Lexical Restrictions
        String profileName = target.getGameProfile().getName();
        String displayName = target.getName().getString();

        if (profileName == null || profileName.isEmpty()) return false;

        // Trap common boilerplate placeholder configurations immediately
        if (profileName.startsWith("CIT-") || displayName.startsWith("CIT-") || 
            profileName.toLowerCase().contains("npc") || profileName.equalsIgnoreCase("dummy")) {
            return false;
        }

        // Catch non-standard account naming patterns (spaces, illegal symbols)
        if (!MOJANG_USERNAME_RULE.matcher(profileName).matches()) {
            return false;
        }

        // Ensure network client components are fully bound and ready
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return false;
        }

        // LAYER 3: Network Player-List Synchronization
        PlayerListEntry networkRegistryEntry = client.getNetworkHandler().getPlayerListEntry(target.getUuid());
        if (networkRegistryEntry == null) {
            return false; 
        }

        // LAYER 4: Latency Telemetry Matrix
        int simulatedPing = networkRegistryEntry.getLatency();
        if (simulatedPing <= 0) {
            return false; 
        }

        // Check for empty profile property tokens (Skin / Cape session texture signatures)
        if (target.getGameProfile().getProperties().isEmpty()) {
            return false;
        }

        // LAYER 5: Behavioral & Anti-Cheat Heuristics
        // A: Check the entity lifecycle age threshold (20 ticks = 1 second)
        if (target.age < 20) {
            // Instantly flag if spawned directly inside high-alert physical contact combat ranges
            double rangeSquared = target.squaredDistanceTo(client.player);
            if (rangeSquared <= 36.0) { // Inside a 6-block radius
                return false;
            }
        }

        // B: Intercept standard invisible entity telemetry trickery
        if (target.isInvisible()) {
            // Real players using potions must carry an explicit potion status payload array
            if (target.getStatusEffects().isEmpty()) {
                return false; 
            }
        }

        // C: Kinematic Motion Consistency Check
        if (!target.isOnGround() && !target.getAbilities().flying && !target.isFallFlying()) {
            // Anti-cheat pathfinding bots frequently lock coordinates relative to your camera frame
            if (target.getVelocity().y == 0.0 && target.getVelocity().horizontalLength() > 0.0) {
                return false; // Moving horizontally with zero vertical gravity drop vector offsets
            }
        }

        return true; // Entity successfully survived all validation criteria
    }
}
Part B: The Render Injection Hook (MixinEntityRenderDispatcher.java)Javapackage net.yourmod.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.yourmod.util.PlayerEntityVerifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

    @Inject(
        method = "render", 
        at = @At("HEAD"), 
        cancellable = true
    )
    private <E extends Entity> void onInterceptEntityRender(
            E entity, 
            double x, 
            double y, 
            double z, 
            float yaw, 
            float tickDelta, 
            MatrixStack matrices, 
            VertexConsumerProvider vertexConsumers, 
            int light, 
            CallbackInfo ci
    ) {
        // Step 1: Only target PlayerEntity models
        if (entity instanceof PlayerEntity player) {
            
            // Step 2: Evaluate the player against the 5-layer pipeline
            boolean isLegitHuman = PlayerEntityVerifier.isLegitimateHumanPlayer(player);

            if (!isLegitHuman) {
                /*
                 * OPTION 1: Complete Client-Side Vanish
                 * Canceling the injection callback drops the execution path out of the renderer. 
                 * This makes anti-cheat bots and fake NPCs 100% invisible on your screen.
                 */
                ci.cancel(); 

                /* * OPTION 2: Feature Specific Suppression (Alternative)
                 * Tell your internal mod modules (ESP, Aim Targeters, Radar) to flag 
                 * this entity handle as a simulated bot rather than completely hiding them.
                 */
                // YourMod.getFeatureManager().getESP().blacklistEntity(player);
                
                return;
            }

            /*
             * Step 3: Human Player Execution Context
             * If the code reaches here, the player has passed every filter step. 
             * You can safely run custom rendering systems like Glow ESP or visual overlays.
             */
            // YourMod.getFeatureManager().getESP().renderCustomOutline(player, matrices, vertexConsumers);
        }
    }
}
5. Architectural Sources & Protocol ReferencesMojang Authentication Specifications (com.mojang.authlib.GameProfile): Profiles generated during active login sessions require signed cryptographic text mappings for active skin/cape assets. Default simulated server profiles routinely leave these object maps unpopulated.Minecraft Protocol Specifications (ClientPlayNetworkHandler): Handles network streams arriving via PlayerListS2CPacket (responsible for rendering players on the tab menu) and BundleS2CPacket. Fake players spawned using internal server tracking hacks circumvent this registration process entirely to avoid cluttering the client UI.Anti-Cheat Bot Mechanical Design (GrimAC / Vulcan / Watchdog): Defensive bot routines manipulate network entity actions by updating position coordinates dynamically using fixed trigonometric algorithms. Because they run entirely on server threads without a physical client attachment, they do not present genuine network jitter or organic movement ticks.