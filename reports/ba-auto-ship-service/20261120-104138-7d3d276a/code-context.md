Framework: core-ng (`core.framework.module.Module`, `App`, `SystemModule`, `HTTPClient`, `WebService`, `MessagePublisher`, `app.properties`)

# Business Context Summary — ba-auto-ship-service

## 1. API Surface

### REST Web Services
The service exposes 88 REST endpoints across 17 `@WebService` interfaces (`ba-auto-ship-service-interface`) and 23 operational routes registered via `http().route(...)` in `BAAutoShipApp.java`.

| Interface / Group | Path Prefix | Count | Role & Criticality |
|---|---|---|---|
| `BACustomerAutoShipWebService` | `/customer/auto-ship` | 29 | **Critical path**: Synchronous customer cart management (view, preview, confirm, skip, items, address, delivery date, 3DS) |
| `BAAutoShipWebService` | `/auto-ship` | 9 | **Critical path**: Customer onboarding, plan enrollment, price estimation, cancel plan, user promotions |
| `BAAutoShipCartWebService` | `/auto-ship/cart` | 1 | Skip reason lookup |
| `BAAutoShipDeliveryAddressWebService` | `/auto-ship/delivery-address` | 7 | Customer address validation, updates, unbinding |
| `BACustomerAutoShipRecommendationAlgorithmWebService` | `/customer/auto-ship/recommendation-algorithm` | 1 | Recommendation algorithm lookup |
| `BAAutoShipWinbackWebService` | `/auto-ship/winback-promotion` | 3 | Winback promotion management |
| `BOBACustomerAutoShipWebService` | `/bo/customer/auto-ship` | 10 | Back-office customer cart, plan, address operations |
| `BOBAAutoShipAssignmentWebService` | `/bo/ba-auto-ship/assignment` | 7 | Back-office cart assignment, cycles, trigger, and reports |
| `BOBAAutoShipCartWebService` | `/bo/ba-auto-ship/cart` | 4 | Back-office cart queries and menu item aggregation |
| `BOBAAutoShipAssignmentConfigWebService` | `/bo/auto-ship/assignment-config` | 5 | Back-office assignment configuration |
| `BOAssignmentSimulationWebService` | `/bo/auto-ship/assignment-simulation` | 4 | Back-office assignment simulation execution and status |
| `BOBAAutoShipPriceSettingWebService` | `/auto-ship/product-line-price-range` | 2 | Back-office product line price ranges |
| `BOBAAutoShipRemoveCartItemWebService` | `/bo/ba-auto-ship/remove-cart-item` | 1 | Back-office dish removal trigger |
| `BOBAAutoShipFulfillmentErrorWebService` | `/bo/ba-auto-ship/fulfillment-errors` | 2 | Back-office fulfillment error investigation |
| `BOBAAutoShipCartAddressWebService` | `/bo/ba-auto-ship/cart-address` | 1 | Back-office cart address lookup |
| `BOToolAutoShipCartWebService` | `/bo/tool/auto-ship` | 1 | Back-office unqualified cart reassignment |
| `BAAutoShipMigrationWebService` | `/ba/auto-ship/migration` | 1 | Data migration unviewed quantity lookup |
| Internal Routes (`http().route`) | `/_app/ba-auto-ship` | 23 | Manual triggers, reconciliations, data migrations, cleanup |

### Message Consumers (Kafka)
Enabled when `app.enableKafka=true` (disabled on HTTP pods, enabled on `ba-auto-ship-task-service` pods). Configured in `MessageSubscribeModule.java`:
- **Default Group `ba-auto-ship-service`** (Concurrency 1, maxPoll 100): Consumes 22 topics:
  - Task & event triggers: `ba-auto-ship-task-trigger-message`, `ba-auto-ship-task-check-message`, `ba-auto-ship-task-batch-check-message`, `ba-auto-ship-reassign-cart-item-message`, `ba-auto-ship-update-plan-message`, `auto-ship-issue-order-check-message`, `auto-ship-fulfillment-trigger-message`, `auto-ship-rebalance-check-message`, `auto-ship-rebalance-trigger-message`, `auto-ship-remove-dish-trigger-message`.
  - Upstream catalog & events: `blue-apron-clean-data-message`, `blue-apron-brand-updated-message`, `blue-apron-global-setting-updated-message`, `blue-apron-hide-item-message`, `blue-apron-item-removed-from-menu-message`, `blue-apron-order-setting-updated-message`, `menu-item-classification-updated`, `ba-menu-item-inventory-updated-message`, `oms-order-event`, `customer-updated`, `wonder-user-updated`, `ba-cycle-item-sku-swap-message`.
