package levosilimo.everlastingskins.skinchanger;

import com.mojang.authlib.properties.Property;
import levosilimo.everlastingskins.EverlastingSkins;
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
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) event.player;
            UUID uuid = playerMP.getUniqueID();
            String name = playerMP.getName();

            EverlastingSkins.skinCommandExecutor.submit(() -> {
                try {
                    Property skin = EverlastingSkins.getCachedSkinByUUID(uuid, name);

                    if (skin == null) {
                        skin = MojangSkinProvider.getSkin(name);
                        if (skin != null) {
                            EverlastingSkins.saveSkinToDatabase(uuid, name, skin);
                        }
                    }

                    if (skin != null) {
                        Property finalSkin = skin;
                        if (SkinRestorer.server != null) {
                            SkinRestorer.server.addScheduledTask(() -> {
                                if (playerMP.connection != null) {
                                    applySkin(playerMP, finalSkin);
                                    new EmulateReconnectHandler(playerMP).emulateReconnect();
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            });
        }
    }
}
