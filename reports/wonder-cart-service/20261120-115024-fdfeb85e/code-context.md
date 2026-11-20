Framework: core-ng (core.framework.*)

## 1. API Surface

### REST Endpoints
Web service endpoints registered via `api().service(...)` and HTTP routes via `http().route(...)` in `backend/wonder-cart-service/src/main/java/app/wondercart/CartModule.java` and `backend/wonder-cart-service/src/main/java/app/wondercart/CartItemModule.java`:

| Web Service Interface | Path Prefix | Method Count | Target Domain / Client |
|---|---|---|---|
| `AppCartWebService` | `/cart`, `/v2/cart` | 30 | Mobile App Consumer cart operations |
| `WonderCartItemWebService` | `/cart/item`, `/cart/quantity` | 7 | Mobile App Cart item operations |
| `WebCartWebService` | `/web-cart`, `/v2/web-cart` | 28 | Web Consumer cart operations |
| `WebCartItemWebService` | `/web-cart/item` | 6 | Web Cart item operations |
| `POPCartWebService` | `/pop/cart` | 12 | Point-of-Purchase (kiosk) cart operations |
| `POPCartItemWebService` | `/pop/cart/item` | 6 | Point-of-Purchase cart item operations |
| `BOWonderCartWebService` | `/bo/cart` | 1 | Back Office App Cart retrieval |
| `BOWebCartWebService` | `/bo/web-cart` | 1 | Back Office Web Cart retrieval |
| `MigrateCartBundleItemChoiceController` | `/app/migrate-cart-bundle-item-choice` | 1 | HTTP route migration endpoint |

- **Critical Path:** Synchronous user-facing endpoints across `AppCartWebService`, `WonderCartItemWebService`, `WebCartWebService`, `WebCartItemWebService`, `POPCartWebService`, and `POPCartItemWebService`. These block customer requests while performing MySQL queries, cache lookups, and remote RPCs to customer, restaurant, and pricing services.
- **Asynchronous Triggers on Critical Path:** Endpoints intercepted by `CartSyncInterceptor` (`backend/wonder-cart-service/src/main/java/app/wondercart/web/interceptor/CartSyncInterceptor.java`) trigger asynchronous task `sync-cart-to-courier-supply-task` via core-ng `Executor`, publishing `CourierSupplyHealthTriggerMessage` to Kafka.

### Scheduled / Batch Jobs
- **Internal Scheduled Jobs:** None configured (no `schedule()` bindings in application modules).
- **Batch Data Cleanup:** Triggered externally via Kafka topic `CommonTopic.CONSUMER_CLEAN_DATA` (`backend/wonder-cart-service/src/main/java/app/kafka/CleanDataMessageHandler.java`). Deletes carts older than 1 month across app, web, carry-over histories, and reorder tables in batches of 100 (`BATCH_SIZE = 100`).
- **Periodic Courier Sync:** Triggered externally via Kafka topic `courier-supply-health-trigger-periodically-message` (`backend/wonder-cart-service/src/main/java/app/kafka/CourierSupplyHealthTriggerPeriodicallyMessageHandler.java`). Queries all online users from notification services and publishes individual sync events.

---

## 2. Database Access Patterns

### MySQL (`wonder_cart`)
Configured in `backend/wonder-cart-service/src/main/java/app/wondercart/CartServiceApp.java`. Read replica registered as `db("replicaDB")`.

