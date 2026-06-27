package levosilimo.everlastingskins;

import levosilimo.everlastingskins.skinchanger.SkinRestorer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mojang.authlib.properties.Property;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(modid = EverlastingSkins.MOD_ID, name = EverlastingSkins.MOD_NAME, version = EverlastingSkins.VERSION, acceptableRemoteVersions = "*")
public class EverlastingSkins {
    public static final String MOD_ID = "everlastingskins";
    public static final String MOD_NAME = "Everlasting Skins";
    public static final String VERSION = "4.1.0";

    public static final Logger logger = LogManager.getLogger("EverlastingSkins");
    public static final ExecutorService skinCommandExecutor = Executors.newCachedThreadPool();

    private static String dbUrl;

    public EverlastingSkins() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new SkinRestorer());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        try {
            File dbFile = new File("everlasting_skins.db");
            dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            Class.forName("org.sqlite.JDBC");

            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement stmt = conn.createStatement()) {

                stmt.execute("CREATE TABLE IF NOT EXISTS cached_skins (username TEXT PRIMARY KEY, value TEXT, signature TEXT);");
                stmt.execute("CREATE TABLE IF NOT EXISTS player_skins (uuid TEXT PRIMARY KEY, target_username TEXT);");
            }
        } catch (Exception e) {
            logger.error("Критическая ошибка запуска базы данных SQLite!", e);
        }
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        SkinRestorer.init(event);
    }

    public static String getStoredSkinName(UUID playerUUID) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement("SELECT target_username FROM player_skins WHERE uuid = ?;")) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("target_username");
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static Property getCachedSkinByUUID(UUID playerUUID, String currentName) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String targetName = currentName;
            String queryPlayer = "SELECT target_username FROM player_skins WHERE uuid = ?;";

            try (PreparedStatement pstmt = conn.prepareStatement(queryPlayer)) {
                pstmt.setString(1, playerUUID.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        targetName = rs.getString("target_username");
                    }
                }
            }

            String querySkin = "SELECT value, signature FROM cached_skins WHERE username = ?;";
            try (PreparedStatement pstmt = conn.prepareStatement(querySkin)) {
                pstmt.setString(1, targetName.toLowerCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new Property("textures", rs.getString("value"), rs.getString("signature"));
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static void saveSkinToDatabase(UUID playerUUID, String targetName, Property skinProperty) {
        if (skinProperty == null) return;
        skinCommandExecutor.submit(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                try (PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO cached_skins(username, value, signature) VALUES(?, ?, ?);")) {
                    pstmt.setString(1, targetName.toLowerCase().trim());
                    pstmt.setString(2, skinProperty.getValue());
                    pstmt.setString(3, skinProperty.getSignature());
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO player_skins(uuid, target_username) VALUES(?, ?);")) {
                    pstmt.setString(1, playerUUID.toString());
                    pstmt.setString(2, targetName.trim());
                    pstmt.executeUpdate();
                }
            } catch (Exception ignored) {}
        });
    }

    public static void clearPlayerSkinInDatabase(UUID playerUUID) {
        skinCommandExecutor.submit(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM player_skins WHERE uuid = ?;")) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.executeUpdate();
            } catch (Exception ignored) {}
        });
    }
}
