package com.github.hiyuuu.event;

import com.github.hiyuuu.config.ConfigUtils;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * コンフィグを保存した際に発生するイベント
 */
public class ConfigSaveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    public ConfigUtils config;

    public ConfigSaveEvent(ConfigUtils configUtils) { this.config = configUtils; }

    public static HandlerList getHandlerList() { return handlers; }

    @Override
    public HandlerList getHandlers() { return handlers; }
}