| Table Name | Entity Class | Access Types | Key Indexes / Query Filter |
|---|---|---|---|
| `carts` | `Cart` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, unique `owner_id`. No index on `updated_time` |
| `cart_items` | `CartItem` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `(cart_id, updated_time)`, `(owner_id, restaurant_id)` |
| `cart_menu_item_options` | `CartMenuItemOption` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `cart_id`, `owner_id`, `cart_item_id` |
| `cart_bundle_item_choices` | `CartBundleItemChoice` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `cart_id`, `owner_id`, `cart_item_id` |
| `cart_bundle_item_choice_options` | `CartBundleItemChoiceOption` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `cart_id`, `owner_id`, `cart_item_id` |
| `cart_carry_over_histories` | `CartCarryOverHistory` | SELECT, INSERT, DELETE | Primary key `id`. No index on `updated_time` |
| `reorder_cart_created_records` | `ReorderCartCreatedRecord` | SELECT, INSERT, DELETE | Primary key `id`, index `ix_created_time` (`created_time`) |
| `web_carts` | `WebCart` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, unique `owner_id`. No index on `updated_time` |
| `web_cart_items` | `WebCartItem` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `(cart_id, updated_time)`, `(owner_id, restaurant_id)` |
| `web_cart_menu_item_options` | `WebCartMenuItemOption` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `cart_id`, `owner_id`, `cart_item_id` |
| `web_cart_bundle_item_choices` | `WebCartBundleItemChoice` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `cart_id`, `owner_id`, `cart_item_id` |
| `web_cart_bundle_item_choice_options` | `WebCartBundleItemChoiceOption` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `cart_id`, `owner_id`, `cart_item_id` |
| `pop_carts` | `POPCart` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, unique `owner_id` |
| `pop_cart_items` | `POPCartItem` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `(cart_id, updated_time)`, `(owner_id, restaurant_id)` |
| `pop_cart_menu_item_options` | `POPCartMenuItemOption` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `cart_id`, `owner_id`, `cart_item_id` |
| `pop_cart_bundle_item_choices` | `POPCartBundleItemChoice` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `cart_id`, `owner_id`, `cart_item_id` |
| `pop_cart_bundle_item_choice_options` | `POPCartBundleItemChoiceOption` | SELECT, INSERT, UPDATE, DELETE | Primary key `id`, indexes `cart_id`, `owner_id`, `cart_item_id` |

- **Large Scans & Unindexed Queries:**
  - `CleanDataMessageHandler`: Executes `select id from web_carts where updated_time <= ? limit 100`, `select id from carts where updated_time <= ? limit 100`, and `select id from cart_carry_over_histories where updated_time <= ? limit 100` repeatedly in `while (true)` loops. None of these three tables have an index on `updated_time`, causing full-table scans on every batch iteration.
  - `CartService.search()` and `WebCartService.search()`: Execute `cartRepository.select().count()` (full table scan `COUNT(*)`) and multi-table queries joining carts with cart items grouped by owner and ordered by `created_time`. The code explicitly invokes `Database.suppressSlowSQLWarning(true)` to suppress slow query logging.
- **N+1 and Unbounded Queries:**
  - `CartContextService.cartItemOptionMap()`: Executes `cartMenuItemOptionRepository.select()` filtered only by `owner_id = ?` without bounding by cart ID or row limit, retrieving all historic options for that owner and filtering in application memory.
  - Cart reads (`CartRepositoryHelper.getByOwnerId()`, `WebCartRepositoryHelper.getByOwnerId()`, `CartContextService.getBaseForCustomer()`): Cart, items, bundle choices, choice options, and menu options are retrieved via sequential individual queries instead of a consolidated read.
- **Connection Pool Configuration:**
  - Database timeout configured to 5 seconds (`db().timeout(Duration.ofSeconds(5))` in `CartServiceApp.java`).
  - Read replica configured via `sys.jdbc.replicaUrl`.
  - Pool sizing uses core-ng defaults (no explicit custom pool size parameters in configuration).

### MongoDB / Atlas
N/A (not used by `wonder-cart-service`).

---

## 3. Kafka Usage

Configured in `backend/wonder-cart-service/src/main/java/app/wondercart/KafkaConsumerModule.java`.

### Topics

| Topic Name | Direction | Handler / Publisher | Concurrency / Limits |
|---|---|---|---|
| `courier-supply-health-trigger-message` | Consumed | `CourierSupplyHealthTriggerMessageHandler` | Throttled via in-memory `Semaphore` (`app.semaphore.permits`, default 5) |
| `courier-supply-health-trigger-periodically-message` | Consumed | `CourierSupplyHealthTriggerPeriodicallyMessageHandler` | Async worker via `Executor` |
| `CONSUMER_CLEAN_DATA` | Consumed | `CleanDataMessageHandler` | 4 parallel executor tasks per message |
| `wonder-configuration-updated` | Consumed | `WonderConfigurationUpdatedMessageHandler` | In-memory cache refresh |
| `corporate-order-client-changed` | Consumed | `CorporateOrderClientChangedMessageHandler` | In-memory cache refresh |
| `courier-supply-health-trigger-message` | Produced | `CartSyncPublisherService` | Published on cart modification events |
| `delivery-courier-supply-health-check` | Produced | `CourierSupplyHealthTriggerMessageHandler` | Published after assembling cart health payload |
| `CONSUMER_CLEAN_DATA` | Produced | Registered dummy publisher | N/A |

- **Consumer Configuration:**
  - Batch poll size and byte limits: `kafka().maxPoll(CommonConfig.MAX_POLL_SIZE, CommonConfig.MAX_POLL_BYTES)`.
  - Concurrency: `kafka().concurrency(CommonConfig.CONCURRENCY)`.
  - Consumer gate: Entire consumer pipeline skipped if `app.enableKafkaConsumer` is `false`.
  - Consumer groups: Managed automatically by core-ng based on application name and handler registration.
