import {test,expect} from '@playwright/test';
async function load(page,name='sample.json',text='{"key":"عربي"}',extra={}){
 await page.addInitScript(()=>{window.nativeMessages=[];window.Native={postMessage:data=>window.nativeMessages.push(JSON.parse(data))};});
 await page.goto('/');
 await page.waitForFunction(()=>window.Disha?.ready);
 await page.evaluate(doc=>Disha.receive(doc),{type:'load',docId:'test',name,text,encoding:'UTF-8',newline:'LF',bom:false,gzip:false,...extra});
}
test('Arabic phone layout, editing, undo, redo and save payload',async({page})=>{
 await load(page);
 await expect(page.locator('html')).toHaveAttribute('dir','rtl');
 await expect(page.locator('#filename')).toHaveText('sample.json');
 await expect(page.locator('.cm-lineNumbers')).toBeVisible();
 await page.locator('.cm-content').click();await page.keyboard.press('Control+End');await page.keyboard.type(' ');
 await expect(page.locator('#dirty')).toContainText('مش محفوظ');
 await page.locator('#undo').click();await expect(page.locator('#dirty')).toHaveText('محفوظ');
 await page.locator('#redo').click();await page.locator('#saveAs').click();
 expect(await page.evaluate(()=>nativeMessages.find(m=>m.action==='saveAs').text)).toBe('{"key":"عربي"} ');
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});
test('HTML is inert and no remote requests',async({page})=>{
 const requests=[];page.on('request',r=>requests.push(r.url()));
 await load(page,'test.html','<img src="https://evil.invalid/a" onerror="window.pwned=true"><script>window.pwned=true</script>');
 expect(await page.evaluate(()=>window.pwned)).toBeUndefined();
 expect(requests.every(u=>u.startsWith('http://127.0.0.1:4173/'))).toBe(true);
 await expect(page.locator('#editor img')).toHaveCount(0);
});
test('format malformed JSON refuses without loss and reports location',async({page})=>{
 const source='{\n  "x": }';await load(page,'bad.json',source);
 await page.locator('#format').click();await page.locator('#indent2').click();
 await expect(page.locator('#message')).toContainText('2');
 expect(await page.evaluate(()=>Disha.snapshot().text)).toBe(source);
});
test('pretty/minify remain undoable and preserve numbers',async({page})=>{
 await load(page,'big.json','{"n":900719925474099312345,"n":2}');
 await page.locator('#format').click();await page.locator('#indent4').click();
 expect(await page.evaluate(()=>Disha.snapshot().text)).toContain('\n    "n": 900719925474099312345');
 await page.locator('#undo').click();expect(await page.evaluate(()=>Disha.snapshot().text)).toBe('{"n":900719925474099312345,"n":2}');
});
test('read-only preview prevents editing and exporting',async({page})=>{
 await load(page,'large.txt','preview',{preview:true});
 await expect(page.locator('.cm-content')).toHaveAttribute('contenteditable','false');
 for(const id of ['save','saveAs','format'])await expect(page.locator(`#${id}`)).toBeDisabled();
});
test('search replace and theme/language controls work',async({page})=>{
 await load(page,'notes.txt','hello hello');await page.locator('#find').click();
 await expect(page.locator('.cm-search')).toBeVisible();
 await page.locator('input[name=search]').fill('hello');await page.locator('input[name=replace]').fill('أهلا');
 await page.getByRole('button',{name:'replace all',exact:true}).click();
 expect(await page.evaluate(()=>Disha.snapshot().text)).toBe('أهلا أهلا');
 await page.locator('#menuButton').click();await page.locator('#language').click();await page.locator('#theme').click();await page.locator('#closeMenu').click();
 await expect(page.locator('html')).toHaveAttribute('lang','en');await expect(page.locator('body')).toHaveClass(/light/);
});
test('copy selection sends exact Unicode and navigation snapshots latest keystroke',async({page})=>{
 await load(page,'sample.md','# عربي');await page.locator('.cm-content').click();await page.keyboard.press('Control+a');await page.locator('#copy').click();
 expect(await page.evaluate(()=>nativeMessages.find(m=>m.action==='copy').text)).toBe('# عربي');
 await page.locator('#menuButton').click();await page.locator('#homeMenu').click();
 expect(await page.evaluate(()=>nativeMessages.find(m=>m.action==='home').text)).toBe('# عربي');
});
