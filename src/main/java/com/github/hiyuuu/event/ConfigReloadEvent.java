package com.github.hiyuuu.event;

import com.github.hiyuuu.config.ConfigUtils;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * コンフィグをリロードした際に発生するイベント
 */
public final class ConfigReloadEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    public ConfigUtils config;

    public ConfigReloadEvent(ConfigUtils configUtils) { this.config = configUtils; }

    public static HandlerList getHandlerList() { return handlers; }

    @Override
    public HandlerList getHandlers() { return handlers;}
}