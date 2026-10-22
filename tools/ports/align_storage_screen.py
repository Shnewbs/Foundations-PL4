"""Port the released compact Wireless Storage layout while retaining native APIs."""
from pathlib import Path
import argparse
from align_performance import replace_once


def screen(source):
    if "w,panelHeight,rowPitch;" in source:
        return source
    source = replace_once(source, "left,top,w;", "left,top,w,panelHeight,rowPitch;")
    source = replace_once(source, "w=Math.min(420,width-12);left=(width-w)/2;top=(height-254)/2;",
        "w=Math.min(420,width-12);left=(width-w)/2;panelHeight=Math.min(254,height-12);top=(height-panelHeight)/2;\n"
        "        rowPitch=Math.min(23,Math.max(18,(panelHeight-114)/WirelessStorage.PAGE_SIZE));")
    for before, after in [("top+73+i*23", "top+73+i*rowPitch"),
                          ("top+79+i*23", "top+79+i*rowPitch"),
                          ("top+254,0xF21B2533", "top+panelHeight,0xF21B2533"),
                          ("top+240,0xFF8CAEC5", "top+panelHeight-14,0xFF8CAEC5")]:
        source = replace_once(source, before, after)
    if source.count("top+214,") != 5:
        raise ValueError("Unexpected Wireless Storage footer controls")
    source = source.replace("top+214,", "top+panelHeight-40,")
    return source


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    path = args.root / "src/main/java/net/foundations/pl4/client/WirelessStorageScreen.java"
    before = path.read_text()
    after = screen(before)
    if screen(after) != after:
        raise ValueError("Screen migration is not idempotent")
    if after != before:
        path.write_text(after)
        print("UPDATED", path)
    else:
        print("ALREADY ALIGNED", path)


if __name__ == "__main__":
    main()
