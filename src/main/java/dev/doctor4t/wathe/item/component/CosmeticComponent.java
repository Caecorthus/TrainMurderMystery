package dev.doctor4t.wathe.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * @param displayName Minecraft Raw JSON Text, parsed via {@link net.minecraft.text.Text.Serialization#fromJson}
 */
public record CosmeticComponent(String cosmeticId, String displayName, String rarity, String textureUrl) {
    public static final Codec<CosmeticComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("cosmeticId").forGetter(CosmeticComponent::cosmeticId),
            Codec.STRING.fieldOf("displayName").forGetter(CosmeticComponent::displayName),
            Codec.STRING.fieldOf("rarity").forGetter(CosmeticComponent::rarity),
            Codec.STRING.fieldOf("textureUrl").forGetter(CosmeticComponent::textureUrl)
    ).apply(instance, CosmeticComponent::new));

    public static final PacketCodec<PacketByteBuf, CosmeticComponent> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, CosmeticComponent::cosmeticId,
            PacketCodecs.STRING, CosmeticComponent::displayName,
            PacketCodecs.STRING, CosmeticComponent::rarity,
            PacketCodecs.STRING, CosmeticComponent::textureUrl,
            CosmeticComponent::new
    );
}
