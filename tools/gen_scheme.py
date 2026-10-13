# Requires: pip install materialyoucolor. Run: python3 tools/gen_scheme.py
# Generates the Material 3 colour scheme (tonal spot, 2021 spec = Material Theme Builder) for the
# LibraryUp seed plus harmonized custom status colours, and prints Kotlin.
from materialyoucolor.hct import Hct
from materialyoucolor.scheme.scheme_tonal_spot import SchemeTonalSpot
from materialyoucolor.dynamiccolor.material_dynamic_colors import MaterialDynamicColors
from materialyoucolor.blend.blend import Blend
from materialyoucolor.palettes.tonal_palette import TonalPalette
import re
SEED = 0xFF283593
roles = ["primary","onPrimary","primaryContainer","onPrimaryContainer","inversePrimary","secondary","onSecondary",
 "secondaryContainer","onSecondaryContainer","tertiary","onTertiary","tertiaryContainer","onTertiaryContainer",
 "background","onBackground","surface","onSurface","surfaceVariant","onSurfaceVariant","surfaceTint","inverseSurface",
 "inverseOnSurface","error","onError","errorContainer","onErrorContainer","outline","outlineVariant","scrim",
 "surfaceBright","surfaceContainer","surfaceContainerHigh","surfaceContainerHighest","surfaceContainerLow",
 "surfaceContainerLowest","surfaceDim"]
snake = lambda n: re.sub(r'([A-Z])', r'_\1', n).lower()
mdc = MaterialDynamicColors(spec="2021")
def hexc(argb): return "Color(0xFF%06X)" % (argb & 0xFFFFFF)
for dark in (False, True):
    s = SchemeTonalSpot(Hct.from_int(SEED), dark, 0.0, spec_version="2021")
    print(f"val {'Dark' if dark else 'Light'}Scheme = {'darkColorScheme' if dark else 'lightColorScheme'}(")
    for r in roles:
        dc = getattr(mdc, r, None)
        if dc is None: print("   // missing", r); continue
        print(f"    {r} = {hexc(dc.get_argb(s))},")
    print(")\n")
customs = {"success": 0xFF2E7D32, "warning": 0xFFF9A825, "overdue": 0xFFEF6C00}
for name, c in customs.items():
    h = Blend.harmonize(c, SEED)
    p = TonalPalette.from_hct(Hct.from_int(h))
    print(name, "light", [hexc(p.tone(t)) for t in (40,100,90,10)], "dark", [hexc(p.tone(t)) for t in (80,20,30,90)])
