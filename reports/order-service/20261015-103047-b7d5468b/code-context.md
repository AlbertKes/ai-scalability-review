# Service Code Analysis — Business Context Extraction: order-service

This document provides a factual, code-observable analysis of the `order-service` application within the `foodtruck-project` repository. It covers the API surface, database access patterns, Kafka usage, caching strategy, resilience configuration, external dependencies, and scalability signals to support a comprehensive scalability review.

---

## 1. API Surface

`order-service` exposes endpoints primarily through Core-NG WebService interfaces registered via `api().service(Interface.class, bean)` and custom HTTP routes registered via `http().route(method, path, controller)`.

### 1.1 WebService Interfaces and Endpoints

The service defines 33 distinct WebService interfaces across `backend/order-service-interface`, exposing a total of 205 API operations:

1. **OrderWebService** (`app.order.api.OrderWebService`):
   - `POST /v2/order`: Primary consumer order placement.
   - `PUT /order/count`: Query order counts by user and status.
   - `GET /v2/order/:orderId`: Retrieve full order details for consumer app.
   - `GET /v2/order/by-order-number/:orderNumber`: Retrieve order by readable order number.
   - `GET /order/:orderId/status`: Lightweight order status lookup.
   - `PUT /order/:orderId/cancel`: Consumer-initiated order cancellation.
   - `POST /v2/order/:orderId/tip`: Post-order tipping.
   - `PUT /v2/order/:orderId/tip`: Adjust post-order tip.
   - `GET /v2/order/:orderId/tip-info`: Retrieve tip configuration and amounts.
   - `PUT /order/timeslot-usage`: Retrieve delivery time slot capacity and utilization.
   - `PUT /order/:orderId/use-timeslot`: Reserve delivery time slot.
   - `PUT /order/:orderId/revert-timeslot`: Release reserved time slot.
   - `PUT /order/:orderId/pre-auth-payment`: Pre-authorize payment for order.
   - `PUT /order/:orderId/revert-pre-auth-payment`: Revert pre-authorized payment.
   - `GET /order/:orderId/receipt`: Generate receipt details for consumer.
   - `GET /order/:orderId/package/status`: Package delivery tracking.

2. **PriceWebService** (`app.order.api.PriceWebService`):
   - `PUT /v3/price`: Comprehensive checkout price calculation including taxes, promotions, delivery fees, and credits.
   - `PUT /v3/subtotal`: Shopping cart subtotal calculation.
   - `PUT /tips`: Tip amount options calculation.
   - `PUT /tip-options`: Tip percentage preset configurations.
   - `PUT /tip/amount`: Specific tip amount calculation.

3. **ConsumerOrderWebService** (`app.order.api.ConsumerOrderWebService`):
   - `PUT /order/bulk-get`: Batch lookup of orders by IDs.
   - `PUT /order/address/most-frequently-ordered`: Retrieve user's most frequently ordered address.
   - `PUT /order/hdr/most-frequently-ordered`: Retrieve user's most frequent fulfillment restaurant/HDR.
   - `PUT /order/history`: Paginated consumer order history.
   - `PUT /order/user/first-completed-order`: Retrieve user's first delivered order.
   - `PUT /order/batch-get-order-charge`: Batch lookup of order charges.
   - `PUT /order/batch-get-order-timeline`: Batch lookup of order timelines.
   - `GET /order/survey-delay-insertion-time`: Configuration for post-order survey delay.

4. **SelfServiceWebService** (`app.order.api.SelfServiceWebService`):
   - `GET /self-service/active-order`: Consumer active order lookup for self-service help.
   - `GET /self-service/completed-order`: Consumer completed order lookup for self-service concessions.
   - `PUT /self-service/order/:orderId/log-issue/preview`: Issue concession preview (refund/credit calculation).
   - `PUT /self-service/order/:orderId/log-issue/submit`: Submit order issue and trigger concession.
   - `POST /self-service/escalation-tracking`: Track consumer escalation to human customer care.
   - `PUT /self-service/tracking/:trackingId`: Update self-service tracking resolution.
   - `PUT /self-service/order/:orderId/change-dining-option/preview`: Preview dining option modification.
   - `PUT /self-service/order/:orderId/change-dining-option/submit`: Submit change from pickup to delivery or vice versa.
   - `PUT /self-service/order/:orderId/change-delivery-address/preview`: Preview delivery address modification.
   - `PUT /self-service/order/:orderId/change-delivery-address/submit`: Submit delivery address change.
   - `POST /self-service/order/:orderId/remake/submit`: Consumer-initiated meal remake request.

5. **HDROrderWebService** (`app.order.api.HDROrderWebService`):
   - `GET /hdr-order/status/:orderId`: Retrieve HDR order status.
   - `PUT /hdr-order/:orderId/cooking`: Mark HDR order as cooking started.
   - `PUT /hdr-order/:orderId/ready-for-pickup`: Mark HDR order as ready for pickup.
   - `PUT /hdr-order/:orderId/delivering`: Mark HDR order as out for delivery.
   - `PUT /hdr-order/:orderId/delivered`: Mark HDR order as delivered.
   - `PUT /hdr-order/:orderId/picked-up`: Mark HDR order as picked up by customer.
   - `PUT /hdr-order/:orderId/delay-reason`: Record kitchen delay reason.

6. **HDR3POrderWebService** (`app.order.api.HDR3POrderWebService`):
   - `POST /hdr-3p-order`: Ingestion and placement of third-party marketplace orders (e.g., Deliverect, DoorDash, UberEats).
   - `GET /hdr-3p-order/:orderId/status`: Third-party order status lookup.
   - `PUT /hdr-3p-order/:orderId/cancel`: Cancel third-party order.

7. **LocalGrubhubOrderWebService** (`app.order.api.LocalGrubhubOrderWebService`):
   - `POST /local/grubhub-order`: Create order from direct Grubhub integration.
   - `PUT /local/grubhub-order/:orderId/cancel`: Cancel Grubhub order.

8. **POPInPersonOrderWebService** (`app.order.api.POPInPersonOrderWebService`):
   - `PUT /pop-in-person-order/:orderId/in-person-payment`: Complete point-of-sale terminal payment.
   - `PUT /pop-in-person-order/:orderId/pay-by-cash`: Record cash payment for point-of-sale order.
   - `PUT /pop-in-person-order/:orderId/cancel`: Cancel point-of-sale in-person order.
   - `PUT /pop-in-person-order/:orderId/confirm-campaign`: Send SMS/email receipt and confirmation.

9. **BAOrderWebService** & **BAAppOrderWebService** (`app.order.api.BAOrderWebService`, `app.order.api.BAAppOrderWebService`):
   - `GET /ba-order/:orderId`: Blue Apron order details.
   - `PUT /ba-order/timeslot-usage`: Blue Apron weekly delivery cycle timeslot capacity.
   - `PUT /ba-order/:orderId/use-timeslot`, `PUT /ba-order/:orderId/revert-timeslot`: Manage delivery window slots.
   - `GET /ba-order/:orderId/packages`: Blue Apron fulfillment packaging and shipment status.
   - `GET /ba-order/:orderId/order-item/for-rating`: Fetch order items eligible for rating.
   - `PUT /ba-app/order/bulk-get`: Batch fetch orders for Blue Apron application.

