package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 门状态变化事件。
 * 在门被撬开或被堵住时触发，供附属模组监听门状态变化。
 *
 * <p>事件类型：</p>
 * <ul>
 *   <li>{@link #BLAST} — 门被撬开时触发（在 {@link DoorBlockEntity#blast()} 末尾调用）</li>
 *   <li>{@link #JAM} — 门被堵住时触发（在 {@link DoorBlockEntity#jam()} 末尾调用）</li>
 * </ul>
 *
 * <p>仅在服务端触发。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * DoorStateChanged.BLAST.register((world, pos, entity) -> {
 *     // 门被撬开时的处理逻辑
 * });
 *
 * DoorStateChanged.JAM.register((world, pos, entity) -> {
 *     // 门被堵住时的处理逻辑
 * });
 * }</pre>
 */
public interface DoorStateChanged {

    /**
     * 门被撬开事件。
     * 在 {@link DoorBlockEntity#blast()} 执行完毕后触发。
     */
    Event<DoorStateChanged> BLAST = createArrayBacked(
            DoorStateChanged.class,
            listeners -> (world, pos, entity) -> {
                for (DoorStateChanged listener : listeners) {
                    listener.onStateChanged(world, pos, entity);
                }
            }
    );

    /**
     * 门被堵住事件。
     * 在 {@link DoorBlockEntity#jam()} 执行完毕后触发。
     */
    Event<DoorStateChanged> JAM = createArrayBacked(
            DoorStateChanged.class,
            listeners -> (world, pos, entity) -> {
                for (DoorStateChanged listener : listeners) {
                    listener.onStateChanged(world, pos, entity);
                }
            }
    );

    /**
     * 处理门状态变化事件。
     *
     * @param world  门所在的世界
     * @param pos    门方块的位置（下半部分）
     * @param entity 门方块实体
     */
    void onStateChanged(World world, BlockPos pos, DoorBlockEntity entity);
}
