Framework: core-ng (`core.framework.*`, `Module`, `bind(...)`, `HTTPClient`, `WebService`, `MessagePublisher`, `app.properties`)

## 1. API Surface

### REST Endpoints
Defined in `backend/wonder-cart-service-interface` and bound in `backend/wonder-cart-service/src/main/java/app/wondercart/CartModule.java` and `CartItemModule.java`:

| Web Service Interface | Path Prefix | Count | Description |
|---|---|:---:|---|
| `AppCartWebService` | `/cart`, `/v2/cart`, `/V2/cart` | 29 | Mobile cart CRUD, address, tip, donation, fast-pass, carry-over, search |
| `WonderCartItemWebService` | `/cart/item`, `/cart/quantity` | 7 | Mobile cart item add/batch-add/update/delete/quantity |
| `WebCartWebService` | `/web-cart`, `/v2/web-cart` | 27 | Web cart CRUD, address, tip, donation, fast-pass, carry-over, search |
| `WebCartItemWebService` | `/web-cart/item` | 6 | Web cart item add/update/delete/batch-delete/quantity |
| `POPCartWebService` | `/pop/cart` | 11 | Point-of-purchase cart CRUD, refresh, summary, utensils, promotion |
| `POPCartItemWebService` | `/pop/cart/item`, `/pop/cart/quantity` | 6 | Point-of-purchase cart item add/update/delete/quantity |
| `BOWonderCartWebService` | `/bo/cart` | 1 | Back-office lookup for App cart |
| `BOWebCartWebService` | `/bo/web-cart` | 1 | Back-office lookup for Web cart |
| Raw HTTP Route (`CartModule.java`) | `/app/migrate-cart-bundle-item-choice` | 1 | Unauthenticated POST triggering async DB migration |

### Message Consumers
Bound in `backend/wonder-cart-service/src/main/java/app/wondercart/KafkaConsumerModule.java`:
- `courier-supply-health-trigger-message` (`WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC`) -> `CourierSupplyHealthTriggerMessageHandler`
- `courier-supply-health-trigger-periodically-message` (`WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_PERIODICALLY_TOPIC`) -> `CourierSupplyHealthTriggerPeriodicallyMessageHandler`
- `consumer-clean-data` (`CommonTopic.CONSUMER_CLEAN_DATA`) -> `CleanDataMessageHandler`
- `wonder-configuration-updated` (`WonderConfigurationTopics.WONDER_CONFIGURATION_UPDATED`) -> `WonderConfigurationUpdatedMessageHandler`
- `corporate-order-client-changed` (`CorporateOrderClientTopics.CORPORATE_ORDER_CLIENT_CHANGED`) -> `CorporateOrderClientChangedMessageHandler`

### Critical Path (Synchronous, User-Facing)
- **Cart Read & Price Calculation:** `PUT /v2/cart/get` (`CartServiceV2.get`), `PUT /v2/web-cart/get` (`WebCartServiceV2.get`), `GET /cart/summary`, `GET /web-cart/summary`.
- **Cart Mutations:** `POST /cart`, `POST /cart/item`, `PUT /cart/item/:itemId`, `DELETE /cart/item/:itemId`, `PUT /cart/quantity`, `PUT /cart/address`, `PUT /cart/payment`, `PUT /V2/cart/tip`, `PUT /cart/promotion`.
- **Execution Characteristic:** `getV2` sequentially fetches cart relations and makes synchronous external RPC calls to `customer-service`, `restaurant-service-v2`, `order-service` (`PriceWebService`), `customer-wallet-service`, and `marketing-service`. If an offer or promo code auto-applies (`promotionCodeUpdated`), the entire cart context and external calls are executed a second time in the same HTTP request thread. Mutating endpoints trigger `CartSyncInterceptor`, which publishes to Kafka asynchronously.

### Batch and Scheduled Jobs
- **Application Scheduled Jobs:** None defined in `wonder-cart-service`.
- **Client Library Periodic Jobs:** `CLBConfig` (`utility/client-side-load-balancing-library/src/main/java/lib/clb/CLBConfig.java`) runs `fixedRate("clb:refresh-all-host-ip", ...)` every 30 seconds for DNS resolution refresh.

---

## 2. Database Access Patterns

