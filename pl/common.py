# -*- coding: utf-8 -*-
import json, os, sys, subprocess, shutil, urllib.request, hashlib

APP = "Sweetie Fox"
VERSION = "2.0"
MC_VERSION = "1.21.11"
UA = "PulseLite-Launcher/2.0"
WIN = sys.platform.startswith("win")
NOWIN = 0x08000000 if WIN else 0
MODS = ["fabric-api", "sodium", "lithium", "ferrite-core", "entityculling",
        "immediatelyfast", "dynamic-fps"]
OWN_JAR = "pulselite-menu.jar"
DEFAULT_CFG = {"nick": "Player", "ram": 2048, "low_mode": True, "server": "mc.funtime.su",
               "autojoin": False, "own_mod": True}


RES = getattr(sys, "_MEIPASS", os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class Paths:
    def __init__(self, root):
        self.root = root
        self.game = os.path.join(root, "game")
        self.libs = os.path.join(root, "libs")
        self.tools = os.path.join(root, "tools")
        self.bundle = os.path.join(root, "bundle")
        self.modsrc = os.path.join(RES, "modsrc")
        self.cfgfile = os.path.join(root, "config.json")
        self.mods = os.path.join(self.game, "mods")


def load_cfg(P):
    try:
        with open(P.cfgfile, encoding="utf-8") as f:
            return {**DEFAULT_CFG, **json.load(f)}
    except Exception:
        return dict(DEFAULT_CFG)


def save_cfg(P, c):
    os.makedirs(P.root, exist_ok=True)
    with open(P.cfgfile, "w", encoding="utf-8") as f:
        json.dump(c, f, ensure_ascii=False, indent=2)


def http_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def download(url, dest, progress=None, timeout=60):
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    tmp = dest + ".part"
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=timeout) as r, open(tmp, "wb") as f:
        total = int(r.headers.get("Content-Length") or 0)
        got = 0
        while True:
            chunk = r.read(1 << 16)
            if not chunk:
                break
            f.write(chunk)
            got += len(chunk)
            if progress and total:
                progress(got / total)
    os.replace(tmp, dest)


def hidden():
    """kwargs для subprocess без окна консоли."""
    if not WIN:
        return {}
    si = subprocess.STARTUPINFO()
    si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
    si.wShowWindow = 0
    return {"creationflags": NOWIN, "startupinfo": si}


def open_folder(p):
    os.makedirs(p, exist_ok=True)
    if WIN:
        os.startfile(p)
    elif sys.platform == "darwin":
        subprocess.Popen(["open", p])
    else:
        subprocess.Popen(["xdg-open", p])


def tree_hash(folder):
    h = hashlib.sha1()
    for dp, dn, fn in os.walk(folder):
        dn[:] = sorted(d for d in dn if d not in ("build", ".gradle", ".git"))
        for n in sorted(fn):
            p = os.path.join(dp, n)
            h.update(os.path.relpath(p, folder).encode())
            with open(p, "rb") as f:
                h.update(f.read())
    return h.hexdigest()


class Ctx:
    """Канал связи фонового потока с интерфейсом."""
    def __init__(self, log, progress, status=None):
        self._log, self._progress, self._status = log, progress, status or (lambda s: None)
        self.lo, self.hi = 0.0, 1.0

    def log(self, m):
        self._log(m)

    def status(self, m):
        self._status(m)

    def step(self, lo, hi):
        self.lo, self.hi = lo, hi
        self._progress(lo)

    def frac(self, f):
        f = max(0.0, min(1.0, f))
        self._progress(self.lo + (self.hi - self.lo) * f)
