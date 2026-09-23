# Business Context & Scalability Analysis — wonder-cart-service

This document provides a highly detailed, fact-based business context and technical scalability analysis of the `wonder-cart-service` (including `wonder-cart-service-interface` and `wonder-cart-service-db-migration`) based on a static review of the codebase.

---

## 1. API Surface

The service exposes a comprehensive REST API and message consumption model across multiple user-facing clients (App, Web, POP) and administrative clients (Back Office).

### REST Endpoints (REST Web Services)

Endpoints are registered using the `core.framework` routing engine. The synchronous REST APIs are user-facing and form part of the critical path for managing shopping carts, modifying contents, updating payment details, and executing checkouts.

#### A. AppCartWebService (Mobile App) — Critical Path
Exposed under the `/cart` namespace (registered in `CartModule` with `WonderAppCartWebServiceImpl`):
- `POST /cart`: Create a new cart (`CreateWonderCartRequest` -> `CreateWonderCartResponse`).
- `DELETE /cart`: Delete a cart (`DeleteWonderCartRequest`).
- `DELETE /v2/cart`: Delete a cart with concurrent modification check (`DeleteWonderCartWithConcurrentCheckRequest`).
- `DELETE /cart/delete-all`: Empty/clear all elements in the cart.
- `PUT /cart/address`: Update cart's delivery address (`UpdateWonderCartAddressRequest`).
- `PUT /cart/payment`: Update cart's payment method and credit card ID (`UpdateWonderCartPaymentRequest`).
- `PUT /cart/utensils`: Set utensil requirement preference (`UpdateWonderCartUtensilsRequest`).
- `PUT /V2/cart/tip`: Set tip option and amount (`UpdateWonderCartTipRequestV2`).
- `PUT /cart/donation`: Enable or disable donation option (`UpdateWonderCartDonationRequest`).
- `GET /cart/summary`: Retrieve cart price and item summary (`GetWonderCartSummaryRequest` -> `GetWonderCartSummaryResponse`).
- `PUT /cart`: Search/query cart with filters (`SearchWonderCartRequest` -> `SearchWonderCartResponse`).
- `PUT /v2/cart/get`: Fetch detailed current cart state (`GetAppCartRequest` -> `GetAppCartResponse`).
- `GET /cart/check`: Run safety checks on cart items (`CheckWonderCartRequest` -> `CheckWonderCartResponse`).
- `PUT /cart/transfer`: Transfer a visitor cart to a registered customer account (`TransferWonderCartRequest` -> `TransferWonderCartResponse`).
- `PUT /cart/clean-address`: Reset the delivery address fields (`CleanWonderCartAddressRequest`).
- `GET /cart/address/:ownerId`: Get the delivery address of the cart by `ownerId`.
- `PUT /cart/promotion`: Apply or modify promotional code (`UpdateWonderCartPromotionRequest`).
- `PUT /cart/hdr`: Update Heated Delivery Restaurant (HDR) identifier or address ID (`WonderCartUpdateHDROrAddressIdRequest`).
- `GET /cart/brand-category`: Determine cart brand category (`GetCartBrandCategoryRequest` -> `GetCartBrandCategoryResponse`).
- `GET /cart/hdr`: Retrieve the current HDR configurations for the cart (`GetCartHDRRequest` -> `GetCartHDRResponse`).
- `GET /cart/restaurant`: Get active restaurant details linked to the cart (`GetWonderCartRestaurantRequest` -> `GetWonderCartRestaurantResponse`).
- `PUT /cart/fast-pass`: Set fast pass delivery flag (`UpdateWonderCartFastPassRequest`).
- `PUT /cart/schedule-time-slot`: Update scheduled delivery slot (`UpdateWonderCartScheduleTimeSlotRequest`).
- `PUT /cart/carry-over`: Carry over items to new cart session (`WonderCartCarryOverRequest`).
- `PUT /cart/carry-over/preview`: Preview items carried over to new session (`WonderCartCarryOverRequest` -> `CarryOverPreviewResponse`).
- `PUT /cart/clean-time-slot`: Clear the delivery time slot (`CleanWonderCartScheduleTimeSlotRequest`).
- `POST /cart/record-reorder-cart`: Track and log a cart reorder event (`RecordReorderCartRequest`).
- `PUT /cart/basic-info`: Modify standard cart properties (`UpdateWonderCartBasicInfoRequest`).
- `PUT /cart/inventory-key/get`: Query inventory validation key (`GetAppCartInventoryKeyRequest` -> `GetAppCartInventoryKeyResponse`).

