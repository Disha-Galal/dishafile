import {EditorState,Compartment} from '@codemirror/state';
import {EditorView,lineNumbers,highlightActiveLine,drawSelection,keymap} from '@codemirror/view';
import {cursorCharLeft,cursorCharRight,cursorLineUp,cursorLineDown,indentSelection,history,historyKeymap,defaultKeymap,undo,redo,indentWithTab} from '@codemirror/commands';
import {search,searchKeymap,openSearchPanel} from '@codemirror/search';
import {syntaxHighlighting,defaultHighlightStyle} from '@codemirror/language';
import {oneDark} from '@codemirror/theme-one-dark';
import {createReadingControls} from './reading-controls.js';
import {createDatabaseViewer} from './database-viewer.js';
import {language} from './languages.js';
import {createAiPanel} from './ai-panel.js';
import {createFlowViewer} from './flow-viewer.js';
import {validateJson,formatJson} from './json-tools.js';

const strings={
 ar:{workspaceLabel:'مساحتك الخاصة',onDevice:'على جهازك',supportedFormats:'أنواع الملفات المدعومة',textSize:'حجم النص',zoomHint:'اضغط مطولًا على A− أو A+ لتغيير الحجم باستمرار، أو على أرقام السطور وقوائم النصوص لفتح هذا التحكم.',resetSize:'الحجم الافتراضي',offline:'ملفات محلية • ذكاء اصطناعي باختيارك',welcome:'ملفاتك، تحت إيدك.',intro:'افتح، عدّل واحفظ ملفات الكود والنصوص بأمان.',open:'افتح ملف',new:'ملف جديد',recover:'استرجاع مسودة / نسخة أمان',recent:'آخر ملفات',formats:'PHP · DB / SQLite · TXT · JSON · JSONL · SQL · MD · HTML · CSS · JS · XML · YAML · PY · SH · CSV · LOG · INI · CONF · TS · TSX · KT · JAVA · C/C++ · CS · GO · RS · RB · TOML · SVG · SRT · VTT · GZ',options:'الاختيارات',home:'الرئيسية',share:'مشاركة / تصدير',validate:'فحص JSON / JSONL',encoding:'ترميز الحفظ',theme:'فاتح / غامق',close:'إغلاق',cancel:'إلغاء',copy:'نسخ التحديد',find:'بحث / استبدال',format:'تنسيق',save:'حفظ',saveAs:'حفظ باسم',undo:'تراجع',redo:'إعادة',wrap:'لف السطور',saved:'محفوظ',unsaved:'● مش محفوظ',preview:'معاينة فقط',formatWarning:'التنسيق هيغيّر المسافات. المفاتيح المتكررة والأرقام الكبيرة بتفضل زي ما هي. راجع الملف قبل الحفظ.',minify:'ضغط JSON',valid:'JSON سليم',line:'سطر',column:'عمود',select:'حدد النص الأول',copied:'اتنسخ',unsupported:'التنسيق متاح لـ JSON فقط. JSONL بيتفحص سجل بسجل.',ready:'جاهز',noRecent:'لسه مفيش ملفات',webOnly:'تجربة متصفح: فتح وحفظ الملفات محتاج نسخة أندرويد.',tooLong:'حجم النص أكبر من حد التعديل. استخدم نسخة أصغر.'},
 en:{workspaceLabel:'YOUR LOCAL WORKSPACE',onDevice:'On device',supportedFormats:'Supported formats',textSize:'Text size',zoomHint:'Hold A− or A+ to keep zooming. Hold the line numbers or text lists to open these controls.',resetSize:'Reset to default',offline:'Local files • Optional AI',welcome:'Your files. Within reach.',intro:'Open, edit and save code and text safely.',open:'Open file',new:'New file',recover:'Recover draft / backup',recent:'Recent files',formats:'PHP · DB / SQLite · TXT · JSON · JSONL · SQL · MD · HTML · CSS · JS · XML · YAML · PY · SH · CSV · LOG · INI · CONF · TS · TSX · KT · JAVA · C/C++ · CS · GO · RS · RB · TOML · SVG · SRT · VTT · GZ',options:'Options',home:'Home',share:'Share / Export',validate:'Validate JSON / JSONL',encoding:'Save encoding',theme:'Light / Dark',close:'Close',cancel:'Cancel',copy:'Copy selection',find:'Find / Replace',format:'Format',save:'Save',saveAs:'Save As',undo:'Undo',redo:'Redo',wrap:'Wrap lines',saved:'Saved',unsaved:'● Unsaved',preview:'Read-only preview',formatWarning:'Formatting changes whitespace. Duplicate keys and large numbers are preserved. Review before saving.',minify:'Minify JSON',valid:'Valid JSON',line:'Line',column:'Column',select:'Select text first',copied:'Copied',unsupported:'Formatting is for JSON only. JSONL is validated one record per line.',ready:'Ready',noRecent:'No recent files yet',webOnly:'Browser demo: file access requires the Android app.',tooLong:'Text exceeds the editing limit. Use a smaller copy.'}
};
let lang='ar',dark=true,wrap=true,fontSize=15,current=null,savedText='',revision=0,draftTimer,view,ready=false,controlsCollapsed=false;
const phrasesSlot=new Compartment(),languageSlot=new Compartment(),themeSlot=new Compartment(),wrapSlot=new Compartment(),fontSlot=new Compartment();
const $=id=>document.getElementById(id),t=key=>strings[lang][key]||key;
const send=(action,data={})=>{if(current && ['open','new','home','recent','recover'].includes(action)) data={...snapshot(),...data};if(window.Native) window.Native.postMessage(JSON.stringify({action,...data})); else if(action!=='ready'&&action!=='draft'&&action!=='settings') notice(t('webOnly'));};
const flowViewer=createFlowViewer(send);
const databaseViewer=createDatabaseViewer(send);
const readingControls=createReadingControls({onChange(value){fontSize=value;view?.dispatch({effects:fontSlot.reconfigure(font())});settings();},onCollapse(value){controlsCollapsed=value;settings();}});
const aiPanel=createAiPanel({getView:()=>view,getCurrent:()=>current,getRevision:()=>revision,send,notice,isEnglish:()=>lang==='en'});
function notice(text){$('message').textContent=text;$('message').classList.toggle('visible',!!text);}
function phrases(){return EditorState.phrases.of(lang==='ar'?{'Find':'بحث','Replace':'استبدال','next':'التالي','previous':'السابق','all':'الكل','match case':'مطابقة الحروف','regexp':'تعبير نمطي','by word':'كلمة كاملة','replace':'استبدل','replace all':'استبدل الكل','close':'إغلاق'}:{});}
function setLanguage(value){flowViewer.language(value);lang=value==='en'?'en':'ar';document.documentElement.lang=lang;document.documentElement.dir=lang==='ar'?'rtl':'ltr';document.querySelectorAll('[data-i18n]').forEach(el=>el.textContent=t(el.dataset.i18n));$('indent2').textContent=lang==='ar'?'مسافتين':'2 spaces';$('indent4').textContent=lang==='ar'?'٤ مسافات':'4 spaces';view?.dispatch({effects:phrasesSlot.reconfigure(phrases())});readingControls.collapse(controlsCollapsed,false);databaseViewer.language();status();}
function settings(){send('settings',{lang,dark,wrap,fontSize,controlsCollapsed});}
function status(){if(!current)return;$('dirty').textContent=current.preview?t('preview'):view.state.doc.toString()===savedText?t('saved'):t('unsaved');}
function font(){return EditorView.theme({'&':{fontSize:`${fontSize}px`},'.cm-content':{direction:'ltr'}});}
function snapshot(){return {text:view.state.doc.toString(),revision,docId:current.docId};}
function draft(){if(current&&!current.preview&&view.state.doc.toString()!==savedText)send('draft',snapshot());}
function load(doc){
 aiPanel.reset();flowViewer.reset();databaseViewer.reset();clearTimeout(draftTimer);current=doc;revision=0;savedText=doc.dirty?'\u0000':doc.text;readingControls.resetEditor();view?.destroy();
 view=new EditorView({parent:$('editor'),state:EditorState.create({doc:doc.text,extensions:[phrasesSlot.of(phrases()),lineNumbers(),history(),drawSelection(),highlightActiveLine(),syntaxHighlighting(defaultHighlightStyle,{fallback:true}),languageSlot.of(language(doc.name)),search({top:true}),keymap.of([...defaultKeymap,...historyKeymap,...searchKeymap,indentWithTab]),themeSlot.of(dark?oneDark:[]),wrapSlot.of(wrap?EditorView.lineWrapping:[]),fontSlot.of(font()),EditorState.readOnly.of(!!doc.preview),EditorView.editable.of(!doc.preview),EditorState.transactionFilter.of(tr=>tr.newDoc.length>8*1024*1024?[]:tr),EditorView.updateListener.of(update=>{if(update.docChanged||update.selectionSet)position();if(update.docChanged){revision++;status();clearTimeout(draftTimer);draftTimer=setTimeout(draft,700);}})]})});
 readingControls.attachEditor(view);document.body.classList.add('editing');
 $('home').hidden=true;$('workspace').hidden=false;$('filename').textContent=doc.name;
 $('metadata').textContent=`${doc.encoding} · ${doc.newline}${doc.bom?' · BOM':''}${doc.gzip?' · GZIP':''}`;
 for(const id of ['save','saveAs','share'])$(id).disabled=!!doc.preview;
 $('format').disabled=!!doc.preview||!doc.name.toLowerCase().replace(/\.gz$/,'').endsWith('.json');
 position();$('wrap').setAttribute('aria-pressed',String(wrap));status();notice(doc.preview?t('preview'):'');
}
function command(action){if(!current)return;clearTimeout(draftTimer);send(action,snapshot());}
function toggleTheme(){document.body.classList.toggle('light',!dark);view?.dispatch({effects:themeSlot.reconfigure(dark?oneDark:[])});}
function closeMenus(){for(const id of ['menu','formatDialog'])$(id).close();}
function validate(){if(!current)return;const ext=current.name.toLowerCase().replace(/\.gz$/,'').split('.').pop();if(!['json','jsonl','ndjson'].includes(ext)){notice(t('unsupported'));return;}const result=validateJson(view.state.doc.toString(),['jsonl','ndjson'].includes(ext));report(result);}
function report(result){notice(result.ok?t('valid'):`${t('line')} ${result.line}, ${t('column')} ${result.column}: ${result.error}`);if(!result.ok&&result.line<=view.state.doc.lines){const line=view.state.doc.line(result.line);view.dispatch({selection:{anchor:Math.min(line.to,line.from+result.column-1)},scrollIntoView:true});}}
function format(indent){closeMenus();if(!current||current.preview)return;const result=formatJson(view.state.doc.toString(),indent);if(!result.ok){report(result);return;}view.dispatch({changes:{from:0,to:view.state.doc.length,insert:result.text}});notice('');}
function bind(id,callback){$(id).addEventListener('click',callback);}
for(const id of ['openHome','openMenu'])bind(id,()=>{closeMenus();send('open');});
for(const id of ['newHome','newMenu'])bind(id,()=>{closeMenus();send('new');});
bind('recoverHome',()=>send('recover'));bind('homeMenu',()=>{closeMenus();send('home');});
bind('menuButton',()=>$('menu').showModal());bind('workspaceMenu',()=>$('menu').showModal());bind('closeMenu',closeMenus);bind('closeFormat',closeMenus);
bind('undo',()=>view&&undo(view));bind('redo',()=>view&&redo(view));
bind('find',()=>{if(view){openSearchPanel(view);}});
bind('copy',()=>{if(!view)return;const text=view.state.sliceDoc(view.state.selection.main.from,view.state.selection.main.to);if(!text){notice(t('select'));return;}send('copy',{text});notice(t('copied'));});
bind('save',()=>command('save'));bind('saveAs',()=>command('saveAs'));bind('share',()=>{closeMenus();command('share');});
bind('encoding',()=>{closeMenus();if(current)command('encoding');});
bind('format',()=>$('formatDialog').showModal());bind('indent2',()=>format(2));bind('indent4',()=>format(4));bind('minify',()=>format(0));bind('validate',()=>{closeMenus();validate();});
bind('language',()=>{setLanguage(lang==='ar'?'en':'ar');settings();});bind('theme',()=>{dark=!dark;toggleTheme();settings();});
bind('wrap',()=>{wrap=!wrap;view?.dispatch({effects:wrapSlot.reconfigure(wrap?EditorView.lineWrapping:[])});$('wrap').setAttribute('aria-pressed',String(wrap));settings();});


