# Seata TCC デモ

## 概要

このデモは、Seata TCC（Try-Confirm-Cancel）パターンを使用した分散トランザクションの実装例です。
参考記事（[Seata TCC 模式详解](https://juejin.cn/post/7487816112626024475)）に基づいて実装されており、
シンプルでわかりやすいデモとして設計されています。本番で使える品質を保ちながら学習しやすい構造になっています。

## アーキテクチャ

```
Order Service (8081)
    ├── TCCオーダー作成（tcc_orderテーブル）
    ├── Storage TCC Try 呼び出し
    ├── Account TCC Try 呼び出し
    └── 実際のオーダー作成（t_orderテーブル）

Storage Service (8082)
    ├── TRY: 在庫を凍結（frozenフィールド）
    ├── CONFIRM: 凍結分を実際の使用分に移動
    └── CANCEL: 凍結分を在庫に戻す

Account Service (8083)
    ├── TRY: 残高を凍結（frozenフィールド）
    ├── CONFIRM: 凍結分を実際の使用分に移動
    └── CANCEL: 凍結分を残高に戻す
```

## TCC パターンの流れ

### 正常ケース
1. **Order Service**: TCCオーダー作成（tcc_orderテーブル、ステータス: 0=PENDING）
2. **Storage TCC Try**: 在庫を凍結（frozenフィールドに移動）
3. **Account TCC Try**: 残高を凍結（frozenフィールドに移動）
4. **Seata**: 全サービスで CONFIRM 実行
   - Storage: 凍結分を実際の使用分に移動
   - Account: 凍結分を実際の使用分に移動
5. **Order Service**: 実際のオーダー作成（t_orderテーブル、ステータス: 1=確定）

### 異常ケース（ロールバック）
1. **Order Service**: TCCオーダー作成（tcc_orderテーブル、ステータス: 0=PENDING）
2. **Storage TCC Try**: 在庫を凍結（frozenフィールドに移動）
3. **Account TCC Try**: 残高不足でエラー
4. **Seata**: 全サービスで CANCEL 実行
   - Storage: 凍結分を在庫に戻す
   - Account: 凍結分を残高に戻す
5. **Order Service**: TCCオーダーは失敗（ステータス: 2=FAILED）

## 起動方法

### 1. データベース起動
```bash
cd _docker
docker-compose -f docker-compose-mysql.yml up -d
```

### 2. Seata Server 起動
```bash
# Seata Server を起動（デフォルト: localhost:8091）
# 設定は file レジストリを使用
```

### 3. サービス起動
```bash
# 各サービスを起動（TCC プロファイル）
mvn spring-boot:run -Dspring-boot.run.profiles=tcc
```

## API テスト

### 正常ケース
```bash
curl -X POST http://localhost:8081/api/orders/tcc \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "count": 1,
    "amount": 10.00,
    "orderNo": "ORDER-001"
  }'
```

### 在庫不足ケース
```bash
curl -X POST http://localhost:8081/api/orders/tcc \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "count": 999999,
    "amount": 10.00,
    "orderNo": "ORDER-002"
  }'
```

### 残高不足ケース
```bash
curl -X POST http://localhost:8081/api/orders/tcc \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "productId": 1,
    "count": 1,
    "amount": 1000000.00,
    "orderNo": "ORDER-003"
  }'
```

## ログの確認

各サービスのログで以下のような TCC の流れを確認できます：

```
=== TCC オーダー作成開始 ===
オーダー作成完了: orderNo=ORDER-001
--- Storage TCC Try 呼び出し ---
=== TCC TRY (Storage) ===
TRY completed: 在庫減算予約を記録しました
Storage TCC Try 成功
--- Account TCC Try 呼び出し ---
=== TCC TRY (Account) ===
TRY completed: 残高凍結予約を記録しました
Account TCC Try 成功
=== TCC オーダー作成完了 ===
```

## 特徴

- **参考記事準拠**: [Seata TCC 模式详解](https://juejin.cn/post/7487816112626024475) に基づいた実装
- **シンプル**: 複雑なサスペンション対策を排除し、デモに集中
- **わかりやすい**: 日本語ログで各フェーズの動作を明確に表示
- **本番品質**: 冪等性、エラーハンドリング、適切なログ出力
- **学習向け**: TCC パターンの理解に最適化された構造
- **RestTemplate使用**: Feignを使わず、シンプルなHTTP通信

## 技術スタック

- Java 17
- Spring Boot 3.x
- Seata 2.0
- MyBatis-Plus
- MySQL 8.0
- Docker Compose
