package dev.pulselite;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Меню PulseLite: Визуал / Производительность / Меню / Инфо. Только клики — без перетаскивания. */
public class PulseMenuScreen extends Screen {
    private static final String[] TABS = {"Визуал", "Производительность", "Меню", "Инфо"};
    private static int lastTab = 0;

    private final Screen parent;
    private int tab = lastTab;
    private final List<Cell> cells = new ArrayList<>();
    private final int[][] tabRects = new int[TABS.length][4];
    private int px, py, pw, ph;
    private String toast = "";
    private long toastUntil = 0;

    public PulseMenuScreen(Screen parent) {
        super(Component.literal("Sweetie Fox"));
        this.parent = parent;
    }

    private Options o() { return Minecraft.getInstance().options; }

    // ------------------------------------------------------------ ячейки
    private abstract class Cell {
        String label;
        int span = 1, h = 20, x, y, w;
        abstract void draw(GuiGraphics g, boolean hov);
        abstract void click(double mx, int btn);

        void bg(GuiGraphics g, boolean hov) {
            Gfx.rr(g, x, y, w, h, hov ? 0x55FFFFFF : 0x30FFFFFF);
        }
    }

    private class Toggle extends Cell {
        final BooleanSupplier get; final Consumer<Boolean> set;
        Toggle(String l, BooleanSupplier g, Consumer<Boolean> s) { label = l; get = g; set = s; }
        void draw(GuiGraphics g, boolean hov) {
            bg(g, hov);
            Gfx.text(g, label, x + 6, y + 6, 0xFFFFFFFF);
            boolean on = get.getAsBoolean();
            int tx = x + w - 28, ty = y + 5;
            Gfx.rr(g, tx, ty, 22, 10, on ? PulseTheme.accent(PulseConfig.get().menuPreset) : 0x66000000);
            g.fill(on ? tx + 13 : tx + 1, ty + 1, on ? tx + 21 : tx + 9, ty + 9, 0xFFFFFFFF);
        }
        void click(double mx, int btn) { set.accept(!get.getAsBoolean()); }
    }

    private class Stepper extends Cell {
        final String[] names; final IntSupplier idx; final IntConsumer set;
        Stepper(String l, String[] n, IntSupplier i, IntConsumer s) { label = l; names = n; idx = i; set = s; }
        void draw(GuiGraphics g, boolean hov) {
            bg(g, hov);
            Gfx.text(g, label, x + 6, y + 6, 0xFFFFFFFF);
            String v = "< " + names[Math.floorMod(idx.getAsInt(), names.length)] + " >";
            Gfx.text(g, v, x + w - 6 - Gfx.width(v), y + 6, PulseTheme.accent(PulseConfig.get().menuPreset));
        }
        void click(double mx, int btn) {
            int n = names.length;
            int i = idx.getAsInt();
            set.accept(Math.floorMod(btn == 1 ? i - 1 : i + 1, n));
        }
    }

    private class Action extends Cell {
        final Runnable run;
        Action(String l, Runnable r) { label = l; run = r; }
        void draw(GuiGraphics g, boolean hov) {
            int a = PulseTheme.accent(PulseConfig.get().menuPreset) & 0xFFFFFF;
            Gfx.rr(g, x, y, w, h, Gfx.argb(hov ? 0xAA : 0x77, a));
            Gfx.ctext(g, label, x + w / 2, y + 6, 0xFFFFFFFF);
        }
        void click(double mx, int btn) { run.run(); }
    }

    private class Banner extends Cell {
        Banner() { span = 2; h = 34; }
        void draw(GuiGraphics g, boolean hov) {
            PulseConfig c = PulseConfig.get();
            PulseTheme.paint(g, x, y, w, h, c.menuPreset, c.menuAnimated, 255);
            Gfx.border(g, x, y, w, h, 0x88FFFFFF);
            Gfx.ctext(g, PulseTheme.NAMES[c.menuPreset], x + w / 2, y + 13, 0xFFFFFFFF);
        }
        void click(double mx, int btn) {}
    }

