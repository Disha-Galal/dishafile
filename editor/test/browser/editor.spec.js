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
 await page.screenshot({path:'test-results/editor-phone.png'});
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
 await page.locator('button[name=replaceAll]').click();
 expect(await page.evaluate(()=>Disha.snapshot().text)).toBe('أهلا أهلا');
 await page.locator('#workspaceMenu').click();await page.locator('#language').click();await page.locator('#theme').click();await page.locator('#closeMenu').click();
 await expect(page.locator('html')).toHaveAttribute('lang','en');await expect(page.locator('body')).toHaveClass(/light/);
});
test('copy selection sends exact Unicode and navigation snapshots latest keystroke',async({page})=>{
 await load(page,'sample.md','# عربي');await page.locator('.cm-content').click();await page.keyboard.press('Control+a');await page.locator('#copy').click();
 expect(await page.evaluate(()=>nativeMessages.find(m=>m.action==='copy').text)).toBe('# عربي');
 await page.locator('#workspaceMenu').click();await page.locator('#homeMenu').click();
 expect(await page.evaluate(()=>nativeMessages.find(m=>m.action==='home').text)).toBe('# عربي');
});
test('precise line selection, cursor controls and AI apply are undoable',async({page})=>{
 await load(page,'main.ts','const x = 1;\nconst name = "عربي";\n');
 await page.locator('#selectRange').click();await page.locator('#rangeFrom').fill('2');await page.locator('#rangeTo').fill('2');await page.locator('#rangeApply').click();
 await page.locator('#copy').click();expect(await page.evaluate(()=>nativeMessages.find(m=>m.action==='copy').text)).toBe('const name = "عربي";');
 await page.locator('#aiOpen').click();await page.locator('#aiInstruction').fill('Rewrite');await page.locator('#aiSend').click();
 const request=await page.evaluate(()=>nativeMessages.find(m=>m.action==='aiRequest'));
 expect(request.allowCloud).toBe(false);expect(request.content).toBe('const name = "عربي";');
 await page.evaluate(id=>Disha.receive({type:'aiResult',requestId:id,model:'qwen',text:'const name = "ديشا";',failures:['llama']}),request.requestId);
 expect(await page.evaluate(()=>Disha.snapshot().text)).toContain('عربي');
 await page.locator('#aiApply').click();expect(await page.evaluate(()=>Disha.snapshot().text)).toBe('const x = 1;\nconst name = "ديشا";\n');
 await page.locator('#undo').click();expect(await page.evaluate(()=>Disha.snapshot().text)).toContain('عربي');
});
test('AI output is inert and a stale suggestion cannot modify the document',async({page})=>{
 await load(page,'test.html','<p>عربي</p>');await page.locator('#aiOpen').click();await page.locator('#aiInstruction').fill('Rewrite');await page.locator('#aiCloud').check();await page.locator('#aiSend').click();
 await expect(page.locator('#aiCloud')).not.toBeChecked();expect(await page.evaluate(()=>nativeMessages.find(m=>m.action==='aiRequest').allowCloud)).toBe(true);
 const id=await page.evaluate(()=>nativeMessages.find(m=>m.action==='aiRequest').requestId);
 await page.evaluate(()=>{Disha.receive({type:'saved',docId:'test',name:'test.html',text:'<p>عربي</p>',encoding:'UTF-8',newline:'LF'});document.querySelector('#aiDialog').close();});
 await page.locator('.cm-content').click();await page.keyboard.press('Control+End');await page.keyboard.type('changed');
 await page.evaluate(id=>{Disha.receive({type:'aiResult',requestId:id,model:'llama',text:'<img src="https://evil.invalid" onerror="window.pwned=true">'});document.querySelector('#aiDialog').showModal();},id);
 await expect(page.locator('#aiOutput img')).toHaveCount(0);await page.locator('#aiApply').click();
 expect(await page.evaluate(()=>Disha.snapshot().text)).toContain('changed');expect(await page.evaluate(()=>window.pwned)).toBeUndefined();
});
test('AI failures, cancellation and read-only previews never replace source',async({page})=>{
 await load(page,'x.txt','preview',{preview:true});await page.locator('#aiOpen').click();await page.locator('#aiInstruction').fill('Explain');await page.locator('#aiSend').click();
 const id=await page.evaluate(()=>nativeMessages.find(m=>m.action==='aiRequest').requestId);
 await page.evaluate(id=>Disha.receive({type:'aiResult',requestId:id,model:'llama',text:'answer'}),id);
 await expect(page.locator('#aiApply')).toBeDisabled();await page.locator('#aiSend').click();await page.locator('#aiClose').click();
 expect(await page.evaluate(()=>nativeMessages.some(m=>m.action==='aiCancel'))).toBe(true);
 expect(await page.evaluate(()=>Disha.snapshot().text)).toBe('preview');
});
for(const [name,text] of [['sample.ts','const اسم: string = "عربي";'],['sample.kt','val name: String = "عربي"'],['sample.toml','name = "عربي"'],['sample.rs','let name = "عربي";'],['sample.tsx','const view = <p>عربي</p>;']]){
 test(`extended syntax highlighting preserves ${name}`,async({page})=>{
  await load(page,name,text);await expect(page.locator('.cm-content .cm-line span').first()).toBeVisible();
  expect(await page.evaluate(()=>Disha.snapshot().text)).toBe(text);
 });
}
test('NDJSON validation reports the malformed record',async({page})=>{
 await load(page,'events.ndjson','{"name":"عربي"}\n{"n": }');await page.locator('#workspaceMenu').click();await page.locator('#validate').click();
 await expect(page.locator('#message')).toContainText('2');
 expect(await page.evaluate(()=>Disha.snapshot().text)).toBe('{"name":"عربي"}\n{"n": }');
});
test('AI refuses oversized Arabic context without silently truncating it',async({page})=>{
 const source='ع'.repeat(4000);await load(page,'arabic.txt',source);
 await page.locator('#aiOpen').click();await page.locator('#aiInstruction').fill('اشرح');await page.locator('#aiSend').click();
 expect(await page.evaluate(()=>nativeMessages.some(m=>m.action==='aiRequest'))).toBe(false);
 await expect(page.locator('#aiStatus')).toContainText('6000');
 expect(await page.evaluate(()=>Disha.snapshot().text)).toBe(source);
});
test('direct Gemini translation requests cloud explicitly and applies only after review',async({page})=>{
 await load(page,'hello.txt','Hello world');await page.locator('#aiOpen').click();
 await page.locator('#aiProvider').selectOption('gemini');await expect(page.locator('#aiCloud')).toBeDisabled();
 await page.locator('#aiTranslate').click();await expect(page.locator('#aiInstruction')).toHaveValue(/Translate.*Arabic/);
 expect(await page.evaluate(()=>nativeMessages.some(m=>m.action==='aiRequest'))).toBe(false);
 await page.locator('#aiSend').click();
 const request=await page.evaluate(()=>nativeMessages.find(m=>m.action==='aiRequest'));
 expect(request.provider).toBe('gemini');expect(request.allowCloud).toBe(true);expect(request.content).toBe('Hello world');
 await page.evaluate(id=>Disha.receive({type:'aiResult',requestId:id,model:'gemini-3.1-flash-lite',text:'مرحبًا بالعالم'}),request.requestId);
 expect(await page.evaluate(()=>Disha.snapshot().text)).toBe('Hello world');
 await page.locator('#aiApply').click();expect(await page.evaluate(()=>Disha.snapshot().text)).toBe('مرحبًا بالعالم');
 await page.locator('#undo').click();expect(await page.evaluate(()=>Disha.snapshot().text)).toBe('Hello world');
});
test('provider errors leave text untouched and settings are accessible from AI',async({page})=>{
 await load(page,'hello.txt','Hello world');await page.locator('#aiOpen').click();await page.locator('#aiTranslateLanguage').selectOption('English');
 await page.locator('#aiTranslate').click();await expect(page.locator('#aiInstruction')).toHaveValue(/Translate.*English/);
 await page.locator('#aiSend').click();const id=await page.evaluate(()=>nativeMessages.find(m=>m.action==='aiRequest').requestId);
 await page.evaluate(requestId=>Disha.receive({type:'aiError',requestId,text:'Model unavailable. Choose and test a Gemini model in settings.'}),id);
 await expect(page.locator('#aiStatus')).toContainText('Model unavailable');await expect(page.locator('#aiApply')).toBeDisabled();
 expect(await page.evaluate(()=>Disha.snapshot().text)).toBe('Hello world');
 await page.locator('#aiConfigure').click();expect(await page.evaluate(()=>nativeMessages.some(m=>m.action==='aiSettings'))).toBe(true);
});
