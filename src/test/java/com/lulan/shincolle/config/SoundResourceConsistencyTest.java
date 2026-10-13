package com.lulan.shincolle.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #8 (RND-01/02/03): sounds.json must stay consistent with the .ogg files
 * and with the default custom sound rate config.
 */
class SoundResourceConsistencyTest {

    // index of the sound rate array -> sound event type name (see ModSounds.getCustomSoundString)
    private static final String[] TYPE_NAMES = {"idle", "hit", "hurt", "death", "marry", "knockback", "item", "feed"};

    private static JsonObject loadSoundsJson() throws IOException {
        try (InputStream in = SoundResourceConsistencyTest.class.getResourceAsStream("/assets/shincolle/sounds.json")) {
            assertTrue(in != null, "sounds.json not found on classpath");
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everySoundInSoundsJsonHasAFile() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : loadSoundsJson().entrySet()) {
            JsonArray sounds = e.getValue().getAsJsonObject().getAsJsonArray("sounds");
            for (JsonElement s : sounds) {
                String name = s.getAsString();
                String path = name.substring(name.indexOf(':') + 1);
                if (SoundResourceConsistencyTest.class.getResource("/assets/shincolle/sounds/" + path + ".ogg") == null) {
                    missing.add(e.getKey() + " -> " + name);
                }
            }
        }
        assertTrue(missing.isEmpty(), "sounds.json references missing files: " + missing);
    }

    @Test
    void everyDefaultConfigSoundKeyHasAnEvent() throws IOException {
        JsonObject json = loadSoundsJson();
        ConfigSound.SOUNDRATE = new HashMap<>();
        ConfigSound cfg = new ConfigSound();
        cfg.parse(cfg.getDefaultContent());

        List<String> missing = new ArrayList<>();
        ConfigSound.SOUNDRATE.forEach((shipId, rates) -> {
            for (int i = 0; i < 8; i++) {
                if (rates[i] > 0F && !json.has("ship-" + TYPE_NAMES[i] + "-" + shipId)) {
                    missing.add("ship-" + TYPE_NAMES[i] + "-" + shipId);
                }
            }
            if (rates[8] > 0F) {
                for (int j = 0; j < 24; j++) {
                    if (!json.has("ship-time" + j + "-" + shipId)) missing.add("ship-time" + j + "-" + shipId);
                }
            }
        });
        assertTrue(missing.isEmpty(), "default config keys without sounds.json event: " + missing);
    }
}
