from pathlib import Path

# بيانات صناعية فقط؛ لا تُستخدم تفريغات المستخدم في الاختبارات.
def tn(value):
    if value is None:
        payload, tag = b'', b'~'
    elif isinstance(value, bool):
        payload, tag = str(value).lower().encode(), b'!'
    elif isinstance(value, int):
        payload, tag = str(value).encode(), b'#'
    elif isinstance(value, float):
        payload, tag = str(value).encode(), b'^'
    elif isinstance(value, str):
        payload, tag = value.encode(), b';'
    elif isinstance(value, bytes):
        payload, tag = value, b','
    elif isinstance(value, list):
        payload, tag = b''.join(tn(item) for item in value), b']'
    else:
        payload, tag = b''.join(tn(key) + tn(item) for key, item in value.items()), b'}'
    return str(len(payload)).encode() + b':' + payload + tag

flows = []
for index in range(4):
    flows.append(tn({b'type': b'http', 'version': 17, 'request': {
        'method': b'POST' if index == 0 else b'GET', 'scheme': b'https', 'host': b'example.test',
        'port': 443, 'path': b'/login?page=2&search=%D8%B9%D8%B1%D8%A8%D9%8A&empty=&plus=%2B+ok',
        'headers': [[b'content-type', b'application/x-www-form-urlencoded']],
        'content': b'username=demo%40example.test&password=sample123&empty=&phone=%2B201012345678',
        'timestamp_start': 1.25}, 'response': {'status_code': 200,
        'headers': [[b'content-type', b'application/json; charset=utf-8'], [b'set-cookie', b'a=1'], [b'set-cookie', b'b=2']],
        'content': '{"user":{"email":"demo@example.test"},"items":[{"id":0}],"ok":false,"nil":null,"empty":"","n":900719925474099312345,"n":2}'.encode()}}))
Path('core/src/test/resources/four-flows.tnet').write_bytes(b''.join(flows))
