#!/usr/bin/env bash
# تجهيز أدوات المستودع فقط؛ لا نعدل ملفات بدء الصدفة ولا نحفظ مفاتيح API.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"
mkdir -p .tools
case "$(uname -m)" in
  aarch64|arm64) tool_arch=aarch64; node_arch=arm64 ;;
  x86_64) tool_arch=x64; node_arch=x64 ;;
  *) echo 'Supported Ubuntu architectures: aarch64, x86_64.' >&2; exit 1 ;;
esac
for tool in curl python3 tar rg sha256sum; do command -v "$tool" >/dev/null || { echo "Missing prerequisite: $tool" >&2; exit 1; }; done
java_dir=''
for candidate in "${JAVA_HOME:-/nonexistent}" "$repo_dir"/.tools/jdk-* /usr/lib/jvm/java-17-openjdk-*; do
  if [[ -x "$candidate/bin/java" ]] && "$candidate/bin/java" -version 2>&1 | rg -q 'version "17\.'; then java_dir="$candidate"; break; fi
done
if [[ -z "$java_dir" ]]; then
  curl -fsSL --retry 3 "https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=$tool_arch&image_type=jdk&os=linux" -o .tools/jdk-download.json
  python3 - <<'PY'
import hashlib,json,pathlib,urllib.request
package=json.loads(pathlib.Path('.tools/jdk-download.json').read_text())[0]['binary']['package']
target=pathlib.Path('.tools/jdk17.tar.gz')
urllib.request.urlretrieve(package['link'],target)
digest=hashlib.sha256()
with target.open('rb') as stream:
    for block in iter(lambda:stream.read(1024*1024),b''):
        digest.update(block)
assert digest.hexdigest()==package['checksum'],'JDK checksum mismatch'
PY
  tar -xzf .tools/jdk17.tar.gz -C .tools
  for candidate in "$repo_dir"/.tools/jdk-*; do
    if [[ -x "$candidate/bin/java" ]] && "$candidate/bin/java" -version 2>&1 | rg -q 'version "17\.'; then java_dir="$candidate"; break; fi
  done
fi
[[ -n "$java_dir" ]] || { echo 'JDK 17 installation failed.' >&2; exit 1; }
node_dir=''
if command -v node >/dev/null && [[ "$(node -p 'process.versions.node.split(".")[0]')" == 22 ]]; then
  node_dir="$(dirname "$(command -v node)")"
else
  node_package="node-v22.22.1-linux-$node_arch"
  curl -fsSL --retry 3 "https://nodejs.org/dist/v22.22.1/$node_package.tar.xz" -o ".tools/$node_package.tar.xz"
  curl -fsSL --retry 3 https://nodejs.org/dist/v22.22.1/SHASUMS256.txt -o .tools/node-checksums.txt
  (cd .tools; rg "  $node_package.tar.xz$" node-checksums.txt | sha256sum -c -)
  tar -xJf ".tools/$node_package.tar.xz" -C .tools
  node_dir="$repo_dir/.tools/$node_package/bin"
fi
printf 'export JAVA_HOME=%q\nexport PATH=%q:%q:"$PATH"\n' "$java_dir" "$java_dir/bin" "$node_dir" > .tools/env.sh
source .tools/env.sh
if [[ "${1:-}" == --android ]]; then
  if [[ "$tool_arch" != x64 ]]; then
    echo 'Android APK packaging requires Ubuntu x86_64. Use GitHub Actions on this ARM64 phone.' >&2
    exit 1
  fi
  sdk_dir="$repo_dir/.tools/android-sdk"
  if [[ ! -x "$sdk_dir/cmdline-tools/latest/bin/sdkmanager" ]]; then
    curl -fsSL --retry 3 https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip -o .tools/android-tools.zip
    echo '4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583  .tools/android-tools.zip' | sha256sum -c -
    mkdir -p "$sdk_dir/cmdline-tools"
    python3 - <<'PY'
import zipfile
with zipfile.ZipFile('.tools/android-tools.zip') as archive:
    archive.extractall('.tools/android-sdk/cmdline-tools')
PY
    mv "$sdk_dir/cmdline-tools/cmdline-tools" "$sdk_dir/cmdline-tools/latest"
    chmod +x "$sdk_dir/cmdline-tools/latest/bin/"*
  fi
  printf 'export ANDROID_HOME=%q\nexport PATH=%q:%q:"$PATH"\n' "$sdk_dir" "$sdk_dir/platform-tools" "$sdk_dir/cmdline-tools/latest/bin" >> .tools/env.sh
  source .tools/env.sh
  sdkmanager --licenses
  sdkmanager 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
fi
(cd editor; npm ci; npx playwright install chromium)
java -version
node --version
echo 'Toolchain ready. Run: source .tools/env.sh'
echo './gradlew -PcoreOnly :core:test :ai:test'
echo 'cd editor && npm run build && npm test && npm run test:browser -- --workers=1'