### MySQL
Configured in `backend/wonder-cart-service/src/main/java/app/wondercart/CartServiceApp.java`:
- Primary DB (`db()`): `sys.jdbc.url`
- Read Replica (`db("replicaDB")`): `sys.jdbc.replicaUrl` (used by `CourierSupplyHealthTriggerMessageHandler` and `CartServiceV2.getCartDiningOption`)
- Query Timeout: 5 seconds (`db().timeout(Duration.ofSeconds(5))` in `CartServiceApp.java`)
- Connection Pool: Framework defaults (core-ng defaults: `minSize=5`, `maxSize=50`). No pool sizing properties specified in code.

#### Tables Accessed
| Table Name | Operations | Indexing & Access Pattern |
|---|---|---|
| `carts` | SELECT, INSERT, UPDATE, DELETE | Primary Key (`id`), Unique Key (`owner_id`). No index on `updated_time`. |
| `cart_items` | SELECT, INSERT, UPDATE, DELETE | Composite Indexes: (`cart_id`, `updated_time`), (`owner_id`, `restaurant_id`). |
| `cart_menu_item_options` | SELECT, INSERT, UPDATE, DELETE | Indexes: `cart_item_id`, `owner_id`, `cart_id`. |
| `cart_bundle_item_choices` | SELECT, INSERT, UPDATE, DELETE, BATCH UPSERT | Indexes: `cart_item_id`, `owner_id`, `cart_id`. |
| `cart_bundle_item_choice_options` | SELECT, INSERT, UPDATE, DELETE | Indexes: `cart_item_id`, `owner_id`, `cart_id`. |
| `web_carts` | SELECT, INSERT, UPDATE, DELETE | Primary Key (`id`), Unique Key (`owner_id`). No index on `updated_time`. |
| `web_cart_items` | SELECT, INSERT, UPDATE, DELETE | Composite Indexes: (`cart_id`, `updated_time`), (`owner_id`, `restaurant_id`). |
| `web_cart_menu_item_options` | SELECT, INSERT, UPDATE, DELETE | Indexes: `cart_item_id`, `owner_id`, `cart_id`. |
| `web_cart_bundle_item_choices` | SELECT, INSERT, UPDATE, DELETE | Indexes: `cart_item_id`, `owner_id`, `cart_id`. |
| `web_cart_bundle_item_choice_options` | SELECT, INSERT, UPDATE, DELETE | Indexes: `cart_item_id`, `owner_id`, `cart_id`. |
| `pop_carts`, `pop_cart_items`, etc. | SELECT, INSERT, UPDATE, DELETE | Primary Key (`id`), Unique Key (`owner_id`), Index on `cart_id`. |
| `cart_carry_over_histories` | SELECT, INSERT, DELETE | Primary Key (`id`). No index on `updated_time`. |
| `reorder_cart_created_records` | SELECT, INSERT, DELETE | Primary Key (`id`), Index on `created_time`. |

#### Large Scans, Warning Suppressions, and Unbounded Queries
- **Full Table Scans in Cleanup (`CleanDataMessageHandler.java`):**
  - `deleteAppCartHandler`: `SELECT id FROM carts WHERE updated_time <= ? LIMIT 100` in a `while(true)` loop. `carts` has no index on `updated_time`, causing repeated full table scans.
  - `deleteWebCartHandler`: `SELECT id FROM web_carts WHERE updated_time <= ? LIMIT 100` in a `while(true)` loop. `web_carts` has no index on `updated_time`.
  - `deleteCartCarryOverHistoryHandler`: `SELECT id FROM cart_carry_over_histories WHERE updated_time <= ? LIMIT 100` in a `while(true)` loop. No index on `updated_time`.
- **Search Query Table Scans & Warning Suppression (`CartService.java`, `WebCartService.java`):**
  - `search()` issues `response.total = cartRepository.select().count()`, performing a full table scan `SELECT COUNT(*)` on every request.
  - Both services invoke `Database.suppressSlowSQLWarning(true)` to silence slow query warnings during search execution.
  - Search queries join cart and cart items with `GROUP BY` and unindexed `ORDER BY cart.created_time ASC` using string-concatenated `limit <skip>, <limit>`.
