const $=id=>document.getElementById(id);
const words={ar:{readonly:'Flow Viewer · قراءة فقط',list:'قائمة التدفقات',overview:'نظرة عامة',request:'الطلب',response:'الاستجابة',extracted:'المستخرج',search:'بحث باسم المفتاح أو القيمة',hide:'إخفاء البيانات الحساسة',export:'تصدير التدفق المحدد إلى JSON جديد',copy:'نسخ القيمة',pair:'نسخ key=value',path:'نسخ المسار',more:'تحميل المزيد',empty:'لا توجد نتائج',loading:'جاري قراءة القيم…',count:'العدد',hidden:'[مخفي]',absent:'الاستجابة غير موجودة أو القسم فارغ',warning:'البيانات محلية وقد تتضمن أسرارًا. HTTP 200 وحده لا يثبت نجاح تسجيل الدخول.'},en:{readonly:'Flow Viewer · Read-only',list:'Flow list',overview:'Overview',request:'Request',response:'Response',extracted:'Extracted',search:'Search key or value',hide:'Hide sensitive data',export:'Export selected flow to new JSON',copy:'Copy value',pair:'Copy key=value',path:'Copy path',more:'Load more',empty:'No results',loading:'Loading values…',count:'Count',hidden:'[Hidden]',absent:'Response missing or section empty',warning:'Local data may contain secrets. HTTP 200 alone does not establish login success.'}};
export function createFlowViewer(send){
 let doc=null,detail=null,section='Request',index=-1,shown=100,listShown=100,lang='ar',timer;
 const text=key=>words[lang][key];
 function node(tag,value,className){const el=document.createElement(tag);if(value!==undefined)el.textContent=value;if(className)el.className=className;return el;}
 function button(label,fn){const el=node('button',label);el.onclick=fn;return el;}
 function reset(){clearTimeout(timer);doc=null;detail=null;index=-1;$('flowViewer').hidden=true;$('flowRows').replaceChildren();$('flowList').replaceChildren();$('flowNotes').textContent='';$('flowName').textContent='';$('flowSearch').value='';$('flowCount').textContent='';}
 function list(){
  $('flowList').replaceChildren();
  for(const item of doc.summaries.slice(0,listShown)){
   const el=button(`#${item.index+1} · ${item.type} · ${item.method} ${item.host}${item.path} · HTTP ${item.status} · Request ${item.requestBytes} B / Response ${item.responseBytes} B${item.timestamp?` · ${item.timestamp}`:''}`,()=>select(item.index));
   el.dir='ltr';el.className='flow-summary';el.setAttribute('aria-pressed',String(index===item.index));$('flowList').append(el);
  }
  if(doc.summaries.length>listShown)$('flowList').append(button(text('more'),()=>{listShown+=100;list();}));
 }
 function select(next){index=next;detail=null;shown=100;section='Request';$('flowSearch').value='';$('flowRows').textContent=text('loading');$('flowNotes').textContent='';$('flowCount').textContent='';$('flowExport').disabled=true;list();tabs();send('flowDetail',{flowId:doc.flowId,index});}
 function tabs(){
  $('flowTabs').replaceChildren();
  for(const [name,key] of [['Overview','overview'],['Request','request'],['Response','response'],['Extracted','extracted']]){
   const el=button(text(key),()=>{section=name;shown=100;tabs();render();});el.dataset.section=name;el.setAttribute('aria-pressed',String(section===name));$('flowTabs').append(el);
  }
 }
 function highlighted(el,value,query){
  if(!query){el.textContent=value;return;}
  const lower=value.toLocaleLowerCase();let pos=0,found;
  while((found=lower.indexOf(query,pos))!==-1){el.append(document.createTextNode(value.slice(pos,found)),node('mark',value.slice(found,found+query.length)));pos=found+query.length;}
  el.append(document.createTextNode(value.slice(pos)));
 }
 function render(){
  if(!detail)return;
  const query=$('flowSearch').value.toLocaleLowerCase().trim(),hide=$('flowHide').checked;
  const source=section==='Extracted'?detail.extracted:detail.rows.filter(row=>row.section===section);
  const rows=source.filter(row=>!query||`${row.key}\n${hide&&row.sensitive?'':row.value}`.toLocaleLowerCase().includes(query));
  $('flowRows').replaceChildren();$('flowCount').textContent=`${text('count')}: ${rows.length}`;
  const counts={};for(const row of rows)counts[row.type]=(counts[row.type]||0)+1;
  if(section==='Extracted')$('flowCount').textContent+=` · ${Object.entries(counts).map(([key,value])=>`${key}: ${value}`).join(' · ')}`;
  const fragment=document.createDocumentFragment();
  for(const row of rows.slice(0,shown)){
   const el=node('article',undefined,'flow-row');
   el.append(node('small',`${row.source} · ${row.type}`,'flow-source'));
   const pair=node('div',undefined,'flow-pair');pair.dir='ltr';const key=node('strong'),value=node('span',undefined,'flow-value');
   highlighted(key,row.key,query);highlighted(value,hide&&row.sensitive?text('hidden'):row.value,query);
   pair.append(key,node('span',' = '),value);el.append(pair);
   const actions=node('div',undefined,'flow-copy');
   for(const [label,content] of [['copy',row.value],['pair',`${row.key}=${row.value}`],['path',row.source]]){
    const copy=button(text(label),()=>send('copy',{text:content}));copy.disabled=hide&&row.sensitive;actions.append(copy);
   }
   el.append(actions);fragment.append(el);
  }
  if(!rows.length)fragment.append(node('p',query?text('empty'):text('absent')));
  if(rows.length>shown)fragment.append(button(text('more'),()=>{shown+=100;render();}));
  $('flowRows').append(fragment);$('flowNotes').textContent=detail.notices.join('\n');
 }
 $('flowSearch').addEventListener('input',()=>{clearTimeout(timer);timer=setTimeout(()=>{shown=100;render();},120);});
 $('flowHide').onchange=render;
 $('flowExport').onclick=()=>{if(doc&&index>=0)send('flowExport',{flowId:doc.flowId,index});};
 $('flowBack').onclick=()=>{if(doc){detail=null;index=-1;$('flowRows').replaceChildren();$('flowNotes').textContent='';$('flowCount').textContent='';$('flowExport').disabled=true;list();$('flowList').focus();}};
 function language(value){lang=value==='en'?'en':'ar';for(const [id,key] of [['flowTitle','readonly'],['flowBack','list'],['flowHideLabel','hide'],['flowExport','export'],['flowWarning','warning']])$(id).textContent=text(key);$('flowSearch').placeholder=text('search');$('flowSearch').setAttribute('aria-label',text('search'));if(doc){tabs();render();}}
 language(lang);
 return {reset,language,receive(message){
  if(message.type==='flowLoad'){
   reset();doc=message;listShown=100;$('flowViewer').hidden=false;$('flowName').textContent=`${doc.name} · ${doc.summaries.length} flows · ${doc.size} bytes`;$('flowHide').checked=false;select(0);return true;
  }
  if(message.type==='flowDetail'){
   if(doc&&doc.flowId===message.flowId&&index===message.index){detail=message;$('flowExport').disabled=false;render();}return true;
  }
  return false;
 }};
}
