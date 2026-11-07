import {test} from 'node:test';
import assert from 'node:assert/strict';
import {formatJson,validateJson} from '../src/json-tools.js';
test('reports exact malformed JSON line and column',()=>{const r=validateJson('{\n  "x": }');assert.equal(r.ok,false);assert.equal(r.line,2);assert.equal(r.column,8);assert.equal(formatJson('{broken').ok,false);});
test('large integers, duplicate keys and string escapes survive formatting',()=>{const src='{"n":900719925474099312345,"x":1,"x":2,"s":"a \\\" b","array":[true,null,{}]}';for(const indent of [0,2,4]){const r=formatJson(src,indent);assert.equal(r.ok,true);assert.match(r.text,/900719925474099312345/);assert.equal((r.text.match(/"x"/g)||[]).length,2);assert.equal(formatJson(r.text,0).text,src);}});
test('pretty print and minify nested structures',()=>{assert.equal(formatJson('{"a":[1,2],"b":{}}',2).text,'{\n  "a": [\n    1,\n    2\n  ],\n  "b": {}\n}');assert.equal(formatJson(' { "x" : "a b" } ',0).text,'{"x":"a b"}');});
test('rejects comments trailing comma empty and multiple roots',()=>{for(const text of ['','//x\n{}','{"x":1,}','{} {}','NaN','[1,]'])assert.equal(validateJson(text).ok,false,text);});
test('JSONL validates each record, detects blank internal lines, permits final newline',()=>{assert.equal(validateJson('{"a":1}\n[2]\n',true).ok,true);assert.equal(validateJson('{}\n{broken}\n',true).line,2);assert.equal(validateJson('{}\n\n{}',true).ok,false);assert.equal(validateJson('',true).ok,false);});
test('prototype-like keys and HTML are inert data',()=>{assert.equal(formatJson('{"__proto__":{"x":1},"html":"<script>alert(1)</script>"}',0).text,'{"__proto__":{"x":1},"html":"<script>alert(1)</script>"}');});