- **Unbounded Migration Offset Scan (`MigrateCartBundleItemChoiceController.java`):**
  - Scans `cart_bundle_item_choices` via `.skip(skip).limit(500).orderBy("created_time asc")` across the entire table.

### MongoDB / Atlas
N/A (Not used).

### N+1 Query Patterns
- **Database queries:** Cart items, options, and bundle choices are queried in bulk using `cart_id=?`, avoiding DB-level N+1 queries.
- **External RPC N+1 in Event Processing (`CourierSupplyHealthDataFetcher.java`):**
  - In `fetchRestaurantInfos()`, iterating restaurant IDs calls `publishedRestaurantWebService.getBaseInfo(restaurantId)` sequentially in a `for` loop on cache miss instead of using a bulk endpoint.

---

## 3. Kafka Usage

Configured in `backend/wonder-cart-service/src/main/java/app/wondercart/KafkaConsumerModule.java`:

### Topics Consumed
- `courier-supply-health-trigger-message` (`WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC`): `CourierSupplyHealthTriggerMessageHandler`. Concurrency gated by in-memory `Semaphore` (`app.semaphore.permits`, default 5).
- `courier-supply-health-trigger-periodically-message` (`WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_PERIODICALLY_TOPIC`): `CourierSupplyHealthTriggerPeriodicallyMessageHandler`. Dispatches async tasks to `Executor`.
- `consumer-clean-data` (`CommonTopic.CONSUMER_CLEAN_DATA`): `CleanDataMessageHandler`. Dispatches 4 concurrent DB purge tasks.
- `wonder-configuration-updated` (`WonderConfigurationTopics.WONDER_CONFIGURATION_UPDATED`): `WonderConfigurationUpdatedMessageHandler`. Updates Redis cache regions.
- `corporate-order-client-changed` (`CorporateOrderClientTopics.CORPORATE_ORDER_CLIENT_CHANGED`): `CorporateOrderClientChangedMessageHandler`. Updates Redis cache regions.

### Topics Produced
- `courier-supply-health-trigger-message`: Published by `CartSyncPublisherService` (via `CartSyncInterceptor` on cart mutations; keyed by `ownerId`).
- `delivery-courier-supply-health-check` (`DeliverySupplyTopic.DELIVERY_COURIER_SUPPLY_HEALTH_CHECK`): Published by `CourierSupplyHealthTriggerMessageHandler` to downstream delivery service.
- `consumer-clean-data`: Registered in `KafkaConsumerModule.java` to fulfill core-ng configuration requirements.

### Consumer Configuration & Polling
- Consumer Group ID: Core-ng default application name (`wonder-cart-service`).
- Max Poll: Configured via `kafka().maxPoll(CommonConfig.MAX_POLL_SIZE, CommonConfig.MAX_POLL_BYTES)`.
- Concurrency: Configured via `kafka().concurrency(CommonConfig.CONCURRENCY)`.
- Feature Flag: Consumers only enabled if `app.enableKafkaConsumer` is `true` (default: `false` in `app.properties`).
- Blocking poll loops: None in application code (managed internally by core-ng).

---

## 4. Caching Strategy

The service utilizes Redis via core-ng `Cache<T>` and raw `Redis` client (`backend/wonder-cart-service/src/main/java/app/wondercart/CartModule.java`).

### Cache Regions and TTLs
Registered in `CartModule.registerCache()`:
- `DeliveryFeeConfigurationCache`: TTL 12 hours
- `ServiceFeeConfigurationCache`: TTL 12 hours
- `HDRFastPassConfigurationCache`: TTL 12 hours
- `HDRTipConfigurationCache`: TTL 12 hours
- `HDRTipConfigurationHardcodedValueCache`: TTL 12 hours
- `CorporateOrderFeeAndDiscountCache`: TTL 12 hours
- `WonderSpotServiceFeeConfigurationCache`: TTL 12 hours
- `CustomerCache`: TTL 10 minutes (customer membership, name, phone)
- `CourierSupplyHealthRequestMessage.Pickup`: TTL 1 day (HDR pickup details)
- `CustomerAddressCache`: TTL 1 day (customer delivery addresses)
- `RestaurantCache`: TTL 1 day (restaurant brand metadata)

