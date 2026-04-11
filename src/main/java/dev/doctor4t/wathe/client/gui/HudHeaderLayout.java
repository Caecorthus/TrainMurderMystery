package dev.doctor4t.wathe.client.gui;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;

public record HudHeaderLayout(
    boolean showTime,
    int playerCount,
    int matchCountTopY,
    int matchCountBottomY,
    int broadcastTopY
) {
    private static final int HEADER_TOP_Y = 6;
    private static final int MATCH_COUNT_BELOW_TIME_Y = 18;
    private static final int BROADCAST_DEFAULT_TOP_Y = 20;
    private static final int BROADCAST_GAP_AFTER_HEADER = 6;

    public static HudHeaderLayout compute(@NotNull TextRenderer renderer, @NotNull ClientPlayerEntity player) {
        GameWorldComponent gwc = GameWorldComponent.KEY.get(player.getWorld());
        boolean running = gwc.isRunning();

        Role role = gwc.getRole(player);
        boolean showTime = running && ((role != null && role.canSeeTime()) || WatheClient.canSeeSpectatorInformation());

        int playerCount = running ? gwc.getAllPlayers().size() : 0;

        int matchCountTopY = showTime ? MATCH_COUNT_BELOW_TIME_Y : HEADER_TOP_Y;
        int matchCountBottomY = matchCountTopY + renderer.fontHeight;

        int broadcastTopY = playerCount > 0
            ? matchCountBottomY + BROADCAST_GAP_AFTER_HEADER
            : BROADCAST_DEFAULT_TOP_Y;

        return new HudHeaderLayout(showTime, playerCount, matchCountTopY, matchCountBottomY, broadcastTopY);
    }

    public boolean showMatchCount() {
        return playerCount > 0;
    }
}
