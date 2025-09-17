# Seata Saga デモ（雛形）

本ディレクトリは Saga モードの分散トランザクション用の雛形です。現時点ではディレクトリ構成と README のみを用意しています。今後、状態遷移 JSON（statemachine）と forward/compensate API を実装します。

## 構成（予定）
- order-service
- storage-service
- account-service
- resources/statemachine/order_saga.json（予定）

## 仕様方針
- 通信: REST
- ポート（予定）: order 8081, storage 8082, account 8083（同時起動は想定しない）
- 失敗条件: 在庫不足/残高不足時に compensate が実行されること

## 次ステップ
- 状態機械 JSON の定義（forward/compensate）
- 正常/異常の結合 API テスト（RestAssured）
- Seata/Nacos 設定、XID をログ出力
