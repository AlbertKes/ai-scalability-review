Framework: **core-ng** (`core.framework.module.App`, `core.framework.module.Module`, `core.framework.db.Database`, `core.framework.db.Repository`, `core.framework.kafka.MessageHandler`, `core.framework.kafka.MessagePublisher`, `core.framework.cache.Cache`, `core.framework.redis.Redis`, `lib.clb.CLBConfig`).

# Service Code Analysis — wonder-cart-service

## 1. API Surface

### REST Endpoints
Defined in `app/wondercart/CartModule.java` and `app/wondercart/CartItemModule.java`.

| Service Interface | Path Prefix | Count | Description |
|---|---|---|---|
| `AppCartWebService` | `/cart`, `/v2/cart` | 29 | Mobile cart lifecycle (create, get, delete, addresses, tips, promos, slots, transfer) |
| `WonderCartItemWebService` | `/cart/item`, `/cart/quantity` | 7 | Mobile cart item CRUD (add, batch-add, get, update, delete, batch-delete, quantity) |
| `WebCartWebService` | `/web-cart`, `/v2/web-cart` | 27 | Web consumer cart lifecycle parallel to `AppCartWebService` |
| `WebCartItemWebService` | `/web-cart/item` | 6 | Web cart item CRUD (add, get, update, delete, batch-delete, quantity) |
| `POPCartWebService` | `/pop/cart` | 11 | Kiosk POP cart lifecycle (create, delete, get, summary, refresh, utensils, phone) |
| `POPCartItemWebService` | `/pop/cart/item`, `/pop/cart/quantity` | 6 | POP cart item CRUD (add, get, update, delete, batch-delete, quantity) |
| `BOWonderCartWebService` | `/bo/cart` | 1 | Back-office app cart query |
| `BOWebCartWebService` | `/bo/web-cart` | 1 | Back-office web cart query |
| `MigrateCartBundleItemChoiceController` | `/app/migrate-cart-bundle-item-choice` | 1 | Bundle migration route |
| **Total** | | **89** | |

### Message Consumers
Registered in `src/main/java/app/wondercart/KafkaConsumerModule.java`.

| Topic | Handler | Trigger / Purpose |
|---|---|---|
| `CommonTopic.CONSUMER_CLEAN_DATA` | `CleanDataMessageHandler` | Periodic cleanup trigger for carts older than 1 month |
| `courier-supply-health-trigger-message` | `CourierSupplyHealthTriggerMessageHandler` | Event-driven; cart mutations via `CartSyncInterceptor` |
| `courier-supply-health-trigger-periodically-message` | `CourierSupplyHealthTriggerPeriodicallyMessageHandler` | Periodic sync; checks online users and triggers supply health checks |
| `WONDER_CONFIGURATION_UPDATED` | `WonderConfigurationUpdatedMessageHandler` | Event-driven; cache refresh for fee/tip updates from `wonder-setting-service` |
| `CORPORATE_ORDER_CLIENT_CHANGED` | `CorporateOrderClientChangedMessageHandler` | Event-driven; cache refresh for corporate discounts |

### Critical Path & Background Jobs
- **Critical Path**: All 86 consumer/kiosk endpoints (`AppCartWebService`, `WonderCartItemWebService`, `WebCartWebService`, `WebCartItemWebService`, `POPCartWebService`, `POPCartItemWebService`) are synchronous and user-facing. Cart mutation endpoints trigger `CartSyncInterceptor`, which asynchronously dispatches Kafka events (`courier-supply-health-trigger-message`).
- **Batch / Scheduled Jobs**: No in-process cron scheduler configured (except internal DNS refresher in `CLBConfig`). Batch cleanup is driven externally via `CONSUMER_CLEAN_DATA` (purges >1-month-old records in batches of 100). Periodic supply sync is driven via `courier-supply-health-trigger-periodically-message`.

---

## 2. Database Access Patterns

### MySQL (`wonder_cart`)
Configured in `src/main/java/app/wondercart/CartServiceApp.java`.
- **Primary DB**: `sys.jdbc.url` (read/write). Query timeout: 5s via `db().timeout(Duration.ofSeconds(5))`.
- **Replica DB**: `sys.jdbc.replicaUrl` (`@Named("replicaDB")`). Read-only queries in `CourierSupplyHealthTriggerMessageHandler` and `CourierSupplyHealthDataHelper`. Query timeout unconfigured (core-ng default 15s).
- **Connection Pools**: Sizing (`minSize`, `maxSize`) unconfigured in code/properties; uses core-ng defaults.

### Tables & Access Patterns