### Direct Redis Keys (Throttling & De-duplication)
Managed in `CourierSupplyHealthTriggerMessageHandler.java` & `CourierSupplyHealthTriggerPeriodicallyMessageHandler.java`:
- `courier:supply:health:trigger:%s` (`ownerId`): TTL = `app.courierSupplyHealthTriggerCache.durationOfSeconds` (default: 10s). Throttles redundant health trigger evaluations.
- `courier_supply:user:%s` (`ownerId`): TTL = 1 minute. Deduplicates periodic batch user syncs.

### Eviction Policy & Degradation
- **Eviction / Invalidation:** Configuration caches are refreshed or evicted upon receiving Kafka messages (`WonderConfigurationUpdatedMessage`, `CorporateOrderClientChangedMessage`).
- **Cache Miss Degradation:** Cache-aside pattern (`cache.get(key, loader)`) queries downstream services on cache miss. Under Redis failure, simultaneous requests pass directly to downstream services (stampede risk).

---

## 5. Resilience Configuration

### Outbound HTTP Timeouts & Retries
Configured in `utility/client-side-load-balancing-library/src/main/java/lib/clb/CLBConfig.java` and `backend/wonder-cart-service/src/main/java/app/wondercart/APIClientModule.java`:
- **CLB WebServices (24 clients):** Connect timeout: 2s; Request/read timeout: 20s; Keep-alive: 5m; Retries: 2 with 100ms wait (`maxRetries = 2`, `retryWaitTimeInMs = Duration.ofMillis(100)`); Health check concurrency: 5.
- **Core-ng WebServices (`api().client(...)`):** `PriceWebService`, `BACycleItemWebService`, `BAGlobalSettingWebService` use core-ng defaults (connect: 2s, read: 30s, 2 retries). No custom timeouts set in `APIClientModule.java`.
- **GoogleBot Fallback WebServices:** `PublishedMenuItemWebService`, `PublishedBundleItemWebService` set timeout to 5s (`Timeout.MILLISECOND_5000` in `APIClientModule.java`) and fall back to stub implementations.

### Bulkheads and Executor Limits
- **Kafka Bulkhead:** `SemaphoreExecutorUtil.java` uses an in-memory `Semaphore` configured by `app.semaphore.permits` (default: 5) to restrict concurrent executions of `CourierSupplyHealthTriggerMessageHandler`.
- **Database Timeout:** 5 seconds (`db().timeout(Duration.ofSeconds(5))` in `CartServiceApp.java`).

### Properties and Defaults
All values below are defaults loaded from configuration files; deployed values can be overridden via environment variables (capitalized, dots to underscores) in Kubernetes manifests:

