import {test,expect} from '@playwright/test';
const rows=[
 {section:'Request',group:'Form',key:'username',value:'demo@example.test',source:'Flow #1 > Request > Form > username',type:'value'},
 {section:'Request',group:'Form',key:'password',value:'sample123',source:'Flow #1 > Request > Form > password',sensitive:true,type:'value'},
 {section:'Request',group:'Form',key:'empty',value:'',source:'Flow #1 > Request > Form > empty',type:'value'},
 {section:'Response',group:'Headers',key:'set-cookie',value:'a=1',source:'Flow #1 > Response > Headers > set-cookie',sensitive:true,type:'value'},
 {section:'Response',group:'Headers',key:'set-cookie',value:'b=2',source:'Flow #1 > Response > Headers > set-cookie',sensitive:true,type:'value'},
 {section:'Response',group:'Body',key:'text preview',value:'<img src="https://evil.invalid/a" onerror="window.pwned=true"><script>window.pwned=true</script>',source:'Flow #1 > Response > Body',type:'text',sensitive:true}
];
async function open(page,extra=[]){
 await page.addInitScript(()=>{window.nativeMessages=[];window.Native={postMessage:data=>window.nativeMessages.push(JSON.parse(data))};});
 await page.goto('/');await page.waitForFunction(()=>window.Disha?.ready);
 await page.evaluate(()=>Disha.receive({type:'flowLoad',flowId:'flows-test',name:'flows (2)',size:554453,summaries:Array.from({length:4},(_,index)=>({index,type:'http',method:index?'GET':'POST',host:'example.test',path:'/login',status:'200',requestBytes:100,responseBytes:200}))}));
 expect(await page.evaluate(()=>nativeMessages.some(m=>m.action==='flowDetail'&&m.index===0))).toBe(true);
 await page.evaluate(items=>Disha.receive({type:'flowDetail',flowId:'flows-test',index:0,rows:items,extracted:[...items.filter(row=>row.type!=='text'),{...items[0],key:'email',type:'email'}],notices:[]}),[...rows,...extra]);
}
test('inline values appear automatically with exact copy, empty values, filtering and hide toggle',async({page})=>{
 await open(page);
 await expect(page.locator('#flowRows')).toContainText('demo@example.test');await expect(page.locator('#flowRows')).toContainText('sample123');
 const empty=page.locator('.flow-row').filter({has:page.locator('strong',{hasText:/^empty$/})});await expect(empty.locator('.flow-value')).toHaveText('');
 const password=page.locator('.flow-row').filter({has:page.locator('strong',{hasText:/^password$/})});
 await password.getByRole('button',{name:'نسخ القيمة',exact:true}).click();
 expect(await page.evaluate(()=>nativeMessages.find(m=>m.action==='copy').text)).toBe('sample123');
 await page.locator('#flowSearch').fill('sample123');await expect(page.locator('.flow-row')).toHaveCount(1);await expect(page.locator('mark')).toHaveText('sample123');
 await page.locator('#flowSearch').fill('password');await expect(password).toBeVisible();await page.locator('#flowHide').check();
 await expect(password.locator('.flow-value')).toHaveText('[مخفي]');await expect(password.getByRole('button',{name:'نسخ القيمة',exact:true})).toBeDisabled();
 await page.locator('#flowHide').uncheck();await expect(password.locator('.flow-value')).toHaveText('sample123');
 await expect(page.locator('#workspace')).toBeHidden();expect(await page.evaluate(()=>Disha.snapshot())).toBeNull();
 await page.locator('#flowExport').click();expect(await page.evaluate(()=>nativeMessages.some(m=>m.action==='flowExport'&&m.index===0))).toBe(true);
});
test('response is inert, duplicate headers visible, extracted sources and language work',async({page})=>{
 const requests=[];page.on('request',request=>requests.push(request.url()));await open(page);
 await page.locator('[data-section=Response]').click();await expect(page.locator('.flow-row strong',{hasText:'set-cookie'})).toHaveCount(2);
 await expect(page.locator('#flowRows')).toContainText('<script>');expect(await page.evaluate(()=>window.pwned)).toBeUndefined();await expect(page.locator('#flowRows img')).toHaveCount(0);
 expect(requests.every(url=>url.startsWith('http://127.0.0.1:4173/'))).toBe(true);
 await page.locator('[data-section=Extracted]').click();await expect(page.locator('#flowRows')).toContainText('Flow #1 > Request > Form > username');await expect(page.locator('#flowCount')).toContainText('email: 1');
 await page.locator('#menuButton').click();await page.locator('#language').click();await page.locator('#closeMenu').click();await expect(page.locator('[data-section=Request]')).toHaveText('Request');
 await page.locator('#flowBack').click();await expect(page.locator('#flowRows')).toBeEmpty();await page.locator('.flow-summary').nth(1).click();
 expect(await page.evaluate(()=>nativeMessages.some(m=>m.action==='flowDetail'&&m.index===1))).toBe(true);
 await page.evaluate(()=>Disha.receive({type:'home'}));await expect(page.locator('#home')).toBeVisible();await expect(page.locator('#flowRows')).toBeEmpty();
});
test('large result sets page, stale details ignored and text editor still loads',async({page})=>{
 await open(page,Array.from({length:350},(_,i)=>({...rows[0],key:`item${i}`,value:`عربي ${i}`})));
 await expect(page.locator('.flow-row')).toHaveCount(100);await page.locator('#flowRows').getByRole('button',{name:'تحميل المزيد'}).click();await expect(page.locator('.flow-row')).toHaveCount(200);
 await page.locator('#flowSearch').fill('عربي 349');await expect(page.locator('.flow-row')).toHaveCount(1);
 await page.evaluate(()=>Disha.receive({type:'flowDetail',flowId:'old-file',index:0,rows:[],extracted:[],notices:[]}));await expect(page.locator('.flow-row')).toHaveCount(1);
 await page.evaluate(()=>Disha.receive({type:'load',docId:'txt',name:'test.txt',text:'أهلا',encoding:'UTF-8',newline:'LF'}));await expect(page.locator('#flowViewer')).toBeHidden();await expect(page.locator('.cm-content')).toContainText('أهلا');
});