function position(){if(!view)return;const s=view.state.selection.main,l=view.state.doc.lineAt(s.head);$('position').textContent=`${t('line')} ${l.number} · ${t('column')} ${s.head-l.from+1} · ${s.to-s.from} ${lang==='ar'?'حرف محدد':'selected'}`;}
bind('selectRange',()=>{if(!view)return;$('rangeFrom').value=view.state.doc.lineAt(view.state.selection.main.from).number;$('rangeTo').value=view.state.doc.lineAt(view.state.selection.main.to).number;$('rangeDialog').showModal();});
bind('rangeApply',()=>{if(!view)return;const from=Number($('rangeFrom').value),to=Number($('rangeTo').value);if(!Number.isInteger(from)||!Number.isInteger(to)||from<1||to<from||to>view.state.doc.lines){notice(lang==='ar'?'راجع نطاق السطور.':'Check line range.');return;}view.dispatch({selection:{anchor:view.state.doc.line(from).from,head:view.state.doc.line(to).to},scrollIntoView:true});$('rangeDialog').close();view.focus();});
bind('rangeClose',()=>$('rangeDialog').close());
for(const [id,cmd] of [['left',cursorCharLeft],['right',cursorCharRight],['up',cursorLineUp],['down',cursorLineDown],['indent',indentSelection]])bind(id,()=>{if(view){cmd(view);view.focus();}});