- **Dedicated Consumer Groups**:
  - `ba-auto-ship-service-fr-update-event`: Concurrency 10 (`task.frUpdateEvent.KafkaConcurrency`), maxPoll 50 (`task.frUpdateEvent.maxPollRecords`) on `fr-fulfillment-progress-update` and `fr-package-update`.
  - `ba-auto-ship-service-customer-auto-ship-event`: Concurrency 3, maxPoll 50 on `ba-customer-auto-ship-event-message`.
  - `ba-auto-ship-service-notification`: Concurrency 3, maxPoll 100 on `auto-ship-notification-message`, `auto-ship-cart-created-notification-message`, `auto-ship-reassign-notification-trigger-message`.
  - `ba-auto-ship-service-auto-assign-cart`: Concurrency 1 (`task.createCart.KafkaConcurrency`) on `ba-auto-ship-auto-assign-cart-message`.
  - `ba-auto-ship-service-generate-order`: Concurrency 1 (`task.generateOrder.KafkaConcurrency`) on `ba-auto-ship-generate-order-message`.
  - `ba-auto-ship-service-auth-cart`: Concurrency 1 (`task.authCart.KafkaConcurrency`) on `ba-auto-ship-auth-cart-message`.
  - `ba-auto-ship-service-fulfillment-event`: Concurrency 1 (`task.fulfillmentEvent.KafkaConcurrency`) on `auto-ship-fulfillment-event-message`.
  - `ba-auto-ship-service-rebalance-event`: Concurrency 1 (`task.rebalanceEvent.KafkaConcurrency`), maxPoll 50 on `auto-ship-rebalance-event-message`.
  - `ba-auto-ship-service-remove-dish-event`: Concurrency 1 (`task.removeDishEvent.KafkaConcurrency`) on `auto-ship-remove-dish-event-message` and `ba-auto-ship-refresh-delivery-option-message`.
  - `ba-auto-ship-service-fulfillment-stuck-recovery`: Concurrency 1 on `auto-ship-fulfillment-stuck-recovery-message`.
  - `ba-auto-ship-service-fulfillment-event-sent-result-check`: Concurrency 1 on `auto-ship-fulfillment-event-sent-result-check-message`.

### Scheduled and Batch Jobs
- **In-process schedule**: `SchedulerLivenessCheckJob` in `SchedulerLivenessModule.java` runs every 1 minute (`Duration.ofMinutes(1)`).
- **Asynchronous batch jobs**: The service does not use in-process cron for business tasks; `blueapron-scheduler-service` sends Kafka messages (`ba-auto-ship-task-trigger-message`, `auto-ship-fulfillment-trigger-message`, `auto-ship-rebalance-trigger-message`, `auto-ship-remove-dish-trigger-message`) to trigger cart creation, order generation, authorization, and outbox flushes.

---

## 2. Database Access Patterns

