// This project By Gogo & Disha ®
export function createReadingControls({onChange, onCollapse}) {
 const $=id=>document.getElementById(id);
 let size=15,collapsed=false,removeEditorHold;
 function apply(value,notify=true){
  size=Number.isFinite(Number(value))?Math.max(11,Math.min(32,Math.round(Number(value)))):15;
  document.documentElement.style.setProperty('--reading-size',`${size}px`);
  $('fontSlider').value=size;$('fontValue').textContent=`${size} px`;
  $('smaller').disabled=size===11;$('larger').disabled=size===32;
  if(notify)onChange(size);
 }
 function collapse(value,notify=true){
  collapsed=!!value;
  $('workspace').classList.toggle('collapsed',collapsed);
  $('toggleControls').setAttribute('aria-expanded',String(!collapsed));
  $('toggleControls').textContent=collapsed?'⌄':'⌃';
  $('toggleControls').setAttribute('aria-label',document.documentElement.lang==='ar'?(collapsed?'إظهار الأدوات':'إخفاء الأدوات'):(collapsed?'Show tools':'Hide tools'));
  if(notify)onCollapse(collapsed);
 }
 function open(){if(!$('fontDialog').open)$('fontDialog').showModal();}
 function hold(element,callback,repeat=false){
  let timer,interval,start,active=false,held=false;
  const listeners=[];
  function listen(target,name,callback,capture=false){target.addEventListener(name,callback,capture);listeners.push(()=>target.removeEventListener(name,callback,capture));}
  const clear=()=>{clearTimeout(timer);clearInterval(interval);start=null;active=false;};
  listen(element,'pointerdown',event=>{
   if(event.button!==0)return;clear();held=false;start={x:event.clientX,y:event.clientY};active=true;
   if(repeat){event.preventDefault();element.setPointerCapture(event.pointerId);callback();}
   timer=setTimeout(()=>{if(!active)return;if(repeat)interval=setInterval(callback,90);else{held=true;callback();clear();}},400);
  });
  listen(element,'pointermove',event=>{if(start&&Math.hypot(event.clientX-start.x,event.clientY-start.y)>12)clear();});
  for(const name of ['pointerup','pointercancel','lostpointercapture','pointerleave'])listen(element,name,clear);
  listen(window,'blur',clear);listen(document,'visibilitychange',clear);
  listen(element,'click',event=>{if(held){event.preventDefault();event.stopImmediatePropagation();held=false;}},true);
  listen(element,'contextmenu',event=>event.preventDefault());
  if(repeat){element.style.touchAction='none';listen(element,'contextmenu',event=>event.preventDefault());listen(element,'click',event=>{if(event.detail===0)callback();});}
  return ()=>{clear();listeners.forEach(remove=>remove());};
 }
 hold($('smaller'),()=>apply(size-1),true);hold($('larger'),()=>apply(size+1),true);
 for(const id of ['recents','flowList','databaseRows'])hold($(id),open);
 $('textSize').onclick=open;$('menuTextSize').onclick=()=>{$('menu').close();open();};
 $('fontSlider').oninput=()=>apply($('fontSlider').value);$('fontReset').onclick=()=>apply(15);$('fontClose').onclick=()=>$('fontDialog').close();
 $('toggleControls').onclick=()=>collapse(!collapsed);
 return {apply,collapse,resetEditor(){removeEditorHold?.();removeEditorHold=null;},attachEditor(view){removeEditorHold?.();removeEditorHold=hold(view.dom.querySelector('.cm-gutters'),open);}};
}
