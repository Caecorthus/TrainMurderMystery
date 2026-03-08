package dev.doctor4t.wathe.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Event fired before a blackout (lights-out) effect is applied to a player.
 * Listeners can cancel the blindness effect for specific players.
 * This event is fired on the SERVER side only.
 */
public final class BlackoutEffect {

    private BlackoutEffect() {
    }

    /**
     * Called BEFORE a blackout blindness effect is applied to a non-killer player.
     * Listeners can cancel the blindness by returning {@code BlackoutResult.cancel()}.
     */
    public static final Event<Before> BEFORE = createArrayBacked(Before.class, listeners -> (player, durationTicks) -> {
        for (Before listener : listeners) {
            BlackoutResult result = listener.beforeBlackoutEffect(player, durationTicks);
            if (result != null) {
                return result;
            }
        }
        return null;
    });

    @FunctionalInterface
    public interface Before {
        /**
         * Called before a blackout blindness effect is applied to a player.
         *
         * @param player        The player who would receive blindness
         * @param durationTicks The duration of the blindness effect in ticks
         * @return {@code BlackoutResult} to override, or {@code null} to defer
         */
        @Nullable
        BlackoutResult beforeBlackoutEffect(ServerPlayerEntity player, int durationTicks);
    }

    /**
     * Result of a blackout effect validation.
     */
    public record BlackoutResult(boolean cancelled) {
        public static BlackoutResult allow() {
            return new BlackoutResult(false);
        }

        public static BlackoutResult cancel() {
            return new BlackoutResult(true);
        }
    }
}