### MySQL
- **Connection**: `sys.jdbc.url` configured in `conf/prod/resources/sys.properties`. Database access is routed via `BADBModule.java` using `RetryDatabase` and `RetryRepository<T>`. No explicit pool settings (`minSize`, `maxSize`, `timeout`) exist; core-ng defaults apply.
- **Tables (60 entities / views)**: Core tables include `auto_ship_plans`, `auto_ship_carts`, `auto_ship_cart_items`, `auto_ship_delivery_addresses`, `auto_ship_cart_fulfillment_events`, `auto_ship_cart_fulfillment_outboxes`, `auto_ship_create_cart_tasks`, `auto_ship_create_cart_task_batches`, `auto_ship_generate_order_tasks`, `auto_ship_generate_order_task_batches`, `auto_ship_auth_cart_tasks`, `auto_ship_auth_cart_task_batches`, and `auto_ship_reassign_*`.
- **Query Types**: Point queries (`get()`), cursor-based queries (`where auto_ship_cart_number > ? order by auto_ship_cart_number asc limit ?`), batch inserts (`batchInsertIgnore`), and optimistic updates (`version = version + 1 WHERE id = ? AND version = ?`).
- **Heavy Queries & Subqueries**:
  - `AutoShipCartQueryService.java` (`searchPendingAuth`): Correlated subquery `EXISTS (SELECT 1 FROM auto_ship_cart_items i WHERE i.auto_ship_cart_id = auto_ship_carts.id)` without index hinting.
  - `AutoShipCartItemQueryService.java` (`aggregateByMenuItem`): Subquery on `auto_ship_cart_items` joined with `auto_ship_carts` with a 5-dimension GROUP BY (`menu_item_id`, `cycle_id`, `fulfillment_center_code`, `status`, `viewed`).
  - Row locking: `SELECT cycle_id FROM auto_ship_reassign_unqualified_cart_item_tasks WHERE cycle_id = ? FOR UPDATE` in `BAAutoShipRebalanceEventMessageHandler.java`.
- **N+1 Query Patterns**:
  - `BAAutoShipFulfillmentEventController.java`: Loops over `autoShipCartIds` executing 4 separate queries (`cart`, `cartItems`, `familyMealChoiceValues`, `address`) in a separate transaction per cart.
  - Sequential retrieval: Message handlers (`BAAutoShipGenerateOrderMessageHandler`, `BAAutoShipAuthCartMessageHandler`) fetch cart items individually via `cartItemQueryService.searchByCartId(cartId)` rather than pre-loading batch items.
  - `AutoShipPlanQueryService.java` (`listByUserIds`): Executes `query.in("user_id", userIds)` without a limit clause.

### MongoDB / Atlas
- **URI & Replicas**: `sys.mongo.uri` in `conf/prod/resources/sys.properties` targeting `prod-blueapron-main-pl-0.e9cxtw.mongodb.net/ba-auto-ship` with secondary read preference.
- **Collections (15)**: Registered in `BAMongoModule.java`:
  `auto_ship_cart_item_trackings`, `auto_ship_plan_change_logs`, `auto_ship_plan_customization_trackings`, `autoship_classification_price_mapping`, `product_line_price_range`, `auto_ship_reassign_cart_item_trackings`, `auto_ship_cart_auth_fail_reasons`, `manually_trigger_create_cart_tracking`, `auto_ship_cart_reassign_results`, `auto_ship_fulfillment_event_sent_result_tracking`, `auto_ship_issue_order_tracking`, `auto_ship_has_active_ba_subscription_customers`, `auto_ship_cart_delivery_date_refresh_results`, `auto_ship_assign_skip_dish_cycles`, `auto_ship_experiment_groups`.
- **Views (2)**: `reassign_notify_user_statistics_view`, `reassign_affected_user_statistics_view`.
- **Unindexed Scan Risk**: TTL index `created_time_1` on `auto_ship_cart_item_trackings` and `auto_ship_reassign_cart_item_trackings` was dropped via migration scripts (`AutoShipCartItemTrackingsScript.java`, `AutoShipReassignCartItemTrackingsScript.java`). Cleanup in `AutoShipCartItemTrackingCleanupService.java` queries `Filters.lt("created_time", cutoffTime)` sorted by `created_time` in batches of 1,000, inducing unindexed collection scans.
- **Updates**: Partial updates use `Updates.set(...)`; config collections use `replaceOne` / `upsert`.

---

## 3. Kafka Usage