| Table Group | Tables Accessed | Operations | Index Coverage & Filter Keys |
|---|---|---|---|
| App Cart | `carts`, `cart_items`, `cart_menu_item_options`, `cart_bundle_item_choices`, `cart_bundle_item_choice_options` | SELECT, INSERT, UPDATE, DELETE | PK `id`; UNIQUE `uk_carts_owner_id (owner_id)` on `carts`; `(cart_id, updated_time)` and `(owner_id, restaurant_id)` on `cart_items`; `cart_item_id`, `owner_id`, `cart_id` on options/choices. |
| Web Cart | `web_carts`, `web_cart_items`, `web_cart_menu_item_options`, `web_cart_bundle_item_choices`, `web_cart_bundle_item_choice_options` | SELECT, INSERT, UPDATE, DELETE | PK `id`; UNIQUE `owner_id` on `web_carts`; secondary indexes mirror App Cart. |
| POP Cart | `pop_carts`, `pop_cart_items`, `pop_cart_menu_item_options`, `pop_cart_bundle_item_choices`, `pop_cart_bundle_item_choice_options` | SELECT, INSERT, UPDATE, DELETE | PK `id`; UNIQUE `owner_id` on `pop_carts`; `(cart_id, updated_time)` on `pop_cart_items`; `cart_item_id`, `cart_id` on options/choices. |
| Audit / History | `cart_carry_over_histories`, `reorder_cart_created_records` | SELECT, INSERT, DELETE | PK `id`. `ix_created_time` on `reorder_cart_created_records`. **No index** on `cart_carry_over_histories.updated_time`. |

### Large Scans & N+1 Patterns
- **Unindexed Scans in Data Cleanup (`CleanDataMessageHandler`)**: `SELECT id FROM carts WHERE updated_time <= ? LIMIT 100`, `SELECT id FROM web_carts WHERE updated_time <= ? LIMIT 100`, and `SELECT id FROM cart_carry_over_histories WHERE updated_time <= ? LIMIT 100` run full table scans on large tables because `carts`, `web_carts`, and `cart_carry_over_histories` lack an index on `updated_time`.
- **N+1 Entity Fetching in Kafka Consumer (`CourierSupplyHealthDataHelper`)**: Per user trigger, 4 sequential queries fetch child records (`cart_items`, `cart_menu_item_options`, `cart_bundle_item_choices`, `cart_bundle_item_choice_options`) from the read replica, grouped in-memory rather than SQL JOINs.
- **Unbounded Tables**: `pop_carts` and `pop_cart_items` are never cleaned up by `CleanDataMessageHandler`.

### MongoDB / Atlas
- **N/A** (not used).

---

## 3. Kafka Usage

- **Topics Produced To**:
  - `courier-supply-health-trigger-message` (`WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC`): Published by `CartSyncPublisherService` on cart mutations.
  - `delivery-courier-supply-health-check` (`DeliverySupplyTopic.DELIVERY_COURIER_SUPPLY_HEALTH_CHECK`): Published by `CourierSupplyHealthTriggerMessageHandler`.
  - `CommonTopic.CONSUMER_CLEAN_DATA`: Publisher registered in `KafkaConsumerModule.java`.
- **Topics Consumed From**: `CONSUMER_CLEAN_DATA`, `courier-supply-health-trigger-message`, `courier-supply-health-trigger-periodically-message`, `WONDER_CONFIGURATION_UPDATED`, `CORPORATE_ORDER_CLIENT_CHANGED`.
- **Consumer Configuration**:
  - Group IDs managed by core-ng based on service name.
  - Concurrency/poll: `kafka().maxPoll(CommonConfig.MAX_POLL_SIZE, CommonConfig.MAX_POLL_BYTES)` and `kafka().concurrency(CommonConfig.CONCURRENCY)` in `KafkaConsumerModule.java`.
  - Master toggle: `app.enableKafkaConsumer` default `false` in `conf/prod/resources/app.properties` (read in `KafkaConsumerModule.java`).
- **Polling Loop**: No direct `poll()` loops in application code; core-ng manages polling threads. Handlers perform synchronous blocking I/O (replica DB queries and REST calls) during message processing.

---

## 4. Caching Strategy

Registered in `src/main/java/app/wondercart/CartModule.java`.

