# Sorts and de-duplicates the import block of each Kotlin file given (keeps code review diffs tidy).
import sys
for path in sys.argv[1:]:
    lines = open(path).read().split("\n")
    idx = [i for i, l in enumerate(lines) if l.startswith("import ")]
    if not idx:
        continue
    first, last = idx[0], idx[-1]
    block = lines[first:last + 1]
    if any(l and not l.startswith("import ") for l in block):
        continue  # comments or code between imports: leave alone
    imports = sorted({l for l in block if l}, key=lambda l: (l.startswith("import java"), l.startswith("import kotlin"), l))
    open(path, "w").write("\n".join(lines[:first] + imports + lines[last + 1:]))