### Topics Produced To
Registered in `MessagePublishModule.java` and `BAAutoShipTopics.java`:
`ba-auto-ship-auto-assign-cart-message`, `ba-auto-ship-generate-order-message`, `ba-customer-auto-ship-event-message`, `ba-auto-ship-reassign-cart-item-message`, `ba-auto-ship-update-plan-message`, `ba-auto-ship-auth-cart-message`, `ba-auto-ship-task-trigger-message`, `auto-ship-fulfillment-event-message`, `auto-ship-rebalance-event-message`, `auto-ship-rebalance-trigger-message`, `auto-ship-rebalance-check-message`, `auto-ship-task-check-message`, `auto-ship-task-batch-check-message`, `auto-ship-notification-message`, `auto-ship-cart-created-notification-message`, `auto-ship-fulfillment-trigger-message`, `auto-ship-fulfillment-stuck-recovery-message`, `auto-ship-fulfillment-event-sent-result-check-message`, `auto-ship-issue-order-check-message`, `auto-ship-remove-dish-trigger-message`, `auto-ship-remove-dish-event-message`, `ba-auto-ship-refresh-delivery-option-message`.

### Polling Behavior
No custom blocking `poll()` loops exist. Core-ng's underlying message container orchestrates worker threads and poll intervals automatically.

---

## 4. Caching Strategy

Configured in `CacheModule.java`.

### Cache Regions and TTLs
- **Redis Caches**:
  - 1 day: `CycleCache`
  - 24 hours: `DesiredCycleCache`, `MenuItemClassificationListCache`, `ZipCodeMappingCache`
  - 12 hours: `OrderSettingCache`, `PricingSettingCache`, `CutoffSettingCache`, `AutoShipBrandAssetCache`
  - 7 days: `ExperimentGroupCache`
  - 6 hours: `AutoShipPlanScanNumberCache` (cursor scan progress)
  - 1 hour: `UpcomingCycleCache`, `SpecificCycleCache`, `CycleItemCache`, `CycleProductCache`, `SimulationAutoShipPlanScanNumberCache`
  - 5 minutes: `BAFacilityConfigCache`
- **In-Memory Local Cache**: `FRCustomerSegmentCache` (1 hour local, `cache().add(...).local()`).
- **Graceful Fallback**: Protected by `RedisCacheFallbackModule`. When Redis fails, requests bypass Redis and query source systems while alerting `#prod-blueapron-fire-alarm`. On fallback, `AutoShipPlanScanNumberCache` defaults cursor to 0, which restarts batch scanning.
- **Distributed Mutex**: `AutoShipRedisLockService.java` uses Redis `SET NX EX` for mutation locking; fails closed on outage.

---

## 5. Resilience Configuration

### Retry Policies
- **Database**: `RetryDatabase` / `RetryRepository` retries transient connection drops and deadlocks.
- **Outbound HTTP**:
  - `CustomerWebService`: Explicitly set to `maxRetries(2)`, `retryWaitTime(200ms)` (`APIClientModule.java`).
  - `SchedulerLivenessProbeClient`: Explicitly set to `maxRetries(0)` (`SchedulerLivenessProbeClient.java`).
  - Other clients use core-ng default (3 retries for GET/PUT, 0 for POST/DELETE).
- **Task Retries**: Property `task.maxRetryCount`, default: `3` (read from `conf/prod/resources/task.properties`).

### Bulkhead & Executor Limits
- `notification-task-pool`: 1 thread (hardcoded in `BAAutoShipApp.java`).
- `cart-item-tracking-cleanup-task`: pool size `task.cartItemTrackingCleanup.threadPoolSize`, default: `1` (`task.properties`).
- Default async executor: Managed by `app.common.executor.ExecutorModule` (`availableProcessors * 2`).

