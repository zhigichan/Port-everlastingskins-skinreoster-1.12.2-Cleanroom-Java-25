package levosilimo.everlastingskins.skinchanger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MojangSkinProvider {

    public static Property getSkin(String username) {
        if (username == null || username.trim().isEmpty()) return null;
        String cleanUsername = username.trim().replace("\n", "").replace("\r", "");

        try {
            String uuidUrl = "https://mojang.com" + cleanUsername;
            String uuidResponse = sendGetRequest(uuidUrl);
            if (uuidResponse == null || uuidResponse.isEmpty()) return null;

            JsonObject uuidJson = new JsonParser().parse(uuidResponse).getAsJsonObject();
            if (!uuidJson.has("id")) return null;
            String uuid = uuidJson.get("id").getAsString().trim();

            String skinUrl = "https://mojang.com" + uuid + "?unsigned=false";
            String skinResponse = sendGetRequest(skinUrl);
            if (skinResponse == null || skinResponse.isEmpty()) return null;

            JsonObject skinJson = new JsonParser().parse(skinResponse).getAsJsonObject();
            if (!skinJson.has("properties")) return null;

            JsonArray properties = skinJson.getAsJsonArray("properties");
            for (int i = 0; i < properties.size(); i++) {
                JsonObject prop = properties.get(i).getAsJsonObject();
                if ("textures".equals(prop.get("name").getAsString())) {
                    String value = prop.get("value").getAsString();
                    String signature = prop.get("signature").getAsString();
                    return new Property("textures", value, signature);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String sendGetRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "MinecraftLauncher/2.2.12214 (Windows 10; 10.0; x86_64)");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() == 200) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        }
        return null;
    }
}