10. **DecagonOrderWebService** (`app.order.api.DecagonOrderWebService`):
    - `POST /decagon/order/:orderId/refund`: AI customer agent initiated refund.
    - `POST /decagon/order/:orderId/remake`: AI customer agent initiated order remake.
    - `POST /decagon/order/:orderId/cancel`: AI customer agent initiated order cancellation.
    - `POST /decagon/order/:orderId/credit`: AI customer agent initiated customer credit.

11. **Back-Office (BO) Management WebServices** (18 interfaces dedicated to admin, operations, customer support):
    - `BOOrderWebService`: Operations including `PUT /bo/order/:orderId/cancel`, `PUT /bo/order/:orderId/address`, `PUT /bo/order/:orderId/time-slot`, `PUT /bo/order/:orderId/resend-email`.
    - `BOOrderModificationWebService`: `POST /bo/order/:orderId/modify/preview`, `POST /bo/order/:orderId/modify/submit`, `PUT /bo/order/:orderId/modify/confirm`.
    - `BOOrderRefundWebService`: `POST /bo/order/:orderId/refund/preview`, `POST /bo/order/:orderId/refund/submit`, `POST /bo/order/:orderId/refund/calculate`.
    - `BOOrderRemakeWebService`: `POST /bo/order/:orderId/remake/preview`, `POST /bo/order/:orderId/remake/submit`.
    - `BOOrderChangeDiningOptionWebService`: `PUT /bo/order/:orderId/change-dining-option/preview`, `PUT /bo/order/:orderId/change-dining-option/submit`.
    - `BOOrderAddPromotionWebService`: `PUT /bo/order/:orderId/add-promotion/preview`, `PUT /bo/order/:orderId/add-promotion/submit`.
    - `BOBlueApronOrderWebService`: `GET /ba/ba-order/:orderId/order-items`, `PUT /ba/ba-order/:orderId/swap-sku`.
    - `BOOrderTimelineWebService`: `GET /bo/order/:orderId/timeline-v2`.
    - `BOCustomerCareWebService`: `PUT /bo/customer-care/black-list/check`, `PUT /bo/customer-care/black-list/block`, `PUT /bo/customer-care/black-list/unblock`.
    - `BOBlueApronDeliveryIncidentSettingWebService`: CRUD operations on delivery incident thresholds.
    - `BOBlueApronRollingDelayFacilityConfigWebService`: CRUD operations on facility delay configs.
    - `BOBrazeWebService`, `BrazeWebService`: Trigger manual campaigns or query status.
    - `OrderModifyHistoryWebService`, `OrderChargeTrackingWebService`, `OrderMaxChargeWebService`, `FirstPlaceOrderWebService`, `DeliOrderWebService`, `BOSelfServiceWebService`, `BOBAOrderSimulatorWebService`.

12. **EtlMigrationWebService** (`app.order.api.EtlMigrationWebService`):
    - `PUT /etl-service/migrate/get-order-service-time-type`: Bulk export order time types for ETL pipeline.

### 1.2 Direct HTTP Routes

In addition to WebService interfaces, the following HTTP routes are bound directly:
- `GET /health-check`: Service liveness and readiness probe (via `HealthCheckController`).
- `GET /_app/hdr-3p-amounts/migrate`: Batch recalculation of 3P order amounts.
- `PUT /_app/ba-order/statistics/migrate`: Backfill Blue Apron statistics.
- `PUT /_app/birdeye/customer-check-in`: Trigger customer review check-in via Birdeye.
- `PUT /_app/ba-order/migrate-unfinished-orders`: Final status sync for legacy Blue Apron orders.
- `GET /_app/migrate/fix-3p-order-amount`: Ad-hoc data patch for 3P amounts.
- `GET /_app/migrate/fix-3p-order-discount`: Ad-hoc data patch for 3P discounts.
- `GET /_app/migrate/check-order-charge-snapshot`: Verification of Mongo charge snapshots.
- `GET /_app/migrate/publish-add-promo-issue-message`: Migration utility for promotion issues.
- `PUT /_app/migrate/continue-order-placed/:orderId`: Manual continuation of stuck order placement.
- `POST /_app/3ds-order/fix`: Repair stuck 3DS authenticated orders.
- `POST /_app/capture-failed-order/fix`: Repair orders where PSP capture timed out.
- `GET /_app/migrate/fix-ba-subscription-order`: Repair Blue Apron subscription payment records.
- `POST /_app/continue-modify-order`: Resume interrupted order modifications.
- `POST /_app/order-item-components/fix`: Fix missing item component associations.

### 1.3 Critical-Path Synchronous Endpoints

The synchronous, user-facing endpoints on the critical path of customer ordering and checkout are:
1. `POST /v2/order`: Primary checkout and placement endpoint. Synchronously executes:
   - External kitchen capacity validation (`HDRPlanningWebService`).
   - Price calculation and tax lookup (`TaxWebService`).
   - Coupon and promotion application (`PromotionWebService`).
   - Distributed locking via MySQL (`OrderOperationSyncService`).
   - A single database transaction across 18+ MySQL tables (`PlaceOrderDBOperationService`).
   - Synchronous payment authorization/capture (`PaymentWebService`).
2. `PUT /v3/price` & `PUT /v3/subtotal`: Invoked repeatedly during user cart creation and checkout screen rendering. Synchronously calls `PromotionWebService` and `TaxWebService`.
3. `PUT /tips`, `PUT /tip-options`, `PUT /tip/amount`: Invoked during checkout for tip selection.
4. `GET /v2/order/:orderId`: Invoked immediately after placement and continuously during order tracking in consumer mobile and web applications.
5. `GET /order/:orderId/status`: Polled frequently by frontends to track live status transitions.
6. `PUT /order/:orderId/cancel`: Immediate cancellation by customer within cancellation grace period.
7. `POST /hdr-3p-order` & `POST /local/grubhub-order`: Ingestion of marketplace orders requiring synchronous acceptance confirmation back to food aggregator channels.
8. `PUT /pop-in-person-order/:orderId/in-person-payment`: Point-of-sale in-person payment processing.

### 1.4 Batch and Scheduled Jobs

`order-service` does not run an internal scheduler (`schedule()` is not used). All scheduled and recurring tasks are orchestrated by external service `scheduler-service`, which triggers `order-service` by publishing to Kafka topics:
1. `OrderTopic.POST_ORDER_DELAYED_TASK`:
   - Triggered every 1 minute (`Duration.ofMinutes(1)`).
   - Consumed by `PostOrderDelayedTaskMessageHandler`. Scans MySQL `order_delay_tasks` for pending tasks (e.g. auto-cancellations, post-delivery surveys, local Grubhub completion).
2. `OrderTopic.ORDER_THREE_D_SECURE_CHECK_TASK`:
   - Triggered every 1 minute (`Duration.ofMinutes(1)`).
   - Consumed by `OrderThreeDSecureCheckTaskMessageHandler`. Scans MySQL `order_three_d_secure_check_tasks` to check status of pending Stripe 3DS challenges.