### Timeouts
All values are defaults from configuration files or code and can be overridden by environment variables in K8s:
- `task.pendingTimeoutInMinutes`: default `10` (`task.properties`).
- `task.processingTimeoutInMinutes`: default `5` (`task.properties`).
- `task.fulfillmentEvent.processingStuckTimeoutInMinutes`: default `90` (`task.properties`).
- `app.fulfillmentEventSentResult.timeOutInMinutes`: default `1200` (`app.properties`).
- **Configured Client Timeouts** (`APIClientModule.java`):
  - `FRFulfillmentWebService`: 150 ms, 500 ms, 1500 ms with fallback.
  - `ForterIntegrationWebService`: 1200 ms with fallback.
  - `CustomerWebService`: 1500 ms with fallback (Connect 1s, Timeout 20s).
  - `BARecommendationWebServiceV2`: 1000 ms, 1500 ms with fallback.
  - `SchedulerLivenessProbeClient`: Connect 1s, Timeout 5s (`SchedulerLivenessProbeClient.java`).
  - **13 Unconfigured Services**: Default core-ng web client timeout (Connect 2s, Call 15s) with **no fallback**.

---

## 6. External Dependencies

All outbound communication uses core-ng `WebServiceClient` or `HTTPClient` beans configured in `APIClientModule.java`, `SchedulerLivenessModule.java`, and `BrazeModule.java`.

| Dependency | Property Key (`app.properties`) | Web Service Interfaces | Configured Timeouts |
|---|---|---|---|
| `ba-inventory-service` | `app.baInventory.serviceURL` | `MenuItemInventoryWebService`, `MenuItemSoftInventoryWebService`, `BOMenuItemInventoryWebService`, `SimulationMenuItemInventoryWebService`, `SimulationMenuItemSoftInventoryWebService`, `BAFacilityConfigWebService` | Default (Connect 2s, Call 15s) |
| `ba-membership-service` | `app.baMembership.serviceURL` | `BACustomerMembershipWebService`, `BAMembershipProductWebService` | Default (Connect 2s, Call 15s) |
| `blueapron-product-service` | `app.product.serviceURL` | `BABrandAssetWebService`, `BAGlobalSettingWebService`, `BACycleWebService`, `BACycleItemWebService`, `BACycleProductWebService`, `BAItemWebService`, `BAProductWebService`, `BOBAMenuItemWebService`, `BOCycleWebService`, `BACycleItemSkuSwapWebService`, `BlueApronOrderSettingWebService`, `BAMenuItemWebService`, `FBMWhitelistWebService` | Default (Connect 2s, Call 15s) |
| `ba-setting-service` | `app.baSetting.serviceURL` | `BAZipCodeMappingWebService` | Default (Connect 2s, Call 15s) |
| `ba-recommendation-service` | `app.baRecommendation.serviceURL` | `BARecommendationWebServiceV2` | 1000 ms & 1500 ms with fallback |
| `ba-customer-service` | `app.customer.serviceURL` | `CustomerWebService`, `BACustomerSettingWebService`, `CustomerCreditCardWebService`, `CustomerAutoShipAddressWebService`, `CustomerCreditWebService`, `BOCustomerWebService` | `CustomerWebService`: 1500 ms with fallback; others default |
| `customer-wallet-service` | `app.customerWallet.serviceURL` | `CustomerWalletWebService` | Default (Connect 2s, Call 15s) |
| `fr-integration-service` | `app.frIntegration.serviceURL` | `FRFulfillmentWebService` | 150 ms, 500 ms, 1500 ms with fallback |
| `order-service` | `app.order.serviceURL` | `PriceWebService`, `BAOrderWebService`, `OrderWebService` | Default (Connect 2s, Call 15s) |
| `order-number-service` | `app.orderNumber.serviceURL` | `OrderNumberWebService` | Default (Connect 2s, Call 15s) |
| `order-search-service` | `app.orderSearch.serviceURL` | `BAAppOrderSearchWebService` | Default (Connect 2s, Call 15s) |
| `ba-marketing-service` | `app.marketing.serviceURL` | `PromotionWebService`, `BAWinBackProgramWebService` | Default (Connect 2s, Call 15s) |
| `payment-service` | `app.payment.serviceURL` | `PaymentServiceProviderSelectorWebService` | Default (Connect 2s, Call 15s) |
| `shipping-service` | `app.shipping.serviceURL` | `BaggedDeliveryCapacityWebService` | Default (Connect 2s, Call 15s) |
| `forter-integration-service` | `app.forterIntegration.serviceURL` | `ForterIntegrationWebService` | 1200 ms with fallback |
| `wonder-setting-service` | `app.wonderSetting.serviceURL` | `BOMenuItemClassificationWebService` | Default (Connect 2s, Call 15s) |
| `blueapron-scheduler-service` | `app.scheduler.serviceURL` | `SchedulerLivenessProbeClient` (HTTP GET `/liveness-probe`) | Connect 1s, Call 5s |
| Braze | `braze.apiKey` (`brazemparticle.properties`) | `BACampaignService` | Default HTTP client |
| Branch | `branch.endPoint` (`branch.properties`) | Deep link URL generation | Default HTTP client |
| Amplitude | `amplitude.apiKey` (`amplitude.properties`) | `AmplitudeModule` | Default HTTP client |
| Slack | `app.slack.token` (`slack.properties`) | `SlackServiceV2` (`#prod-ba-alerts`, `#prod-blueapron-fire-alarm`) | Default HTTP client |

