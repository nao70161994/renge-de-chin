# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

```bash
bash build.sh
```

初回実行時に必要ツール（openjdk-17, aapt2, d8, apksigner 等）を自動インストールし、`app-debug.apk` を生成する。

## インストール

```bash
# ダウンロードフォルダにコピー
cp app-debug.apk /storage/emulated/0/Download/

# ローカルHTTPサーバーで配信（ブラウザからインストール）
python -m http.server 8080
# → ブラウザで http://localhost:8080/app-debug.apk を開く
```

## Architecture

シンプルな単一Activity構成のAndroidアプリ。

- `app/src/main/java/com/mycompany/myapp/MainActivity.java` — 唯一のActivity。`range()` / `oven()` / `toast()` の3メソッドがXMLの `android:onClick` から呼ばれ、`showPopup()` で吹き出しを表示する。複数ボタンを連続で押すと `PopupWindow` が同時に複数表示される。
- `app/src/main/res/layout/main.xml` (元ビルド内) — 縦並びのボタン3つ。`android:onClick` でメソッド名を直接指定。

## Build System の注意点

**リソースは元ビルドの `resources.ap_` を流用している。**

Termux 環境では aapt v1 / aapt2 と手元の `android.jar` の形式が合わず、新しいリソースをゼロからビルドできない。そのため：

- `app/build/bin/resources.ap_` — 元の AIDE/ADRT ビルドが生成した resources（マニフェスト・レイアウト・アイコン含む）をそのまま使用
- `sdk-android.jar` — Google リポジトリ（`platform-34-ext12_r01.zip`）から取得したクラススタブ。aapt2 による R.java 生成と javac コンパイルに使用
- `android.jar` — Robolectric 版（javac には使わない）

**レイアウトを変更する場合** は `resources.ap_` の再生成が必要になるため、AIDE 等の別ツールでビルドし直す必要がある。メソッド名（`range` / `oven` / `toast`）は `resources.ap_` 内の layout に `android:onClick` でハードコードされているため、変更時は合わせること。

## Notes

- Termux環境で動作。長いコマンドはスクリプトファイルに書いて実行する。
- `debug.keystore` はビルド時に自動生成される（初回のみ）。
- `app/build/` と `android.jar`・`sdk-android.jar` は git 管理対象外（`.gitignore`）。
