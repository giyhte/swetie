# -*- coding: utf-8 -*-
"""Подготовка окружения без консоли: ставит Pillow и minecraft-launcher-lib в папку libs."""
import os, sys, subprocess, threading, traceback, time

NOWIN = 0x08000000 if sys.platform.startswith("win") else 0
REQS = ["Pillow>=10.1", "minecraft-launcher-lib>=7.0"]


def fix_stdio():
    for n in ("stdout", "stderr"):
        if getattr(sys, n) is None:
            setattr(sys, n, open(os.devnull, "w"))


def _libs(root):
    p = os.path.join(root, "libs")
    if p not in sys.path:
        sys.path.insert(0, p)
    return p


def _ok():
    try:
        import PIL.ImageTk, minecraft_launcher_lib  # noqa
        return True
    except Exception:
        return False


def _python_exe():
    exe = sys.executable
    if os.path.basename(exe).lower() == "pythonw.exe":
        cand = os.path.join(os.path.dirname(exe), "python.exe")
        if os.path.exists(cand):
            return cand
    return exe


def pythonw_exe():
    exe = sys.executable
    if os.path.basename(exe).lower() == "python.exe":
        cand = os.path.join(os.path.dirname(exe), "pythonw.exe")
        if os.path.exists(cand):
            return cand
    return exe


def ensure_deps(root):
    libs = _libs(root)
    if _ok():
        return
    import tkinter as tk
    from tkinter import messagebox
    w, h = 460, 200
    r = tk.Tk()
    r.title("PulseLite")
    r.resizable(False, False)
    r.geometry(f"{w}x{h}+{(r.winfo_screenwidth()-w)//2}+{(r.winfo_screenheight()-h)//2}")
    c = tk.Canvas(r, width=w, height=h, highlightthickness=0, bd=0)
    c.pack()
    a, b = (27, 20, 100), (15, 155, 215)
    for y in range(h):
        k = y / h
        col = "#%02x%02x%02x" % tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))
        c.create_line(0, y, w, y, fill=col)
    c.create_text(w // 2, 70, text="PULSELITE", fill="white", font=("Segoe UI", -34, "bold"))
    c.create_text(w // 2, 112, text="Подготавливаю компоненты (один раз)…", fill="#dfe6ff",
                  font=("Segoe UI", -14))
    bar = c.create_rectangle(60, 150, 100, 158, fill="white", width=0)
    c.create_rectangle(60, 150, w - 60, 158, outline="#ffffff55" if False else "#8fb4ff")
    res = {}

    def work():
        try:
            os.makedirs(libs, exist_ok=True)
            cmd = [_python_exe(), "-m", "pip", "install", "--target", libs, "--upgrade",
                   "--disable-pip-version-check", "--no-warn-script-location"] + REQS
            p = subprocess.run(cmd, capture_output=True, text=True, creationflags=NOWIN)
            res["code"] = p.returncode
            res["out"] = (p.stdout or "") + (p.stderr or "")
        except Exception as e:
            res["code"], res["out"] = 1, str(e)

    th = threading.Thread(target=work, daemon=True)
    th.start()
    st = {"x": 60, "d": 6}

    def anim():
        if not th.is_alive():
            r.destroy()
            return
        st["x"] += st["d"]
        if st["x"] > w - 60 - 60 or st["x"] < 60:
            st["d"] = -st["d"]
        c.coords(bar, st["x"], 151, st["x"] + 60, 157)
        r.after(30, anim)

    anim()
    r.mainloop()
    sys.path_importer_cache.clear()
    if res.get("code") != 0 or not _ok():
        rr = tk.Tk(); rr.withdraw()
        messagebox.showerror("PulseLite", "Не удалось установить компоненты.\nПроверь интернет и попробуй снова.\n\n"
                             + res.get("out", "")[-600:])
        sys.exit(1)


def crash(root):
    tb = traceback.format_exc()
    try:
        with open(os.path.join(root, "pulselite_crash.log"), "a", encoding="utf-8") as f:
            f.write(time.strftime("%Y-%m-%d %H:%M:%S\n") + tb + "\n")
    except Exception:
        pass
    try:
        import tkinter as tk
        from tkinter import messagebox
        r = tk.Tk(); r.withdraw()
        messagebox.showerror("PulseLite", "Ошибка:\n\n" + tb[-900:] + "\n\nЛог: pulselite_crash.log")
    except Exception:
        pass
