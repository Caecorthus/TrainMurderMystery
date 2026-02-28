package dev.doctor4t.wathe.client.skin;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import java.util.*;

/**
 * Generates BakedQuads from a NativeImage skin texture, replicating
 * Minecraft's ItemModelGenerator pixel-scanning algorithm.
 * This ensures side quads match the skin's actual transparent/opaque pixel boundaries.
 */
public final class ItemSkinQuadGenerator {

    private static final float Z_FRONT = 8.5f / 16f;
    private static final float Z_BACK = 7.5f / 16f;

    public record SkinQuadData(
            Map<Direction, List<BakedQuad>> faceQuads,
            List<BakedQuad> unculledQuads
    ) {
        public List<BakedQuad> getQuads(Direction face) {
            if (face == null) return unculledQuads;
            return faceQuads.getOrDefault(face, List.of());
        }
    }

    private ItemSkinQuadGenerator() {}

    public static SkinQuadData generate(NativeImage image, Sprite dummySprite) {
        int w = image.getWidth();
        int h = image.getHeight();

        Map<Direction, List<BakedQuad>> faceQuads = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            faceQuads.put(dir, new ArrayList<>());
        }
        List<BakedQuad> unculledQuads = new ArrayList<>();

        // Front face (SOUTH) and back face (NORTH) — always full-size
        unculledQuads.add(createFrontFace(dummySprite));
        unculledQuads.add(createBackFace(dummySprite));

        // Side quads from pixel edge detection
        List<Frame> frames = getFrames(image, w, h);
        for (Frame frame : frames) {
            BakedQuad quad = createSideQuad(frame, w, h, dummySprite);
            faceQuads.get(frame.side.direction).add(quad);
        }

