#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# Read only recognized settings; do not execute the properties file as shell code.
while IFS='=' read -r key value; do
    case "$key" in
        COMPILE_SDK|MIN_SDK|TARGET_SDK|VERSION_CODE|VERSION_NAME|SDK_ARCHIVE_URL|SDK_JAR_ENTRY)
            printf -v "$key" '%s' "$value" ;;
    esac
done < app-config.properties
for key in COMPILE_SDK MIN_SDK TARGET_SDK VERSION_CODE; do
    [[ "${!key}" =~ ^[0-9]+$ ]] || { echo "Invalid setting: $key" >&2; exit 1; }
done

SRC_DIR="app/src/main/java"
MANIFEST="app/src/main/AndroidManifest.xml"
BUILD_DIR="app/build/output"
SDK_JAR="sdk-android-${COMPILE_SDK}.jar"
KEYSTORE="debug.keystore"

echo "=== 1. ツールのインストール ==="
if command -v pkg >/dev/null; then
    pkg install -y openjdk-17 aapt aapt2 d8 apksigner android-tools wget unzip zip
fi
for tool in javac aapt2 d8 apksigner zipalign keytool wget unzip zip; do
    command -v "$tool" >/dev/null || { echo "Missing tool: $tool" >&2; exit 1; }
done

echo "=== 2. SDK android.jar の取得 ==="
if [ ! -f "$SDK_JAR" ]; then
    SDK_DOWNLOAD_DIR=$(mktemp -d)
    trap 'rm -rf "$SDK_DOWNLOAD_DIR"' EXIT
    wget -q -O "$SDK_DOWNLOAD_DIR/platform.zip" \
        "$SDK_ARCHIVE_URL"
    unzip -j -q "$SDK_DOWNLOAD_DIR/platform.zip" "$SDK_JAR_ENTRY" -d "$SDK_DOWNLOAD_DIR"
    mv "$SDK_DOWNLOAD_DIR/android.jar" "$SDK_JAR"
fi

echo "=== 3. ビルドディレクトリの準備 ==="
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/classes" "$BUILD_DIR/dex" "$BUILD_DIR/res_flat"

echo "=== 4. R.java の生成（aapt2）==="
aapt2 compile -o "$BUILD_DIR/res_compiled.zip" --dir "app/src/main/res"
aapt2 link \
    -o "$BUILD_DIR/tmp.apk" \
    --manifest "$MANIFEST" \
    -I "$SDK_JAR" \
    --java "$BUILD_DIR/gen" \
    --min-sdk-version "$MIN_SDK" --target-sdk-version "$TARGET_SDK" \
    --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
    -A app/src/main/assets \
    -0 ogg -0 wav \
    "$BUILD_DIR/res_compiled.zip"

echo "=== 5. Java コンパイル ==="
mapfile -d '' JAVA_FILES < <(find "$SRC_DIR" "$BUILD_DIR/gen" -name "*.java" -print0)
javac --release 8 \
    -classpath "$SDK_JAR" \
    -d "$BUILD_DIR/classes" \
    "${JAVA_FILES[@]}"

echo "=== 6. DEX 変換 ==="
mapfile -d '' CLASS_FILES < <(find "$BUILD_DIR/classes" -name "*.class" -print0)
d8 --output "$BUILD_DIR/dex" \
    --lib "$SDK_JAR" \
    --min-api "$MIN_SDK" \
    "${CLASS_FILES[@]}"

echo "=== 7. ソースから生成したリソースにDEXを追加 ==="
cp "$BUILD_DIR/tmp.apk" "$BUILD_DIR/unsigned.apk"
cd "$BUILD_DIR/dex"
zip -q -0 -u "../unsigned.apk" classes.dex
cd -

echo "=== 8. zipalign ==="
zipalign -f 4 "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/aligned.apk"

echo "=== 9. 署名キーの作成 ==="
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -storepass android \
        -alias androiddebugkey \
        -keypass android \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null
fi

echo "=== 10. APK に署名 ==="
apksigner sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "app-debug.apk" \
    "$BUILD_DIR/aligned.apk"

echo ""
echo "✓ ビルド完了: app-debug.apk"
