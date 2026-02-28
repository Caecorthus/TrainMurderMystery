package dev.doctor4t.wathe.client.skin;

import dev.doctor4t.wathe.Wathe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ItemSkinTextureManager {
    private static final ItemSkinTextureManager INSTANCE = new ItemSkinTextureManager();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public enum TextureState { LOADING, READY, FAILED }

    private final ConcurrentHashMap<String, TextureState> textureStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Identifier> registeredTextures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ItemSkinQuadGenerator.SkinQuadData> quadCache = new ConcurrentHashMap<>();
    private Path cacheDir;

    private ItemSkinTextureManager() {}

    public static ItemSkinTextureManager getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        Path watheDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("wathe");
        cacheDir = watheDir.resolve("skin_cache");
        try {
            Files.createDirectories(cacheDir);
            invalidateCacheOnVersionChange(watheDir);
        } catch (Exception e) {
            Wathe.LOGGER.warn("[SkinTexture] Failed to create cache directory: {}", e.getMessage());
        }
    }

    private void invalidateCacheOnVersionChange(Path watheDir) {
        Path versionFile = watheDir.resolve(".skin_cache_version");
        String currentVersion = Wathe.MOD_VERSION;
        try {
            if (Files.exists(versionFile)) {
                String cachedVersion = Files.readString(versionFile).trim();
                if (currentVersion.equals(cachedVersion)) {
                    return;
                }
                Wathe.LOGGER.info("[SkinTexture] Mod version changed ({} -> {}), clearing skin cache",
                        cachedVersion, currentVersion);
            } else {
                Wathe.LOGGER.info("[SkinTexture] First run, initializing skin cache version marker");
            }

            // Delete all cached skin files
            try (var files = Files.list(cacheDir)) {
                files.filter(p -> p.toString().endsWith(".png")).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (Exception e) {
                        Wathe.LOGGER.warn("[SkinTexture] Failed to delete cached file {}: {}", p.getFileName(), e.getMessage());
                    }
                });
            }

            Files.writeString(versionFile, currentVersion);
        } catch (Exception e) {
            Wathe.LOGGER.warn("[SkinTexture] Failed to check/update skin cache version: {}", e.getMessage());
        }
    }

    public TextureState getState(String textureUrl) {
        return textureStates.getOrDefault(textureUrl, TextureState.LOADING);
    }

    public @Nullable Identifier getTextureId(String textureUrl) {
        if (textureStates.get(textureUrl) != TextureState.READY) return null;
        return registeredTextures.get(textureUrl);
    }

    public void ensureLoaded(String textureUrl) {
        if (textureStates.containsKey(textureUrl)) return;
        textureStates.put(textureUrl, TextureState.LOADING);

        CompletableFuture.supplyAsync(() -> {
            try {
                String hash = sha256(textureUrl);
                Path cachedFile = cacheDir.resolve(hash + ".png");

                if (Files.exists(cachedFile)) {
                    return Files.readAllBytes(cachedFile);
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(textureUrl))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    throw new RuntimeException("HTTP " + response.statusCode());
                }

                byte[] bytes = response.body();
                Files.write(cachedFile, bytes);
                return bytes;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, Util.getMainWorkerExecutor()).thenAcceptAsync(bytes -> {
            try {
                InputStream stream = new ByteArrayInputStream(bytes);
                NativeImage image = NativeImage.read(stream);

                // Generate quads from the skin image before creating the GPU texture
                Sprite dummySprite = MinecraftClient.getInstance()
                        .getBakedModelManager().getMissingModel().getParticleSprite();
                ItemSkinQuadGenerator.SkinQuadData quads = ItemSkinQuadGenerator.generate(image, dummySprite);
                quadCache.put(textureUrl, quads);

                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                Identifier texId = Wathe.id("skins/" + sha256(textureUrl));

                MinecraftClient.getInstance().getTextureManager().registerTexture(texId, texture);
                registeredTextures.put(textureUrl, texId);
                textureStates.put(textureUrl, TextureState.READY);
            } catch (Exception e) {
                Wathe.LOGGER.warn("[SkinTexture] Failed to register texture for {}: {}", textureUrl, e.getMessage());
                textureStates.put(textureUrl, TextureState.FAILED);
            }
        }, MinecraftClient.getInstance()).exceptionally(e -> {
            Wathe.LOGGER.warn("[SkinTexture] Failed to download {}: {}", textureUrl, e.getMessage());
            textureStates.put(textureUrl, TextureState.FAILED);
            return null;
        });
    }

    public @Nullable ItemSkinQuadGenerator.SkinQuadData getQuads(String textureUrl) {
        return quadCache.get(textureUrl);
    }

    public void clearAll() {
        for (Identifier texId : registeredTextures.values()) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(texId);
        }
        registeredTextures.clear();
        textureStates.clear();
        quadCache.clear();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