- **Blocking Loops:** No blocking `poll()` loops in application logic (handled by core-ng runtime).

---

## 4. Caching Strategy

Configured in `backend/wonder-cart-service/src/main/java/app/wondercart/CartModule.java`.

### Cache Regions (`core.framework.cache.Cache<T>`)

| Cache Region | Type | Configured TTL | Invalidation / Refresh Mechanism |
|---|---|---|---|
| `DeliveryFeeConfigurationCache` | Fee tiers | 12 hours | Refreshed on `wonder-configuration-updated` Kafka event |
| `ServiceFeeConfigurationCache` | Fee tiers | 12 hours | Refreshed on `wonder-configuration-updated` Kafka event |
| `HDRFastPassConfigurationCache` | Fast-pass config | 12 hours | Refreshed on `wonder-configuration-updated` Kafka event |
| `HDRTipConfigurationCache` | Tip suggestions | 12 hours | Refreshed on `wonder-configuration-updated` Kafka event |
| `HDRTipConfigurationHardcodedValueCache` | Default tips | 12 hours | Refreshed on `wonder-configuration-updated` Kafka event |
| `CorporateOrderFeeAndDiscountCache` | Corporate config | 12 hours | Refreshed on `wonder-configuration-updated` or `corporate-order-client-changed` |
| `WonderSpotServiceFeeConfigurationCache` | Wonder Spot fee | 12 hours | Refreshed on `wonder-configuration-updated` Kafka event |
| `CustomerCache` | Customer summary | 10 minutes | Time-based TTL; lazy fetch on miss |
| `CourierSupplyHealthRequestMessage.Pickup` | HDR pickup info | 1 day | Time-based TTL; lazy fetch on miss |
| `CustomerAddressCache` | Address info | 1 day | Time-based TTL; lazy fetch on miss |
| `RestaurantCache` | Restaurant info | 1 day | Time-based TTL; lazy fetch on miss |

### Redis Direct Keys
- `courier:supply:health:trigger:%s`: TTL of `app.courierSupplyHealthTriggerCache.durationOfSeconds` (default 10 seconds). Used in `CourierSupplyHealthTriggerMessageHandler` to deduplicate burst events per owner.
- `courier_supply:user:%s`: TTL of 1 minute (`Duration.ofMinutes(1)`). Used in `CourierSupplyHealthTriggerPeriodicallyMessageHandler` to avoid re-triggering sync for active users.

- **Degradation on Cache Miss:** Synchronous downstream RPC call via supplier lambda (`cache.get(key, k -> ...)`). If upstream service fails, an exception is thrown; there is no graceful fallback or cached stale state.

---

## 5. Resilience Configuration

### Timeouts and Bulkhead Settings

| Setting | Default Value | Property Key | Source File | Description |
|---|---|---|---|---|
| Database Timeout | 5 seconds | N/A (hardcoded in code) | `backend/wonder-cart-service/src/main/java/app/wondercart/CartServiceApp.java` | Core-ng database query timeout |
| GoogleBot RPC Timeout | 5000 ms | N/A (`Timeout.MILLISECOND_5000`) | `backend/wonder-cart-service/src/main/java/app/wondercart/APIClientModule.java` | Timeout for fallback restaurant bundle and menu item clients |
| Other Outbound HTTP Clients | Framework default | N/A | `backend/wonder-cart-service/src/main/java/app/wondercart/APIClientModule.java` | No explicit connect/read timeouts configured on remaining 26 client bindings |
| Semaphore Permits | `5` (default) | `app.semaphore.permits` | `backend/wonder-cart-service/conf/prod/resources/app.properties` | In-memory `Semaphore` throttling concurrent courier health trigger execution |
| Deduplication Window | `10` seconds (default) | `app.courierSupplyHealthTriggerCache.durationOfSeconds` | `backend/wonder-cart-service/conf/prod/resources/app.properties` | Redis deduplication key expiration for courier health triggers |
| Kafka Consumer Enabled | `false` (default) | `app.enableKafkaConsumer` | `backend/wonder-cart-service/conf/prod/resources/app.properties` | Gate for starting Kafka consumer bindings |

*Note on properties:* Property values above are defaults from configuration files in the source repository. Deployed values may be overridden via environment variables set in Kubernetes manifests (e.g., `APP_SEMAPHORE_PERMITS`, `APP_ENABLEKAFKACONSUMER`).

