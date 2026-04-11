package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.item.WalkieTalkieItem;
import dev.doctor4t.wathe.item.component.WalkieTalkieComponent;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class WalkieTalkieChatHandler {

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register(WalkieTalkieChatHandler::handleChatMessage);
    }

    public static void handleChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
        ItemStack mainHandStack = sender.getMainHandStack();
        if (!(mainHandStack.getItem() instanceof WalkieTalkieItem)) {
            return;
        }

        WalkieTalkieComponent component = mainHandStack.getOrDefault(
                WatheDataComponentTypes.WALKIE_TALKIE,
                WalkieTalkieComponent.DEFAULT
        );
        int senderChannel = component.channel();
        String chatMessage = message.getContent().getString();

        sender.getServerWorld().playSound(
                null,
                sender.getX(),
                sender.getY(),
                sender.getZ(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
        );

        for (ServerPlayerEntity receiver : sender.getServer().getPlayerManager().getPlayerList()) {
            if (!TrainVoicePlugin.isReceivingChannel(receiver, senderChannel)) {
                continue;
            }

            ServerPlayNetworking.send(receiver, new WalkieTalkieBroadcastPayload(senderChannel, chatMessage));
        }
    }
}
