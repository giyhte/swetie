package dev.pulselite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Мелкие помощники для рисования. */
public final class Gfx {
    private Gfx() {}

    public static int argb(int a, int rgb) { return (a << 24) | (rgb & 0xFFFFFF); }

    /** Прямоугольник со срезанными углами (без наложения полос — прозрачность не удваивается). */
    public static void rr(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x + 2, y, x + w - 2, y + 1, c);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, c);
        g.fill(x, y + 2, x + w, y + h - 2, c);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, c);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, c);
    }

    public static void border(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x + 2, y, x + w - 2, y + 1, c);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, c);
        g.fill(x, y + 2, x + 1, y + h - 2, c);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, c);
    }

    public static int width(String s) { return Minecraft.getInstance().font.width(s); }

    public static void text(GuiGraphics g, String s, int x, int y, int c) {
        g.drawString(Minecraft.getInstance().font, s, x, y, c, true);
    }

    public static void ctext(GuiGraphics g, String s, int cx, int y, int c) {
        g.drawString(Minecraft.getInstance().font, s, cx - width(s) / 2, y, c, true);
    }

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
