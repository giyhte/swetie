package dev.pulselite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PulseConfig {
    public boolean hudFps = true;
    public boolean hudCoords = false;
    public boolean hudRam = false;
    public boolean hudTime = false;
    public boolean hudBackground = true;
    public int hudCorner = 0;
    public boolean zoom = true;
    public boolean customTitle = true;
    public boolean menuAnimated = true;
    public int menuPreset = 0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PulseConfig inst = new PulseConfig();

    public static PulseConfig get() { return inst; }

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("pulselite.json"); }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                PulseConfig c = GSON.fromJson(Files.readString(p), PulseConfig.class);
                if (c != null) inst = c;
            }
        } catch (Exception e) {
            System.err.println("[PulseLite] config load failed: " + e);
        }
        if (inst.menuPreset < 0 || inst.menuPreset >= PulseTheme.NAMES.length) inst.menuPreset = 0;
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(inst));
        } catch (Exception e) {
            System.err.println("[PulseLite] config save failed: " + e);
        }
    }

    public static void reset() { inst = new PulseConfig(); save(); }
}
