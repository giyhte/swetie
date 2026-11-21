package dev.pulselite;

import net.minecraft.client.gui.GuiGraphics;

/** Градиентные палитры и отрисовка анимированного фона. */
public final class PulseTheme {
    private PulseTheme() {}

    public static final String[] NAMES = {"Аврора", "Закат", "Океан", "Неон", "Лес", "Лава", "Мята", "Ночь"};
    public static final int[][] PALETTES = {
            {0x7C3AED, 0x2563EB, 0x06B6D4},
            {0xF97316, 0xEC4899, 0x7C3AED},
            {0x0EA5E9, 0x1D4ED8, 0x0B1437},
            {0xF72585, 0x7209B7, 0x3A0CA3},
            {0x34D399, 0x059669, 0x064E3B},
            {0xEF4444, 0xB91C1C, 0x3B0A0A},
            {0x5EEAD4, 0x22D3EE, 0x6366F1},
            {0x4338CA, 0x1E1B4B, 0x0B0B1A}
    };

    private static int lerp(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (g << 8) | bl;
    }

    public static int sample(int[] p, double t) {
        t = Math.max(0, Math.min(1, t));
        double s = t * 2;
        return s < 1 ? lerp(p[0], p[1], s) : lerp(p[1], p[2], s - 1);
    }

    /** Яркий акцентный цвет (ARGB), плавно «дышит» во времени. */
    public static int accent(int preset) {
        int[] p = PALETTES[Math.floorMod(preset, PALETTES.length)];
        double t = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 1500.0);
        return 0xFF000000 | lerp(sample(p, t), 0xFFFFFF, 0.35);
    }

    public static void paint(GuiGraphics g, int x, int y, int w, int h, int preset, boolean animated, int alpha) {
        int[] p = PALETTES[Math.floorMod(preset, PALETTES.length)];
        double time = animated ? System.currentTimeMillis() / 1000.0 : 0;
        int cols = Math.max(8, Math.min(64, w / 10));
        int mid = h / 2;
        for (int i = 0; i < cols; i++) {
            int x0 = x + i * w / cols;
            int x1 = x + (i + 1) * w / cols;
            double u = (i + 0.5) / cols;
            int top = Gfx.argb(alpha, sample(p, param(u, 0.0, time)));
            int mi = Gfx.argb(alpha, sample(p, param(u, 0.5, time)));
            int bot = Gfx.argb(alpha, sample(p, param(u, 1.0, time)));
            g.fillGradient(x0, y, x1, y + mid, top, mi);
            g.fillGradient(x0, y + mid, x1, y + h, mi, bot);
        }
    }

    private static double param(double u, double v, double time) {
        return 0.5 * u + 0.5 * v + 0.18 * Math.sin(time * 0.5 + u * 4.0 + v * 2.0) - 0.09;
    }
}
