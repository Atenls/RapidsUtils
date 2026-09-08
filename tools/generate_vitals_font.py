"""Generate Roboto Mono Medium HUD atlases (fonttools 4.64.0, Pillow 12.3.0).

Usage: python tools/generate_vitals_font.py path/to/RobotoMono-variable.ttf
Source: https://github.com/google/fonts/blob/main/ofl/robotomono/RobotoMono%5Bwght%5D.ttf
Reviewed Git blob: f21d1d716bce3cc756bc618d32be71ce6f733f81
Derivatives are named Rapids Vitals and remain under SIL OFL 1.1.
"""
import hashlib
import json
import sys
from pathlib import Path
from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from PIL import Image, ImageDraw, ImageFont

source = Path(sys.argv[1])
data = source.read_bytes()
assert hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest() == "f21d1d716bce3cc756bc618d32be71ce6f733f81"
font = instantiateVariableFont(TTFont(source, recalcTimestamp=False), {"wght": 500}, inplace=True)
options = subset.Options()
options.name_IDs = [0, 1, 2, 3, 4, 5, 6, 13, 14, 16, 17]
options.name_legacy = True
subsetter = subset.Subsetter(options=options)
subsetter.populate(unicodes=range(32, 127))
subsetter.subset(font)
names = {1: "Rapids Vitals", 2: "Regular", 3: "RapidsVitals-Mono-Medium-3",
         4: "Rapids Vitals Medium", 6: "RapidsVitals-Medium", 16: "Rapids Vitals", 17: "Medium"}
for record in font["name"].names:
    if record.nameID in names:
        record.string = names[record.nameID].encode(record.getEncoding())
root = Path("src/main/resources/assets/rapidsutils")
font_path = root / "font/vitals.ttf"
font.save(font_path)

# A 112-pixel cap height master is retained at 16x. Each output density is
# area-filtered from that master, never enlarged from a seven-pixel bitmap.
master_scale, cell, padding, baseline = 16, 16, 4, 11
size = round(7 * master_scale * font["head"].unitsPerEm / font["OS/2"].sCapHeight)
raster_font = ImageFont.truetype(str(font_path), size)
master = Image.new("L", (16 * cell * master_scale, 6 * cell * master_scale))
draw = ImageDraw.Draw(master)
advances = []
for code in range(32, 127):
    character = chr(code)
    natural = raster_font.getlength(character) / master_scale
    advance = 2.5 if character == "." else 6.0
    advances.append(round(advance, 5))
    slot = code - 32
    x = (slot % 16 * cell + padding + (advance - natural) / 2) * master_scale
    y = (slot // 16 * cell + baseline) * master_scale
    draw.text((x, y), character, font=raster_font, fill=255, anchor="ls")

densities = [*range(1, 9), 16]
target = root / "textures/font"
target.mkdir(parents=True, exist_ok=True)
for density in densities:
    # BOX integrates coverage; unlike nearest-neighbor minification it does not
    # discard thin strokes. Constant white RGB prevents dark transparent fringes.
    alpha = master.resize((16 * cell * density, 6 * cell * density), Image.Resampling.BOX)
    atlas = Image.new("RGBA", alpha.size, (255, 255, 255, 0))
    atlas.putalpha(alpha)
    path = target / f"vitals_{density}x.png"
    atlas.save(path, optimize=True)
    path.with_suffix(".png.mcmeta").write_text(
        '{"texture":{"blur":true,"clamp":true}}\n', encoding="utf-8")
metadata = {"cell": cell, "padding": padding, "capHeight": 7,
            "densities": densities, "advances": advances}
(root / "vitals_metrics.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
print(f"Generated {len(densities)} antialiased atlas densities from a {7 * master_scale}px master")