    private class Palette extends Cell {
        Palette() { span = 2; h = 22; }
        void draw(GuiGraphics g, boolean hov) {
            int n = PulseTheme.PALETTES.length;
            int sw = (w - (n - 1) * 3) / n;
            for (int i = 0; i < n; i++) {
                int sx = x + i * (sw + 3);
                int[] p = PulseTheme.PALETTES[i];
                g.fillGradient(sx, y, sx + sw, y + h, 0xFF000000 | p[0], 0xFF000000 | p[2]);
                if (i == PulseConfig.get().menuPreset) Gfx.border(g, sx - 1, y - 1, sw + 2, h + 2, 0xFFFFFFFF);
            }
        }
        void click(double mx, int btn) {
            int n = PulseTheme.PALETTES.length;
            int sw = (w - (n - 1) * 3) / n;
            int i = (int) ((mx - x) / (sw + 3));
            if (i >= 0 && i < n) PulseConfig.get().menuPreset = i;
        }
    }

    private class Note extends Cell {
        Note(String l) { label = l; span = 2; h = 12; }
        void draw(GuiGraphics g, boolean hov) { Gfx.text(g, label, x + 2, y + 2, 0xFFD0D4F0); }
        void click(double mx, int btn) {}
    }

    // ------------------------------------------------------------ вкладки
    private Stepper ints(String l, int[] vals, IntSupplier get, IntConsumer set, String suffix) {
        String[] names = new String[vals.length];
        for (int i = 0; i < vals.length; i++) names[i] = vals[i] >= 260 ? "∞" : vals[i] + suffix;
        return new Stepper(l, names, () -> {
            int cur = get.getAsInt(), best = 0;
            for (int i = 0; i < vals.length; i++) if (Math.abs(vals[i] - cur) < Math.abs(vals[best] - cur)) best = i;
            return best;
        }, i -> { set.accept(vals[i]); o().save(); });
    }

    private Toggle optBool(String l, java.util.function.Supplier<Boolean> get, Consumer<Boolean> set) {
        return new Toggle(l, get::get, v -> { set.accept(v); o().save(); });
    }

