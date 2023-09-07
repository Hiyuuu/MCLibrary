package com.github.hiyuuu.sender;

import org.bukkit.command.CommandSender;

public class SendUtil {

    private static String prefix = "";

    /**
     * プレフィックスを設定します
     * @param prefix
     */
    public static void setPrefix(String prefix) { SendUtil.prefix = prefix; }

    /**
     * パラメーターメッセージを print します
     * @param message メッセージ表示内容
     * @param prefix 指定した場合は、そのprefixに置き換わります
     */
    public static void print(String message, String prefix) {
        System.out.print(prefix.replaceAll("[&§][a-fk-or0-9]", "") + " " + message.replaceAll("[&§][a-fk-o0-9]", ""));
    }
    public static void print(String message) { print(message, prefix); }

    /**
     * パラメーターメッセージを println します
     * @param message メッセージ表示内容
     * @param prefix 指定した場合は、そのprefixに置き換わります
     */
    public static void println(String message, String prefix) {
        System.out.println(prefix.replaceAll("[&§][a-fk-or0-9]", "") + message.replaceAll("[&§][a-fk-o0-9]", ""));
    }
    public static void println(String message) {
        println(message, prefix);
    }

    /**
     * パラメーターメッセージを sendMessage します
     * @param sender 送信者 (console or player)
     * @param message メッセージ表示内容
     * @param prefix 指定した場合は、そのprefixに置き換わります
     */
    public static void message(CommandSender sender, String message, String prefix) {
        for (String m : message.split("\n")) {
            sender.sendMessage(prefix.replace("&", "§") + m.replace("&", "§"));
        }
    }
    public static void message(CommandSender sender, String message) {
        message(sender, message, prefix);
    }

}