window.Disha={snapshot:()=>current?snapshot():null,receive(message){
 if(aiPanel.receive(message))return;
 if(message.type==='databaseLoad'||message.type==='flowLoad'){databaseViewer.reset();flowViewer.reset();document.body.classList.remove('editing');aiPanel.reset();clearTimeout(draftTimer);current=null;readingControls.resetEditor();view?.destroy();view=null;$('home').hidden=true;$('workspace').hidden=true;notice('');}
 if(databaseViewer.receive(message)||flowViewer.receive(message))return;
 switch(message.type){
 case 'load':load(message);break;
 case 'saved':if(current?.docId===message.docId){savedText=message.text;Object.assign(current,{name:message.name,encoding:message.encoding,bom:message.bom,newline:message.newline,gzip:message.gzip});$('filename').textContent=message.name;$('metadata').textContent=`${current.encoding} · ${current.newline}${current.bom?' · BOM':''}${current.gzip?' · GZIP':''}`;view.dispatch({effects:languageSlot.reconfigure(language(message.name))});status();}break;
 case 'notice':notice(message.text);break;
 case 'busy':document.body.classList.toggle('busy',message.value);break;
 case 'home':document.body.classList.remove('editing');aiPanel.reset();flowViewer.reset();databaseViewer.reset();clearTimeout(draftTimer);current=null;readingControls.resetEditor();view?.destroy();view=null;$('home').hidden=false;$('workspace').hidden=true;notice('');break;
 case 'settings':setLanguage(message.lang);dark=message.dark;wrap=message.wrap;fontSize=Number.isFinite(message.fontSize)?Math.max(11,Math.min(32,message.fontSize)):15;controlsCollapsed=!!message.controlsCollapsed;readingControls.apply(fontSize,false);readingControls.collapse(controlsCollapsed,false);toggleTheme();view?.dispatch({effects:[wrapSlot.reconfigure(wrap?EditorView.lineWrapping:[]),fontSlot.reconfigure(font())]});$('wrap').setAttribute('aria-pressed',String(wrap));break;
 case 'recents':$('recents').replaceChildren();for(const item of message.items){const button=document.createElement('button');button.textContent=item.name;button.dir='auto';button.onclick=()=>send('recent',{uri:item.uri});$('recents').append(button);}break;
 case 'flush':draft();break;
 }},get ready(){return ready;}};
setLanguage('ar');ready=true;send('ready');