3. `OrderTopic.ORDER_PAYMENT_RETRY_CAPTURE_TASK_JOB`:
   - Triggered every 1 minute (`Duration.ofMinutes(1)`).
   - Consumed by `OrderPaymentRetryCaptureTaskMessageHandler`. Scans MySQL `auto_ship_order_payment_retry_capture_tasks` with `limit 2` to retry failed subscription auto-ship payments.
4. `OrderTopic.CHECK_YESTERDAY_NON_FINAL_STATUS_ORDERS`:
   - Triggered daily at 01:00 (`LocalTime.of(1, 0)`).
   - Consumed by `CheckYesterdayNonFinalStatusOrdersHandler`. Scans MySQL for orders older than 24 hours that remain in non-final statuses (`ASSIGNED`, `COOKING`, `DELIVERING`) and logs warnings for operational investigation.

---

## 2. Database Access Patterns

`order-service` operates a dual-database persistence architecture: **MySQL** (Azure MySQL Flexible Server) as the primary relational system of record, and **MongoDB** (MongoDB Atlas) for request audit trails, large denormalized extensions, and atomic counters.

### 2.1 MySQL Architecture and Tables

The service maps 118 entity classes to MySQL tables. From database catalog metrics, the top tables in production by storage footprint and row count are:

| Table Name | Approx. Rows | Data Size (MB) | Index Size (MB) | Total Size (MB) | Primary Usage |
| :--- | ---: | ---: | ---: | ---: | :--- |
| `order_timelines_v2` | 183,877,761 | 188,788.58 | 70,914.16 | 259,702.73 | Fine-grained lifecycle event logs for all orders |
| `order_item_options` | 70,119,720 | 63,477.72 | 24,840.89 | 88,318.61 | Customization selections per item |
| `order_items` | 23,018,485 | 11,985.34 | 5,038.56 | 17,023.91 | Line items ordered |
| `orders` | 11,043,923 | 8,985.61 | 4,059.39 | 13,045.00 | Master order records |
| `order_status_tracking` | 27,472,610 | 7,126.39 | 4,964.67 | 12,091.06 | Historical state transitions with timestamps |
| `order_charges` | 10,919,252 | 3,921.03 | 1,911.97 | 5,833.00 | Financial breakdowns (subtotal, tax, tip, fees) |
| `order_item_components`| 16,036,547 | 2,746.52 | 1,777.48 | 4,524.00 | Recipe bill-of-materials and ingredient links |
| `order_customers` | 10,879,273 | 2,682.02 | 1,607.98 | 4,290.00 | Customer identity and profile snapshot |
| `order_addresses` | 10,771,947 | 2,607.30 | 1,592.70 | 4,200.00 | Physical delivery address records |
| `order_payments` | 12,969,737 | 2,243.61 | 1,406.39 | 3,650.00 | PSP transaction and authorization references |
| `order_item_option_preset_tracking` | 10,132,328 | 1,514.48 | 1,025.52 | 2,540.00 | Preset customization tracking |
| `order_configs` | 10,317,465 | 1,505.52 | 994.48 | 2,500.00 | Order configuration flags (JSON payloads) |
| `order_charge_items` | 8,857,001 | 1,488.23 | 911.77 | 2,400.00 | Line item financial allocations |
| `order_restaurants` | 11,365,655 | 1,280.12 | 719.88 | 2,000.00 | Associated restaurant/brand mappings |
| `order_locations` | 10,866,975 | 1,180.45 | 669.55 | 1,850.00 | Geolocation coordinates |
| `order_operation_version_tracks` | 16,211,927 | 1,150.34 | 699.66 | 1,850.00 | Audit trail of concurrent operations |
| `order_credits` | 10,659,484 | 1,120.21 | 679.79 | 1,800.00 | Credit applied to order |
| `order_operation_versions` | 11,029,977 | 950.15 | 549.85 | 1,500.00 | Optimistic locking version counters |
| `order_eta_delay_tracking` | 11,041,878 | 820.67 | 479.33 | 1,300.00 | Fulfillment delay notifications sent |
| `order_issues_v2` | 1,234,705 | 390.11 | 250.89 | 641.00 | Customer care issues and concession logs |

#### MySQL Connection Pool Configuration
- Database configuration is initialized via Core-NG's `SystemModule("sys.properties")`.
- No custom `poolSize(min, max)` or `timeout(Duration)` is defined in `OrderModuleDBRegistry` or `OrderServiceApp`.
- As a result, the connection pool uses Core-NG framework defaults:
  - **Minimum pool size**: 5 connections.
  - **Maximum pool size**: 50 connections.
  - **Connection timeout**: 15 seconds.
  - **Slow query threshold**: 5 seconds (warnings emitted above 5s unless explicitly suppressed).

#### MySQL Query Types and Patterns
1. **Massive Multi-Table Insert Transactions (Order Placement):**
   - In `PlaceOrderDBOperationService.submitOrder()`, order placement executes within a single `database.beginTransaction()`, inserting rows sequentially into 18+ separate tables:
     `orders`, `order_addresses`, `order_hdr_addresses`, `order_locations`, `order_customers`, `order_end_customers`, `order_restaurants`, `order_items`, `order_item_options`, `order_item_ingredients`, `order_bundle_items`, `order_bundle_item_choices`, `order_discount_usage`, `order_payments`, `order_charge_item_extension`, `order_blue_apron_time_slots`, `order_flags`, `order_charges`, `order_charge_items`, `order_operation_versions`, `order_status_tracking`, and `order_configs`.
   - Any locking delay or query latency in this sequence holds the database transaction lock, keeping connection pool slots busy.
2. **Distributed Locking Table (`order_operation_locks`):**
   - Implemented via `lockRepository.insertIgnore(lock)` on PK `order_id`. If insert succeeds, lock is granted. Unlocking executes `lockRepository.delete(orderId)`.
3. **Sequential Point-Query Hydration:**
   - In `BOGetOrderDetailContextService.buildContext(orderId)`, loading a single order executes **20 separate sequential queries** against MySQL (`orders`, `order_charges`, `order_addresses`, `order_hdr_addresses`, `order_locations`, `order_items`, `order_flags`, `order_restaurants`, `order_item_options`, `order_bundle_items`, `order_bundle_item_choices`, `order_charge_items`, `order_configs`, `order_item_option_preset_tracking`, `order_issues_v2`, `order_resolution_tracking`, `order_item_oos_components`, `appliance_failure_impacted_order_items`, `order_item_unavailable_flags`, `ba_family_meal_pdfs`).
4. **Heavy Window Functions and Subquery Aggregations:**
   - In `OrderStatAddressCountService` and `OrderStatHDRCountService`, analytical queries compute SHA-256 address hashes and window partition counts directly against operational tables:
     ```sql
     SELECT address_hash, user_id, order_id, created_time,
            ROW_NUMBER() OVER (PARTITION BY address_hash ORDER BY created_time DESC) AS rn,
            COUNT(1) OVER (PARTITION BY address_hash) AS order_count
     FROM (
         SELECT SHA2(CONCAT_WS(?, IFNULL(t.address_line, ?), IFNULL(t.city, ?), IFNULL(t.state, ?)), 256) AS address_hash,
                ...
         FROM order_addresses t WHERE t.user_id = ?
     ) ...
     ```