#### B. WonderCartItemWebService (Mobile App Item Operations) — Critical Path
Exposed under the `/cart/item` and `/cart/quantity` paths:
- `POST /cart/item`: Add item to cart (`AddWonderCartItemRequest` -> `AddWonderCartItemResponse`).
- `POST /cart/item/reorder/batch-add`: Batch add items during reordering (`BatchAddWonderCartItemRequest`).
- `GET /cart/item/:itemId`: Query details of an individual item.
- `PUT /cart/item/:itemId`: Update specific item options/customizations (`UpdateWonderCartItemRequest`).
- `DELETE /cart/item/:itemId`: Remove an item from the cart.
- `PUT /cart/item/batch-delete`: Remove multiple items in a single call (`BatchDeleteWonderCartItemRequest`).
- `PUT /cart/quantity`: Directly update item quantity (`UpdateWonderCartQuantityRequest`).

#### C. WebCartWebService (Web Client) — Critical Path
Exposed under the `/web-cart` namespace:
- `POST /web-cart`, `DELETE /web-cart`, `DELETE /web-cart/delete-with-concurrent-check`, `DELETE /web-cart/delete-all`
- `PUT /web-cart/address`, `PUT /web-cart/payment`, `PUT /web-cart/utensils`, `PUT /web-cart/tip`, `PUT /web-cart/donation`
- `GET /web-cart/summary`, `PUT /web-cart` (search), `PUT /v2/web-cart/get`, `GET /web-cart/check`, `PUT /web-cart/transfer`, `PUT /web-cart/clean-address`
- `GET /web-cart/address/:ownerId`, `PUT /web-cart/promotion`, `PUT /web-cart/hdr`, `PUT /web-cart/hdr-and-address`
- `GET /web-cart/hdr`, `GET /web-cart/brand-category`, `PUT /web-cart/fast-pass`, `GET /web-cart/restaurant-id`, `PUT /web-cart/schedule-time-slot`
- `PUT /web-cart/carry-over`, `PUT /web-cart/carry-over/preview`

#### D. WebCartItemWebService (Web Client Item Operations) — Critical Path
Exposed under `/web-cart/item` paths:
- `POST /web-cart/item`, `GET /web-cart/item/:itemId`, `PUT /web-cart/item/:itemId`, `DELETE /web-cart/item/:itemId`, `PUT /web-cart/item/batch-delete`, `PUT /web-cart/item/quantity`

#### E. POPCartWebService & POPCartItemWebService (Point-of-Presence Client) — Critical Path
Exposed under `/pop/cart` paths:
- `POST /pop/cart`, `DELETE /pop/cart`, `DELETE /pop/cart/concurrent-check`, `DELETE /pop/cart/delete-all`, `PUT /pop/cart/utensils`, `GET /pop/cart/summary`, `PUT /pop/cart/refresh`, `GET /pop/cart`, `GET /pop/cart/hdr`, `PUT /pop/cart/promotion`, `PUT /pop/cart/phone`
- `POST /pop/cart/item`, `GET /pop/cart/item/:itemId`, `PUT /pop/cart/item/:itemId`, `DELETE /pop/cart/item/:itemId`, `PUT /pop/cart/item/batch-delete`, `PUT /pop/cart/quantity`

#### F. Back Office Web Services — Admin Path
- `BOWonderCartWebService`: `GET /bo/cart` (Get cart details).
- `BOWebCartWebService`: `GET /bo/web-cart` (Get web cart details).

