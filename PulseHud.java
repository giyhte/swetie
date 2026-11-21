package dev.pulselite;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class PulseHud {
    private PulseHud() {}

    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        PulseConfig c = PulseConfig.get();
        if (mc.player == null || mc.options.hideGui) return;
        List<String> lines = new ArrayList<>();
        if (c.hudFps) lines.add("FPS " + mc.getFps());
        if (c.hudCoords) {
            lines.add("XYZ " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ());
        }
        if (c.hudRam) {
            Runtime r = Runtime.getRuntime();
            lines.add("ОЗУ " + ((r.totalMemory() - r.freeMemory()) >> 20) + "/" + (r.maxMemory() >> 20) + " МБ");
        }
        if (c.hudTime) lines.add(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (lines.isEmpty()) return;

        int w = 0;
        for (String s : lines) w = Math.max(w, Gfx.width(s));
        int h = lines.size() * 10;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int x = (c.hudCorner % 2 == 0) ? 6 : sw - w - 12;
        int y = (c.hudCorner < 2) ? 6 : sh - h - 8;
        int accent = PulseTheme.accent(c.menuPreset);
        if (c.hudBackground) {
            Gfx.rr(g, x - 4, y - 3, w + 10, h + 4, 0x70000000);
            g.fill(x - 4, y - 1, x - 3, y + h, accent);
        }
        for (int i = 0; i < lines.size(); i++) Gfx.text(g, lines.get(i), x, y + i * 10, 0xFFFFFFFF);
    }
}
