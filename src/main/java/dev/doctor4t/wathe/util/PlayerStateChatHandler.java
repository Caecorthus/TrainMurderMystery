package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PlayerStateChatHandler {

    private PlayerStateChatHandler() {
    }

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!shouldSplitChat(sender)) {
                return true;
            }

            WalkieTalkieChatHandler.handleChatMessage(message, sender, params);
            resendToMatchingState(message, sender, params);
            return false;
        });
    }

    private static boolean shouldSplitChat(ServerPlayerEntity sender) {
        GameWorldComponent game = GameWorldComponent.KEY.get(sender.getWorld());
        if (!game.isRunning() || !game.hasAnyRole(sender.getUuid())) {
            return false;
        }

        return GameFunctions.isPlayerPlayingAndAlive(sender) || game.isPlayerDead(sender.getUuid());
    }

    private static void resendToMatchingState(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
        SentMessage sentMessage = SentMessage.of(message);
        boolean senderAlive = GameFunctions.isPlayerPlayingAndAlive(sender);

        sender.getServer().logChatMessage(
                message.getContent(),
                params,
                message.hasSignature() ? null : "Not Secure"
        );

        for (ServerPlayerEntity receiver : sender.getServer().getPlayerManager().getPlayerList()) {
            if (!isSameChatState(receiver, senderAlive)) {
                continue;
            }

            sentMessage.send(receiver, sender.shouldFilterMessagesSentTo(receiver), params);
        }
    }

    private static boolean isSameChatState(ServerPlayerEntity player, boolean aliveState) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (!game.isRunning() || !game.hasAnyRole(player.getUuid())) {
            return false;
        }

        if (aliveState) {
            return GameFunctions.isPlayerPlayingAndAlive(player);
        }

        return game.isPlayerDead(player.getUuid());
    }
}