5. **Slow Queries with Suppressed Warnings:**
   - `MigrateAverageOrderValueUserAttributeService`:
     ```sql
     Database.suppressSlowSQLWarning(true);
     SELECT DISTINCT(user_id) FROM orders WHERE brand_category = ? AND status = ?
     ```
     Performs a full table scan across 11+ million rows in `orders` without a composite index on `(brand_category, status)`.
   - `OrderRepository.searchOrderBeforeCooking()`:
     ```sql
     SELECT id, brand_category, order_logic_type, channel_order_id, order_channel
     FROM orders
     WHERE status IN (?, ?) AND created_time >= ?
     ```
     Scans active orders from the past 24 hours.

### 2.2 MongoDB Architecture and Collections

MongoDB is configured via Core-NG's `MongoConfig` with connection URI `sys.mongo.uri`. Ten collections are registered in `OrderModuleDBRegistry`:

1. `first_place_order_questions`: Stores questions presented to first-time ordering users. Read using `findOne` with `ReadPreference.secondaryPreferred()`.
2. `order_operation_locks`: Legacy distributed locking collection (when `app.useMysqlForLock=false`). Documents keyed by `_id = orderId`.
3. `daily_new_order_counts`: Daily order numbering counters.
   - **Update pattern**: Atomic `findAndModify` via `mongo.runCommand(...)` executing `$inc: {"order_count": 1}` and `$set: {"updated_time": now}` with `upsert: true`.
   - **Indexes**: TTL index on `updated_time` expiring after 14 days.
4. `order_hdr_3p_place_requests`: Payload archive of third-party marketplace incoming orders.
   - **Indexes**: Unique compound index `(channel_order_id, order_channel)`, unique index on `order_id`, TTL index on `created_time` expiring after 60 days.
5. `order_hdr_3p_check_inventory_requests`: Payload archive of third-party menu/inventory availability checks.
   - **Indexes**: TTL index on `created_time` expiring after 30 days.
6. `order_items_extension`: Large structured JSON attributes and metadata for order items that do not fit into relational columns.
   - Keyed by `_id = orderId`.
   - **Update pattern**: Full document replacement via `orderItemsExtensionMongoCollection.replace(extension)`.
   - **Indexes**: TTL index on `created_time` expiring after 30 days.
7. `change_dining_option_records`: Audit history of dining option changes (pickup vs delivery).
   - **Indexes**: Index on `order_id` (DESC) and `created_time` (DESC).
8. `order_charge_snapshots`: Audit snapshot of financial breakdown before and after concessions/modifications.
   - Written asynchronously in background executor thread (`executor.submit("insert-order-charge-snapshot", ...)`).
   - **Indexes**: Indexes on `order_id` (DESC), `source_id` (DESC), `created_time` (DESC).
9. `hdr_order_modify_records`: Audit record of kitchen order modifications.
   - **Indexes**: Index on `order_id` (ASC).
10. `local_grubhub_order_tracks`: Audit trail of Grubhub webhook events.
    - **Indexes**: Indexes on `order_id` (DESC), `event_type` (DESC), TTL index on `created_time` expiring after 90 days.

#### MongoDB Aggregation Pipelines and Query Patterns
- **Aggregation Pipelines**: None. The codebase contains zero occurrences of `.aggregate()`.
- **Query Patterns**: Almost exclusively point lookups by `_id` (`collection.get(id)`) or simple filtered queries by `order_id` (`collection.find(eq("order_id", orderId))`).
- **Update Patterns**: Entirely full document `insert()` or `replace()`, with the sole exception of `DailyNewOrderCountCollection` which uses `$inc` / `$set` via `findAndModify`.

### 2.3 Identified N+1 and Unbounded Query Patterns

1. **Loop-Driven DB Gets in Order Cancellation Timeline (`OrderCancellationTimelineExpandedService`):**
   - In `reinstatedCredit()` and `reinstatedGiftCard()`, comma-delimited refund IDs are split and queried one by one in a loop:
     ```java
     for (String refundId : refundIds) {
         reinstatedCredit = reinstatedCredit + orderPaymentRefundRepository.get(refundId)...;
     }
     ```
2. **Sequential Multi-Table Query Amplification (`BOGetOrderDetailContextService`):**
   - Retrieving order details for back-office and customer care operations executes 20+ sequential individual SELECT queries against MySQL instead of utilizing combined views, batched queries, or cached projections.
3. **Unbounded Historical Order Aggregation (`OrderStatCompletedTotalAmountService`):**
   - Aggregates user lifetime spend using `INNER JOIN orders o ON oc.order_id = o.id WHERE o.user_id = ? AND o.status IN (?, ?)` without date boundaries or pagination. For power users with hundreds of orders, this joins and sums over large record sets on every call.
4. **Full-Scan Table Migrations (`MigrateAverageOrderValueUserAttributeService`):**
   - Executes `SELECT DISTINCT(user_id) FROM orders WHERE brand_category = ? AND status = ?` on table `orders` (11M rows) without an index supporting the filter, forcing a full table scan.

---

## 3. Kafka Usage

`order-service` heavily utilizes Apache Kafka for event-driven coordination, asynchronous side effects, and incoming integration webhooks.

### 3.1 Topics Produced To

| Topic Name | Message Class | Purpose / Trigger |
| :--- | :--- | :--- |
| `OrderTopic.OMS_ORDER_EVENT` (`oms-order-event`) | `OMSOrderEventMessage` | Core order event bus. Emits 14 event types: `STATUS_CHANGED`, `PENDING_PAYMENT`, `PLACE_ORDER`, `ETA_CHANGED`, `AMOUNT_CHANGED`, `CANCELED`, `COMPLETE`, `MODIFIED`, `ITEM_SWAP`, `ISSUE_CREATED`, `PROMOTION_CHANGED`, `DELIVERY_ADDRESS_CHANGED`, `DINING_OPTION_CHANGED`, `CUSTOMER_CHANGED`. |
| `OrderTopic.CLEAR_ORDER_HDR_3P_ADDRESSES` (`clear-order-hdr-3p-addresses`) | `ClearOrderHDR3PAddressesMessage` | Asynchronous trigger to scrub third-party customer PII addresses after fulfillment. |
| `OrderTopic.ORDER_GROSS_VALUE_CHANGED` (`order-gross-value-changed`) | `OrderGrossValueChangedMessage` | Broadcast financial changes (e.g. refunds, item adjustments) for accounting/ETL. |
| `CustomerMessageTopics.UPDATE_CUSTOMER_ORDER_DONATION_MESSAGE` | `UpdateCustomerOrderDonationMessage` | Notifies customer service of donation additions. |
| `ZendeskTopic.CREATE_ALERT_TYPE_ZENDESK_TICKET` | `CreateAlertTypeZendeskTicketMessage` | Triggers automated Zendesk ticket creation on delivery incidents. |
| `CustomerMessageTopics.SYNC_CUSTOMER_ATTRIBUTE` | `SyncCustomerAttributeMessage` | Pushes updated customer attributes (e.g., total spend, order frequency) to CRM. |
| `CustomerMessageTopics.CUSTOMER_MEMBERSHIP_USAGE_REQUEST_UPDATE` | `CustomerMembershipUsageRequestUpdateMessage` | Updates subscription benefit usage counts. |
| `BAMembershipTopics.UPDATE_BA_MEMBERSHIP_USAGE_MESSAGE` | `UpdateBAMembershipUsageMessage` | Updates Blue Apron membership benefit usage. |
| `OrderTopic.SUBSCRIPTION_ORDER_PAYMENT_CHARGE_FAILED` | `SubscriptionOrderPaymentChargeFailedMessage` | Alerts on subscription recurring charge failures. |
| `OrderTopic.ORDER_FACILITY_CODE_CHANGED` | `OrderFacilityCodeChangedMessage` | Facility reassignment notification. |
| `ba-sous-chef-recipe-combination-pdf-requests` | `byte[].class` (Protobuf) | Custom Protobuf Kafka request for PDF combo generation. |