    private void buildCells() {
        cells.clear();
        PulseConfig c = PulseConfig.get();
        if (tab == 0) {
            cells.add(new Toggle("Счётчик FPS", () -> c.hudFps, v -> c.hudFps = v));
            cells.add(new Toggle("Координаты", () -> c.hudCoords, v -> c.hudCoords = v));
            cells.add(new Toggle("Память (ОЗУ)", () -> c.hudRam, v -> c.hudRam = v));
            cells.add(new Toggle("Часы", () -> c.hudTime, v -> c.hudTime = v));
            cells.add(new Toggle("Подложка HUD", () -> c.hudBackground, v -> c.hudBackground = v));
            cells.add(new Stepper("Угол HUD", new String[]{"Лево-верх", "Право-верх", "Лево-низ", "Право-низ"},
                    () -> c.hudCorner, i -> c.hudCorner = i));
            cells.add(new Toggle("Зум (зажать C)", () -> c.zoom, v -> c.zoom = v));
            cells.add(optBool("Без тряски при уроне", () -> o().damageTiltStrength().get() == 0.0,
                    v -> o().damageTiltStrength().set(v ? 0.0 : 1.0)));
            cells.add(optBool("Без FOV-эффектов", () -> o().fovEffectScale().get() == 0.0,
                    v -> o().fovEffectScale().set(v ? 0.0 : 1.0)));
            cells.add(optBool("Покачивание камеры", () -> o().bobView().get(), v -> o().bobView().set(v)));
        } else if (tab == 1) {
            cells.add(new Action("Макс. FPS", () -> preset(0)));
            cells.add(new Action("Баланс", () -> preset(1)));
            cells.add(new Action("Красиво", () -> preset(2)));
            cells.add(new Action("Как в ваниле", () -> preset(3)));
            cells.add(ints("Прорисовка", new int[]{2, 3, 4, 6, 8, 10, 12, 16, 24, 32},
                    () -> o().renderDistance().get(), v -> o().renderDistance().set(v), " чк"));
            cells.add(ints("Симуляция", new int[]{5, 6, 8, 10, 12, 16},
                    () -> o().simulationDistance().get(), v -> o().simulationDistance().set(v), " чк"));
            cells.add(ints("Лимит FPS", new int[]{30, 60, 90, 120, 160, 200, 240, 260},
                    () -> o().framerateLimit().get(), v -> o().framerateLimit().set(v), ""));
            cells.add(optBool("VSync", () -> o().enableVsync().get(), v -> o().enableVsync().set(v)));
            cells.add(new Stepper("Частицы", new String[]{"Все", "Меньше", "Минимум"}, () -> {
                ParticleStatus s = o().particles().get();
                return s == ParticleStatus.ALL ? 0 : s == ParticleStatus.DECREASED ? 1 : 2;
            }, i -> {
                o().particles().set(i == 0 ? ParticleStatus.ALL : i == 1 ? ParticleStatus.DECREASED : ParticleStatus.MINIMAL);
                o().save();
            }));
            cells.add(new Stepper("Облака", new String[]{"Выкл", "Быстро", "Красиво"}, () -> {
                CloudStatus s = o().cloudStatus().get();
                return s == CloudStatus.OFF ? 0 : s == CloudStatus.FAST ? 1 : 2;
            }, i -> {
                o().cloudStatus().set(i == 0 ? CloudStatus.OFF : i == 1 ? CloudStatus.FAST : CloudStatus.FANCY);
                o().save();
            }));
            cells.add(optBool("Тени сущностей", () -> o().entityShadows().get(), v -> o().entityShadows().set(v)));
            cells.add(ints("Mipmap", new int[]{0, 1, 2, 3, 4},
                    () -> o().mipmapLevels().get(), v -> o().mipmapLevels().set(v), ""));
            cells.add(ints("Сглаж. биомов", new int[]{0, 1, 2, 3, 5, 7},
                    () -> o().biomeBlendRadius().get(), v -> o().biomeBlendRadius().set(v), ""));
            double[] ed = {0.5, 0.75, 1.0, 1.5, 2.0};
            cells.add(new Stepper("Дальн. сущностей", new String[]{"50%", "75%", "100%", "150%", "200%"}, () -> {
                double cur = o().entityDistanceScaling().get();
                int best = 0;
                for (int i = 0; i < ed.length; i++) if (Math.abs(ed[i] - cur) < Math.abs(ed[best] - cur)) best = i;
                return best;
            }, i -> { o().entityDistanceScaling().set(ed[i]); o().save(); }));
        } else if (tab == 2) {
            cells.add(new Toggle("Градиентное главное меню", () -> c.customTitle, v -> c.customTitle = v));
            cells.add(new Toggle("Анимация градиента", () -> c.menuAnimated, v -> c.menuAnimated = v));
            cells.add(new Banner());
            cells.add(new Palette());
            cells.add(new Note("Палитра красит и меню, и акценты HUD."));
            cells.add(new Note("Главное меню применится при следующем открытии."));
        } else {
            cells.add(new Note("Right Shift — открыть это меню"));
            cells.add(new Note("C (зажать) — зум; кнопки меняются в Управлении"));
            cells.add(new Note("ЛКМ — вперёд по списку, ПКМ — назад"));
            cells.add(new Note("Настройки: config/pulselite.json"));
            cells.add(new Action("Сбросить настройки PulseLite", () -> { PulseConfig.reset(); say("Сброшено"); }));
        }
    }

    private void say(String s) { toast = s; toastUntil = System.currentTimeMillis() + 1800; }

    private void preset(int n) {
        Options o = o();
        int[][] iv = {  // прорисовка, симуляция, fps, mipmap, biome
                {6, 5, 240, 0, 0}, {10, 8, 160, 2, 1}, {16, 12, 120, 4, 3}, {12, 12, 120, 4, 2}};
        int[] v = iv[n];
        o.renderDistance().set(v[0]);
        o.simulationDistance().set(v[1]);
        o.framerateLimit().set(v[2]);
        o.mipmapLevels().set(v[3]);
        o.biomeBlendRadius().set(v[4]);
        o.enableVsync().set(n >= 2);
        o.entityShadows().set(n >= 2);
        o.bobView().set(n >= 2);
        o.entityDistanceScaling().set(n == 0 ? 0.75 : n == 1 ? 1.0 : 1.5);
        o.particles().set(n == 0 ? ParticleStatus.MINIMAL : n == 1 ? ParticleStatus.DECREASED : ParticleStatus.ALL);
        o.cloudStatus().set(n == 0 ? CloudStatus.OFF : n == 1 ? CloudStatus.FAST : CloudStatus.FANCY);
        o.save();
        say(new String[]{"Макс. FPS применён", "Баланс применён", "Красивая графика", "Ванильные значения"}[n]);
    }

    // ------------------------------------------------------------ раскладка
    @Override
    protected void init() { relayout(); }

