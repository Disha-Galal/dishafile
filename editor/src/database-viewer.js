// This project By Gogo & Disha ®
export function createDatabaseViewer(send){
 const $=id=>document.getElementById(id);
 let session=null,table='',offset=0;
 const en=()=>document.documentElement.lang==='en';
 function request(name,next=0){
  if(!session)return;table=name;offset=next;
  $('databaseRows').replaceChildren();$('databaseStatus').textContent=en()?'Loading rows…':'تحميل الصفوف…';
  $('databasePrevious').disabled=true;$('databaseNext').disabled=true;
  send('databasePage',{databaseId:session.databaseId,table,offset});
 }
 function labels(){
  $('databaseTitle').textContent=en()?'Database explorer':'عارض قاعدة البيانات';
  $('databaseReadOnly').textContent=en()?'READ ONLY · SQLITE':'للقراءة فقط · SQLITE';
  $('databaseHint').textContent=en()?'Standalone SQLite 3 snapshot. Up to 50 rows per page. Long text and binary cells show marked previews. Export a closed database from the source app; WAL sidecars are not imported.':'نسخة SQLite 3 مستقلة. حتى 50 صفًا في الصفحة، والنص الطويل والبيانات الثنائية بمعاينة واضحة. صدّر قاعدة مغلقة؛ ملفات WAL الخارجية لا تُستورد.';
  $('databaseTableLabel').textContent=en()?'Table':'الجدول';
  $('databasePrevious').textContent=en()?'Previous':'السابق';$('databaseNext').textContent=en()?'Next':'التالي';
 }
 $('databaseTables').onchange=()=>request($('databaseTables').value);
 $('databasePrevious').onclick=()=>request(table,Math.max(0,offset-50));
 $('databaseNext').onclick=()=>request(table,offset+50);
 return {language:labels,reset(){session=null;$('databaseViewer').hidden=true;$('databaseRows').replaceChildren();$('databaseTables').replaceChildren();},receive(message){
  if(message.type==='databaseLoad'){
   session=message;labels();$('databaseViewer').hidden=false;$('databaseName').textContent=message.name;
   $('databaseTables').replaceChildren();
   for(const name of message.tables){const option=document.createElement('option');option.value=name;option.textContent=name;$('databaseTables').append(option);}
   if(message.tables.length)request(message.tables[0]);else $('databaseStatus').textContent=en()?'No supported tables. Views and virtual tables are excluded.':'لا توجد جداول مدعومة. العروض والجداول الافتراضية مستبعدة.';
   return true;
  }
  if(message.type==='notice'&&session)$('databaseStatus').textContent=en()?'Choose a supported table to continue.':'اختار جدولًا مدعومًا للمتابعة.';
  if(message.type!=='databasePage')return false;
  if(!session||message.databaseId!==session.databaseId||message.table!==table||message.offset!==offset)return true;
  const grid=document.createElement('table'),caption=document.createElement('caption'),head=document.createElement('thead'),tr=document.createElement('tr'),body=document.createElement('tbody');
  caption.textContent=table;grid.append(caption);
  for(const name of message.columns){const th=document.createElement('th');th.scope='col';th.textContent=name;th.dir='auto';tr.append(th);}head.append(tr);grid.append(head,body);
  for(const row of message.rows){const tr=document.createElement('tr');for(const cell of row){const td=document.createElement('td');td.dir='auto';td.textContent=cell.type==='null'?'NULL':cell.type==='blob'?`HEX ${cell.value}`:cell.value;if(cell.type==='null')td.classList.add('db-null');if(cell.truncated){const badge=document.createElement('small');badge.textContent=en()?'Preview…':'معاينة…';td.append(badge);}tr.append(td);}body.append(tr);}
  $('databaseRows').replaceChildren(grid);$('databasePrevious').disabled=offset===0;$('databaseNext').disabled=!message.hasMore;
  $('databaseStatus').textContent=message.rows.length?`${offset+1}–${offset+message.rows.length}${message.limited?(en()?' · Preview limit reached':' · نهاية حد المعاينة'):''}`:(en()?'Empty table':'جدول فارغ');
  return true;
 }};
}
