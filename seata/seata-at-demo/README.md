# Seata AT デモ (Spring Boot 3 / Java 17)

本ディレクトリは AT モードの分散トランザクションの雛形です。各サービスは独立した Spring Boot アプリ（order, storage, account）です。まずはヘルスチェックの起動/テストが通る最小構成を用意しています。

今後、MyBatis-Plus と Seata クライアント設定（Nacos/Seata Server）を追加し、@GlobalTransactional を用いた分散トランザクションを実装します。

## 構成
- order-service (port 8081)
- storage-service (port 8082)
- account-service (port 8083)

## 事前準備（MySQL）
リポジトリルートの `docker/docker-compose-mysql.yml` を利用して MySQL(8.0, 3307) を起動します。

```bash
cd ../../docker
docker compose -f docker-compose-mysql.yml up -d
```

起動時に以下が自動で作成されます。
- DB: seata_order, seata_storage, seata_account, seata
- テーブル: 各ビジネスDDL＋undo_log、Seataメタテーブル
- 初期データ: 在庫とアカウント残高

接続情報:
- host: 127.0.0.1
- port: 3307
- user: root / pass: 123456

## ビルド/テスト
各サービスは Spring Boot アプリとしてビルドできます。現時点のテストは Actuator のヘルスエンドポイント検証のみです。

```bash
# ルート（本ディレクトリ）で
mvn -q -DskipTests=false -pl order-service test
mvn -q -DskipTests=false -pl storage-service test
mvn -q -DskipTests=false -pl account-service test
```

## 実行
```bash
# それぞれ別ターミナルで
mvn -q -pl order-service spring-boot:run
mvn -q -pl storage-service spring-boot:run
mvn -q -pl account-service spring-boot:run
```

ヘルス確認:
```bash
curl -s localhost:8081/actuator/health | jq
curl -s localhost:8082/actuator/health | jq
curl -s localhost:8083/actuator/health | jq
```

## 今後の追加（計画）
- Seata クライアント設定（registry/config=Nacos、tx-service-group など）
- MyBatis-Plus による DB アクセス、undo_log の適用
- OrderService を起点とした @GlobalTransactional サンプル実装
- 正常/異常系の結合テスト（RestAssured）
- ログに XID（%X{XID}）を出力
