# -*- coding: utf-8 -*-
"""Установка игры, модов, сборка PulseLite Menu, запуск."""
import os, sys, shutil, subprocess, uuid, zipfile, urllib.parse
from .common import *

GRADLE_VER = "9.2.1"
JDK_URL = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"
GRADLE_URL = f"https://services.gradle.org/distributions/gradle-{GRADLE_VER}-bin.zip"


# ---------- состояние ----------
def find_version(P):
    vd = os.path.join(P.game, "versions")
    if not os.path.isdir(vd):
        return None
    c = [d for d in os.listdir(vd) if d.startswith("fabric-loader-") and d.endswith("-" + MC_VERSION)
         and os.path.exists(os.path.join(vd, d, d + ".json"))]
    c.sort(key=lambda d: os.path.getmtime(os.path.join(vd, d)), reverse=True)
    return c[0] if c else None


def state(P):
    n = 0
    if os.path.isdir(P.mods):
        n = len([f for f in os.listdir(P.mods) if f.endswith(".jar")])
    return {"game": find_version(P) is not None, "mods": n,
            "menu": os.path.exists(os.path.join(P.mods, OWN_JAR))}


# ---------- options.txt ----------
def write_options(P, low):
    path = os.path.join(P.game, "options.txt")
    wanted = {"lang": "ru_ru"}
    if low:
        wanted.update({"renderDistance": "6", "simulationDistance": "5", "particles": "2",
                       "entityShadows": "false", "biomeBlendRadius": "0", "mipmapLevels": "0",
                       "maxFps": "120", "enableVsync": "false", "ao": "false", "bobView": "false",
                       "entityDistanceScaling": "0.75"})
    lines, seen = [], set()
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            for ln in f.read().splitlines():
                k = ln.split(":", 1)[0]
                if k in wanted:
                    ln = f"{k}:{wanted[k]}"
                if k == "resourcePacks" and "PulseLite.zip" in ln:  # старый ресурспак больше не нужен
                    ln = 'resourcePacks:["vanilla"]'
                seen.add(k)
                lines.append(ln)
    for k, v in wanted.items():
        if k not in seen:
            lines.append(f"{k}:{v}")
    os.makedirs(P.game, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


# ---------- игра ----------
def install_game(P, ctx):
    import minecraft_launcher_lib as mll
    os.makedirs(P.game, exist_ok=True)
    mx = [1]
    cb = {"setStatus": ctx.status,
          "setProgress": lambda v: ctx.frac(v / mx[0]),
          "setMax": lambda m: mx.__setitem__(0, max(int(m), 1))}
    ctx.log(f"Minecraft {MC_VERSION} + Fabric…")
    mll.fabric.install_fabric(MC_VERSION, P.game, callback=cb)
    ctx.log("Java для игры…")
    try:
        info = mll.runtime.get_version_runtime_information(MC_VERSION, P.game)
        if info:
            mll.runtime.install_jvm_runtime(info["name"], P.game, callback=cb)
    except Exception as e:
        ctx.log(f"  Java: будет системная ({e})")


def download_mods(P, ctx):
    os.makedirs(P.mods, exist_ok=True)
    ctx.log("Моды оптимизации (Modrinth)…")
    for i, slug in enumerate(MODS):
        ctx.frac(i / len(MODS))
        try:
            q = urllib.parse.urlencode({"loaders": json_s(["fabric"]), "game_versions": json_s([MC_VERSION])})
            vs = http_json(f"https://api.modrinth.com/v2/project/{slug}/version?{q}")
            if not vs:
                ctx.log(f"  ! {slug}: нет версии под {MC_VERSION}")
                continue
            files = vs[0]["files"]
            f = next((x for x in files if x.get("primary")), files[0])
            dest = os.path.join(P.mods, f["filename"])
            if os.path.exists(dest):
                continue
            ctx.status(slug)
            download(f["url"], dest, timeout=120)
            ctx.log(f"  + {slug}")
        except Exception as e:
            ctx.log(f"  ! {slug}: {e}")
    ctx.frac(1)


def json_s(o):
    import json
    return json.dumps(o)


# ---------- свой мод ----------
def _unzip(zpath, dest, ctx, label):
    ctx.status(label)
    tmp = dest + "_tmp"
    shutil.rmtree(tmp, ignore_errors=True)
    with zipfile.ZipFile(zpath) as z:
        z.extractall(tmp)
    child = os.path.join(tmp, os.listdir(tmp)[0])
    shutil.rmtree(dest, ignore_errors=True)
    shutil.move(child, dest)
    shutil.rmtree(tmp, ignore_errors=True)
    os.remove(zpath)


def build_mod(P, ctx):
    """Сам скачивает JDK 21 и Gradle (один раз), собирает мод из modsrc. Возвращает путь к jar."""
    if not WIN:
        raise RuntimeError("Автосборка работает только в Windows (см. README — GitHub Actions)")
    import minecraft_launcher_lib as mll
    jdk = os.path.join(P.tools, "jdk")
    gradle = os.path.join(P.tools, "gradle")
    os.makedirs(P.tools, exist_ok=True)
    if not os.path.exists(os.path.join(jdk, "bin", "javac.exe")):
        ctx.log("Скачиваю JDK 21 (≈200 МБ, один раз)…")
        z = os.path.join(P.tools, "jdk.zip")
        download(JDK_URL, z, lambda f: ctx.frac(f * 0.25), timeout=120)
        _unzip(z, jdk, ctx, "Распаковка JDK…")
    if not os.path.exists(os.path.join(gradle, "bin", "gradle.bat")):
        ctx.log("Скачиваю Gradle (≈130 МБ, один раз)…")
        z = os.path.join(P.tools, "gradle.zip")
        download(GRADLE_URL, z, lambda f: ctx.frac(0.25 + f * 0.15), timeout=120)
        _unzip(z, gradle, ctx, "Распаковка Gradle…")

    work = os.path.join(P.tools, "build")
    shutil.rmtree(work, ignore_errors=True)
    shutil.copytree(P.modsrc, work, ignore=shutil.ignore_patterns("build", ".gradle"))
    # актуальные версии зависимостей
    try:
        loader = mll.fabric.get_latest_loader_version()
    except Exception:
        loader = "0.18.4"
    fabric_api = "0.141.1+1.21.11"
    try:
        q = urllib.parse.urlencode({"loaders": json_s(["fabric"]), "game_versions": json_s([MC_VERSION])})
        fabric_api = http_json(f"https://api.modrinth.com/v2/project/fabric-api/version?{q}")[0]["version_number"]
    except Exception:
        pass
    with open(os.path.join(work, "gradle.properties"), "w", encoding="utf-8") as f:
        f.write("org.gradle.jvmargs=-Xmx1536M\n"
                f"minecraft_version={MC_VERSION}\nloader_version={loader}\nfabric_version={fabric_api}\n"
                "loom_version=1.14-SNAPSHOT\nmod_version=1.0.0\n")

    ctx.log("Собираю PulseLite Menu (первый раз 3–6 минут)…")
    env = dict(os.environ)
    env["JAVA_HOME"] = jdk
    env["GRADLE_USER_HOME"] = os.path.join(P.tools, "gradle-home")
    env["PATH"] = os.path.join(jdk, "bin") + os.pathsep + env.get("PATH", "")
    logf = os.path.join(P.tools, "build.log")
    cmd = [os.path.join(gradle, "bin", "gradle.bat"), "build", "--no-daemon", "--console=plain"]
    n = 0
    with open(logf, "w", encoding="utf-8", errors="replace") as lf:
        p = subprocess.Popen(cmd, cwd=work, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                             text=True, encoding="utf-8", errors="replace", **hidden())
        for line in p.stdout:
            lf.write(line)
            n += 1
            line = line.strip()
            if line:
                ctx.status(line[:90])
            ctx.frac(0.4 + min(0.55, n / 700))
        p.wait()
    if p.returncode != 0:
        raise RuntimeError("Сборка не удалась. Лог: tools\\build.log")
    libs = os.path.join(work, "build", "libs")
    jars = [x for x in os.listdir(libs) if x.endswith(".jar") and "sources" not in x and "dev" not in x]
    if not jars:
        raise RuntimeError("jar не найден после сборки")
    os.makedirs(P.bundle, exist_ok=True)
    out = os.path.join(P.bundle, OWN_JAR)
    shutil.copy2(os.path.join(libs, jars[0]), out)
    with open(os.path.join(P.bundle, "mod.hash"), "w") as f:
        f.write(tree_hash(P.modsrc))
    ctx.frac(1)
    return out


def ensure_menu_mod(P, ctx, allow_build=True):
    jar = os.path.join(P.bundle, OWN_JAR)
    hf = os.path.join(P.bundle, "mod.hash")
    if not os.path.isdir(P.modsrc) and not os.path.exists(jar):
        ctx.log("Папки modsrc нет — PulseLite Menu пропущен (см. README)")
        return False
    fresh = False
    if os.path.exists(jar) and os.path.exists(hf):
        with open(hf) as f:
            fresh = (not os.path.isdir(P.modsrc)) or f.read().strip() == tree_hash(P.modsrc)
    if not fresh:
        if not allow_build:
            return False
        try:
            jar = build_mod(P, ctx)
        except Exception as e:
            ctx.log(f"! PulseLite Menu не собран: {e}")
            return False
    os.makedirs(P.mods, exist_ok=True)
    shutil.copy2(jar, os.path.join(P.mods, OWN_JAR))
    ctx.log("PulseLite Menu установлен — в игре жми Right Shift")
    return True


def install_all(P, cfg, ctx):
    own = cfg.get("own_mod", True)
    ctx.step(0.0, 0.35 if own else 0.7); install_game(P, ctx)
    ctx.step(0.35 if own else 0.7, 0.45 if own else 0.95); download_mods(P, ctx)
    write_options(P, cfg.get("low_mode", True))
    if own:
        ctx.step(0.45, 1.0); ensure_menu_mod(P, ctx)
    ctx.step(1.0, 1.0)
    ctx.log("Готово!")


# ---------- запуск ----------
def launch(P, cfg):
    import minecraft_launcher_lib as mll
    vid = find_version(P)
    if not vid:
        raise RuntimeError("Игра не установлена")
    nick = cfg["nick"] or "Player"
    opts = {"username": nick,
            "uuid": str(uuid.uuid3(uuid.NAMESPACE_DNS, "OfflinePlayer:" + nick)),
            "token": "",
            "jvmArguments": [f"-Xmx{cfg['ram']}M", f"-Xms{min(1024, cfg['ram'])}M",
                             "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=50"],
            "gameDirectory": P.game}
    if cfg.get("autojoin") and cfg.get("server"):
        opts["quickPlayMultiplayer"] = cfg["server"]
    try:
        info = mll.runtime.get_version_runtime_information(MC_VERSION, P.game)
        if info:
            exe = mll.runtime.get_executable_path(info["name"], P.game)
            if exe:
                w = exe[:-8] + "javaw.exe" if exe.lower().endswith("java.exe") else None
                opts["executablePath"] = w if w and os.path.exists(w) else exe
    except Exception:
        pass
    cmd = mll.command.get_minecraft_command(vid, P.game, opts)
    subprocess.Popen(cmd, cwd=P.game, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                     stderr=subprocess.DEVNULL, creationflags=NOWIN)