---

## 7. Scalability Signals

### In-Process State & Singletons
- `SchedulerLivenessMonitorService.java`: Tracks scheduler liveness in-memory (`lastKnownAlive`). In `ba-auto-ship-task-service` (3 replicas), each replica runs the 1-minute probe independently, causing redundant probes and potentially duplicate Slack alerts.
- `FRCustomerSegmentCache`: Local in-memory cache (`1-hour` TTL). Changes to customer segments will not synchronize across replicas until local TTL expiration.

### Concurrency Guards Across Replicas
- **Background Task Triggers**: In `AutoShipCreateCartTaskTriggerService.java` and `AutoShipGenerateOrderTaskTriggerService.java`, batch trigger execution checks `existingProcessingBatch()` via a non-locking `SELECT count(*) WHERE status = 'PROCESSING'`. Without a distributed Redis lock or database lock, concurrent trigger events across task service pods race and create duplicate batches.
- **Cart Scan Progress**: `AutoShipPlanScanNumberCache` tracks batch progress in Redis. If Redis degrades to fallback, the cursor defaults to 0, restarting full table scans.
- **Row-Level DB Locks**: `BAAutoShipRebalanceEventMessageHandler.java` uses `SELECT ... FOR UPDATE` on `auto_ship_reassign_unqualified_cart_item_tasks` to synchronize completion counters.

### Stateful Operations
- Pods are stateless; no local filesystem writes are performed.

---

## Key Scalability Signals

1. **Race Condition in Multi-Replica Task Triggers**: `AutoShipCreateCartTaskTriggerService` and `AutoShipGenerateOrderTaskTriggerService` guard batch execution via an uncommitted `SELECT count(*)` check (`existingProcessingBatch()`) without a Redis lock or DB lock. Multiple task pods (`ba-auto-ship-task-service` has 3 replicas) can concurrently start duplicate batches.
2. **Missing Timeouts on 13 Downstream Microservices**: 13 internal dependencies (including `order-service`, `ba-inventory-service`, `ba-membership-service`, and `blueapron-product-service`) use unconfigured core-ng HTTP clients with a 15-second default timeout and no fallbacks. Downstream latency spikes can tie up worker threads and exhaust connection pools.
3. **Unindexed MongoDB Batch Cleanup**: Dropped TTL index on `auto_ship_cart_item_trackings` forces `AutoShipCartItemTrackingCleanupService` to run batch deletes using `Filters.lt("created_time", ...)` with an in-memory sort, causing full collection scans.
4. **Correlated Subqueries & Heavy Aggregations in MySQL**: `AutoShipCartQueryService.searchPendingAuth` uses a correlated `EXISTS (SELECT 1 FROM auto_ship_cart_items WHERE ...)` subquery, and `AutoShipCartItemQueryService.aggregateByMenuItem` executes multi-table GROUP BY queries across cart tables.
5. **N+1 Entity Loading in Fulfillment and Event Pipelines**: Handlers like `BAAutoShipFulfillmentEventController` and task consumers sequentially fetch cart details, cart items, addresses, and meal choices per cart inside per-item database transactions rather than batch loading.