#### G. Direct Route Registration
- `POST /app/migrate-cart-bundle-item-choice`: Controller mapping to migrate cart bundle choices.

### Asynchronous Message Consumers & Batch Jobs

Rather than JVM-based local schedulers, the service utilizes Kafka messages to scale out asynchronous and periodic task executions:

1. **CleanDataMessageHandler (Data Cleanup Job):**
   - **Trigger:** Consumes `ConsumerCleanDataMessage` on topic `CommonTopic.CONSUMER_CLEAN_DATA` (only when `app.enableKafkaConsumer=true`).
   - **Action:** Triggers parallel asynchronous executions of cleanup logic targeting database records (carts, cart items, web carts, web cart items, carry over histories, and reorder records) that have not been updated for more than one month.
   - **Frequency:** Typically triggered via an external scheduler publishing periodically.

2. **CourierSupplyHealthTriggerMessageHandler (Reactive Trigger Task):**
   - **Trigger:** Consumes `CourierSupplyHealthTriggerMessage` on topic `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC`.
   - **Action:** Gathers cart information and evaluates supply/demand data for couriers by publishing a request.
   - **Frequency:** Highly frequent, executing reactively whenever active carts are created or updated.

3. **CourierSupplyHealthTriggerPeriodicallyMessageHandler (Periodic Sync Task):**
   - **Trigger:** Consumes `CourierSupplyHealthTriggerPeriodicallyMessage` on topic `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_PERIODICALLY_TOPIC`.
   - **Action:** Requests all online users from `AppOnlineUserWebService` and `WebOnlineUserWebService` and publishes individual trigger messages for each to check and sync courier supply.
   - **Frequency:** Run periodically, serving as a scheduled system sync.

---

## 2. Database Access Patterns

The service is backed by a MySQL database (`wonder_cart`) configured using the custom `core.framework` ORM layer. It separates read and write traffic by establishing primary and replica connections.

### Connection Pool Configuration
- **Command Timeout:** Hardcoded timeout of `5 seconds` (`db().timeout(Duration.ofSeconds(5))`) inside `CartServiceApp.java`.
- **Pool Size:** Configured to a max of `20` connections (`db().poolSize(20)`). This is a custom pool size intended to support high concurrent load. No dynamic setting exists in the properties file.

### Primary Database Entities & Repositories
The following repositories are managed on the primary DB for read-write operations:
- **App/Wonder Carts & Relations:** `Cart`, `CartItem`, `CartMenuItemOption`, `CartBundleItemChoice`, `CartBundleItemChoiceOption`, `ReorderCartCreatedRecord`
- **Web Carts & Relations:** `WebCart`, `WebCartItem`, `WebCartMenuItemOption`, `WebCartBundleItemChoice`, `WebCartBundleItemChoiceOption`
- **POP Carts & Relations:** `POPCart`, `POPCartItem`, `POPCartMenuItemOption`, `POPCartBundleItemChoice`, `POPCartBundleItemChoiceOption`
- **Audit/History:** `CartCarryOverHistory`

### Read Replica Database Entities & Repositories
Read-only repositories are explicitly registered under the `@Named("replicaDB")` tag (pointing to `sys.jdbc.replicaUrl` in properties). Only select classes have replica repositories:
- `Cart`, `CartItem`, `WebCart`, `WebCartItem`, `CartMenuItemOption`, `CartBundleItemChoice`, `CartBundleItemChoiceOption`, `WebCartMenuItemOption`, `WebCartBundleItemChoice`, `WebCartBundleItemChoiceOption`

### Database Views
The database registration includes two custom SQL query view entities:
- `SearchCartView`
- `SearchWebCartView`

### Major Scalability Concerns in Queries & Indexes

#### A. Full Table Scans on High-Volume Tables during Cleanups (Critical Concern)
The asynchronous cleanup handler `CleanDataMessageHandler` queries the database in batches of 100 rows using `updated_time` comparison:
- `SELECT id FROM carts WHERE updated_time <= ? LIMIT 100`
- `SELECT id FROM web_carts WHERE updated_time <= ? LIMIT 100`
- `SELECT id FROM cart_carry_over_histories WHERE updated_time <= ? LIMIT 100`

