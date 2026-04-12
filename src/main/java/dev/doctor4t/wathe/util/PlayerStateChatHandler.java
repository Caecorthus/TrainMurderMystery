package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PlayerStateChatHandler {

    private PlayerStateChatHandler() {
    }

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!shouldSplitChat(sender)) {
                return true;
            }

            // 对讲机广播优先于普通聊天分发
            WalkieTalkieChatHandler.handleChatMessage(message, sender, params);
            resendToMatchingState(message, sender, params);
            return false;
        });
    }

    private static boolean shouldSplitChat(ServerPlayerEntity sender) {
        // 只有在游戏运行中并且发送者持有角色时，才按存活/死亡状态分流聊天
        GameWorldComponent game = GameWorldComponent.KEY.get(sender.getWorld());
        return game.isRunning() && game.hasAnyRole(sender.getUuid());
    }

    private static void resendToMatchingState(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
        // GameWorldComponent 只按 sender 所在世界查询一次，避免循环中每个接收者重复取组件
        GameWorldComponent game = GameWorldComponent.KEY.get(sender.getWorld());
        boolean senderDead = game.isPlayerDead(sender.getUuid());

        SentMessage sentMessage = SentMessage.of(message);
        MinecraftServer server = sender.getServer();
        if (server == null) return;

        // 由于服务器使用 No Chat Report，消息签名会被剥离，hasSignature() 基本恒为 false，
        // 所有消息都会被标记为 "Not Secure"——这是 No Chat Report 的预期行为。
        server.logChatMessage(
                message.getContent(),
                params,
                message.hasSignature() ? null : "Not Secure"
        );

        for (ServerPlayerEntity receiver : server.getPlayerManager().getPlayerList()) {
            // 跨维度保护：不同世界的玩家不参与此次分发
            if (receiver.getWorld() != sender.getWorld()) {
                continue;
            }

            // 接收者必须持有角色，且与发送者处于同一存活/死亡状态
            if (!game.hasAnyRole(receiver.getUuid())) {
                continue;
            }
            if (game.isPlayerDead(receiver.getUuid()) != senderDead) {
                continue;
            }

            sentMessage.send(receiver, sender.shouldFilterMessagesSentTo(receiver), params);
        }
    }
}
