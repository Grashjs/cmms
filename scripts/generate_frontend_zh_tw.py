#!/usr/bin/env python3
"""Convert frontend/src/i18n/translations/zh_cn.ts -> zh_tw.ts using OpenCC (s2twp)
and report any keys present in en.ts but missing from zh_cn.ts.

Usage (from the root of your cloned Grashjs/cmms repo):
    pip install opencc-python-reimplemented
    python3 scripts/generate_frontend_zh_tw.py
"""

import re
import sys
from pathlib import Path

try:
    from opencc import OpenCC
except ImportError:
    sys.exit("請先安裝 OpenCC：pip install opencc-python-reimplemented")

ROOT = Path.cwd()
EN_PATH = ROOT / "frontend/src/i18n/translations/en.ts"
ZH_CN_PATH = ROOT / "frontend/src/i18n/translations/zh_cn.ts"
ZH_TW_PATH = ROOT / "frontend/src/i18n/translations/zh_tw.ts"
MISSING_PATH = ROOT / "frontend_missing_keys_en.txt"

KEY_RE = re.compile(r"^\s*(['\"]?)([A-Za-z0-9_.-]+)\1\s*:", re.MULTILINE)


def main() -> None:
    if not ZH_CN_PATH.exists() or not EN_PATH.exists():
        sys.exit("請在 Grashjs/cmms 專案根目錄執行此腳本。")

    cc = OpenCC("s2twp")  # 簡體中文 -> 臺灣正體中文（含常用詞彙轉換）
    zh_cn_text = ZH_CN_PATH.read_text(encoding="utf-8")
    en_text = EN_PATH.read_text(encoding="utf-8")

    # 僅轉換字串值，保留 key 與程式碼語法不變
    def convert_line(line: str) -> str:
        m = re.match(r"^(\s*['\"]?[A-Za-z0-9_.-]+['\"]?\s*:\s*)(.+)$", line)
        if not m:
            return line
        prefix, value = m.groups()
        return prefix + cc.convert(value)

    zh_tw_lines = [convert_line(ln) for ln in zh_cn_text.splitlines()]
    try:
        with ZH_TW_PATH.open("x", encoding="utf-8") as output:
            output.write("\n".join(zh_tw_lines) + "\n")
    except FileExistsError:
        sys.exit(f"已存在 {ZH_TW_PATH}，請先備份並移開現有翻譯。")

    en_keys = {m.group(2) for m in KEY_RE.finditer(en_text)}
    zh_keys = {m.group(2) for m in KEY_RE.finditer(zh_cn_text)}
    missing = sorted(en_keys - zh_keys)

    if missing:
        missing_lines = []
        for ln in en_text.splitlines():
            km = KEY_RE.match(ln)
            if km and km.group(2) in missing:
                missing_lines.append(ln)
        MISSING_PATH.write_text("\n".join(missing_lines) + "\n", encoding="utf-8")
        print(f"已產生 {ZH_TW_PATH}")
        print(f"偵測到 {len(missing)} 個 en.ts 有但 zh_cn.ts 缺少的 key，已輸出至 {MISSING_PATH}")
    else:
        print(f"已產生 {ZH_TW_PATH}，且與 en.ts key 完全一致。")


if __name__ == "__main__":
    main()