However, checking the SQL table definition migrations (`R__carts.sql`, `R__web_carts.sql`, `R__cart_carry_over_histories.sql`) confirms:
- **No index exists on `updated_time`** in any of these three tables! The only indexes configured are the primary keys (`id`) and unique indexes on `owner_id`.
- This missing index forces MySQL to run **Full Table Scans** to evaluate the `updated_time` filter on tables containing potentially millions of rows. This will saturate database CPU and introduce long-duration transactional locking in production environments.
- *Note:* The index `ix_created_time` does exist on `reorder_cart_created_records` for its cleanup path.

#### B. Extensive Raw SQL Batch Deletes
To purge cart relations, `CleanDataMessageHandler` executes raw batch deletes using `IN` clauses constructed dynamically:
- `DELETE FROM tableName WHERE column IN (?, ?, ...)`
This generates hundreds of dynamic SQL parameterized queries, adding to compile and execution overhead.

---

## 3. MongoDB / Atlas Access Patterns

- **Status:** **N/A**
- The service does not use MongoDB. It depends entirely on MySQL for relational persistence. No MongoDB configurations, migrations, or repositories are registered.

---

## 4. Kafka Usage

Kafka messaging is configured under `KafkaConsumerModule` and `CartModule`.

### Produced Topics
- `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC` (for `CourierSupplyHealthTriggerMessage`)
- `DeliverySupplyTopic.DELIVERY_COURIER_SUPPLY_HEALTH_CHECK` (for `CourierSupplyHealthRequestMessage` - registered dynamically when `app.enableKafkaConsumer=true`)
- `CommonTopic.CONSUMER_CLEAN_DATA` (for `ConsumerCleanDataMessage`)

### Consumed Topics
The message listeners are gated behind the `app.enableKafkaConsumer` properties flag. If set to `true`, the following topics are subscribed to:
- `CommonTopic.CONSUMER_CLEAN_DATA` -> Handled by `CleanDataMessageHandler`
- `WonderConfigurationTopics.WONDER_CONFIGURATION_UPDATED` -> Handled by `WonderConfigurationUpdatedMessageHandler`
- `CorporateOrderClientTopics.CORPORATE_ORDER_CLIENT_CHANGED` -> Handled by `CorporateOrderClientChangedMessageHandler`
- `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC` -> Handled by `CourierSupplyHealthTriggerMessageHandler` (using a specific TTL-based cache window)
- `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_PERIODICALLY_TOPIC` -> Handled by `CourierSupplyHealthTriggerPeriodicallyMessageHandler`

### Consumer Group & Concurrency Configuration
- **Consumer Concurrency:** Configured using the system constant `CommonConfig.CONCURRENCY`.
- **Max Poll Configuration:** Polling thresholds are restricted through `kafka().maxPoll(CommonConfig.MAX_POLL_SIZE, CommonConfig.MAX_POLL_BYTES)`.
- **Blocking Loops:** No blocking `poll()` statements are in user code. Message processing is dispatched to a background thread pool managed by the framework's async `Executor`.

---

## 5. Caching Strategy

The service utilizes a Redis-based cache framework (`core.framework.cache.Cache<T>`) to limit database reads on non-volatile configuration and cache downstream responses.

### Registered Cache Regions & TTL configurations
Registered within `CartModule.registerCache()`:
- `DeliveryFeeConfigurationCache.class` (TTL: 12 hours)
- `ServiceFeeConfigurationCache.class` (TTL: 12 hours)
- `HDRFastPassConfigurationCache.class` (TTL: 12 hours)
- `HDRTipConfigurationCache.class` (TTL: 12 hours)
- `HDRTipConfigurationHardcodedValueCache.class` (TTL: 12 hours)
- `CorporateOrderFeeAndDiscountCache.class` (TTL: 12 hours)
- `WonderSpotServiceFeeConfigurationCache.class` (TTL: 12 hours)
- `CustomerCache.class` (TTL: 10 minutes)
- `CourierSupplyHealthRequestMessage.Pickup.class` (TTL: 1 day)
- `CustomerAddressCache.class` (TTL: 1 day)
- `RestaurantCache.class` (TTL: 1 day)