### 3.2 Topics Consumed From

| Topic Name | Message Class | Consumer Name / Group | Handler Class | Concurrency |
| :--- | :--- | :--- | :--- | :--- |
| `FulfillmentTopics.HDR_FULFILLMENT_ETA_UPDATED` | `HDRFulfillmentETAUpdatedMessage` | `order-service-eta` | `HDRFulfillmentETAUpdatedMessageBulkHandler` | Default (CPU-based) |
| `FulfillmentTopics.HDR_FULFILLMENT_STATUS_UPDATED` | `HDRFulfillmentStatusUpdatedMessage` | `order-service-status` | `HDRFulfillmentStatusUpdatedMessageHandler` | Default (CPU-based) |
| `PaymentTopic.PAYMENT_CHARGE_EVENT` | `PaymentChargeEvent` | `order-service-payment-charge-event` | `PaymentChargeEventHandler` | Default (CPU-based) |
| `GrubhubTopics.GRUBHUB_ORDER_EVENT_MESSAGE` | `GrubhubOrderEventMessage` | `order-service-local-grubhub-order` | `GrubhubOrderEventMessageHandler` | Default (CPU-based) |
| `CorporateOrderClientTopics.CORPORATE_ORDER_CLIENT_CHANGED` | `CorporateOrderClientChangedMessage` | `order-service-corporate-order-client-changed` | `CorporateOrderClientChangedMessageHandler` | Default (CPU-based) |
| `FulfillmentTopics.BLUE_APRON_FULFILLMENT_EVENT` | `BlueApronFulfillmentEventMessage` | `order-service-blue-apron-fulfillment` | `BlueApronFulfillmentEventMessageHandler` | Default (CPU-based) |
| `FulfillmentTopics.BLUE_APRON_FULFILLMENT_PACKAGE_EVENT` | `BlueApronFulfillmentPackageEventMessage` | `order-service-blue-apron-fulfillment` | `BlueApronFulfillmentPackageEventMessageHandler` | Default (CPU-based) |
| `FulfillmentTopics.FR_FULFILLMENT_REQUEST_RETRY_EVENT` | `FRFulfillmentRequestRetryEventMessage` | `order-service-blue-apron-fulfillment` | `FRFulfillmentRequestRetryEventMessageHandler` | Default (CPU-based) |
| `BlueApronProductTopics.BA_CYCLE_ITEM_SKU_SWAP_MESSAGE` | `BACycleItemSkuSwapMessage` | `order-service-ba-cycle-item-sku-swap` | `BACycleItemSkuSwapMessageHandler` | **1** |
| `OrderTopic.POST_ORDER_DELAYED_TASK` | `PostOrderDelayedTaskMessage` | `order-service` | `PostOrderDelayedTaskMessageHandler` | Default |
| `OrderTopic.CHECK_YESTERDAY_NON_FINAL_STATUS_ORDERS` | `CheckYesterdayNonFinalStatusOrdersMessage` | `order-service` | `CheckYesterdayNonFinalStatusOrdersHandler` | Default |
| `DeliveryTaskV2Topic.DELIVERY_TASK_UPDATED` | `DeliveryTaskV2UpdatedMessage` | `order-service` | `DeliveryTaskV2UpdatedMessageHandler` | Default |
| `CustomerMessageTopics.ALIAS_USER_CONVERTED_MESSAGE` | `AliasUserConvertedMessage` | `order-service` (or `order-service-update-user-id`) | `AliasUserConvertedMessageHandler(V2)` | Default |
| `CustomerSurveyTopics.ORDER_RATING_SUBMIT` | `OrderRatingSubmitMessage` | `order-service` | `OrderRatingSubmitMessageHandler` | Default |
| `CustomerSurveyTopics.SAVE_BLUE_APRON_ORDER_RATING_MESSAGE` | `SaveBlueApronOrderRatingMessage` | `order-service` | `SaveBlueApronOrderRatingMessageHandler` | Default |
| `OrderTopic.ORDER_THREE_D_SECURE_CHECK_TASK` | `OrderThreeDSecureCheckMessage` | `order-service` | `OrderThreeDSecureCheckTaskMessageHandler` | Default |
| `OrderTopic.ORDER_PAYMENT_RETRY_CAPTURE_TASK_JOB` | `OrderPaymentRetryCaptureTaskMessage` | `order-service` | `OrderPaymentRetryCaptureTaskMessageHandler` | Default |
| `TaxTopics.VERTEX_REPORT_EVENT` | `VertexReportEventMessage` | `order-service` | `VertexReportMessageHandler` | Default |
| `KOMServiceTopics.MARKED_COMPONENT_AS_OOS_EVENT` | `MarkComponentAsOOSMessage` | `order-service` | `MarkComponentAsOOSMessageHandler` | Default |
| `KOMServiceTopics.APPLIANCE_FAILURE_IMPACTED_ORDERS_EVENT` | `ApplianceFailureImpactedOrdersMessage` | `order-service` | `ApplianceFailureImpactedOrdersMessageHandler` | Default |
| `KOMServiceTopics.ORDER_RUNNER_SCAN_SUMMARY_EVENT` | `OrderRunnerScanSummaryMessage` | `order-service` | `OrderRunnerScanSummaryMessageHandler` | Default |
| `ba-sous-chef-recipe-combination-pdf-responses` | `byte[].class` (Protobuf) | `order-service-blue-apron-family-meal-pdf` | `RecipeCombinationPdfResponseHandler` | Concurrency: CPU-based |

### 3.3 Consumer Group IDs and Concurrency Configuration

- **Consumer Group IDs**: Core-NG assigns the default consumer group as the application name (`order-service`). For named Kafka configs created via `module.config(KafkaConfig.class, name)` or `namedKafka(module, name)`, Core-NG appends the name (`order-service-${name}`, e.g. `order-service-eta`, `order-service-status`, `order-service-blue-apron-fulfillment`).
- **Concurrency**:
  - The default concurrency in Core-NG is dynamically calculated from available processors (`Runtime.getRuntime().availableProcessors() * 16` or standard listener pool).
  - Explicit concurrency override: `kafkaConfig.concurrency(1)` is set for `"ba-cycle-item-sku-swap"` to ensure strict sequential processing of SKU swap events across weekly Blue Apron menu cycles.