| Property Key | Default Value | Source File | Usage |
|---|---|---|---|
| `app.enableKafkaConsumer` | `false` | `backend/wonder-cart-service/src/main/resources/app.properties` | Toggles Kafka consumer registration |
| `app.semaphore.permits` | `5` | `backend/wonder-cart-service/src/main/resources/app.properties` | In-memory Semaphore permit count in `SemaphoreExecutorUtil` |
| `app.courierSupplyHealthTriggerCache.durationOfSeconds` | `10` | `backend/wonder-cart-service/src/main/resources/app.properties` | Redis TTL for courier health trigger throttling |
| `app.order.serviceURL` | `https://localhost:15012` | `backend/wonder-cart-service/src/main/resources/app.properties` | Target URL for `PriceWebService` |
| `app.restaurant.serviceURL` | `https://restaurant-service-v2-headless:8443` | `backend/wonder-cart-service/src/main/resources/app.properties` | Target URL for fallback restaurant services |
| `app.blueApronProduct.serviceURL` | `https://blueapron-product-service.dev-ba-consumer` | `backend/wonder-cart-service/src/main/resources/app.properties` | Target URL for Blue Apron services |
| `app.kitchenManagement.serviceURL` | `http://localhost:8099` | `backend/wonder-cart-service/src/main/resources/app.properties` | Target URL for `HDRWebService` |
| `app.consumer.restaurant.serviceURL` | `https://consumer-restaurant-service-v2-headless:8443` | `backend/wonder-cart-service/src/main/resources/consumer-app.properties` | Target URL for restaurant/menu services |
| `app.consumer.customer.serviceURL` | `https://consumer-customer-service-headless:8443` | `backend/wonder-cart-service/src/main/resources/consumer-app.properties` | Target URL for customer/address services |
| `app.consumer.customerWallet.serviceURL` | `https://consumer-customer-wallet-service-headless:8443` | `backend/wonder-cart-service/src/main/resources/consumer-app.properties` | Target URL for `CustomerWalletWebService` |
| `app.consumer.marketing.serviceURL` | `https://consumer-marketing-service-headless:8443` | `backend/wonder-cart-service/src/main/resources/consumer-app.properties` | Target URL for promotion/membership services |
| `app.consumer.search.serviceURL` | `https://search-service-headless:8443` | `backend/wonder-cart-service/src/main/resources/consumer-app.properties` | Target URL for `SearchWebService` |
| `app.consumer.wonderSetting.serviceURL` | `https://consumer-wonder-setting-service-headless:8443` | `backend/wonder-cart-service/src/main/resources/consumer-app.properties` | Target URL for configuration/fee services |
| `app.consumer.appOnlineUser.serviceURL` | `http://wonder-app-notification-service-headless:8080` | `backend/wonder-cart-service/src/main/resources/consumer-app.properties` | Target URL for `AppOnlineUserWebService` |
| `app.consumer.webOnlineUser.serviceURL` | `http://wonder-web-notification-service-headless:8080` | `backend/wonder-cart-service/src/main/resources/consumer-app.properties` | Target URL for `WebOnlineUserWebService` |
| `sys.jdbc.url` | `jdbc:mysql://127.0.0.1:3306/wonder_cart?useUnicode=true&characterEncoding=utf-8&allowPublicKeyRetrieval=true` | `backend/wonder-cart-service/src/main/resources/sys.properties` | Primary MySQL JDBC connection string |
| `sys.jdbc.replicaUrl` | `jdbc:mysql://127.0.0.1:3306/wonder_cart?useUnicode=true&characterEncoding=utf-8&allowPublicKeyRetrieval=true` | `backend/wonder-cart-service/src/main/resources/sys.properties` | Read replica MySQL JDBC connection string |
| `sys.jdbc.user` | `root` | `backend/wonder-cart-service/src/main/resources/sys.properties` | MySQL database user |
| `sys.jdbc.password` | `root` | `backend/wonder-cart-service/src/main/resources/sys.properties` | MySQL database password |
| `sys.redis.host` | `localhost` | `backend/wonder-cart-service/src/main/resources/sys.properties` | Redis server hostname |
| `sys.cache.host` | `local` | `backend/wonder-cart-service/src/main/resources/sys.properties` | Cache host identifier |
| `sys.kafka.uri` | `localhost:9092` | `backend/wonder-cart-service/src/main/resources/sys.properties` | Kafka broker endpoints |
| `sys.log.appender` | `console` | `backend/wonder-cart-service/src/main/resources/sys.properties` | Logging appender target |

---

## 6. External Dependencies

All outbound HTTP calls originate from clients registered in `backend/wonder-cart-service/src/main/java/app/wondercart/APIClientModule.java`:

| Target Service / Group | Client Interfaces | Transport / Library | Configured Timeouts |
|---|---|---|---|
| `restaurant-service-v2` | `PublishedRestaurantWebService`, `PublishedMenuItemWebService`, `PublishedBundleItemWebService` | CLB HTTPClient | Connect: 2s, Request: 20s |
| `customer-service` | `CustomerWebService`, `CustomerAddressWebService`, `CustomerMembershipWebService`, `CustomerCreditCardWebService`, `VisitorWebService`, `VisitorAddressWebService`, `AliasUserWebService` | CLB HTTPClient | Connect: 2s, Request: 20s |
| `order-service` | `PriceWebService` | Core-ng API Client | Connect: 2s, Read: 30s (Default) |
| `customer-wallet-service` | `CustomerWalletWebService` | CLB HTTPClient | Connect: 2s, Request: 20s |
| `marketing-service` | `PromotionWebService`, `MembershipProductWebService`, `SegmentWebService` | CLB HTTPClient | Connect: 2s, Request: 20s |
| `wonder-setting-service` | `ConfigurationWebService`, `HDRFeeConfigurationWebService`, `HDRTipConfigurationWebService`, `CorporateOrderClientWebService`, `WonderSpotFeeConfigurationWebService`, `FeeConfigurationWebService` | CLB HTTPClient | Connect: 2s, Request: 20s |
| `search-service` | `SearchWebService` | CLB HTTPClient | Connect: 2s, Request: 20s |
| `kitchen-management-service` | `HDRWebService` | CLB HTTPClient | Connect: 2s, Request: 20s |
| `wonder-app-notification-service` | `AppOnlineUserWebService` | CLB HTTPClient | Connect: 2s, Request: 20s |
| `wonder-web-notification-service` | `WebOnlineUserWebService` | CLB HTTPClient | Connect: 2s, Request: 20s |
| `blueapron-product-service` | `BACycleItemWebService`, `BAGlobalSettingWebService` | Core-ng API Client | Connect: 2s, Read: 30s (Default) |
| Fallback Restaurant Services | `PublishedMenuItemWebService`, `PublishedBundleItemWebService` | CLB Fallback | Request: 5s (`Timeout.MILLISECOND_5000`) |