    private void relayout() {
        buildCells();
        pw = Math.min(470, width - 16);
        py = 0;
        int bottom = layout();
        ph = bottom + 26;
        if (ph > height - 8) ph = height - 8;
        py = Math.max(4, (height - ph) / 2);
        px = (width - pw) / 2;
        layout();
        int tx = px + 14;
        for (int i = 0; i < TABS.length; i++) {
            int tw = Gfx.width(TABS[i]) + 16;
            tabRects[i][0] = tx; tabRects[i][1] = py + 36; tabRects[i][2] = tw; tabRects[i][3] = 16;
            tx += tw + 4;
        }
    }

    private int layout() {
        int x0 = (width - pw) / 2 + 14, cw = (pw - 28 - 6) / 2, cy = py + 62, col = 0, rowH = 0;
        for (Cell c : cells) {
            if (c.span == 2 && col == 1) { col = 0; cy += rowH + 4; rowH = 0; }
            c.x = x0 + col * (cw + 6);
            c.y = cy;
            c.w = c.span == 2 ? pw - 28 : cw;
            rowH = Math.max(rowH, c.h);
            if (c.span == 2) { cy += c.h + 4; col = 0; rowH = 0; }
            else if (++col == 2) { col = 0; cy += rowH + 4; rowH = 0; }
        }
        if (col == 1) cy += rowH + 4;
        return cy - py;
    }

    // ------------------------------------------------------------ рисование
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        PulseConfig c = PulseConfig.get();
        if (minecraft != null && minecraft.level == null) {
            PulseTheme.paint(g, 0, 0, width, height, c.menuPreset, c.menuAnimated, 255);
            g.fill(0, 0, width, height, 0x55000000);
        } else {
            g.fill(0, 0, width, height, 0x99000000);
        }
        Gfx.rr(g, px, py, pw, ph, 0xE0101224);
        Gfx.border(g, px, py, pw, ph, 0x66FFFFFF);
        PulseTheme.paint(g, px + 2, py + 1, pw - 4, 3, c.menuPreset, c.menuAnimated, 255);

        Gfx.text(g, "SWEETIE", px + 14, py + 12, 0xFFFFFFFF);
        Gfx.text(g, "FOX", px + 14 + Gfx.width("SWEETIE") + 1, py + 12, PulseTheme.accent(c.menuPreset));
        Gfx.text(g, "Right Shift — закрыть", px + pw - 14 - Gfx.width("Right Shift — закрыть"), py + 12, 0xFF8A91C4);

        for (int i = 0; i < TABS.length; i++) {
            int[] r = tabRects[i];
            boolean sel = i == tab, hov = Gfx.in(mx, my, r[0], r[1], r[2], r[3]);
            int a = PulseTheme.accent(c.menuPreset) & 0xFFFFFF;
            Gfx.rr(g, r[0], r[1], r[2], r[3], sel ? Gfx.argb(0xCC, a) : (hov ? 0x44FFFFFF : 0x22FFFFFF));
            Gfx.text(g, TABS[i], r[0] + 8, r[1] + 4, 0xFFFFFFFF);
        }
        for (Cell cell : cells) cell.draw(g, Gfx.in(mx, my, cell.x, cell.y, cell.w, cell.h));

        if (System.currentTimeMillis() < toastUntil) {
            Gfx.ctext(g, toast, px + pw / 2, py + ph - 16, PulseTheme.accent(c.menuPreset));
        } else if (tab == 1) {
            Gfx.ctext(g, "FPS сейчас: " + (minecraft != null ? minecraft.getFps() : 0), px + pw / 2, py + ph - 16, 0xFF8A91C4);
        }
    }

    // ------------------------------------------------------------ ввод
    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        double mx = e.x(), my = e.y();
        int b = e.button();
        for (int i = 0; i < TABS.length; i++) {
            int[] r = tabRects[i];
            if (Gfx.in(mx, my, r[0], r[1], r[2], r[3])) {
                tab = i; lastTab = i; relayout();
                return true;
            }
        }
        for (Cell cell : cells) {
            if (Gfx.in(mx, my, cell.x, cell.y, cell.w, cell.h)) {
                cell.click(mx, b);
                PulseConfig.save();
                return true;
            }
        }
        return super.mouseClicked(e, dbl);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public void onClose() {
        PulseConfig.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
