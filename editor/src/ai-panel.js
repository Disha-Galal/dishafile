// النتائج نصوص خام للمراجعة؛ لا تُنفذ ولا تُحفظ تلقائيًا.
export function createAiPanel({getView,getCurrent,getRevision,send,notice,isEnglish}) {
 const $=id=>document.getElementById(id),tr=(ar,en)=>isEnglish()?en:ar;
 let pending=null,result=null;
 function reset(){if(pending)send('aiCancel');pending=null;result=null;$('aiSend').disabled=false;$('aiApply').disabled=true;$('aiOutput').textContent='';$('aiStatus').textContent='';$('aiCloud').checked=false;}
 $('aiOpen').onclick=()=>{
  if(!getView())return;
  $('aiDialog').showModal();
  const v=getView(),s=v.state.selection.main;
  $('aiScope').textContent=tr(`التحديد: ${s.to-s.from} حرفًا. بدون تحديد يُرسل الملف كله، بميزانية إجمالية 6000 بايت UTF-8 مع الطلب.`,`Selection: ${s.to-s.from} characters. Without a selection, send the whole file, within 6000 UTF-8 bytes including your request.`);
 };
 $('aiSettings').onclick=()=>send('aiSettings');
 $('aiClose').onclick=()=>{$('aiDialog').close();reset();};
 $('aiDialog').addEventListener('cancel',reset);
 $('aiSend').onclick=()=>{
  const view=getView(),doc=getCurrent();if(!view||!doc||pending)return;
  const s=view.state.selection.main,from=s.empty?0:s.from,to=s.empty?view.state.doc.length:s.to;
  const content=view.state.sliceDoc(from,to),instruction=$('aiInstruction').value.trim();
  if(!instruction||instruction.length>2000||content.length>16000||new TextEncoder().encode(instruction+content).length>6000){$('aiStatus').textContent=tr('اكتب الطلب وحدد نصًا أصغر؛ الحد الإجمالي 6000 بايت UTF-8 والطلب حتى 2000 حرف.','Enter a request and select less text; total limit is 6000 UTF-8 bytes, with a request up to 2000 characters.');return;}
  result=null;$('aiOutput').textContent='';$('aiApply').disabled=true;
  pending={requestId:crypto.randomUUID(),docId:doc.docId,revision:getRevision(),from,to,original:content};
  $('aiSend').disabled=true;$('aiStatus').textContent=tr('بانتظار تأكيد الإرسال في أندرويد…','Waiting for Android transfer confirmation…');
  send('aiRequest',{...pending,content,instruction,allowCloud:$('aiCloud').checked});
  $('aiCloud').checked=false;
 };
 $('aiApply').onclick=()=>{
  const v=getView(),d=getCurrent();
  if(!result||!v||d?.preview||d?.docId!==result.docId||getRevision()!==result.revision||v.state.sliceDoc(result.from,result.to)!==result.original){notice(tr('الملف اتغير؛ اطلب اقتراحًا جديدًا.','Document changed; request a new suggestion.'));return;}
  if(v.state.doc.length-(result.to-result.from)+result.text.length>8*1024*1024){notice(tr('الاقتراح يتجاوز حد حجم المحرر.','Suggestion exceeds the editor size limit.'));return;}
  v.dispatch({changes:{from:result.from,to:result.to,insert:result.text},selection:{anchor:result.from,head:result.from+result.text.length},scrollIntoView:true});
  $('aiDialog').close();reset();v.focus();
 };
 $('aiCopy').onclick=()=>{if(result)send('copy',{text:result.text});};
 return {reset,receive(m){
  if(!['aiProgress','aiResult','aiError'].includes(m.type))return false;
  if(!pending||m.requestId!==pending.requestId)return true;
  if(m.type==='aiProgress'){$('aiStatus').textContent=tr('جاري المحاولة: ','Trying: ')+m.model;return true;}
  if(m.type==='aiResult'){
   result={...pending,text:m.text};$('aiOutput').textContent=m.text;
   $('aiStatus').textContent=m.model+(m.failures?.length?tr(' • تم التحويل بعد تعطل: ',' • Switched after failure: ')+m.failures.join(', '):'');
   $('aiApply').disabled=!!getCurrent()?.preview;
  }else $('aiStatus').textContent=m.text;
  pending=null;$('aiSend').disabled=false;return true;
 }};
}