### 3.4 Custom Kafka Implementation and Blocking Poll Loops

In `app.order.rpc.protokafka`, `order-service` maintains a custom Protobuf Kafka consumer listener (`MessageListenerThread`):
- **Blocking Poll**: `consumer.poll(Duration.ofSeconds(30))` blocks the consumer thread up to 30 seconds waiting for records.
- **Consumer Thread Blocking on Virtual Threads**: In `MessageListenerThread.processAll()`, the consumer thread spawns virtual threads for incoming messages and then executes:
  ```java
  semaphore.acquire(concurrency);
  try {
      consumer.commitAsync();
  } finally {
      semaphore.release(concurrency);
  }
  ```
  The main consumer loop thread blocks waiting for all spawned virtual threads to finish processing before committing Kafka offsets. If any virtual thread encounters slow database queries or remote HTTP timeouts, the consumer thread remains blocked, delaying subsequent poll cycles and risking partition rebalancing if processing time approaches `MAX_POLL_INTERVAL_MS`.
- **Error Sleep**: If an exception occurs in the poll loop, the consumer thread sleeps synchronously for 10 seconds (`Threads.sleepRoughly(Duration.ofSeconds(10))`).

---

## 4. Caching Strategy

### 4.1 Redis and In-Memory Cache Configuration

- **No Remote Redis Cluster**: In `sys.properties`, the cache configuration is:
  ```properties
  sys.cache.host=local
  ```
  `order-service` does not connect to an external Redis instance. Core-NG's `SystemModule` initializes a local in-memory cache (`LocalCache`) inside the JVM memory of each individual pod.
- **Cache Regions and Keys**:
  The application defines only **one single cache region** across its entire codebase, registered in `OMSAzureModule`:
  - **Type**: `AzureUserDelegationKey.class`
  - **Key**: `"CACHE_KEY"` (static string constant)
  - **TTL**: 1 day (`Duration.ofDays(1)`)
  - **Eviction Policy**: Local memory eviction (Core-NG `LocalCache` default size limit 10,000 entries).

### 4.2 Graceful Degradation on Cache Miss

- On a cache miss for `"CACHE_KEY"`, `FileContainerService` executes a cache loader lambda:
  ```java
  return userDelegationKeyCache.get(CACHE_KEY, key -> {
      var userDelegationKey = azureBlobClient.getUserDelegationKey();
      if (userDelegationKey == null) {
          throw new ConflictException("failed to get azure user delegation key, please retry", "AZURE_BLOB_ERROR");
      }
      return AzureUserDelegationKey.of(userDelegationKey);
  }).toUserDelegationKey();
  ```
- If the remote call to Azure Blob Storage fails or returns null, it immediately throws a `ConflictException` with error code `AZURE_BLOB_ERROR`. The service **cannot degrade gracefully** on cache miss; file uploads and temporary SAS URL generations immediately fail.

### 4.3 Total Absence of Domain Object Caching

There is **zero caching** of domain entities (orders, order lines, charges, menu items, restaurants, customer wallets, or configurations). All read requests (including `GET /v2/order/:orderId`, `GET /order/:orderId/status`, `PUT /v3/price`) query MySQL and MongoDB directly on every invocation.

---

## 5. Resilience Configuration

### 5.1 Circuit Breakers

- **No Circuit Breakers Configured**: There is no circuit-breaker library (e.g. Resilience4j, Sentinel, Hystrix) configured or present in the application dependencies. None of the outbound HTTP web service clients have circuit breakers attached.

### 5.2 Retry Policies

The service implements retries through two mechanisms: database repository retry wrappers and application-level `Retry.of(...)` helpers.

1. **Database Retries (`RetryRepository` / `RetryDatabase`):**
   - Configured in `OrderModuleDBRegistry` for most operational MySQL tables (`Order`, `OrderItem`, `OrderAddress`, `OrderCharge`, `OrderPayment`, etc.).
   - Automatically catches transient database connection drops and deadlocks to retry operations before bubbling errors up.
2. **Application Retries (`app.util.Retry.of`):**
   Explicit retry blocks are applied to specific external and internal calls:
   - `taxWebService.calculate(...)`: Retries up to 3 times with 100ms base interval if an `HTTPClientException` occurs and elapsed time is < 100ms.
   - `itemWebService.bulkGetOutOfStockName(...)` & `itemWebService.ccpBulkGetItemBOM(...)`: Retries up to 3 times with 500ms base interval.
   - `PaymentWebServiceAdaptor.capture(...)`: Retries up to 6 times with 200ms base interval on payment gateway transient responses.
   - `boSilentCaptureAuthorizedPSPPaymentService.tryCapture(...)`: Retries up to 3 times with 200ms base interval.
   - `boSilentAuthorizeOrCapturePSPPaymentService.tryAuthorizeOrCapturePayment(...)`: Retries up to 3 times with 200ms base interval.
   - `serviceDateWebService.calculateServiceDateForCCP(...)`: Retries up to 3 times with 100ms base interval.
   - `orderOperationSyncService.lock(...)`: Retries up to 3 times with 100ms base interval.
   - `forterIntegrationWebService.updateOrderStatus(...)`: Retries up to 3 times.
   - `BirdeyeService.birdeyeCheckIn(...)`: Retries up to 3 times with 500ms base interval.

### 5.3 Bulkhead Limits

Bulkhead isolation is implemented in-memory using static `Semaphore` instances defined in `SemaphoreExecutorHelper` and service classes:
- `SEMAPHORE_10` (10 permits): Throttles asynchronous tasks for Slack alerts, mParticle event uploads, and order cancellation event uploads.
- `SEMAPHORE_20` (20 permits): Throttles asynchronous communication tasks to Braze (order confirmation emails, SMS notifications, push notifications, refund notices).
- `SEMAPHORE_30` (30 permits): Throttles out-of-stock item modifications (`boModifyOrderServiceOOSAdaptor.removeOOSItem`).
- `SEMAPHORE_SEND_GRID` (10 permits): Throttles transactional emails dispatched via SendGrid.
- `FORTER_REQUEST_LIMITER` (10 permits in `ForterOrderStatusService`): Throttles outbound risk status updates to Forter.

**Critical Limitation**: These bulkheads are JVM-local static singletons. They do not coordinate across cluster replicas. Furthermore, they only protect background asynchronous tasks; **no bulkhead limits protect the synchronous critical path**.

### 5.4 Missing Circuit Breakers and Bulkheads on Critical External Calls

None of the synchronous external web services called during user checkout or status checking are protected by bulkheads or circuit breakers:
- `PaymentWebService` (Payment authorization/capture)
- `TaxWebService` (Tax calculations during checkout)
- `PromotionWebService` (Discount and promo validation)
- `HDRPlanningWebService` / `HDRWebService` (Kitchen capacity and order placement)
- `CustomerWebService` / `CustomerWalletWebService` (Customer profile and wallet checks)
- `ItemWebService` (Item BOM and component validation)

