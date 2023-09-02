package com.github.hiyuuu;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.function.Consumer;

/**
 * Bukkitスレッドを簡易的に提供する
 */
public class ThreadUtils {

    private static Plugin plugin;

    /**
     * 使用する前に一度呼び出してください
     * @param plugin
     */
    public static void onEnable(Plugin plugin) { ThreadUtils.plugin = plugin; }

    /**
     * 同期処理
     * @param runnable
     * @return bukkitTask
     */
    public static BukkitTask Sync(Consumer<BukkitRunnable> runnable) {
        return new BukkitRunnable()  {
            @Override public void run() { runnable.accept(this); }
        }.runTask(plugin);
    }

    /**
     * 遅延ありの同期処理
     * @param delay
     * @param runnable
     * @return bukkitTask
     */
    public static BukkitTask Sync(Integer delay, Consumer<BukkitRunnable> runnable) {
        return new BukkitRunnable()  {
            @Override public void run() { runnable.accept(this); }
        }.runTaskLater(plugin, Long.valueOf(delay));
    }

    /**
     * 同期タイマー
     * @param interval
     * @param runnable
     * @return bukkitTask
     */
    public static BukkitTask SyncTimer(int interval, Consumer<BukkitRunnable> runnable) {
        return new BukkitRunnable() {
            @Override public void run() { runnable.accept(this); }
        }.runTaskTimer(plugin, 0, interval);
    }

    /**
     * 非同期処理
     * @param runnable
     * @return bukkitTask
     */
    public static BukkitTask Async(Consumer<BukkitRunnable> runnable) {
        return new BukkitRunnable()  {
            @Override public void run() { runnable.accept(this); }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * 遅延ありの非同期処理
     * @param delay
     * @param runnable
     * @return bukkiTask
     */
    public static BukkitTask Async(int delay, Consumer<BukkitRunnable> runnable) {
        return new BukkitRunnable()  {
            @Override public void run() { runnable.accept(this); }
        }.runTaskLaterAsynchronously(plugin, (long) delay);
    }

    /**
     * 非同期タイマー
     * @param interval
     * @param runnable
     * @return bukkitTask
     */
    public static BukkitTask AsyncTimer(int interval, Consumer<BukkitRunnable> runnable) {
        return new BukkitRunnable() {
            @Override public void run() { runnable.accept(this); }
        }.runTaskTimerAsynchronously(plugin, 0, interval);
    }

}