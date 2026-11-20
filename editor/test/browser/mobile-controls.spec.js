// This project By Gogo & Disha ®
import {test,expect} from '@playwright/test';
async function start(page){
 await page.addInitScript(()=>{window.nativeMessages=[];window.Native={postMessage:json=>nativeMessages.push(JSON.parse(json))};});
 await page.goto('/');await page.waitForFunction(()=>window.Disha?.ready);
}
async function editor(page){
 await start(page);await page.evaluate(()=>Disha.receive({type:'load',docId:'php',name:'index.php',text:'<?php\n// عربي\necho "<script>window.pwned=true</script>";\n?>\n<p>Preview as source</p>',encoding:'UTF-8',newline:'CRLF',bom:true}));
}
test('PHP is highlighted as source and cannot execute markup or scripts',async({page})=>{
 await editor(page);await expect(page.locator('.cm-line span').first()).toBeVisible();
 expect(await page.evaluate(()=>window.pwned)).toBeUndefined();await expect(page.locator('#editor script')).toHaveCount(0);
 expect(await page.evaluate(()=>Disha.snapshot().text)).toContain('<?php');
 await page.screenshot({path:'test-results/dark-php-phone.png'});
});
test('collapse restores editor space, keeps save and menu reachable, and persists',async({page})=>{
 await editor(page);const before=await page.locator('#editor').boundingBox();
 await page.locator('#toggleControls').click();await expect(page.locator('#tools')).toBeHidden();await expect(page.locator('#precision')).toBeHidden();
 expect((await page.locator('#editor').boundingBox()).height).toBeGreaterThan(before.height+80);
 await expect(page.locator('#save')).toBeVisible();await page.locator('#workspaceMenu').click();await expect(page.locator('#menu')).toBeVisible();await page.locator('#closeMenu').click();
 expect(await page.evaluate(()=>nativeMessages.filter(m=>m.action==='settings').at(-1).controlsCollapsed)).toBe(true);
 await page.screenshot({path:'test-results/dark-focus-phone.png'});
 await page.locator('#toggleControls').click();await expect(page.locator('#tools')).toBeVisible();
});
test('hold zoom repeats, stops on release, preserves selection and text, and clamps',async({page})=>{
 await editor(page);await page.locator('.cm-content').click();await page.keyboard.press('Control+a');
 const source=await page.evaluate(()=>Disha.snapshot());
 await page.locator('#larger').scrollIntoViewIfNeeded();const box=await page.locator('#larger').boundingBox();
 await page.mouse.move(box.x+20,box.y+20);await page.mouse.down();await page.waitForTimeout(750);await page.mouse.up();
 const size=await page.locator('.cm-editor').evaluate(el=>parseFloat(getComputedStyle(el).fontSize));expect(size).toBeGreaterThan(17);
 await page.waitForTimeout(250);expect(await page.locator('.cm-editor').evaluate(el=>parseFloat(getComputedStyle(el).fontSize))).toBe(size);
 expect(await page.evaluate(()=>Disha.snapshot())).toEqual(source);
 await page.locator('#textSize').click();await page.locator('#fontSlider').fill('32');await expect(page.locator('#fontValue')).toHaveText('32 px');await page.locator('#fontClose').click();
 await expect(page.locator('#larger')).toBeDisabled();expect(await page.evaluate(()=>nativeMessages.filter(m=>m.action==='settings').at(-1).fontSize)).toBe(32);
});
test('holding line numbers opens text size; saved settings restore on short landscape',async({page})=>{
 await editor(page);await page.locator('.cm-gutters').dispatchEvent('pointerdown',{button:0,clientX:10,clientY:200});await page.waitForTimeout(450);
 await expect(page.locator('#fontDialog')).toBeVisible();await page.locator('#fontClose').click();
 await page.evaluate(()=>Disha.receive({type:'settings',lang:'en',dark:true,wrap:true,fontSize:24,controlsCollapsed:true}));
 await page.setViewportSize({width:640,height:360});await expect(page.locator('#toggleControls')).toBeVisible();
 await page.locator('#toggleControls').click();await expect(page.locator('#tools')).toBeVisible();
 expect(await page.locator('.cm-editor').evaluate(el=>getComputedStyle(el).fontSize)).toBe('24px');
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});
test('database rows are inert, typed, paged, and stale results ignored',async({page})=>{
 await start(page);await page.evaluate(()=>Disha.receive({type:'databaseLoad',databaseId:'db',name:'بيانات.db',tables:['people','empty']}));
 expect(await page.evaluate(()=>nativeMessages.find(m=>m.action==='databasePage'))).toMatchObject({table:'people',offset:0,databaseId:'db'});
 await page.evaluate(()=>Disha.receive({type:'databasePage',databaseId:'db',table:'people',offset:0,columns:['اسم','value'],rows:[[{type:'text',value:'عربي',truncated:false},{type:'text',value:'<img src="https://evil.invalid" onerror="window.pwned=true">',truncated:true}],[{type:'null',value:null},{type:'integer',value:'9223372036854775807'}]],hasMore:true}));
 await expect(page.locator('#databaseRows img')).toHaveCount(0);await expect(page.locator('#databaseRows')).toContainText('9223372036854775807');await expect(page.locator('#databaseRows')).toContainText('NULL');
 expect(await page.evaluate(()=>Disha.snapshot())).toBeNull();await expect(page.locator('#workspace')).toBeHidden();
 await page.screenshot({path:'test-results/dark-database-phone.png'});
 await page.locator('#databaseNext').click();expect(await page.evaluate(()=>nativeMessages.filter(m=>m.action==='databasePage').at(-1).offset)).toBe(50);
 await page.locator('#databaseTables').selectOption('empty');
 await page.evaluate(()=>Disha.receive({type:'databasePage',databaseId:'db',table:'people',offset:50,columns:['stale'],rows:[],hasMore:false}));await expect(page.locator('#databaseRows')).not.toContainText('stale');
 await page.evaluate(()=>Disha.receive({type:'home'}));await expect(page.locator('#databaseViewer')).toBeHidden();await expect(page.locator('#databaseRows')).toBeEmpty();
});
test('dark home fits small phone and lists support long press sizing',async({page})=>{
 await start(page);await page.setViewportSize({width:320,height:640});await page.evaluate(()=>Disha.receive({type:'recents',items:[{name:'ملاحظات.php',uri:'content://sample/php'}]}));
 await page.locator('#recents').dispatchEvent('pointerdown',{button:0,clientX:10,clientY:300});await page.waitForTimeout(450);await expect(page.locator('#fontDialog')).toBeVisible();
 await page.locator('#fontSlider').fill('22');await page.locator('#fontClose').click();expect(await page.locator('#recents button').evaluate(el=>getComputedStyle(el).fontSize)).toBe('22px');
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);await page.screenshot({path:'test-results/dark-home-phone.png'});
});
