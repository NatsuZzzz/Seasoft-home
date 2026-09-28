// Tu host thu vien + font cho frontend (khong phu thuoc CDN ben thu ba).
// Chay lai sau khi nang version thu vien hoac them icon moi:  npm run vendor
//
//  - GSAP, ScrollTrigger, Lenis      -> Html/vendor/
//  - Font Inter (latin, latin-ext, tieng Viet)  -> Html/fonts/
//  - Material Symbols: CHI cac icon dang dung (quet tu HTML/JS) -> Html/fonts/material-symbols.woff2
//  - Html/css/fonts.css  (@font-face cho ca 2 font + class .material-symbols-outlined)
import fs from "node:fs";
import path from "node:path";

const ROOT = path.resolve(import.meta.dirname, "..");
const HTML = path.join(ROOT, "Html");
const NM = path.join(ROOT, "node_modules");
const out = (...p) => path.join(HTML, ...p);

fs.mkdirSync(out("vendor"), { recursive: true });
fs.mkdirSync(out("fonts"), { recursive: true });

// ---------- 1. Thu vien JS ----------
for (const [src, dest] of [
  ["gsap/dist/gsap.min.js", "gsap.min.js"],
  ["gsap/dist/ScrollTrigger.min.js", "ScrollTrigger.min.js"],
  ["lenis/dist/lenis.min.js", "lenis.min.js"],
  ["lenis/dist/lenis.css", "lenis.css"],
]) {
  fs.copyFileSync(path.join(NM, src), out("vendor", dest));
}

// ---------- 2. Font Inter (chi 3 bo ky tu can cho tieng Viet) ----------
const interCss = fs.readFileSync(path.join(NM, "@fontsource-variable/inter/index.css"), "utf8");
const wanted = ["vietnamese", "latin-ext", "latin"];
let fontsCss = "/* File sinh tu dong boi scripts/vendor.mjs - khong sua tay */\n\n";
for (const subset of wanted) {
  const file = `inter-${subset}-wght-normal.woff2`;
  const block = interCss.match(new RegExp(`/\\* inter-${subset}-wght-normal \\*/\\s*@font-face \\{[^}]*\\}`));
  if (!block) throw new Error("Khong tim thay @font-face cho " + subset);
  fs.copyFileSync(path.join(NM, "@fontsource-variable/inter/files", file), out("fonts", file));
  fontsCss += block[0]
    .replace("'Inter Variable'", "'Inter'")
    .replace(`./files/${file}`, `../fonts/${file}`) + "\n\n";
}

// ---------- 3. Material Symbols: quet icon dang dung ----------
const files = [
  ...fs.readdirSync(HTML).filter((f) => f.endsWith(".html")).map((f) => path.join(HTML, f)),
  ...["js", "js/admin"].flatMap((d) =>
    fs.readdirSync(out(d)).filter((f) => f.endsWith(".js")).map((f) => out(d, f)),
  ),
];
const icons = new Set();
for (const f of files) {
  const s = fs.readFileSync(f, "utf8");
  for (const m of s.matchAll(/material-symbols-outlined[^>]*>\s*([a-z0-9_]+)\s*</g)) icons.add(m[1]);
  for (const m of s.matchAll(/icon:\s*"([a-z0-9_]+)"/g)) icons.add(m[1]);
  // Icon chon bang code: danh dau trong file JS/HTML bang comment  /* icons: a, b, c */
  for (const m of s.matchAll(/icons:\s*([a-z0-9_,\s]+)\*\//g)) m[1].split(/[,\s]+/).filter(Boolean).forEach((i) => icons.add(i));
}
const iconList = [...icons].sort();
const url =
  "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0" +
  `&icon_names=${iconList.join(",")}&display=block`;
const ua = { headers: { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/140 Safari/537.36" } };
const css = await (await fetch(url, ua)).text();
const fontUrl = css.match(/url\((https:\/\/fonts\.gstatic\.com[^)]+)\)/)?.[1];
if (!fontUrl) throw new Error("Google Fonts khong tra ve font icon:\n" + css.slice(0, 500));
fs.writeFileSync(out("fonts", "material-symbols.woff2"), Buffer.from(await (await fetch(fontUrl, ua)).arrayBuffer()));

fontsCss += `/* Material Symbols Outlined - chi ${iconList.length} icon: ${iconList.join(", ")} */
@font-face {
  font-family: 'Material Symbols Outlined';
  font-style: normal;
  font-weight: 400;
  font-display: block;
  src: url(../fonts/material-symbols.woff2) format('woff2');
}
.material-symbols-outlined {
  font-family: 'Material Symbols Outlined';
  font-weight: normal;
  font-style: normal;
  font-size: 24px;
  line-height: 1;
  letter-spacing: normal;
  text-transform: none;
  display: inline-block;
  white-space: nowrap;
  word-wrap: normal;
  direction: ltr;
  -webkit-font-feature-settings: 'liga';
  -webkit-font-smoothing: antialiased;
}
`;
fs.writeFileSync(out("css", "fonts.css"), fontsCss);

const size = (p) => (fs.statSync(p).size / 1024).toFixed(1) + " KB";
console.log(`vendor: gsap ${size(out("vendor/gsap.min.js"))}, ScrollTrigger ${size(out("vendor/ScrollTrigger.min.js"))}, lenis ${size(out("vendor/lenis.min.js"))}`);
console.log(`fonts : Inter x3, Material Symbols ${iconList.length} icon = ${size(out("fonts/material-symbols.woff2"))}`);
