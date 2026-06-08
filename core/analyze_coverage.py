import csv
from pathlib import Path

rows = list(csv.DictReader(Path("target/site/jacoco/jacoco.csv").open()))
m = sum(int(r["INSTRUCTION_MISSED"]) for r in rows)
c = sum(int(r["INSTRUCTION_COVERED"]) for r in rows)
print(f"Total: {100*c/(m+c):.1f}% ({c} covered, {m} missed)")
rows.sort(key=lambda r: int(r["INSTRUCTION_MISSED"]), reverse=True)
for r in rows[:30]:
    total = int(r["INSTRUCTION_MISSED"]) + int(r["INSTRUCTION_COVERED"])
    pct = 100 * int(r["INSTRUCTION_COVERED"]) / total if total else 100
    if int(r["INSTRUCTION_MISSED"]) > 0:
        print(f"{r['CLASS']:50} {pct:5.1f}% missed={r['INSTRUCTION_MISSED']:>4}")
