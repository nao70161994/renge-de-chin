#!/data/data/com.termux/files/usr/bin/bash
set -e

PACKAGE="com.mycompany.myapp"
SRC_DIR="app/src/main/java"
MANIFEST="app/src/main/AndroidManifest.xml"
BUILD_DIR="app/build/output"
SDK_JAR="sdk-android.jar"
ORIG_RESOURCES="app/build/bin/resources.ap_"
KEYSTORE="debug.keystore"

echo "=== 1. ツールのインストール ==="
pkg install -y openjdk-17 aapt2 d8 apksigner android-tools wget unzip zip 2>/dev/null || true

echo "=== 2. SDK android.jar の取得 ==="
if [ ! -f "$SDK_JAR" ]; then
    wget -q -O /tmp/platform-34.zip \
        "https://dl.google.com/android/repository/platform-34-ext12_r01.zip"
    unzip -j -q /tmp/platform-34.zip "android-34-ext12/android.jar" -d .
    mv android.jar "$SDK_JAR"
    rm /tmp/platform-34.zip
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
    "$BUILD_DIR/res_compiled.zip"

echo "=== 5. Java コンパイル ==="
JAVA_FILES=$(find "$SRC_DIR" "$BUILD_DIR/gen" -name "*.java" | tr '\n' ' ')
javac --release 8 \
    -classpath "$SDK_JAR" \
    -d "$BUILD_DIR/classes" \
    $JAVA_FILES

echo "=== 6. DEX 変換 ==="
CLASS_FILES=$(find "$BUILD_DIR/classes" -name "*.class" | tr '\n' ' ')
d8 --output "$BUILD_DIR/dex" \
    --lib "$SDK_JAR" \
    --min-api 21 \
    $CLASS_FILES

echo "=== 7. 元のresources.ap_に新しいDEXを追加 ==="
cp "$ORIG_RESOURCES" "$BUILD_DIR/unsigned.apk"
cd "$BUILD_DIR/dex"
zip -q -0 -u "../unsigned.apk" classes.dex
cd -

echo "=== 7b. アセットの追加 ==="
ASSETS_SRC="app/src/main/assets"
if [ -d "$ASSETS_SRC" ] && [ "$(ls -A $ASSETS_SRC 2>/dev/null)" ]; then
    STAGE=$(mktemp -d)
    mkdir -p "$STAGE/assets"
    cp -r "$ASSETS_SRC/"* "$STAGE/assets/"
    ABS_APK=$(realpath "$BUILD_DIR/unsigned.apk")
    cd "$STAGE"
    zip -0 -u "$ABS_APK" assets/*
    cd -
    rm -rf "$STAGE"
fi

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
