import {build} from 'esbuild';
import {mkdir, copyFile, writeFile, readFile} from 'node:fs/promises';
const out = '../app/src/main/assets/editor';
await mkdir(out, {recursive:true});
await build({entryPoints:['src/main.js'], bundle:true, outfile:`${out}/editor.js`, minify:true, target:'chrome80', legalComments:'eof'});
for (const file of ['index.html','style.css']) await copyFile(`src/${file}`, `${out}/${file}`);
const lock = JSON.parse(await readFile('package-lock.json','utf8'));
const notices = [];
for (const [path, pkg] of Object.entries(lock.packages)) {
  if (!path || pkg.dev || pkg.optional) continue;
  let license = '';
  for (const file of ['LICENSE','LICENSE.md','LICENSE.txt']) {
    try { license = await readFile(`${path}/${file}`, 'utf8'); break; } catch {}
  }
  notices.push(`${path} ${pkg.version} (${pkg.license || 'see package'})\n${license}`);
}
await writeFile(`${out}/THIRD_PARTY_NOTICES.txt`, notices.join('\n\n'));