| Cache Region / Key | Type | TTL | Purpose |
|---|---|---|---|
| `DeliveryFeeConfigurationCache` | core-ng `Cache<T>` (Redis) | 12 hours | Delivery fees by brand category and HDR |
| `ServiceFeeConfigurationCache` | core-ng `Cache<T>` (Redis) | 12 hours | Service fees by brand category and HDR |
| `HDRFastPassConfigurationCache` | core-ng `Cache<T>` (Redis) | 12 hours | Fast pass options per HDR |
| `HDRTipConfigurationCache` / `...HardcodedValueCache` | core-ng `Cache<T>` (Redis) | 12 hours | HDR tip configurations and presets |
| `CorporateOrderFeeAndDiscountCache` | core-ng `Cache<T>` (Redis) | 12 hours | Corporate order fee rules and discounts |
| `WonderSpotServiceFeeConfigurationCache` | core-ng `Cache<T>` (Redis) | 12 hours | Wonder Spot fee configuration |
| `CustomerCache` | core-ng `Cache<T>` (Redis) | 10 minutes | Customer profile and membership status |
| `CourierSupplyHealthRequestMessage.Pickup` | core-ng `Cache<T>` (Redis) | 1 day | Restaurant pickup location cache |
| `CustomerAddressCache`, `RestaurantCache` | core-ng `Cache<T>` (Redis) | 1 day | Customer address details and restaurant info |
| `courier:supply:health:trigger:{ownerId}` | Direct Redis key | 10 seconds | Reactive trigger de-duplication window (`app.courierSupplyHealthTriggerCache.durationOfSeconds`) |
| `courier_supply:user:{userId}` | Direct Redis key | 1 minute | Throttles periodic sync triggers per user |

- **Eviction Policy**: TTL expiration in Redis. Proactive eviction/update occurs on Kafka events (`WonderConfigurationUpdatedMessageHandler`, `CorporateOrderClientChangedMessageHandler`).
- **Degradation on Cache Miss**: **No graceful degradation**. On cache miss, `Cache.get(key, loader)` synchronously invokes upstream HTTP services (`FeeConfigurationWebService`, etc.). Upstream failures (except `404`) throw `RemoteServiceException`, causing immediate request failure without stale cache fallback.

---

## 5. Resilience Configuration

- **Bulkhead / Concurrency Limits**: In-process `Semaphore` in `SemaphoreExecutorUtil.java` initialized with `app.semaphore.permits` (default `5` in `conf/prod/resources/app.properties`, read in `KafkaConsumerModule.java`). Restricts concurrent `courier-supply-health-trigger-message` tasks per JVM pod.
- **Retry Policies**:
  - Outbound HTTP via `CLBConfig` (`lib/clb/CLBConfig.java`): `maxRetries = 2`, `retryWaitTimeInMs = 100ms`.
  - Core-ng native clients (`PriceWebService`, Blue Apron): No retries configured.
  - Kafka consumers: Exceptions logged via `Markers.errorCode(...)`; no DLQ or retry topic.
- **Timeouts**:

| Component / Setting | Property Key / Configuration | Default Value | Source File |
|---|---|---|---|
| Primary MySQL Timeout | Hardcoded `db().timeout(...)` | 5 seconds | `src/main/java/app/wondercart/CartServiceApp.java` |
| Replica MySQL Timeout | None | core-ng default (15s) | `src/main/java/app/wondercart/CartServiceApp.java` |
| CLB HTTP Connect Timeout | Hardcoded `httpClientConnectTimeout` | 2 seconds | `utility/client-side-load-balancing-library/src/main/java/lib/clb/CLBConfig.java` |
| CLB HTTP Read Timeout | Hardcoded `httpClientTimeout` | 20 seconds | `utility/client-side-load-balancing-library/src/main/java/lib/clb/CLBConfig.java` |
| GoogleBot REST Timeout | Explicit `Timeout.MILLISECOND_5000` | 5 seconds | `src/main/java/app/wondercart/APIClientModule.java` |
| Native API Clients (`PriceWebService`, BA) | None | core-ng default (60s) | `src/main/java/app/wondercart/APIClientModule.java` |
| Courier Trigger TTL | `app.courierSupplyHealthTriggerCache.durationOfSeconds` | 10 seconds | `conf/prod/resources/app.properties` (read in `KafkaConsumerModule.java`) |
| In-Process Semaphore | `app.semaphore.permits` | 5 | `conf/prod/resources/app.properties` (read in `KafkaConsumerModule.java`) |
| Kafka Consumer Enabled | `app.enableKafkaConsumer` | false | `conf/prod/resources/app.properties` (read in `KafkaConsumerModule.java`) |

*Note: Property values are codebase defaults and can be overridden by uppercase/underscore environment variables in Kubernetes deployment manifests.*

---

## 6. External Dependencies

Configured in `src/main/java/app/wondercart/APIClientModule.java`.

