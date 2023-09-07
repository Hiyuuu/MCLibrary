package com.github.hiyuuu.config.events;

import com.github.hiyuuu.config.ConfigUtils;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * コンフィグに改変を加えた際に発生するイベント
 */
public class ConfigSetEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    public ConfigUtils config;

    public ConfigSetEvent(ConfigUtils configUtils) { this.config = configUtils; }

    public static HandlerList getHandlerList() { return handlers; }

    @Override
    public HandlerList getHandlers() { return handlers;}
}