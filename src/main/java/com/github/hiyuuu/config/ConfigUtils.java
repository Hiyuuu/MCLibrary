package com.github.hiyuuu.config;

import com.github.hiyuuu.config.events.ConfigReloadEvent;
import com.github.hiyuuu.config.events.ConfigSaveDefaultEvent;
import com.github.hiyuuu.config.events.ConfigSaveEvent;
import com.github.hiyuuu.config.events.ConfigSetEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * 自動再読み込みに対応したコンフィグユーティリティ
 */
public class ConfigUtils extends YamlConfiguration {

    private final Plugin plugin;
    private File configFile;
    private final boolean isAutoReload;
    private long fileModifiedHistory;
    public boolean isLoaded = false;

    /**
     * コンフィグユーティリティ
     * @param plugin プラグイン
     * @param filePath 読み込みコンフィグファイルを指定します。
     * / 又は \ 等を含まない場合、プラグインのデータディレクトリ内へ自動的にスコープします。
     * @param isAutoReload 外部からファイルをエディター等で保存した場合に、即座に自動読み込みを行うか否か
     */
    public ConfigUtils(Plugin plugin, String filePath, boolean isAutoReload) throws IOException, InvalidConfigurationException {

        this.plugin = plugin;
        this.isAutoReload = isAutoReload;

        // ファイルパスを生成
        String path = plugin
                .getDataFolder()
                .getAbsolutePath()
                + File.separator
                + filePath.replaceAll("\\|/", File.separator);

        // ファイルを変数へ代入
        configFile = new File(path);

        // デフォルトコンフィグの配置
        saveDefaultConfig();

        // 存在しない場合、ファイルを初期化
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();
        }

        // コンフィグをロード
        try {
            this.load(configFile);
        } catch (Exception e) {
            System.out.println(filePath + " ファイルのロードに失敗しました。原因: " + e.toString());
            return;
        }

        // デフォルトセクションの設定
        saveDefaultSection();

        // ロード完了フラグ
        isLoaded = true;

        // 自動リロード
        if (!isAutoReload) return;

        // 自動リロード用ハンドル
        ConfigUtils configUtils = this;
        new BukkitRunnable() {
            @Override
            public void run() {

                long lastModified = configFile.lastModified();
                if (fileModifiedHistory != lastModified) {
                    try {
                        reloadConfig();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Bukkit.getPluginManager().callEvent(new ConfigReloadEvent(configUtils));
                        });
                    } catch (IOException | InvalidConfigurationException ignored) {}
                }
            }

        }.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }

    /**
     * ファイルを取得
     * @return File
     */
    public File getFile() { return this.configFile; }

    @Override
    public void set(String path, @Nullable Object value) {
        super.set(path, value);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getPluginManager().callEvent(new ConfigSetEvent(this));
        });
    }

    /**
     * 変更内容を保存します
     * @throws IOException
     */
    public ConfigUtils saveConfig() throws IOException {
        this.save(configFile);
        ConfigUtils configUtils = this;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getPluginManager().callEvent(new ConfigSaveEvent(configUtils));
        });
        return this;
    }

    /**
     * 自動リロード用処理を実装した、Resourcesのコンフィグファイル出力メソッド
     */
    public void saveDefaultConfig() {
        if (configFile.exists()) return;
        try {
            plugin.saveResource(configFile.getName(), false);
            resetFileModifiedHistory();
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getPluginManager().callEvent(new ConfigSaveDefaultEvent(this));
            });
        } catch (IllegalArgumentException ignored) {}
    }

    /**
     * 設定されていないセクションを新たに追加
     * @throws IOException
     */
    public void saveDefaultSection() throws IOException {

        // resourcesのファイル読み取り
        InputStream is = plugin.getResource(configFile.getName());
        if (is == null) return;

        // YamlConfiguration を生成
        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        YamlConfiguration yamlConf = YamlConfiguration.loadConfiguration(isr);
        Set<String> keys = yamlConf.getKeys(true);

        // 全キーを処理
        keys.forEach(k -> {
            List<String> comments = yamlConf.getComments(k);
            List<String> inlineComments = yamlConf.getInlineComments(k);
            Object obj = yamlConf.get(k);

            // resources ファイルとの差分を抽出
            if (!isConfigurationSection(k) && !isSet(k)) {
                this.setComments(k, comments);
                this.set(k, obj);
                this.setInlineComments(k, inlineComments);
            }
        });

        // 保存
        this.saveConfig();
    }

    /**
     * 自動リロード用処理を実装したコンフィグ再読み込みメソッド
     *
     * @throws IOException
     * @throws InvalidConfigurationException
     */
    public void reloadConfig() throws IOException, InvalidConfigurationException {
        this.load(configFile);
        resetFileModifiedHistory();
    }

    /**
     * 自動リロード用処理を実装した保存メソッド
     * @param file
     * @throws IOException
     */
    @Override
    public void save(File file) throws IOException {
        super.save(file);
        resetFileModifiedHistory();
    }

    /**
     * 自動リロード用処理を実装した保存メソッド
     * @param file
     * @throws IOException
     */
    @Override
    public void save(String file) throws IOException {
        super.save(file);
        resetFileModifiedHistory();
    }

    /**
     * ファイルロードメソッドの実装
     * @param file File to load from.
     * @throws IOException
     * @throws InvalidConfigurationException
     */
    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {

        // コンフィグファイルを設定
        resetFileModifiedHistory();
        this.configFile = file;

        // コンフィグをロード
        try {
            super.load(configFile);
        } catch (Exception e) {
            System.out.println(file.getName() + " ファイルのロードに失敗しました。原因: " + e);
        }
    }

    /**
     * ファイルロードメソッドの実装
     * @param filePath File to load from.
     * @throws IOException
     * @throws InvalidConfigurationException
     */
    @Override
    public void load(@NotNull String filePath) throws IOException, InvalidConfigurationException {

        // ファイルパスを生成
        String path = plugin
                .getDataFolder()
                .getAbsolutePath()
                + File.separator
                + filePath.replaceAll("\\|/", File.separator);

        // ファイルを変数へ代入
        File file = new File(path);

        // 読み込み
        this.load(file);
    }

    /**
     * ファイル更新履歴をリセット
     */
    public void resetFileModifiedHistory() {
        fileModifiedHistory = configFile.lastModified();
    }

}