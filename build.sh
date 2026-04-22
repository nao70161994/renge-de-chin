#!/data/data/com.termux/files/usr/bin/bash
set -e

PACKAGE="com.mycompany.myapp"
SRC_DIR="app/src/main/java"
RES_DIR="app/src/main/res"
MANIFEST="app/src/main/AndroidManifest.xml"
BUILD_DIR="app/build/output"
ANDROID_JAR="$BUILD_DIR/android.jar"
KEYSTORE="debug.keystore"

echo "=== 1. ツールのインストール ==="
pkg install -y openjdk-17 aapt2 d8 apksigner android-tools wget unzip 2>/dev/null || true

echo "=== 2. ビルドディレクトリの準備 ==="
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/classes" "$BUILD_DIR/dex"

echo "=== 3. android.jar の取得 ==="
if [ ! -f "$ANDROID_JAR" ]; then
    echo "android.jar をダウンロード中..."
    wget -q -O "$BUILD_DIR/platform.zip" \
        "https://dl.google.com/android/repository/platform-34_r02.zip"
    unzip -j -q "$BUILD_DIR/platform.zip" "android-34/android.jar" -d "$BUILD_DIR"
    rm "$BUILD_DIR/platform.zip"
    echo "android.jar 取得完了"
fi

echo "=== 4. リソースのコンパイル ==="
aapt2 compile -o "$BUILD_DIR/res_compiled.zip" --dir "$RES_DIR"

echo "=== 5. リソースのリンク（R.java生成）==="
aapt2 link \
    -o "$BUILD_DIR/unsigned.apk" \
    --manifest "$MANIFEST" \
    -I "$ANDROID_JAR" \
    --java "$BUILD_DIR/gen" \
    --rename-manifest-package "$PACKAGE" \
    --min-sdk-version 21 \
    --target-sdk-version 34 \
    --version-code 1 \
    --version-name "1.0" \
    "$BUILD_DIR/res_compiled.zip"

echo "=== 6. Java コンパイル ==="
JAVA_FILES=$(find "$SRC_DIR" "$BUILD_DIR/gen" -name "*.java" | tr '\n' ' ')
javac -source 1.8 -target 1.8 \
    -classpath "$ANDROID_JAR" \
    -d "$BUILD_DIR/classes" \
    $JAVA_FILES

echo "=== 7. DEX 変換 ==="
CLASS_FILES=$(find "$BUILD_DIR/classes" -name "*.class" | tr '\n' ' ')
d8 --output "$BUILD_DIR/dex" \
    --lib "$ANDROID_JAR" \
    --min-api 21 \
    $CLASS_FILES

echo "=== 8. APK に DEX を追加 ==="
cd "$BUILD_DIR/dex"
zip -q -u "../unsigned.apk" classes.dex
cd -

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
    "$BUILD_DIR/unsigned.apk"

echo ""
echo "✓ ビルド完了: app-debug.apk"