---

## 7. Scalability Signals

- **In-Process Semaphore (`SemaphoreExecutorUtil`):** Bulkhead concurrency control in `CourierSupplyHealthTriggerMessageHandler` uses an in-memory `java.util.concurrent.Semaphore` (`app.semaphore.permits`, default 5). Because this state is local to each JVM process, scaling to $N$ Kubernetes replicas increases cluster-wide concurrency to $5 \times N$, bypassing downstream protection.
- **Unguarded Background Migration Task:** `MigrateCartBundleItemChoiceController` (`POST /app/migrate-cart-bundle-item-choice`) submits an asynchronous task to `Executor` without distributed locks or deduplication. Multiple invocations run concurrently, contending on database locks and table scans.
- **Distributed Scheduled Task Guarding:** N/A (service does not declare scheduled cron jobs; periodic tasks are event-driven via Kafka or internal DNS refresh).
- **Stateful Operations / Local Filesystem:** Stateless service; does not write to local disks or maintain local memory state between requests. All persistent state resides in MySQL and Redis.

---

## 8. Key Scalability Signals

1. **Full Table Scans and Suppressed Warnings on High-Frequency Operations:**
   - Both `CartService.search()` and `WebCartService.search()` execute unindexed queries and explicitly call `Database.suppressSlowSQLWarning(true)` to silence framework warnings. Each search request issues `SELECT COUNT(*)` across the entire `carts` or `web_carts` table.
   - `CleanDataMessageHandler` executes `SELECT id FROM carts WHERE updated_time <= ? LIMIT 100` in a `while(true)` loop. Neither `carts`, `web_carts`, nor `cart_carry_over_histories` have indexes on `updated_time`, causing repeated full table scans during cleanup.
2. **In-Process Bulkhead Multiplies Under Horizontal Autoscaling:**
   - `SemaphoreExecutorUtil` uses a local JVM `Semaphore` initialized to 5 permits (`app.semaphore.permits`). As pod replicas increase under HPA (up to 4 pods for web API and 2 pods for Kafka consumer), total concurrent background tasks scale to $5 \times N$, eliminating downstream backpressure protection for `delivery-supply` and `restaurant-service`.
3. **Synchronous Call Amplification and Re-Execution on Critical Path:**
   - Serving `getV2` cart pricing involves synchronous fan-out to 5+ downstream microservices (`customer-service`, `restaurant-service`, `order-service`, `customer-wallet-service`, `marketing-service`). If an offer or promo code auto-applies during evaluation (`promotionCodeUpdated`), the entire context is re-fetched and re-evaluated from scratch in the same request thread, doubling database and network overhead.
4. **Iterative N+1 HTTP Calls in Event Processor:**
   - `CourierSupplyHealthDataFetcher.fetchRestaurantInfos()` iterates through unique restaurant IDs in each cart item and invokes single-restaurant RPCs (`publishedRestaurantWebService.getBaseInfo()`) sequentially on cache miss, multiplying latency and thread starvation inside the background consumer.
5. **Unbounded Offset Pagination in Migration Endpoint:**
   - `POST /app/migrate-cart-bundle-item-choice` lacks authentication, rate-limiting, and distributed locking. It processes `cart_bundle_item_choices` using incremental `skip(skip).limit(500)` pagination across the whole table while issuing synchronous RPC calls per batch.