        return new SkinQuadData(
                Collections.unmodifiableMap(faceQuads),
                Collections.unmodifiableList(unculledQuads)
        );
    }

    // ── Front / Back faces ──────────────────────────────────────────────

    /**
     * SOUTH face at z=8.5/16. CubeFace.SOUTH winding:
     *   v0: (x_min, y_max, z) uv(0, 0)
     *   v1: (x_min, y_min, z) uv(0, 1)
     *   v2: (x_max, y_min, z) uv(1, 1)
     *   v3: (x_max, y_max, z) uv(1, 0)
     */
    private static BakedQuad createFrontFace(Sprite sprite) {
        int[] data = new int[32];
        putVertex(data, 0, 0f, 1f, Z_FRONT, 0f, 0f);
        putVertex(data, 1, 0f, 0f, Z_FRONT, 0f, 1f);
        putVertex(data, 2, 1f, 0f, Z_FRONT, 1f, 1f);
        putVertex(data, 3, 1f, 1f, Z_FRONT, 1f, 0f);
        return new BakedQuad(data, -1, Direction.SOUTH, sprite, true);
    }

    /**
     * NORTH face at z=7.5/16. CubeFace.NORTH winding:
     *   v0: (x_max, y_max, z) uv(1, 0)
     *   v1: (x_max, y_min, z) uv(1, 1)
     *   v2: (x_min, y_min, z) uv(0, 1)
     *   v3: (x_min, y_max, z) uv(0, 0)
     */
    private static BakedQuad createBackFace(Sprite sprite) {
        int[] data = new int[32];
        putVertex(data, 0, 1f, 1f, Z_BACK, 1f, 0f);
        putVertex(data, 1, 1f, 0f, Z_BACK, 1f, 1f);
        putVertex(data, 2, 0f, 0f, Z_BACK, 0f, 1f);
        putVertex(data, 3, 0f, 1f, Z_BACK, 0f, 0f);
        return new BakedQuad(data, -1, Direction.NORTH, sprite, true);
    }

    // ── Side quad generation ────────────────────────────────────────────

    /**
     * Creates a thin side quad for an edge frame, replicating
     * ItemModelGenerator.addSubComponents coordinate logic.
     */
    private static BakedQuad createSideQuad(Frame frame, int texW, int texH, Sprite sprite) {
        float px = 16f / texW;   // pixel-to-model-unit scale X
        float py = 16f / texH;   // pixel-to-model-unit scale Y
        float r = frame.min;     // run start (along edge direction)
        float s = frame.max;     // run end
        float t = frame.level;   // depth perpendicular to run

        // ModelElement coordinates (0-16 model space)
        float fromX, fromY, toX, toY;
        // UV coordinates in 0-16 space (before normalization to texture dimensions)
        float uvMinU, uvMinV, uvMaxU, uvMaxV;

        switch (frame.side) {
            case UP -> {
                fromX = r * px;
                toX = (s + 1) * px;
                fromY = 16f - t * py;
                toY = fromY;                    // flat
                uvMinU = r;  uvMinV = t;
                uvMaxU = s + 1;  uvMaxV = t + 1;
            }
            case DOWN -> {
                fromX = r * px;
                toX = (s + 1) * px;
                fromY = 16f - (t + 1) * py;
                toY = fromY;                    // flat
                uvMinU = r;  uvMinV = t;
                uvMaxU = s + 1;  uvMaxV = t + 1;
            }
            case LEFT -> {
                fromX = t * px;
                toX = fromX;                    // flat
                fromY = 16f - r * py;
                toY = 16f - (s + 1) * py;
                uvMinU = t;  uvMinV = r;
                uvMaxU = t + 1;  uvMaxV = s + 1;
            }
            case RIGHT -> {
                fromX = (t + 1) * px;
                toX = fromX;                    // flat
                fromY = 16f - r * py;
                toY = 16f - (s + 1) * py;
                uvMinU = t;  uvMinV = r;
                uvMaxU = t + 1;  uvMaxV = s + 1;
            }
            default -> throw new IllegalStateException();
        }

        // Convert to 0-1 model space
        float x0 = fromX / 16f, x1 = toX / 16f;
        float y0 = fromY / 16f, y1 = toY / 16f;

        // Normalize UV to 0-1 texture space
        float u0 = uvMinU / texW, v0 = uvMinV / texH;
        float u1 = uvMaxU / texW, v1 = uvMaxV / texH;

        Direction dir = frame.side.direction;
        int[] data = new int[32];

        switch (frame.side) {
            case UP -> {
                // CubeFace.UP: (x_min,y,z_min), (x_min,y,z_max), (x_max,y,z_max), (x_max,y,z_min)
                putVertex(data, 0, x0, y0, Z_BACK,  u0, v0);
                putVertex(data, 1, x0, y0, Z_FRONT, u0, v1);
                putVertex(data, 2, x1, y0, Z_FRONT, u1, v1);
                putVertex(data, 3, x1, y0, Z_BACK,  u1, v0);
            }
            case DOWN -> {
                // CubeFace.DOWN: (x_min,y,z_max), (x_min,y,z_min), (x_max,y,z_min), (x_max,y,z_max)
                putVertex(data, 0, x0, y0, Z_FRONT, u0, v0);
                putVertex(data, 1, x0, y0, Z_BACK,  u0, v1);
                putVertex(data, 2, x1, y0, Z_BACK,  u1, v1);
                putVertex(data, 3, x1, y0, Z_FRONT, u1, v0);
            }
            case LEFT -> {
                // Direction.EAST → CubeFace.EAST:
                // (x,y_max,z_max), (x,y_min,z_max), (x,y_min,z_min), (x,y_max,z_min)
                // Here y0=fromY (larger, top), y1=toY (smaller, bottom)
                putVertex(data, 0, x0, y0, Z_FRONT, u0, v0);
                putVertex(data, 1, x0, y1, Z_FRONT, u0, v1);
                putVertex(data, 2, x0, y1, Z_BACK,  u1, v1);
                putVertex(data, 3, x0, y0, Z_BACK,  u1, v0);
            }
            case RIGHT -> {
                // Direction.WEST → CubeFace.WEST:
                // (x,y_max,z_min), (x,y_min,z_min), (x,y_min,z_max), (x,y_max,z_max)
                putVertex(data, 0, x0, y0, Z_BACK,  u0, v0);
                putVertex(data, 1, x0, y1, Z_BACK,  u0, v1);
                putVertex(data, 2, x0, y1, Z_FRONT, u1, v1);
                putVertex(data, 3, x0, y0, Z_FRONT, u1, v0);
            }
        }

        return new BakedQuad(data, -1, dir, sprite, true);
    }

    // ── Vertex packing ──────────────────────────────────────────────────

    private static void putVertex(int[] data, int index, float x, float y, float z, float u, float v) {
        int i = index * 8;
        data[i]     = Float.floatToRawIntBits(x);
        data[i + 1] = Float.floatToRawIntBits(y);
        data[i + 2] = Float.floatToRawIntBits(z);
        data[i + 3] = -1;  // color: white (0xFFFFFFFF)
        data[i + 4] = Float.floatToRawIntBits(u);
        data[i + 5] = Float.floatToRawIntBits(v);
        data[i + 6] = 0;
        data[i + 7] = 0;
    }

    // ── Pixel edge detection (ported from ItemModelGenerator) ───────────

    private static List<Frame> getFrames(NativeImage image, int w, int h) {
        List<Frame> frames = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean opaque = !isTransparent(image, x, y, w, h);
                for (Side side : Side.values()) {
                    if (opaque && isTransparent(image, x + side.offsetX, y + side.offsetY, w, h)) {
                        addToFrames(frames, side, x, y);
                    }
                }
            }
        }
        return frames;
    }

    private static void addToFrames(List<Frame> frames, Side side, int x, int y) {
        int level = side.isVertical() ? y : x;
        int pos = side.isVertical() ? x : y;

        for (Frame f : frames) {
            if (f.side == side && f.level == level) {
                f.expand(pos);
                return;
            }
        }
        frames.add(new Frame(side, pos, level));
    }

    private static boolean isTransparent(NativeImage image, int x, int y, int w, int h) {
        if (x < 0 || y < 0 || x >= w || y >= h) return true;
        int color = image.getColor(x, y);
        int alpha = (color >>> 24) & 0xFF;
        return alpha == 0;
    }

    // ── Inner types ─────────────────────────────────────────────────────

    private static final class Frame {
        final Side side;
        int min;
        int max;
        final int level;

        Frame(Side side, int pos, int level) {
            this.side = side;
            this.min = pos;
            this.max = pos;
            this.level = level;
        }

        void expand(int value) {
            if (value < min) min = value;
            else if (value > max) max = value;
        }
    }

    enum Side {
        UP(Direction.UP, 0, -1),
        DOWN(Direction.DOWN, 0, 1),
        LEFT(Direction.EAST, -1, 0),
        RIGHT(Direction.WEST, 1, 0);

        final Direction direction;
        final int offsetX;
        final int offsetY;

        Side(Direction direction, int offsetX, int offsetY) {
            this.direction = direction;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        boolean isVertical() {
            return this == UP || this == DOWN;
        }
    }
}
