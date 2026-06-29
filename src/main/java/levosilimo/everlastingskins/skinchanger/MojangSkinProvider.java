package levosilimo.everlastingskins.skinchanger;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.MinecraftServer;

public class MojangSkinProvider {

    public static Property getSkin(String username) {
        return fetchPropertyFromMojang(username);
    }

    private static Property fetchPropertyFromMojang(String username) {
        if (SkinRestorer.server == null || username == null || username.trim().isEmpty()) return null;
        try {
            MinecraftServer server = SkinRestorer.server;
            MinecraftSessionService sessionService = server.getMinecraftSessionService();

            com.mojang.authlib.GameProfileRepository repo = server.getGameProfileRepository();
            final GameProfile[] foundProfile = new GameProfile[1];

            repo.findProfilesByNames(new String[]{username.trim()}, com.mojang.authlib.Agent.MINECRAFT,
                    new com.mojang.authlib.ProfileLookupCallback() {
                        @Override
                        public void onProfileLookupSucceeded(GameProfile profile) {
                            foundProfile[0] = profile;
                        }
                        @Override
                        public void onProfileLookupFailed(GameProfile profile, Exception e) {
                        }
                    }
            );

            GameProfile onlineProfile = foundProfile[0];

            if (onlineProfile != null) {
                GameProfile freshProfile = new GameProfile(onlineProfile.getId(), onlineProfile.getName());
                freshProfile = sessionService.fillProfileProperties(freshProfile, true);

                if (freshProfile.getProperties().containsKey("textures")) {
                    for (Property prop : freshProfile.getProperties().get("textures")) {
                        return prop;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