| Target Service | Clients / Interfaces | Client Mechanism & Configured Timeouts |
|---|---|---|
| `consumer-restaurant-service-v2` | `PublishedRestaurantWebService`, `PublishedMenuItemWebService`, `PublishedBundleItemWebService` | `CLBConfig`: 2s connect, 20s read, 2 retries |
| `consumer-restaurant-service-v2` (GoogleBot) | `PublishedMenuItemWebService`, `PublishedBundleItemWebService` | `CLBConfig` with fallback: 2s connect, 5s read (`Timeout.MILLISECOND_5000`) |
| `consumer-customer-service` | `CustomerWebService`, `CustomerAddressWebService`, `VisitorWebService`, `VisitorAddressWebService`, `CustomerCreditCardWebService`, `CustomerMembershipWebService`, `AliasUserWebService` | `CLBConfig`: 2s connect, 20s read, 2 retries |
| `consumer-customer-wallet-service` | `CustomerWalletWebService` | `CLBConfig`: 2s connect, 20s read, 2 retries |
| `kitchen-management-service` | `HDRWebService` | `CLBConfig`: 2s connect, 20s read, 2 retries |
| `consumer-wonder-setting-service` | `ConfigurationWebService`, `HDRFeeConfigurationWebService`, `HDRTipConfigurationWebService`, `CorporateOrderClientWebService`, `WonderSpotFeeConfigurationWebService`, `FeeConfigurationWebService` | `CLBConfig`: 2s connect, 20s read, 2 retries |
| `consumer-marketing-service` | `PromotionWebService`, `MembershipProductWebService`, `SegmentWebService` | `CLBConfig`: 2s connect, 20s read, 2 retries |
| `search-service` | `SearchWebService` | `CLBConfig`: 2s connect, 20s read, 2 retries |
| `wonder-app-notification-service` | `AppOnlineUserWebService` | `CLBConfig`: 2s connect, 20s read, 2 retries |
| `wonder-web-notification-service` | `WebOnlineUserWebService` | `CLBConfig`: 2s connect, 20s read, 2 retries |
| `order-service` | `PriceWebService` | Core-ng `api().client(...)`: **no explicit timeout** (default 60s), no retries |
| `blueapron-product-service` | `BACycleItemWebService`, `BAGlobalSettingWebService` | Core-ng `api().client(...)`: **no explicit timeout** (default 60s), no retries |

Total: 26 WebService interfaces across 11 downstream services.

---

## 7. Scalability Signals

- **Per-JVM Static Semaphore (`SemaphoreExecutorUtil.java`)**: `SemaphoreExecutorUtil` uses an in-memory JVM `Semaphore` (default 5 permits via `app.semaphore.permits`). Across $N$ horizontal replicas (e.g. HPA 2–4 pods), total concurrency expands to $5 \times N$, bypassing intended cluster-wide rate limits to downstream supply health services.
- **Uncoordinated Periodic Tasks**: `CourierSupplyHealthTriggerPeriodicallyMessageHandler` has no distributed lock or leader election. Multiple consumer pods receiving the Kafka trigger independently iterate through all active online users and publish duplicate triggers.
- **Uncoordinated Batch Cleanup**: `CleanDataMessageHandler` consumes `CONSUMER_CLEAN_DATA` without distributed coordination; multiple pods execute concurrent `SELECT ... LIMIT 100` and `DELETE` batches.
- **Stateful Operations**: All persistent cart state is in MySQL and Redis. No local disk or JVM session state. `pop_carts` tables lack purging logic.

---

## Key Scalability Signals

1. **Non-Distributed In-Process Semaphore**: `SemaphoreExecutorUtil` relies on a local JVM `Semaphore` (default 5 permits) instead of a distributed rate limiter. When HPA scales pods (2–4 replicas), cluster concurrency multiplies ($5 \times N$), undermining downstream protection.
2. **Full Table Scans During Cleanup**: `CleanDataMessageHandler` purges expired carts via `SELECT id FROM carts WHERE updated_time <= ? LIMIT 100` and `web_carts WHERE updated_time <= ? LIMIT 100`. Neither table has an index on `updated_time`, causing sequential scans over large tables.
3. **High Default HTTP Read Timeout (20s) and Missing Timeouts on Pricing**: Outbound calls via `CLBConfig` default to 20s timeouts with 2 retries (up to 60s total latency). Core-ng native clients (`PriceWebService` on `order-service`) have no timeout configured (default 60s). Latency spikes in downstream services will quickly exhaust Undertow request threads and DB connections on the user checkout critical path.
4. **No Cache Degradation on Upstream Failure**: Configuration caches query external setting services synchronously on cache miss. If downstream services fail, `wonder-cart-service` re-throws `RemoteServiceException` instead of falling back to default values or serving stale cache data.
5. **Lack of Distributed Coordination for Periodic Sync**: `CourierSupplyHealthTriggerPeriodicallyMessageHandler` processes periodic sync events across all replicas without distributed locking, duplicating online user queries and message publishing.