### Caching Mechanisms and Key Formats
1. **Deduplication and Idempotency:**
   - In `CourierSupplyHealthTriggerMessageHandler`, direct `Redis` client calls prevent duplicate supply triggers:
     - Deduplication key: `"courier:supply:health:trigger:" + ownerId`
     - Periodical deduplication key: `"courier_supply:user:" + ownerId`
2. **Cache-Aside & Load-on-Miss:**
   - Managed config services like `HDRTipConfigurationCacheService` retrieve configurations using key structures:
     - Global: `"GLOBAL_HDR_TIP_CONFIGURATION_CACHE"`
     - Per HDR: `"HDR_TIP_CONFIGURATION_CACHE:" + hdrId`
     - If not cached, the cache loader calls external RPCs like `hdrTipConfigurationWebService.getGlobalTipConfiguration()`.
3. **Reactive Invalidation/Eviction:**
   - Cache invalidation is decoupled and handled asynchronously. When configuration settings update, `WonderConfigurationUpdatedMessageHandler` receives the `WonderConfigurationUpdatedMessage` over Kafka and invokes `refreshCache(...)` to overwrite the cached setting with the newest values.

### Graceful Degradation
The cache-aside loading pattern naturally falls back to downstream database or HTTP calls on a cache miss. However, a major Redis outage will cause significant latency degradation and could overwhelm the downstream `wonder-setting-service` and core databases.

---

## 6. Resilience Configuration

The service defines resilience properties using the custom configuration client `CLBConfig`.

### External SRE Review & Visibility Metadata
The client registration module (`APIClientModule`) defines rigid operational parameters:
- **SRE Stability Review Tag:** `@StabilityReview(ticket = "I-12345", reviewers = {"sre-team", "architect-team"})`
- **Escalation Routing Metadata:** `@EscalationPath(owner = "cart-team", channels = {"#cart-alerts", "pagerduty-cart-team"})`
- **Best Practice Assertion:** `@BestPracticeReminder(reminder = "All external API clients must have timeouts and fallbacks configured.")`

### HTTP Client Connect & Read Timeouts
Most client bindings specify an explicit timeout of `5000 milliseconds` (5 seconds) using `CLBConfig` options:
- e.g., `config(CLBConfig.class).client(PublishedRestaurantWebService.class, url, Timeout.MILLISECOND_5000);`

### Fallbacks & Circuit Breakers
Only Back Office HTTP clients have explicit circuit breaker configurations with fallback implementations:
- `GoogleBotPublishedMenuItemWebServiceFallback` (bound to `PublishedMenuItemWebService` clients on Google Bot route).
- `GoogleBotPublishedBundleItemWebServiceFallback` (bound to `PublishedBundleItemWebService` clients on Google Bot route).

### Missing Timeouts & Risk Vectors (Critical Concern)
Despite the `@BestPracticeReminder` asserting that "All external API clients must have timeouts configured", static code analysis reveals **critical omissions**:
- **PriceWebService:** The client bean `PriceWebService.class` (which handles order/pricing calculation) is registered with `api().client(...)` instead of `CLBConfig`, completely bypassing timeout and retry overrides:
  ```java
  api().client(PriceWebService.class, orderServiceURL);
  ```
- **Blue Apron Product Web Services:** Both `BACycleItemWebService.class` and `BAGlobalSettingWebService.class` also omit `CLBConfig` definitions:
  ```java
  api().client(BACycleItemWebService.class, blueApronProductServiceURL);
  ```
These omissions present a risk. A slow response from `order-service` (PriceWebService) or `blueapron-product-service` will tie up server worker threads in `wonder-cart-service`, potentially leading to thread pool exhaustion.

