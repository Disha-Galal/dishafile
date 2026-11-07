import {parseTree, printParseErrorCode} from 'jsonc-parser';
export function validateJson(text, jsonl = false) {
  if (jsonl) {
    const lines = text.split('\n');
    if (lines.at(-1) === '') lines.pop();
    if (!lines.length) return {ok:false,line:1,column:1,error:'ValueExpected'};
    for (let i=0;i<lines.length;i++) {
      const result = validateJson(lines[i]);
      if (!result.ok) return {...result,line:i+1};
    }
    return {ok:true};
  }
  const errors = [];
  const tree = parseTree(text, errors, {allowTrailingComma:false, disallowComments:true, allowEmptyContent:false});
  if (errors.length) {
    const e = errors[0], before = text.slice(0,e.offset), last = before.lastIndexOf('\n');
    return {ok:false,line:before.split('\n').length,column:e.offset-last,error:printParseErrorCode(e.error)};
  }
  return {ok:true,tree};
}
// بنغير الفراغات فقط؛ الأرقام الكبيرة والمفاتيح المتكررة وترتيبها بتفضل زي الأصل.
export function formatJson(text, indent = 2) {
  const valid = validateJson(text);
  if (!valid.ok) return valid;
  let out='', depth=0, quoted=false, escaped=false;
  const pad = () => '\n' + ' '.repeat(depth*indent);
  for (let i=0;i<text.length;i++) {
    const ch=text[i];
    if (quoted) {
      out+=ch;
      if (escaped) escaped=false; else if (ch==='\\') escaped=true; else if(ch==='"') quoted=false;
      continue;
    }
    if (/\s/.test(ch)) continue;
    if(ch==='"') { quoted=true; out+=ch; }
    else if(ch==='{' || ch==='[') {
      out+=ch; depth++;
      if(indent && !/^[\s]*[}\]]/.test(text.slice(i+1))) out+=pad();
    } else if(ch==='}' || ch===']') {
      depth--;
      if(indent && !/[{[]\s*$/.test(out)) out+=pad();
      out+=ch;
    } else if(ch===',') out+=ch+(indent?pad():'');
    else if(ch===':') out+=ch+(indent?' ':'');
    else out+=ch;
  }
  return {ok:true,text:out};
}
