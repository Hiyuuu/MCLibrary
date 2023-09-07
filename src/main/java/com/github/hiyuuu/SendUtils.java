package com.github.hiyuuu;

import com.github.hiyuuu.config.ConfigUtils;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Function;

public class SendUtils {
    
    private String prefix = "";
    private ConfigUtils configUtils;

    /**
     * 本クラスを使用する前に一度呼び出してください
     * @param prefix
     * @param messageConfig
     */
    public SendUtils(String prefix, ConfigUtils messageConfig) {
            this.prefix = prefix;
            this.configUtils = messageConfig;
    }

    /**
     * プレフィックスをコンフィグから取得
     * 取得できなかった場合は、初期化時に設定したプレフィックスを使用
     * @return prefix
     */
    public String prefix() {
            String getPrefix = configUtils.getString("prefix");
            return getPrefix != null ? getPrefix : prefix;
    }

    /**
     * コンフィグからprefixを取得し、パラメーターメッセージとともに print します
     * @param message メッセージ表示内容
     * @param prefix 指定した場合は、そのprefixに置き換わります
     */
    public void print(String message, String prefix) {
        System.out.print(prefix.replaceAll("[&§][a-fk-or0-9]", "") + " " + message.replaceAll("[&§][a-fk-o0-9]", ""));
    }
    public void print(String message) { print(message, prefix()); }

    /**
     * コンフィグからprefix及びメッセージキーに対応するメッセージを取得し、print します
     * @param messageKey メッセージキー
     * @param prefix 指定した場合は、そのprefixに置き換わります
     */
    public void configPrint(String messageKey, Function<String, String> lamdaCallBack, String prefix) {
        String message = configUtils.getString(messageKey);
        if (message == null) { return; }
        print(lamdaCallBack.apply(message), prefix);
    }
    public void configPrint(String messageKey, Function<String, String> lamdaCallBack) {
        configPrintln(messageKey, lamdaCallBack, prefix());
    }
    public void configPrint(String messageKey) {
        configPrintln(messageKey, l -> l, prefix());
    }

    /**
     * コンフィグからprefixを取得し、パラメーターメッセージとともに println します
     * @param message メッセージ表示内容
     * @param prefix 指定した場合は、そのprefixに置き換わります
     */
    public void println(String message, String prefix) {
        System.out.println(prefix.replaceAll("[&§][a-fk-or0-9]", "") + message.replaceAll("[&§][a-fk-o0-9]", ""));
    }
    public void println(String message) {
        println(message, prefix());
    }

    /**
     * コンフィグからprefix及びメッセージキーに対応するメッセージを取得し、println します
     * @param messageKey メッセージキー
     * @param prefix 指定した場合は、そのprefixに置き換わります
     */
    public void configPrintln(String messageKey, Function<String, String> lamdaCallBack, String prefix) {
        String message = configUtils.getString(messageKey);
        if (message == null) { return; }
        println(lamdaCallBack.apply(message), prefix);
    }
    public void configPrintln(String messageKey, Function<String, String> lamdaCallBack) {
        configPrintln(messageKey, lamdaCallBack, prefix());
    }
    public void configPrintln(String messageKey) {
        configPrintln(messageKey, l -> l, prefix());
    }

    /**
     * コンフィグからprefixを取得し、パラメーターメッセージとともに sendMessage します
     * @param sender 送信者 (console or player)
     * @param message メッセージ表示内容
     * @param prefix 指定した場合は、そのprefixに置き換わります
     */
    public void message(CommandSender sender, String message, String prefix) {
        for (String m : message.split("\n")) {
            sender.sendMessage(prefix.replace("&", "§") + m.replace("&", "§"));
        }
    }
    public void message(CommandSender sender, String message) {
        message(sender, message, prefix());
    }

    /**
     * コンフィグからprefix及びメッセージキーに対応するメッセージを取得し、sendMessage します
     * @param sender 送信者 (console or player)
     * @param messageKey メッセージキー
     * @param lamdaCallBack ラムダ処理
     * @param prefix 指定した場合は、そのprefixに置き換わります
     */
    public void configMessage(CommandSender sender, String messageKey, Function<String, String> lamdaCallBack, String prefix) {
        String message = configUtils.getString(messageKey);
        if (message == null) { return; }
        message(sender, lamdaCallBack.apply(message), prefix);
    }

    /**
     * コンフィグからprefix及びメッセージキーに対応するメッセージを取得し、sendMessage します
     * @param sender 送信者 (console or player)
     * @param messageKey メッセージキー
     * @param lamdaCallBack ラムダ処理
     */
    public void configMessage(CommandSender sender, String messageKey, Function<String, String> lamdaCallBack) {
        configMessage(sender, messageKey, lamdaCallBack, prefix());
    }

    /**
     * コンフィグからprefix及びメッセージキーに対応するメッセージを取得し、sendMessage します
     * @param sender 送信者 (console or player)
     * @param messageKey メッセージキー
     */
    public void configMessage(CommandSender sender, String messageKey) {
        configMessage(sender, messageKey, l -> l, prefix());
    }

    /**
     * コンフィグからサウンドキーに対応するサウンド情報を取得し、playSound します
     * @param player 再生するプレイヤー
     * @param soundKey サウンドするキー
     * @param volumeKey 音量キー
     * @param pitchKey ピッチキー
     */
    public void configSound(Player player, String soundKey, String volumeKey, String pitchKey) {
        String soundString = configUtils.getString(soundKey);
        double volume = configUtils.getDouble(volumeKey, 1.0);
        double pitch = configUtils.getDouble(pitchKey, 1.0);

        Sound sound = null;
        try {
            sound = Sound.valueOf(soundString.toUpperCase());
        } catch (Exception ignored) {}
        if (sound != null) {
            player.playSound(player.getLocation(), sound, (float) volume, (float) pitch);
        }
    }
        
}