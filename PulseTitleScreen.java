package dev.pulselite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Своё главное меню: анимированный градиент и стеклянные кнопки. */
public class PulseTitleScreen extends Screen {
    private final List<String> labels = new ArrayList<>();
    private final List<Runnable> actions = new ArrayList<>();
    private int bx, by, bw, bh, gap;

    public PulseTitleScreen() {
        super(Component.literal("Sweetie Fox"));
    }

    @Override
    protected void init() {
        labels.clear();
        actions.clear();
        add("Одиночная игра", () -> minecraft.setScreen(new SelectWorldScreen(this)));
        add("Сетевая игра", () -> minecraft.setScreen(new JoinMultiplayerScreen(this)));
        add("Sweetie Fox Menu", () -> minecraft.setScreen(new PulseMenuScreen(this)));
        add("Настройки Minecraft", () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
        add("Выход", () -> minecraft.stop());
        bw = Math.min(220, width - 40);
        bh = 24;
        gap = 6;
        bx = (width - bw) / 2;
        int total = labels.size() * (bh + gap) - gap;
        by = Math.max(height / 2 - 6, 90);
        if (by + total > height - 24) by = height - 24 - total;
    }

    private void add(String l, Runnable r) {
        labels.add(l);
        actions.add(r);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        PulseConfig c = PulseConfig.get();
        PulseTheme.paint(g, 0, 0, width, height, c.menuPreset, c.menuAnimated, 255);
        g.fillGradient(0, 0, width, height / 3, 0x55000000, 0x00000000);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0x99000000);

        // логотип
        float scale = Math.max(2f, Math.min(5f, width / 120f));
        String a = "SWEETIE", b = "FOX";
        int wa = Gfx.width(a), wb = Gfx.width(b);
        int total = wa + wb;
        g.pose().pushMatrix();
        g.pose().translate(width / 2f - total * scale / 2f, Math.max(10, by - 64 - 6 * scale));
        g.pose().scale(scale, scale);
        Gfx.text(g, a, 0, 0, 0xFFFFFFFF);
        Gfx.text(g, b, wa, 0, PulseTheme.accent(c.menuPreset));
        g.pose().popMatrix();

        for (int i = 0; i < labels.size(); i++) {
            int y = by + i * (bh + gap);
            boolean hov = Gfx.in(mx, my, bx, y, bw, bh);
            boolean primary = i == 0;
            int fill = primary ? (hov ? 0x88FFFFFF : 0x66FFFFFF) : (hov ? 0x66FFFFFF : 0x40000000);
            Gfx.rr(g, bx, y, bw, bh, fill);
            Gfx.border(g, bx, y, bw, bh, hov ? 0xCCFFFFFF : 0x55FFFFFF);
            Gfx.ctext(g, labels.get(i), width / 2, y + (bh - 8) / 2, 0xFFFFFFFF);
        }
        Gfx.ctext(g, "Right Shift — меню Sweetie Fox в игре", width / 2, height - 14, 0xAAFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (e.button() == 0) {
            for (int i = 0; i < labels.size(); i++) {
                if (Gfx.in(e.x(), e.y(), bx, by + i * (bh + gap), bw, bh)) {
                    actions.get(i).run();
                    return true;
                }
            }
        }
        return super.mouseClicked(e, dbl);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        return super.keyPressed(e);
    }
}