---

## 7. External Dependencies

The service serves as a coordinator on the critical path, interacting with various core services synchronously:

### Synchronous (Blocking) External APIs
- `restaurant-service-v2` (for published menu items, bundle items, and restaurants)
- `customer-service` (for customer details, addresses, memberships, credit cards, and visitors)
- `order-service` (for price calculations via `PriceWebService`)
- `marketing-service` (for promotion codes and membership products)
- `wonder-setting-service` (for tip configurations, service fees, and general configurations)
- `customer-wallet-service` (for wallet details)
- `search-service` (for query lookups)
- `kitchen-management-service` (for HDR status)

### Asynchronous (Non-Blocking) External APIs
- `wonder-app-notification-service` / `wonder-web-notification-service` (queried periodically during syncs to fetch list of active users)
- `blueapron-product-service` (product attributes and settings)

---

## 8. Scalability Signals

### JVM In-Process Semaphore Disabled (Replica Safety Factor)
In early implementations, the service used `java.util.concurrent.Semaphore` inside `SemaphoreExecutorUtil` to limit parallel executions of the high-frequency courier supply health trigger tasks to `app.semaphore.permits` (set to `5`).
- **Replica Risk:** Local JVM Semaphores are not replica-safe. Across `N` horizontal replicas, the total concurrency becomes `5 * N`, which risks overwhelming downstream services.
- **Code Safeguard:** The current implementation has disabled the local semaphore completely. The initialization code outputs:
  ```
  Local semaphore is disabled. A distributed semaphore is needed to safely control concurrency across multiple replicas. Concurrency limit is currently unbounded.
  ```
  While this avoids JVM lock leakage and replica multiplier errors, the task execution queue is now completely unbounded, creating a risk of high CPU and memory pressure under extreme transaction bursts.

### Horizontal Scaling Gating via Redis Lock Deduplication
Because scheduling is decoupled using Kafka message consumption, the service can scale horizontally without task duplications. Multiple replicas will consume different partitions. For critical cron-like periodic tasks (e.g. syncing active users periodically), horizontal duplicates are prevented using a short-term Redis deduplication key (`"courier_supply:user:" + userId` with a 1-minute TTL).

### Full Statelessness
The application remains completely stateless. No operational data is stored on local disk or inside static memory, ensuring that nodes can be torn down and scaled dynamically.

---

## Key Scalability Signals (Top Concerns)

1. **Full Table Scans on `carts` and `web_carts` during Cleanup Jobs (Critical Database Vector)**
   The `CleanDataMessageHandler` issues monthly batch delete lookups using `updated_time <= ?` conditions. However, the schema definitions for `carts`, `web_carts`, and `cart_carry_over_histories` **do not have an index** on `updated_time`. This causes MySQL to execute massive Full Table Scans under load, spiking DB utilization and causing severe lock contention.

2. **Omitted Timeouts on Critical Price Calculation Dependency (Cascading Failure Risk)**
   `PriceWebService` is bound to the `order-service` using a raw `api().client()` definition. It lacks the 5-second `CLBConfig` connect/read timeout and circuit breaker fallback applied to other clients. If `order-service` runs slow, incoming user checkout/cart updates will block worker threads, threatening thread pool exhaustion.

3. **Unbounded Task Queue due to Disabled Concurrency Semaphore**
   Because JVM Semaphores are not replica-safe, the local semaphore inside `SemaphoreExecutorUtil` has been disabled. However, without a distributed Redis semaphore to replace it, concurrency limits for the heavy reactive `CourierSupplyHealthTriggerMessageHandler` tasks are now completely unbounded, exposing the service to CPU and memory saturation under high-concurrency event spikes.

4. **Memory Pressure on Mass Online User Periodical Syncs**
   `CourierSupplyHealthTriggerPeriodicallyMessageHandler` pulls the entire list of active users from notification services synchronously, and loops over them sequentially in a single step. Under extreme user concurrency, handling very large collections of active user IDs in memory can lead to GC pauses and memory exhaustion.
