#!/usr/bin/env python3
# فحص بعبارة تجريبية ثابتة؛ لا يقرأ مستندات المستخدم ولا يطبع المفتاح أو ردود الخطأ الخام.
import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request


class CheckError(Exception):
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def request(url, key=None, body=None):
    headers = {'Content-Type': 'application/json; charset=utf-8'}
    if key:
        headers['x-goog-api-key'] = key
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode('utf-8')
    try:
        with urllib.request.build_opener(NoRedirect).open(urllib.request.Request(url, data, headers), timeout=180) as response:
            payload = response.read(1024 * 1024 + 1)
            if len(payload) > 1024 * 1024:
                raise CheckError('Response exceeds 1 MiB')
            return json.loads(payload.decode('utf-8', errors='strict'))
    except urllib.error.HTTPError as error:
        payload = error.read(16384).decode('utf-8', errors='strict')
        if not key and ('binary not found' in payload or 'runner not found' in payload):
            detail = 'Ollama model runner is missing. Install the complete Ollama distribution, including lib/ollama; ollama list does not test generation.'
        else:
            detail = {400: 'Invalid request or API key.', 401: 'Key rejected.', 403: 'Permission denied.',
                      404: 'Model not available. Choose another model and test generation.',
                      429: 'Rate limit or quota exceeded.'}.get(error.code, 'Provider or runtime failure.')
        raise CheckError(f'HTTP {error.code}: {detail}') from None


def main():
    parser = argparse.ArgumentParser(description='Test Ollama/Gemini translation without sending any user documents.')
    parser.add_argument('--provider', choices=['ollama', 'gemini'], required=True)
    parser.add_argument('--endpoint', default='http://127.0.0.1:11434')
    parser.add_argument('--model')
    parser.add_argument('--list-models', action='store_true')
    args = parser.parse_args()
    cloud = args.provider == 'gemini'
    key = os.environ.get('GEMINI_API_KEY', '') if cloud else None
    if cloud and not key:
        raise CheckError('Set GEMINI_API_KEY in this terminal. Android keeps its own key in AI settings.')
    base = 'https://generativelanguage.googleapis.com/v1beta' if cloud else args.endpoint.rstrip('/')
    if not cloud:
        parsed = urllib.parse.urlsplit(base)
        if parsed.scheme != 'http' or parsed.hostname not in ('127.0.0.1', 'localhost', '::1') or parsed.path or parsed.query or parsed.fragment or parsed.username or parsed.password:
            raise CheckError('Only a local loopback Ollama endpoint is allowed.')
    if args.list_models:
        result = request(base + ('/models?pageSize=1000' if cloud else '/api/tags'), key)
        for model in result.get('models', []):
            if not cloud or 'generateContent' in model.get('supportedGenerationMethods', []):
                print(model['name'])
        return
    model = args.model or ('gemini-3.1-flash-lite' if cloud else 'qwen2.5:0.5b')
    if cloud and (not model or len(model) > 100 or any(c not in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-' for c in model)):
        raise CheckError('Invalid Gemini model name.')
    prompt = 'Translate to Arabic. Return only the translated text: Hello world'
    if cloud:
        result = request(base + '/models/' + model + ':generateContent', key,
                         {'contents': [{'role': 'user', 'parts': [{'text': prompt}]}], 'generationConfig': {'maxOutputTokens': 8192}})
        candidate = result.get('candidates', [{}])[0]
        if candidate.get('finishReason') != 'STOP':
            raise CheckError('Gemini response is blocked, incomplete or invalid.')
        text = ''.join(part.get('text', '') for part in candidate.get('content', {}).get('parts', []) if not part.get('thought'))
    else:
        result = request(base + '/api/generate', body={'model': model, 'prompt': prompt, 'stream': False,
                         'keep_alive': 0, 'options': {'num_ctx': 8192, 'num_predict': 1024}})
        if not result.get('done') or result.get('done_reason') == 'length' or 'error' in result:
            raise CheckError('Ollama response is incomplete or invalid.')
        text = result.get('response', '')
    if not text.strip():
        raise CheckError('Empty translation.')
    print(f'{model}: {text}')


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        # نطبع رسائل الفحص المعروفة فقط؛ استثناءات الشبكة قد تحتوي بيانات المصادقة.
        print(str(error) if isinstance(error, CheckError) else type(error).__name__, file=sys.stderr)
        sys.exit(1)
