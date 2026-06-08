import { copyFileSync, mkdirSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = dirname(fileURLToPath(import.meta.url));
const frontendRoot = join(root, "..");
const packageRoot = join(frontendRoot, "node_modules", "@searchstax-inc", "searchstudio-ux-js", "dist");
const clientlibRoot = join(
  frontendRoot,
  "..",
  "content",
  "jcr_root",
  "apps",
  "searchstaxconnector",
  "clientlibs",
  "clientlib-search-ux-vendor"
);

const copies = [
  {
    from: join(packageRoot, "@searchstax-inc", "searchstudio-ux-js.iife.js"),
    to: join(clientlibRoot, "js", "searchstudio-ux-js.js")
  },
  {
    from: join(packageRoot, "styles", "mainTheme.css"),
    to: join(clientlibRoot, "css", "mainTheme.css")
  }
];

for (const entry of copies) {
  if (!existsSync(entry.from)) {
    console.error("Missing vendor file:", entry.from);
    process.exit(1);
  }
  mkdirSync(dirname(entry.to), { recursive: true });
  copyFileSync(entry.from, entry.to);
  console.log("Copied", entry.to);
}
