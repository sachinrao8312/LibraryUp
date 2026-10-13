# Sorts, de-duplicates and drops unused imports in the Kotlin files given.
# Delegate operators (getValue/setValue) and wildcard imports are always kept.
import re, sys
KEEP = {"getValue", "setValue", "provideDelegate", "*"}
for path in sys.argv[1:]:
    lines = open(path).read().split("\n")
    idx = [i for i, l in enumerate(lines) if l.startswith("import ")]
    if not idx:
        continue
    first, last = idx[0], idx[-1]
    block = lines[first:last + 1]
    if any(l and not l.startswith("import ") for l in block):
        continue
    body = "\n".join(lines[last + 1:])
    def used(imp):
        name = imp.split(" as ")[-1].split(".")[-1].strip()
        return name in KEEP or re.search(r"\b%s\b" % re.escape(name), body)
    imports = sorted({l for l in block if l and used(l)}, key=lambda l: (l.startswith("import java"), l.startswith("import kotlin"), l))
    open(path, "w").write("\n".join(lines[:first] + imports + lines[last + 1:]))