- **Retry Policies:** No automated retry policies or exponential backoff configured for database or outbound HTTP client calls.
- **Bulkhead / Concurrency Limits:** `SemaphoreExecutorUtil.init(...)` instantiates an in-memory `Semaphore(semaphorePermits)`. This bulkhead restricts local thread concurrency per JVM instance, but provides no distributed coordination across multiple replicas.

---

## 6. External Dependencies

Outbound HTTP WebService client bindings declared in `backend/wonder-cart-service/src/main/java/app/wondercart/APIClientModule.java`:

| Service Interface | Target Service | Default Property URL (Production) | Timeout Configured |
|---|---|---|---|
| `PublishedRestaurantWebService` | Restaurant Service V2 | `app.consumer.restaurant.serviceURL` (`https://consumer-restaurant-service-v2-headless:8443` in `conf/prod/resources/consumer-app.properties`) | Default (missing explicit timeout) |
| `PublishedMenuItemWebService` | Restaurant Service V2 | `app.consumer.restaurant.serviceURL` | Default (missing explicit timeout) |
| `PublishedBundleItemWebService` | Restaurant Service V2 | `app.consumer.restaurant.serviceURL` | Default (missing explicit timeout) |
| `PublishedMenuItemWebService` (GoogleBot fallback) | Restaurant Service BO | `app.restaurant.serviceURL` (`https://restaurant-service-v2-headless:8443` in `conf/prod/resources/app.properties`) | 5000 ms (`Timeout.MILLISECOND_5000`) |
| `PublishedBundleItemWebService` (GoogleBot fallback) | Restaurant Service BO | `app.restaurant.serviceURL` | 5000 ms (`Timeout.MILLISECOND_5000`) |
| `CustomerWebService` | Customer Service | `app.consumer.customer.serviceURL` (`https://consumer-customer-service-headless:8443` in `conf/prod/resources/consumer-app.properties`) | Default (missing explicit timeout) |
| `CustomerAddressWebService` | Customer Service | `app.consumer.customer.serviceURL` | Default (missing explicit timeout) |
| `CustomerCreditCardWebService` | Customer Service | `app.consumer.customer.serviceURL` | Default (missing explicit timeout) |
| `CustomerMembershipWebService` | Customer Service | `app.consumer.customer.serviceURL` | Default (missing explicit timeout) |
| `VisitorWebService` | Customer Service | `app.consumer.customer.serviceURL` | Default (missing explicit timeout) |
| `VisitorAddressWebService` | Customer Service | `app.consumer.customer.serviceURL` | Default (missing explicit timeout) |
| `AliasUserWebService` | Customer Service | `app.consumer.customer.serviceURL` | Default (missing explicit timeout) |
| `PriceWebService` | Order Service | `app.order.serviceURL` (`https://order-service.prod-consumer.wonder.internal` in `conf/prod/resources/app.properties`) | Default (missing explicit timeout) |
| `HDRWebService` | Kitchen Management (KMS) | `app.kitchenManagement.serviceURL` (`https://kitchen-management-service.prod-hdr.wonder.internal` in `conf/prod/resources/app.properties`) | Default (missing explicit timeout) |
| `ConfigurationWebService` | Wonder Setting Service | `app.consumer.wonderSetting.serviceURL` (`https://consumer-wonder-setting-service-headless:8443` in `conf/prod/resources/consumer-app.properties`) | Default (missing explicit timeout) |
| `HDRFeeConfigurationWebService` | Wonder Setting Service | `app.consumer.wonderSetting.serviceURL` | Default (missing explicit timeout) |
| `HDRTipConfigurationWebService` | Wonder Setting Service | `app.consumer.wonderSetting.serviceURL` | Default (missing explicit timeout) |
| `CorporateOrderClientWebService` | Wonder Setting Service | `app.consumer.wonderSetting.serviceURL` | Default (missing explicit timeout) |
| `WonderSpotFeeConfigurationWebService` | Wonder Setting Service | `app.consumer.wonderSetting.serviceURL` | Default (missing explicit timeout) |
| `FeeConfigurationWebService` | Wonder Setting Service | `app.consumer.wonderSetting.serviceURL` | Default (missing explicit timeout) |
| `CustomerWalletWebService` | Customer Wallet Service | `app.consumer.customerWallet.serviceURL` (`https://consumer-customer-wallet-service-headless:8443` in `conf/prod/resources/consumer-app.properties`) | Default (missing explicit timeout) |
| `PromotionWebService` | Marketing Service | `app.consumer.marketing.serviceURL` (`https://consumer-marketing-service-headless:8443` in `conf/prod/resources/consumer-app.properties`) | Default (missing explicit timeout) |
| `MembershipProductWebService` | Marketing Service | `app.consumer.marketing.serviceURL` | Default (missing explicit timeout) |
| `SegmentWebService` | Marketing Service | `app.consumer.marketing.serviceURL` | Default (missing explicit timeout) |
| `SearchWebService` | Search Service | `app.consumer.search.serviceURL` (`https://search-service-headless:8443` in `conf/prod/resources/consumer-app.properties`) | Default (missing explicit timeout) |
| `AppOnlineUserWebService` | App Notification Service | `app.consumer.appOnlineUser.serviceURL` (`http://wonder-app-notification-service-headless:8080` in `conf/prod/resources/consumer-app.properties`) | Default (missing explicit timeout) |
| `WebOnlineUserWebService` | Web Notification Service | `app.consumer.webOnlineUser.serviceURL` (`http://wonder-web-notification-service-headless:8080` in `conf/prod/resources/consumer-app.properties`) | Default (missing explicit timeout) |
| `BACycleItemWebService` | Blue Apron Product Service | `app.blueApronProduct.serviceURL` (`https://blueapron-product-service.prod-ba-consumer` in `conf/prod/resources/app.properties`) | Default (missing explicit timeout) |
| `BAGlobalSettingWebService` | Blue Apron Product Service | `app.blueApronProduct.serviceURL` | Default (missing explicit timeout) |