If any of these downstream services experiences high latency or downtime, `order-service` request threads will block waiting for responses, quickly exhausting HTTP worker threads and database connections.

---

## 6. External Dependencies

`order-service` relies on 58+ WebService client interfaces across 9 internal services, as well as several third-party vendor HTTP APIs.

### 6.1 Downstream WebService Clients (Core-NG HTTP RPC)

Registered in `OrderServiceApp.java`:

| Target Service | Configured Client Interfaces | Connect Timeout | Read Timeout |
| :--- | :--- | :--- | :--- |
| **Kitchen Management Service** (`app.kitchenManagement.serviceURL`) | `HDRWebService`, `HDRPlanningWebService`, `BOHDRRestaurantWebServiceV3`<br>`ServiceDateWebService` | Default (25s)<br>**2s** | Default (60s)<br>**100ms** |
| **Recipe / Item Service** (`app.recipeV2.serviceURL`) | `ItemWebService` | **2s** | **2s** |
| **Tax Service** (`app.tax.serviceURL`) | `TaxWebService` (via `CALCULATE_TAX_CLIENT`) | **2s** | **1s** |
| **Payment Service** (`app.payment.serviceURL`) | `PaymentWebService`, `SyncPaymentWebService`, `InPersonPaymentWebService` | Default (25s) | Default (60s) |
| **Marketing Service** (`app.marketing.serviceURL`) | `SegmentWebService`, `PromotionWebService`, `ChallengeWebService`, `BOPromotionWebService` | Default (25s) | Default (60s) |
| **Customer Service** (`app.customer.serviceURL`) | `CustomerWebService`, `BOCustomerWebService`, `CustomerCreditWebService`, `BOCustomerCreditWebService`, `CustomerChallengeWebService`, `BOCustomerAddressWebService`, `CustomerCreditCardWebService`, `BOCustomerMembershipWebService`, `BOCustomerCreditCardWebService`, `BACustomerSettingWebService` | Default (25s) | Default (60s) |
| **Customer Wallet Service** (`app.customerWallet.serviceURL`) | `CustomerWalletWebService` | Default (25s) | Default (60s) |
| **Wonder Setting Service** (`app.wonderSetting.serviceURL`) | `ConfigurationWebService`, `BOConfigurationWebService`, `BOFeeConfigurationWebService`, `BOHDRFeeConfigurationWebService`, `BOHDRTipConfigurationWebService` | Default (25s) | Default (60s) |
| **Kitchen Order Service** (`app.kitchenOrder.serviceURL`) | `TimeSlotWebService`, `KitchenOrderWebService`, `ModifyOrderWebService`, `OrderIssueWebService`, `BOKitchenOrderWebService` | Default (25s) | Default (60s) |
| **Restaurant Service** (`app.restaurant.serviceURL`) | `MenuItemWebService`, `BORestaurantBrandWebService`, `RestaurantMigrateWebService`, `PublishedMenuItemWebService`, `BOPublishedMenuItemWebService`, `PublishedRestaurantWebService`, `BOPublishedRestaurantWebService`, `HybridPublishedMenuItemWebService`, `HybridPublishedRestaurantWebService` | Default (25s) | Default (60s) |
| **Blue Apron Product Service** (`app.blueApronProduct.serviceURL`) | `BABrandAssetWebService`, `BAItemWebService`, `BACycleWebService`, `BOCycleWebService` | Default (25s) | Default (60s) |
| **Fulfillment Service** (`app.fulfillment.serviceURL`) | `BOHDRFulfillmentWebService`, `BOMarketplaceFulfillmentWebService`, `BOBlueApronFulfillmentWebService` | Default (25s) | Default (60s) |
| **Order Search / OMS** (`app.omsOrderSearch.serviceURL`) | `BOBAOrderWebService`, `BOOrderSearchWebService` | Default (25s) | Default (60s) |
| **Search Service** (`app.search.serviceURL`) | `SearchWebService` | Default (25s) | Default (60s) |
| **Other Internal Services** | `HDROrderETAWebService`, `OrderNumberWebService`, `DeliveryZoneWebService`, `CustomerOrderInventoryWebService`, `TaskWebService`, `ForterIntegrationWebService`, `ImageWebService`, `GrubhubOrderWebService` | Default (25s) | Default (60s) |

### 6.2 Standalone Third-Party HTTP Clients

1. **BrazeClient** (`app.order.braze.BrazeClient`):
   - Used for sending push notifications, SMS campaigns, and customer emails.
   - Configured with dedicated `HTTPClient`:
     - `connectTimeout`: 2 seconds.
     - `timeout` (read): **300 milliseconds**.
     - `maxRetries`: 2 retries on HTTP 429 and 5xx responses.
2. **BranchClient** (`app.order.branch.BranchClient`):
   - Used for generating mobile deep links in communications.
   - Injects the default Core-NG `HTTPClient` with **no custom timeouts** (relies on default 60s read timeout). Emits a warning log only if elapsed time exceeds 30 seconds (`SLOW_BRANCH_THRESHOLD_IN_NANOS`).
3. **BirdeyeService** (`app.order.birdeye.BirdeyeService`):
   - Used for customer review check-in invitations upon order delivery.
   - Uses `HTTPClient` with connect timeout 2s, read timeout 5s. Submits execution to `executor` and then calls `.get()` synchronously on the calling thread.
4. **AzureBlobClient** (`app.azure.AzureBlobClient`):
   - Communicates with Azure Blob Storage for PDF file storage and user delegation SAS tokens.

### 6.3 Missing Timeout Configurations

Of the 58+ WebService client interfaces registered in `OrderServiceApp`, **only three** have explicitly configured client timeouts:
- `TaxWebService`: connect 2s, read 1s.
- `ServiceDateWebService`: connect 2s, read 100ms.
- `ItemWebService`: connect 2s, read 2s.

All other 55+ client interfaces (notably `PaymentWebService`, `PromotionWebService`, `HDRPlanningWebService`, `CustomerWebService`, `CustomerWalletWebService`) use Core-NG's default `WebServiceClient` without explicit timeouts, allowing read requests to hang up to 60 seconds during downstream degradation.

---

## 7. Scalability Signals

### 7.1 In-Process Singletons and Replicated State Inconsistencies

1. **Static Concurrency Semaphores (`SemaphoreExecutorHelper`):**
   - The semaphores `SEMAPHORE_10`, `SEMAPHORE_20`, `SEMAPHORE_30`, and `SEMAPHORE_SEND_GRID` are declared as `private static final Semaphore` within a JVM helper class.
   - In a multi-replica deployment (e.g. 3 replicas), total cluster concurrency to external vendors scales linearly to 3x the configured limits (60 concurrent calls to Braze, 30 to SendGrid, 30 to Forter). If pods scale up to handle load spikes, downstream third-party rate limits can be tripped, leading to widespread 429 HTTP errors.
2. **Pod-Local In-Memory Cache (`sys.cache.host=local`):**
   - Because cache host is configured as `local`, each pod maintains its own in-memory cache for `AzureUserDelegationKey`. Pods cannot share cached keys or invalidate tokens across replicas.

