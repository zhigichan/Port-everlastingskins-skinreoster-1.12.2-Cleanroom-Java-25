package levosilimo.everlastingskins;

import levosilimo.everlastingskins.skinchanger.SkinRestorer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mojang.authlib.properties.Property;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(modid = EternalSkins.MOD_ID, name = EternalSkins.MOD_NAME, version = EternalSkins.VERSION, acceptableRemoteVersions = "*")
public class EternalSkins {
    public static final String MOD_ID = "eternalskins";
    public static final String MOD_NAME = "Eternal Skins";
    public static final String VERSION = "1.0.2";

    public static final Logger logger = LogManager.getLogger("EternalSkins");
    public static final ExecutorService skinCommandExecutor = Executors.newCachedThreadPool();

    private static File cacheFolder;
    private static File playersFolder;

    public EternalSkins() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new SkinRestorer());
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        try {
            cacheFolder = new File("eternal_skins_cache");
            if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
                logger.warn("[EternalSkins] Не удалось создать папку кэша скинов или она уже создана.");
            }

            playersFolder = new File(cacheFolder, "players");
            if (!playersFolder.exists() && !playersFolder.mkdirs()) {
                logger.warn("[EternalSkins] Не удалось создать папку игроков.");
            }
        } catch (Exception e) {
            logger.error(e);
        }
        SkinRestorer.init(event);
    }

    private static JsonObject readPlayerFile(UUID playerUUID) {
        File playerFile = new File(playersFolder, playerUUID.toString() + ".json");
        if (!playerFile.exists()) return null;
        try (FileReader reader = new FileReader(playerFile)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getStoredSkinHash(UUID playerUUID) {
        JsonObject json = readPlayerFile(playerUUID);
        return (json != null && json.has("hash")) ? json.get("hash").getAsString() : null;
    }

    public static Property getSkinFromFile(String hash) {
        if (hash == null || hash.isEmpty()) return null;
        File file = new File(cacheFolder, hash + ".json");
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String value = json.get("value").getAsString();
            String signature = json.get("signature").getAsString();
            return new Property("textures", value, signature);
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    public static void saveSkinData(UUID playerUUID, String targetName, String hash, Property skinProperty) {
        if (targetName == null || hash == null || hash.isEmpty()) return;

        skinCommandExecutor.submit(() -> {
            try {
                File skinFile = new File(cacheFolder, hash + ".json");
                if (!skinFile.exists() && skinProperty != null) {
                    JsonObject json = new JsonObject();
                    json.addProperty("value", skinProperty.getValue());
                    json.addProperty("signature", skinProperty.getSignature());
                    try (FileWriter writer = new FileWriter(skinFile)) {
                        writer.write(json.toString());
                    }
                }

                File playerFile = new File(playersFolder, playerUUID.toString() + ".json");
                JsonObject playerData = new JsonObject();
                playerData.addProperty("username", targetName.trim());
                playerData.addProperty("hash", hash);

                try (FileWriter writer = new FileWriter(playerFile)) {
                    writer.write(playerData.toString());
                }
            } catch (Exception e) {
                logger.error(e);
            }
        });
    }
}
