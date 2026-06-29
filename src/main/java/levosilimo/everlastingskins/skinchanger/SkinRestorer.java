package levosilimo.everlastingskins.skinchanger;

import com.mojang.authlib.properties.Property;
import levosilimo.everlastingskins.EternalSkins;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import java.util.UUID;

public class SkinRestorer {
    public static MinecraftServer server;

    public static void init(FMLServerStartingEvent event) {
        server = event.getServer();
        event.registerServerCommand(new SkinCommand());
    }

    public static void applySkin(EntityPlayerMP playerEntity, Property skin) {
        if (skin != null && playerEntity != null && playerEntity.getGameProfile() != null) {
            playerEntity.getGameProfile().getProperties().removeAll("textures");
            playerEntity.getGameProfile().getProperties().put("textures", skin);
        }
    }

    @SubscribeEvent
    public void onPlayerLoading(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP playerMP) {
            UUID uuid = playerMP.getUniqueID();

            EternalSkins.skinCommandExecutor.submit(() -> {
                try {
                    String hashFromDatabase = EternalSkins.getStoredSkinHash(uuid);
                    Property finalSkin = null;

                    if (hashFromDatabase != null && !hashFromDatabase.isEmpty()) {
                        finalSkin = EternalSkins.getSkinFromFile(hashFromDatabase);
                    }

                    if (finalSkin != null) {
                        Property skinToApply = finalSkin;
                        SkinRestorer.server.addScheduledTask(() -> {
                            if (playerMP.connection != null) {
                                applySkin(playerMP, skinToApply);
                                new EmulateReconnectHandler(playerMP).emulateReconnect();
                            }
                        });
                    } else {
                        EternalSkins.logger.info("[EternalSkins] Локальный скин для игрока {} не найден. Используется дефолтный.", playerMP.getName());
                    }

                } catch (Exception e) {
                    EternalSkins.logger.error("[EternalSkins] Ошибка при локальной загрузке скина для {}", playerMP.getName(), e);
                }
            });
        }
    }
}
