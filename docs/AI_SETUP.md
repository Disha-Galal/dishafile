# Ollama, Gemini and Ubuntu setup

This project By Gogo & Disha ®

FileKit 1.3 supports local Ollama, direct Gemini, and optional Gemini fallback. Translation results are reviewed before replacement and saved only through the usual protected save flow.

## Android app

1. Open **AI settings** from Options or the AI panel.
2. Keep Ollama at `http://127.0.0.1:11434`, with `llama3.2:3b` then `qwen2.5:0.5b`. The service must run on the same phone. For slower phones, put Qwen first.
3. Enter your Gemini key in the native key field. A shell `export GEMINI_API_KEY=…` does not configure Android. Blank keeps the encrypted saved key; Delete removes it.
4. Use `gemini-3.1-flash-lite` for Gemini, or **Available Gemini models** to choose another. Existing saved model names are preserved: update a saved `gemini-2.5-flash` if its generation requests return 404.
5. Run **Test Ollama translation** or **Test Gemini translation**, then Save. Tests send only a fixed “Hello world” phrase. Model listing alone is not a generation test.
6. Open text, select a paragraph, open **✦ AI**, choose the provider and translation language, prepare the translation request, and Send. Confirm the destination, review the result, then replace the selection if desired.

Gemini requires internet, a usable API key and available quota. Provider model access can change. The app reports missing keys, rejected keys, quota errors, unavailable models, missing Ollama runners, insufficient memory, and incomplete answers separately. It never displays provider error bodies that might contain credentials or document text.

## Ubuntu inside Termux PRoot (ARM64)

Use Ubuntu commands here, not Termux `pkg` commands. Prerequisites are Bash, curl, Python 3, tar/xz, ripgrep and sha256sum. The setup script installs tools inside this repository’s ignored `.tools/`; it does not change shell startup files or store API keys.

```bash
cd /root/disha-filekit
bash scripts/setup-ubuntu.sh
source .tools/env.sh
./gradlew -PcoreOnly :core:test :ai:test
cd editor
npm run build
npm test
npm run test:browser -- --workers=1
```

If Chromium reports missing system libraries on a new Ubuntu installation, install the Linux dependencies documented by [Playwright](https://playwright.dev/docs/browsers#install-system-dependencies). Android APK packaging and API 33 emulator tests run in GitHub Actions on Ubuntu x86_64; the Android SDK’s host packaging tools are not supported by this project on ARM64 PRoot. Installing the APK needs no PC or Android Studio.

## Terminal AI checks

These commands use a fixed test phrase; they never read or execute a user document. Read the key privately at the prompt so the value is not recorded in shell history. This configures this terminal only.

```bash
cd /root/disha-filekit
read -rsp 'Gemini API key: ' GEMINI_API_KEY
export GEMINI_API_KEY
python3 scripts/check-ai.py --provider gemini
python3 scripts/check-ai.py --provider gemini --list-models
python3 scripts/check-ai.py --provider ollama
python3 scripts/check-ai.py --provider ollama --model llama3.2:3b
```

The default test model is `gemini-3.1-flash-lite` for Gemini and `qwen2.5:0.5b` for Ollama. Use `--endpoint http://127.0.0.1:PORT` for a different local port. Redirects are refused. API keys are sent in the Gemini authentication header, never the URL.

## Ollama models appear, but generation returns HTTP 500

The 2026-11-18 investigation reproduced this exact failure for both installed models: the service reported a missing `llama-server` executable. Its metadata endpoint still listed the model weights correctly. This is an incomplete Ollama runtime installation, independent of the APK and API key.

Install the complete [official Linux distribution](https://docs.ollama.com/linux), including the bundled `lib/ollama` model runner and CPU libraries, in the Ubuntu environment that runs your service. Copying only the `ollama` executable is insufficient. Keep your existing model storage; do not delete or redownload weights to fix a missing runner. Restart the repaired service, run the translation check above, then use the same loopback port in Android settings.

## Repair applied in this workspace (2026-11-18)

The complete official Ollama v0.40.16 ARM64 package was downloaded and its SHA-256 verified (`e45b8e6dcb86cb9f3057396351655079c9a4879b4ced2678c9350d375dcbc224`). Its CPU runtime is installed under ignored `.tools/ollama/`; CUDA/Vulkan/ROCm bundles were excluded. The incomplete service was restarted with the repaired runtime on `http://127.0.0.1:11434`, retaining the existing model store and both model names. No model weights were downloaded or deleted.

The local launcher is `.tools/start-ollama.sh`. If the service stops, run it in Ubuntu and keep the terminal alive. Do not start another service while port 11434 is already in use. `source .tools/env.sh` selects the local JDK, Node and repaired Ollama CLI for this shell; build setup and the runtime install are separate. The launcher and binaries are local generated files, not included in Git or the APK. After a fresh clone, install the complete official distribution as described above.

The app's compiled `AiClient` successfully generated text with Qwen and Gemini. Gemini translated the test phrase correctly. Qwen's Arabic answer was unreliable, and the Llama test exceeded the 180-second deadline on this phone. Use direct Gemini for Arabic translation; local transport support does not guarantee a small model's translation quality or speed. No successful Llama translation or physical Android app test is claimed.

## Ubuntu x86_64 / GitHub Android build

```bash
bash scripts/setup-ubuntu.sh --android
source .tools/env.sh
./gradlew :core:test :ai:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

The optional Android setup installs SDK platform 35 and build tools 35.0.0, and asks you to accept SDK licenses. CI already sets up JDK 17, Node 22 and those SDK packages, runs JVM/browser checks, packages the APK and test APK, then runs API 33 smoke tests. See [BUILD_STATUS.md](BUILD_STATUS.md) for actual results and [ACCEPTANCE.md](ACCEPTANCE.md) for physical phone checks.

Provider references: [Ollama generate](https://docs.ollama.com/api/generate), [Gemini generateContent](https://ai.google.dev/api/generate-content), [Gemini API keys](https://ai.google.dev/gemini-api/docs/api-key).