- **Missing Timeout Settings:** 27 out of 29 client declarations do not specify connect or read timeouts, inheriting standard framework defaults.

---

## 7. Scalability Signals

- **Singletons and In-Process State:**
  - `SemaphoreExecutorUtil` (`backend/wonder-cart-service/src/main/java/app/common/util/SemaphoreExecutorUtil.java`) contains static `Semaphore` instances (`SEMAPHORE_5` and `semaphoreFromConfig`). These enforce local per-JVM permit limits rather than a distributed cluster-wide limit. Under horizontal scaling across $N$ replicas, downstream systems receive up to $N \times 5$ concurrent requests.
  - Configuration cache instances are maintained in local process memory and synchronized across replicas via Kafka broadcast events (`wonder-configuration-updated`).
- **Scheduled Tasks & Concurrency Guards:**
  - `CleanDataMessageHandler` and `CourierSupplyHealthTriggerPeriodicallyMessageHandler` are invoked via Kafka without distributed locking or database advisory locks. If duplicate messages arrive or if multiple replicas consume from partitioned topics without strict partition assignment, concurrent cleanup batches or duplicate user sync scans will execute simultaneously across replicas.
- **Stateful Operations:**
  - No local file system writes.
  - Cache deduplication keys are stored in shared Redis, making deduplication safe across replicas.

---

## Key Scalability Signals

1. **Unindexed Cleanup Query Causing Repetitive Full-Table Scans:** `CleanDataMessageHandler` loops `select id from web_carts/carts/cart_carry_over_histories where updated_time <= ? limit 100` until exhaustion. None of these three tables index `updated_time`, requiring sequential table scans for every 100 deleted rows, creating severe MySQL I/O and CPU contention during cleanup runs.
2. **Missing Outbound RPC Timeouts on Critical User Paths:** 27 of 29 HTTP client bindings lack explicit connect/read timeouts. Key synchronous dependencies on the cart checkout and summary path (`PriceWebService`, `CustomerWebService`, `PublishedRestaurantWebService`) rely on default timeouts, exposing worker threads to exhaustion if an upstream service degrades.
3. **Per-JVM In-Process Semaphore Throttling:** `SemaphoreExecutorUtil` throttles courier supply trigger tasks using a static local `Semaphore` (default 5 permits). Horizontal auto-scaling increases downstream traffic linearly with replica count, bypassing intended concurrency limits and risking downstream overload.
4. **Unbounded User Enumeration with N+1 Redis Operations:** `CourierSupplyHealthTriggerPeriodicallyMessageHandler` fetches all online users from `AppOnlineUserWebService` and `WebOnlineUserWebService` in an unbounded list, then performs single-key sequential `redis.get()` lookups in a loop, publishing separate Kafka messages per user without batching or distributed concurrency controls.
5. **Slow SQL Suppression on Search Queries:** `CartService.search()` and `WebCartService.search()` explicitly call `Database.suppressSlowSQLWarning(true)` to mask execution of an unindexed `COUNT(*)` over the entire cart repository and an unindexed multi-table `JOIN` with `GROUP BY` and `ORDER BY created_time ASC`.