### 7.2 Scheduled Tasks and Kafka Polling Across Multiple Replicas

1. **Unprotected Concurrent Task Selection (`PostOrderDelayedTaskMessageHandler`):**
   - Every 1 minute, `scheduler-service` publishes `PostOrderDelayedTaskMessage` to `OrderTopic.POST_ORDER_DELAYED_TASK`.
   - In `PostOrderDelayedTaskMessageHandler.tasksHandle()`, the query is:
     ```java
     var orderDelayTasks = findTasks(taskType, queryLimit);
     // findTasks: select * from order_delay_tasks where handled = false and handle_time < ? order by handle_time asc limit ?
     if (orderDelayTasks.isEmpty()) return;
     updateTask(orderDelayTasks, Boolean.TRUE);
     ```
   - There is **no row-level lock** (`SELECT ... FOR UPDATE`), no atomic CAS state update, and no database-level distributed lock on the task scan.
   - If two replicas process the message or if duplicate messages are published, both replicas query the exact same unhandled tasks concurrently and execute them twice (e.g., duplicate order cancellations or duplicate notification alerts).
2. **Unprotected Concurrent 3DS Check Selection (`OrderThreeDSecureCheckTaskMessageHandler`):**
   - `orderThreeDSecureCheckTaskRepository.select("handled = ? AND handle_time < ? ...")` selects unhandled 3DS checks without locking. Concurrent consumer executions will process and mutate identical tasks simultaneously.
3. **Low Batch Limit Bottleneck (`OrderPaymentRetryCaptureTaskMessageHandler`):**
   - The task polling query is hardcoded to:
     ```sql
     SELECT * FROM auto_ship_order_payment_retry_capture_tasks WHERE handle_time <= ? ORDER BY handle_time ASC LIMIT 2
     ```
   - Only 2 payment retries can be processed per minute across the entire cluster. If an upstream payment outage causes hundreds of subscription payments to fail, recovering the backlog will take hours due to this artificial throughput bottleneck.
   - While `handleTaskWithLock` locks the individual order, concurrent pods polling the table both retrieve the same 2 tasks; one acquires the lock, while the other fails with a `ConflictException` and logs an error.

### 7.3 Stateful Operations and Distributed Lock Failure Modes

1. **Permanent Deadlock Vulnerability on Pod Crash (`OrderOperationSyncService`):**
   - When `app.useMysqlForLock=true`, distributed locking uses table `order_operation_locks`:
     ```java
     var lock = new OrderOperationLockV2();
     lock.orderId = orderId;
     lock.operationCode = operationCode;
     lock.operationId = operationId;
     lock.createdTime = ZonedDateTime.now();
     var inserted = lockRepository.insertIgnore(lock);
     return inserted ? new OrderOperationLockResult(true, new OPLock(lock)) : new OrderOperationLockResult(false, null);
     ```
   - Unlocking simply deletes the row: `lockRepository.delete(orderId)`.
   - **Vulnerability**: There is **no lease expiration, TTL column, or automatic expiry check** in MySQL. If a pod crashes (e.g., Kubernetes OOM kill, node preemption, or network disconnection) after `lockByMysql()` inserts the row but before `unlockByMysql()` is called, the row remains in `order_operation_locks` indefinitely.
   - Because `insertIgnore` returns `false` on duplicate primary key, **all subsequent operations on that order will permanently fail** with `ConflictException("ORDER_CHANGED", "ORDER_SYNC_OPERATION_FAILED")` until an engineer manually issues a `DELETE` query against MySQL.
2. **Synchronous Thread Blocking on Async Tasks (`BirdeyeService`):**
   - In `BirdeyeService.birdeyeCheckIn()`, the code submits a task to the background `executor` and immediately blocks the calling thread with `.get()`:
     ```java
     Retry<String> retry = executor.submit("call-birdeye-check-in-api", ...).get();
     ```
   - This occurs synchronously during order delivery/completion. If the executor thread pool is busy or birdeye experiences latency, the main request thread blocks twice (once waiting for an available executor thread, and again waiting for HTTP execution).

---

## 8. Key Scalability Signals

The following are the top 5 scalability and reliability concerns directly observable in the `order-service` codebase:

1. **Synchronous Critical-Path Call Chain Without Circuit Breakers or Granular Timeouts:**
   The consumer checkout flow (`POST /v2/order`) and price calculation (`PUT /v3/price`) synchronously depend on multiple downstream services (`PaymentWebService`, `TaxWebService`, `PromotionWebService`, `HDRPlanningWebService`). Over 95% of web service clients (55+ out of 58) have no explicit connect/read timeouts configured, falling back to Core-NG's 60-second default, and zero circuit breakers are configured. Any latency spike or brownout in downstream dependencies will immediately tie up incoming request threads and cascade into complete thread exhaustion in `order-service`.

2. **Monolithic Multi-Table Transactions and High Sequential Query Amplification in MySQL:**
   Every single order placement executes an ACID transaction spanning 18+ MySQL tables within a single connection. Furthermore, order detail retrieval (`BOGetOrderDetailContextService`) issues 20+ sequential individual queries against MySQL per request. With production tables already storing tens to hundreds of millions of rows (e.g. `order_timelines_v2` with 183.8M rows / 259 GB, `order_item_options` with 70.1M rows / 88 GB), this query pattern puts severe pressure on the 50-connection pool and risks high lock contention under peak order volumes.

3. **Permanent Order Deadlock Risk in MySQL Distributed Locking (`OrderOperationSyncService`):**
   The primary distributed locking mechanism relies on `INSERT IGNORE` into the `order_operation_locks` MySQL table without a lease duration, timestamp-based TTL check, or automated background reaper. If a Kubernetes pod is terminated, restarted, or OOM-killed while holding an order lock, the lock record persists in MySQL indefinitely. The affected order becomes permanently stuck in its current state, causing all future consumer or back-office operations on that order to fail.

4. **Race Conditions in Scheduled Task Handlers Across Clustered Replicas:**
   The Kafka-triggered message handlers for periodic tasks (`POST_ORDER_DELAYED_TASK` and `ORDER_THREE_D_SECURE_CHECK_TASK`) execute plain `SELECT ... WHERE handled = FALSE` queries on task tables without pessimistic row locking (`SELECT ... FOR UPDATE`), optimistic version checks, or atomic update transitions. In a multi-replica deployment, concurrent consumer deliveries will select identical task batches simultaneously, leading to duplicated order cancellations, redundant status mutations, and duplicate customer notifications.

5. **Static JVM-Local Concurrency Bulkheads Multiplying Under Cluster Scaling:**
   Outbound throttles for third-party integrations (Braze, SendGrid, Forter, Slack) are implemented using static in-memory Java `Semaphore` instances (`SemaphoreExecutorHelper`). Because these limits are strictly local to each JVM process, horizontally scaling `order-service` replicas (e.g. from 1 to 3 to 10 pods) proportionally multiplies outbound traffic spikes against third-party API rate limits, risking vendor throttling (HTTP 429) during peak traffic events.
