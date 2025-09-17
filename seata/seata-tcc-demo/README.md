# Seata TCC デモ（雛形）

本ディレクトリは TCC (Try-Confirm-Cancel) モードの分散トランザクション用の雛形です。現時点ではディレクトリ構成と README のみを用意しています。今後、各サービスに Try/Confirm/Cancel を実装します。

## 構成（予定）
- order-service
- storage-service
- account-service

## 仕様方針
- 通信: REST（必要に応じて OpenFeign）
- ポート（予定）: order 8081, storage 8082, account 8083（同時起動想定なしのため AT と競合しないよう適宜調整）
- 失敗条件: 在庫不足/残高不足時に Cancel が走ること

## 次ステップ
- @TwoPhaseBusinessAction を用いた TCC 実装（prepare/commit/rollback）
- 正常/異常の結合 API テスト（RestAssured）
- Seata/Nacos 設定、XID をログ出力
