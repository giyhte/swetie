# -*- coding: utf-8 -*-
import os, threading
from .common import *
from . import engine
from .ui import Host, Scene, VIOLET, CYAN

W, H = 880, 620
SUB = "#b8b4e6"
OK, BAD = (134, 239, 172), (251, 113, 133)


class LauncherScene(Scene):
    def __init__(self, host, P):
        super().__init__(host)
        self.P = P
        self.cfg = load_cfg(P)
        self.busy, self.prog = False, 0.0
        self.st = engine.state(P)
        pt = self.pt
        self.nav = "home"
        ic = pt.icon(56)
        ic_big = pt.icon(120)

        # общий каркас
        self.under.append(lambda ov: pt.glass(ov, (16, 16, 864, 604), 30, 110, 18))
        self.under.append(lambda ov: pt.image(ov, ic, (40, 34)))
        self.under.append(lambda ov: pt.glass(ov, (36, 112, 212, 584), 20, 80, 14))
        self.text("brand", 108, 38, "sweetie fox", 28, True, "#ffffff")
        self.text("welcome", 110, 76, "Добро пожаловать!", 14, False, SUB)
        for i, (k, name) in enumerate([("home", "Главная"), ("set", "Настройки"), ("about", "О лаунчере")]):
            self.button("n_" + k, (48, 126 + i * 52, 200, 168 + i * 52), name, (lambda k=k: self.go(k)), "nav", 15,
                        sel=(lambda k=k: self.nav == k))
        self.under.append(lambda ov: pt.image(ov, ic_big, (60, 440)))
        self.text("minec", 124, 566, "Minecraft Launcher", 11, False, SUB, "center")

        # --- Главная ---
        self.tag = "home"
        self.under.append(("home", lambda ov: pt.inset(ov, (236, 112, 848, 262), 20)))
        self.under.append(("home", lambda ov: pt.inset(ov, (236, 280, 560, 346), 18)))
        self.under.append(("home", lambda ov: pt.glass(ov, (236, 392, 848, 584), 20, 80, 14)))
        self.text("ban1", 542, 150, "sweetie fox ♡", 40, True, "#ffffff", "center")
        self.text("ban2", 542, 204, "Играй с комфортом!", 18, False, "#e9e4ff", "center")
        self.dot(262, 313, lambda: OK if self.st["game"] else BAD, )
        self.text("ver", 282, 296, "Minecraft " + MC_VERSION, 17, True, "#ffffff")
        self.text("verst", 282, 320, "", 12, False, SUB)
        self.button("b_main", (580, 280, 848, 346), "", self.on_main, "primary", 22, lambda: not self.busy)
        self.text("status", 236, 358, "", 12, False, SUB, width=612)
        self.bar((236, 376, 848, 384), lambda: self.prog)
        self.text("nh", 258, 410, "Последние новости", 17, True, "#ffffff")
        self.text("n1", 258, 450, "Добро пожаловать в sweetie fox!", 16, True, "#ffffff")
        self.text("n2", 258, 478, "Спасибо, что выбрали наш лаунчер.\nИграйте с удовольствием ♡", 14, False, SUB, width=560)
        self.text("n3", 258, 540, "Меню в игре: Right Shift  •  Зум: C", 12, False, "#8f89c8")

        # --- Настройки ---
        self.tag = "set"
        self.under.append(("set", lambda ov: pt.glass(ov, (236, 112, 848, 584), 20, 80, 14)))
        self.text("t_nick", 262, 134, "НИК", 11, True, SUB)
        self.text("t_srv", 556, 134, "СЕРВЕР", 11, True, SUB)
        self.nick = self.entry((262, 156, 536, 194), self.cfg["nick"])
        self.srv = self.entry((556, 156, 822, 194), self.cfg["server"])
        self.text("t_ram", 262, 222, "ПАМЯТЬ", 11, True, SUB)
        self.text("v_ram", 822, 220, "", 14, True, "#ffffff", "ne")
        self.slider((262, 250, 822, 270), lambda: self.cfg["ram"], self._set_ram, 1024, 8192, 256)
        opts = [("low_mode", "Режим для слабого ПК", "минимум графики: дальность 6, без частиц и теней"),
                ("own_mod", "Sweetie Fox Menu в игре", "меню на Right Shift: визуал, буст FPS, градиентное меню"),
                ("autojoin", "Автовход на сервер", "сразу подключаться к серверу из поля выше")]
        for i, (k, name, hint) in enumerate(opts):
            y = 304 + i * 56
            self.text("o_" + k, 262, y, name, 15, True, "#ffffff")
            self.text("h_" + k, 262, y + 22, hint, 12, False, "#8f89c8")
            self.switch((766, y + 4, 822, y + 30), (lambda k=k: self.cfg[k]), (lambda v, k=k: self._set(k, v)))
        self.button("b_inst", (262, 492, 540, 538), "Переустановить / обновить", self.on_install, "ghost", 14,
                    lambda: not self.busy)
        self.button("b_dir", (556, 492, 822, 538), "Папка игры", lambda: open_folder(P.game), "ghost", 14)

        # --- О лаунчере ---
        self.tag = "about"
        self.under.append(("about", lambda ov: pt.glass(ov, (236, 112, 848, 584), 20, 80, 14)))
        self.text("a1", 262, 140, "sweetie fox launcher", 24, True, "#ffffff")
        self.text("a2", 262, 184, "Лаунчер Minecraft " + MC_VERSION + " (Fabric) с оптимизацией для слабых ПК.\n\n"
                  "• сам проверяет игру и моды: нет — скачивает, есть — не трогает\n"
                  "• Sodium, Lithium, FerriteCore и другие моды на FPS\n"
                  "• меню в игре (Right Shift): визуал, буст FPS, градиентное меню\n"
                  "• вход по нику (оффлайн)", 14, False, SUB, width=560)
        self.text("a3", 262, 540, "Версия " + VERSION, 12, False, "#8f89c8")

        self.tag = None
        self.refresh_state()
        self.set_page("home")

    # --- состояние ---
    def go(self, k):
        self.nav = k
        self.set_page(k)

    def _set(self, k, v):
        self.cfg[k] = v
        if k == "own_mod":
            self.refresh_state()

    def _set_ram(self, v):
        self.cfg["ram"] = v
        self.set_text("v_ram", f"{v} МБ")

    def refresh_state(self):
        self.st = engine.state(self.P)
        need_menu = self.st["game"] and self.cfg["own_mod"] and not self.st["menu"]
        if not self.st["game"]:
            self.set_text("verst", "игра не установлена")
        else:
            self.set_text("verst", "Fabric • моды: %d%s" % (self.st["mods"], " • меню ✓" if self.st["menu"] else ""))
        self.set_text("v_ram", f"{self.cfg['ram']} МБ")
        self.set_text("b_main", "УСТАНОВИТЬ МЕНЮ" if need_menu else ("▶  ИГРАТЬ" if self.st["game"] else "УСТАНОВИТЬ"))
        self.refresh()

    def save(self):
        self.cfg["nick"] = self.nick.get().strip() or "Player"
        self.cfg["server"] = self.srv.get().strip()
        save_cfg(self.P, self.cfg)

    # --- действия ---
    def on_main(self):
        if self.busy:
            return
        if self.st["game"] and self.cfg["own_mod"] and not self.st["menu"]:
            self.on_install(menu_only=True)
        elif self.st["game"]:
            self.save()
            try:
                engine.launch(self.P, self.cfg)
                self.set_text("status", "Игра запускается — окно появится через несколько секунд")
            except Exception as e:
                self.set_text("status", f"Ошибка: {e}")
        else:
            self.on_install()

    def on_install(self, menu_only=False):
        if self.busy:
            return
        self.save()
        self.busy, self.prog = True, 0.0
        self.set_text("b_main", "УСТАНОВКА…")
        self.set_page("home"); self.nav = "home"
        post = self.host.post
        ctx = Ctx(lambda m: post(lambda: self.set_text("status", m)),
                  lambda f: post(lambda: self._progress(f)),
                  lambda s: post(lambda: self.set_text("status", s)))

        def work():
            try:
                if menu_only:
                    ctx.step(0.0, 1.0)
                    engine.ensure_menu_mod(self.P, ctx)
                else:
                    engine.install_all(self.P, self.cfg, ctx)
            except Exception as e:
                ctx.log(f"ОШИБКА: {e}")
            post(self._done)
        threading.Thread(target=work, daemon=True).start()

    def _progress(self, f):
        self.prog = f
        self.refresh()

    def _done(self):
        self.busy, self.prog = False, 1.0
        self.refresh_state()


def main(root):
    P = Paths(root)
    host = Host("Sweetie Fox Launcher", W, H)
    host.set_scene(LauncherScene(host, P))
    host.run()
