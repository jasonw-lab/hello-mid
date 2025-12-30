# Kafka Streams Alert ruleまとめ（AlertStreamsConfig / OrderPaymentTransformer）

## 目的
注文・決済イベント（`OrderConfirmed` / `PaymentSucceeded`）をストリームで監視し、以下の異常を検知して **ALERTS トピックへアラートイベント**を出力する。

- **Rule A（P2）**: `PaymentSucceeded` が来たのに、一定時間内に `OrderConfirmed` が来ない  
- **Rule B（P2）**: `OrderConfirmed` が来たのに、一定時間内に `PaymentSucceeded` が来ない  
- **Rule C（P1）**: 同一 orderId に対して `PaymentSucceeded` が複数回発生（多重決済）

---

## 全体アーキテクチャ
### 入出力（イベントストリーム）
- Input Topics
  - `TopicNames.ORDERS` : 注文系イベント（例: `OrderConfirmed`）
  - `TopicNames.PAYMENTS` : 決済系イベント（例: `PaymentSucceeded`）
- Output Topic
  - `TopicNames.ALERTS` : アラート（`AlertRaisedEvent` JSON、key=orderId）

### 状態管理（State Store）
- Store名: `order-payment-store`
- 種別: `persistentKeyValueStore`（通常 RocksDB）
- Key: `orderId`
- Value: `OrderPaymentState`（JSON）

---

## AlertStreamsConfig（Topology構築）
### 役割
- Kafka Streams トポロジー定義
- 永続 StateStore 登録
- ORDERS / PAYMENTS を統合し Transformer を適用

### 処理概要
1. StateStore を builder に登録
2. 2トピックを `merge`
3. `OrderPaymentTransformer` を適用
4. ALERTS トピックへアラート出力

---

## OrderPaymentTransformer（検知ロジック）
### transform() の役割
- イベント1件ごとの状態更新
- 即時判定できるルール（Rule C）の検知とアラート発火

### punctuator の役割
- 一定間隔で StateStore を全件スキャン
- 期限超過によるルール（Rule A/B）の検知

---

## transform() と punctuator の役割分担

| 観点 | transform() | punctuator |
|---|---|---|
| 実行タイミング | イベント到着ごと | 定期実行 |
| 主な責務 | 状態更新 + 即時検知 | 時間経過が必要な検知 |
| 対象ルール | Rule C | Rule A / Rule B |
| スキャン範囲 | 単一 orderId | 全 orderId |
| コスト特性 | イベント数比例 | Store件数比例 |

---

## 設計の評価ポイント
- transform / punctuator の責務分離が明確
- 状態付きストリーム処理として実務的
- 監視・異常検知用途に適した Kafka Streams 活用例

---

## 本番向け改善ポイント
- Store 全件スキャンのスケール対策
- 例外処理・メトリクス強化
- 冪等性・重複アラート対策
