# -*- coding: utf-8 -*-
import os, sys
FROZEN = getattr(sys, "frozen", False)
ROOT = os.path.dirname(sys.executable) if FROZEN else os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ROOT)
from pl import boot
boot.fix_stdio()
try:
    if FROZEN:
        from pl import installer
        if not installer.is_installed() and not os.path.exists(os.path.join(ROOT, 'portable.txt')):
            installer.main()
            sys.exit(0)
    else:
        boot.ensure_deps(ROOT)
    from pl.launcher_app import main
    main(ROOT)
except SystemExit:
    raise
except Exception:
    boot.crash(ROOT)
