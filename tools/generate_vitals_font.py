"""Build a static ASCII Nunito SemiBold subset (Python + fonttools 4.64.0).

Usage: python tools/generate_vitals_font.py path/to/Nunito-variable.ttf
Source: https://github.com/googlefonts/nunito/blob/main/fonts/variable/Nunito%5Bwght%5D.ttf
Git blob: 2ec1f4b0676c83ef33049db87b311171699e9192
The bundled derivative is named Rapids Vitals and remains under SIL OFL 1.1.
"""
import hashlib
import sys
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

source = Path(sys.argv[1])
data = source.read_bytes()
blob_hash = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
if blob_hash != "2ec1f4b0676c83ef33049db87b311171699e9192":
    raise ValueError("Source font differs from the reviewed Nunito revision")

font = instantiateVariableFont(TTFont(source, recalcTimestamp=False), {"wght": 600}, inplace=True)
options = subset.Options()
options.name_IDs = [0, 1, 2, 3, 4, 5, 6, 13, 14, 16, 17]
options.name_legacy = True
subsetter = subset.Subsetter(options=options)
subsetter.populate(unicodes=range(32, 127))
subsetter.subset(font)
names = {
    1: "Rapids Vitals", 2: "Regular", 3: "RapidsVitals-SemiBold-1",
    4: "Rapids Vitals SemiBold", 6: "RapidsVitals-SemiBold",
    16: "Rapids Vitals", 17: "SemiBold",
}
for record in font["name"].names:
    if record.nameID in names:
        record.string = names[record.nameID].encode(record.getEncoding())

advances = {font["hmtx"][font.getBestCmap()[c]][0] for c in range(48, 58)}
assert advances == {600}, "Keep tabular digits at six GUI pixels with size 10"
output = Path("src/main/resources/assets/rapidsutils/font/vitals.ttf")
font.save(output)
print(f"Wrote {output} ({output.stat().st_size} bytes)")
