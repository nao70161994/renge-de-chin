# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

```bash
bash build.sh
```

初回実行時に必要ツール（openjdk-17, aapt2, d8, apksigner, ffmpeg 等）を自動インストールし、`app-debug.apk` を生成する。

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
- `app/src/main/assets/toos.ogg` — トースターボタン用の音声ファイル。`SoundPool` で再生。
- `app/src/main/res/layout/main.xml` (元ビルド内) — 縦並びのボタン3つ。`android:onClick` でメソッド名を直接指定。

### サウンド実装

| ボタン | 音 | 実装 |
|--------|-----|------|
| レンジ | チン（ベル） | AudioTrack で波形合成（880Hz、非整数倍音つき減衰サイン波） |
| オーブン | ブン | AudioTrack で波形合成（短い周波数ドロップ、250ms） |
| トースター | トゥース | `assets/toos.ogg` を SoundPool で再生 |

音声ファイルを差し替える場合は `app/src/main/assets/toos.ogg` を上書きしてビルドし直す。

## Build System の注意点

**リソースは元ビルドの `resources.ap_` を流用している。**

Termux 環境では aapt v1 / aapt2 と手元の `android.jar` の形式が合わず、新しいリソースをゼロからビルドできない。そのため：

- `app/build/bin/resources.ap_` — 元の AIDE/ADRT ビルドが生成した resources（マニフェスト・レイアウト・アイコン含む）をそのまま使用
- `sdk-android.jar` — Google リポジトリ（`platform-34-ext12_r01.zip`）から取得したクラススタブ。aapt2 による R.java 生成と javac コンパイルに使用
- `app/src/main/assets/` 以下のファイルは build.sh がビルド時に APK zip へ自動追加する

**レイアウトを変更する場合** は `resources.ap_` の再生成が必要になるため、AIDE 等の別ツールでビルドし直す必要がある。メソッド名（`range` / `oven` / `toast`）は `resources.ap_` 内の layout に `android:onClick` でハードコードされているため、変更時は合わせること。

## Notes

- Termux環境で動作。長いコマンドはスクリプトファイルに書いて実行する。
- `debug.keystore` はビルド時に自動生成される（初回のみ）。
- `app/build/`・`android.jar`・`sdk-android.jar`・`app-debug.apk`・`debug.keystore` は git 管理対象外（`.gitignore`）。
