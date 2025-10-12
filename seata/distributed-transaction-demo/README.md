# Seata 分散トランザクション デモ (AT / TCC / SAGA)

Spring Boot 3 / Java 17。AT、TCC、SAGA の3モードを同一構成（order, storage, account の3サービス）で動作検証できる学習用デモです。

## 技術スタック
- Java 17
- Spring Boot 3.x
- Seata 2.0
- MyBatis-Plus
- MySQL 8.0
- Docker Compose

## 構成
- order-service (port 8081)
- storage-service (port 8082)
- account-service (port 8083)


## 事前準備（MySQL と Seata Server）
- MySQL はリポジトリ直下の `_docker/docker-compose-mysql.yml` で起動します（8.0, 3307）。
  ```bash
  cd _docker
  docker compose -f docker-compose-mysql.yml up -d
  ```
- 起動時に自動作成されるもの:
  - DB: seata_order, seata_storage, seata_account, seata
  - テーブル: 各ビジネス DDL＋undo_log、Seata メタテーブル
  - 初期データ: 在庫とアカウント残高
- 接続情報:
  - host: 127.0.0.1
  - port: 3307
  - user: root / pass: 123456
- Seata Server は 127.0.0.1:8091 で稼働させてください（本デモのプロファイル設定は registry=config=file, grouplist=127.0.0.1:8091 を前提にしています）。

## ビルド/基本テスト
各サービスは Spring Boot アプリとしてビルドできます。現時点の自動テストは Actuator のヘルスのみです。
```bash
# 本ディレクトリで
mvn -q -DskipTests=false -pl order-service test
mvn -q -DskipTests=false -pl storage-service test
mvn -q -DskipTests=false -pl account-service test
```

## 実行（共通）
- Spring Profile によってモードを切り替えます: `at` / `tcc` / `saga`
- 例（それぞれ別ターミナルで起動）:
  ```bash
  mvn -q -pl storage-service spring-boot:run -Dspring-boot.run.profiles=at
  mvn -q -pl account-service  spring-boot:run -Dspring-boot.run.profiles=at
  mvn -q -pl order-service    spring-boot:run -Dspring-boot.run.profiles=at
  ```
  プロファイルを `tcc` または `saga` に置き換えると各モードで起動します。

ヘルス確認:
```bash
curl -s localhost:8081/actuator/health | jq
curl -s localhost:8082/actuator/health | jq
curl -s localhost:8083/actuator/health | jq
```

---

## AT モード
- Profile: `at`
- エンドポイント:
  - POST `http://localhost:8081/api/orders`（注文作成）
- サンプルリクエスト:
  ```bash
  curl -L -X POST 'http://127.0.0.1:8081/api/orders' \
    -H 'Content-Type: application/json' --data '{
      "userId": 1,
      "productId": 1,
      "orderNo": "",
      "count": 2,
      "amount": 20.0
    }'
  ```
- 解説:
  - order-service が在庫・口座サービスを呼び出し、Seata AT により一括コミット/ロールバックします。
  - 各サービスの `application-at.yaml` にて `tx-service-group: at_tx_group` を利用します。

## TCC モード
- Profile: `tcc`
- エンドポイント:
  - POST `http://localhost:8081/api/orders/tcc`（TCC 注文作成）
- 簡易テスト:
  ```bash
  ./test-tcc.sh
  ```
  もしくは手動で:
  ```bash
  curl -L -X POST 'http://127.0.0.1:8081/api/orders/tcc' \
    -H 'Content-Type: application/json' --data '{
      "userId": 1,
      "productId": 1,
      "orderNo": "",
      "count": 10,
      "amount": 10.0
    }'
  ```
- 詳細な流れや背景は `TCC-README.md` を参照してください。

## SAGA モード
- Profile: `saga`
- エンドポイント:
  - POST `http://localhost:8081/api/orders/saga`（State Language による SAGA 実行）
  - POST `http://localhost:8081/api/orders/saga/sample`（簡易サンプル）
- ステートマシン定義:
  - `order-service/src/main/resources/statelang/order_create_saga.json`
- 簡易テスト:
  ```bash
  # 既定値(count=10, amount=10.0)
  ./test-saga.sh

  # パラメータを変えて異常系を再現
  ./test-saga.sh 999999 10.0     # 在庫不足
  ./test-saga.sh 1 1000000.0     # 残高不足
  ```
- メモ:
  - 各サービスの `application-saga.yaml` にて `tx-service-group: saga_tx_group` を利用します。

---

## トラブルシュート
- Seata Server が 127.0.0.1:8091 で稼働しているかを確認してください。
- MySQL が 3307 ポートで起動し、初期データが投入されているかを確認してください。
- プロファイルは明示指定がおすすめです（例: `-Dspring-boot.run.profiles=tcc`）。
