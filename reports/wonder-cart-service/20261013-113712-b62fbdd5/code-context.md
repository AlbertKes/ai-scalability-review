# Service Code Analysis — wonder-cart-service

This document provides a structured business context summary and technical analysis of **wonder-cart-service**, compiled for the purpose of a scalability review. All findings are derived from a factual review of the source code.

---

## 1. API Surface

The service exposes a rich set of REST endpoints categorized by client platform (Mobile App, Web, Point of Purchase (POP), and Back Office (BO)), and consumes asynchronous Kafka messages to trigger asynchronous processing and periodic background tasks.

### REST Endpoints
All endpoints are synchronous and form part of the user-facing critical path (except Back Office endpoints).

#### `AppCartWebService` (Mobile App Client, prefix `/cart`)
*   `POST /cart` - Create a new mobile cart.
*   `DELETE /cart` - Delete a mobile cart.
*   `DELETE /v2/cart` - Delete a mobile cart with a concurrent modification check.
*   `DELETE /cart/delete-all` - Empty a cart (delete all items).
*   `PUT /cart/address` - Update cart delivery address.
*   `PUT /cart/payment` - Update cart payment information (card ID, payment method).
*   `PUT /cart/utensils` - Update utensil preference (`needUtensils`).
*   `PUT /V2/cart/tip` - Update tip amount (`UpdateWonderCartTipRequestV2`).
*   `PUT /cart/donation` - Update donation preference (`has_donation`).
*   `GET /cart/summary` - Get mobile cart price summary and counts.
*   `PUT /cart` - Search and list carts matching criteria.
*   `PUT /v2/cart/get` - Get full details of the mobile cart.
*   `GET /cart/check` - Check cart status and validity.
*   `PUT /cart/transfer` - Transfer/merge cart when a visitor registers or logs in.
*   `PUT /cart/clean-address` - Clear cart delivery address.
*   `GET /cart/address/:ownerId` - Get cart delivery address by owner ID.
*   `PUT /cart/promotion` - Apply or update a promotion code on the cart.
*   `PUT /cart/hdr` - Update Heated Delivery Restaurant (HDR) or address ID.
*   `GET /cart/brand-category` - Get brand categories in the cart.
*   `GET /cart/hdr` - Get cart Heated Delivery Restaurant (HDR) info.
*   `GET /cart/restaurant` - Get restaurant information for the cart.
*   `PUT /cart/fast-pass` - Update fast-pass selection.
*   `PUT /cart/schedule-time-slot` - Update scheduled delivery time slot.
*   `PUT /cart/carry-over` - Execute cart carry-over (transfer items between slots/restaurants).
*   `PUT /cart/carry-over/preview` - Preview cart carry-over changes.
*   `PUT /cart/clean-time-slot` - Clear delivery schedule time slot.
*   `POST /cart/record-reorder-cart` - Record a reorder action to duplicate a past cart.
*   `PUT /cart/basic-info` - Update basic cart properties.
*   `PUT /cart/inventory-key/get` - Retrieve inventory keys associated with the cart.

#### `WonderCartItemWebService` (App Cart Item Management, prefix `/cart/item`)
*   `POST /cart/item` - Add an item to the mobile cart.
*   `POST /cart/item/reorder/batch-add` - Batch add items to the cart for a reorder.
*   `GET /cart/item/:itemId` - Retrieve specific cart item details.
*   `PUT /cart/item/:itemId` - Update specific cart item options/quantity.
*   `DELETE /cart/item/:itemId` - Delete an item from the mobile cart.
*   `PUT /cart/item/batch-delete` - Batch delete multiple items from the cart.
*   `PUT /cart/quantity` - Update cart item quantity.

#### `WebCartWebService` (Web Client, prefix `/web-cart`)
*   `POST /web-cart` - Create a new web cart.
*   `DELETE /web-cart` - Delete a web cart.
*   `DELETE /web-cart/delete-with-concurrent-check` - Delete a web cart with concurrent check.
*   `DELETE /web-cart/delete-all` - Empty a web cart.
*   `PUT /web-cart/address` - Update web cart delivery address.
*   `PUT /web-cart/payment` - Update web cart payment information.
*   `PUT /web-cart/utensils` - Update utensil preference on web cart.
*   `PUT /web-cart/tip` - Update tip amount on web cart.
*   `PUT /web-cart/donation` - Update donation preference on web cart.
*   `GET /web-cart/summary` - Get web cart price summary.
*   `PUT /web-cart` - Search and list web carts.
*   `PUT /v2/web-cart/get` - Get full details of the web cart.
*   `GET /web-cart/restaurant` - Get web cart restaurant information.
*   `GET /web-cart/check` - Check web cart status and validity.
*   `PUT /web-cart/transfer` - Transfer/merge web cart.
*   `PUT /web-cart/clean-address` - Clear web cart delivery address.
*   `GET /web-cart/address/:ownerId` - Get web cart delivery address by owner ID.
*   `PUT /web-cart/promotion` - Apply or update a promotion code on web cart.
*   `PUT /web-cart/hdr` - Update Heated Delivery Restaurant (HDR) info on web cart.
*   `PUT /web-cart/hdr-and-address` - Update HDR or address ID on web cart.
*   `GET /web-cart/hdr` - Get Heated Delivery Restaurant (HDR) info of web cart.
*   `GET /web-cart/brand-category` - Get web cart brand category info.
*   `PUT /web-cart/fast-pass` - Update fast-pass selection on web cart.
*   `GET /web-cart/restaurant-id` - Get restaurant IDs associated with the web cart.
*   `PUT /web-cart/schedule-time-slot` - Update web cart delivery schedule time slot.
*   `PUT /web-cart/carry-over` - Execute web cart carry-over.
*   `PUT /web-cart/carry-over/preview` - Preview web cart carry-over changes.

#### `WebCartItemWebService` (Web Cart Item Management, prefix `/web-cart/item`)
*   `POST /web-cart/item` - Add an item to the web cart.
*   `GET /web-cart/item/:itemId` - Retrieve specific web cart item details.
*   `PUT /web-cart/item/:itemId` - Update specific web cart item options/quantity.
*   `DELETE /web-cart/item/:itemId` - Delete an item from the web cart.
*   `PUT /web-cart/item/batch-delete` - Batch delete multiple items from the web cart.
*   `PUT /web-cart/item/quantity` - Update web cart item quantity.

#### `POPCartWebService` (POP Client, prefix `/pop/cart`)
*   `POST /pop/cart` - Create a new POP cart.
*   `DELETE /pop/cart` - Delete a POP cart.
*   `DELETE /pop/cart/concurrent-check` - Delete a POP cart with concurrent check.
*   `DELETE /pop/cart/delete-all` - Empty a POP cart.
*   `PUT /pop/cart/utensils` - Update utensil preference on POP cart.
*   `GET /pop/cart/summary` - Get POP cart price summary.
*   `PUT /pop/cart/refresh` - Refresh POP cart details.
*   `GET /pop/cart` - Get POP cart details.
*   `GET /pop/cart/hdr` - Get POP cart Heated Delivery Restaurant (HDR) info.
*   `PUT /pop/cart/promotion` - Apply promotion code on POP cart.
*   `PUT /pop/cart/phone` - Update customer phone number on POP cart.

#### `POPCartItemWebService` (POP Cart Item Management, prefix `/pop/cart/item`)
*   `POST /pop/cart/item` - Add an item to the POP cart.
*   `GET /pop/cart/item/:itemId` - Retrieve specific POP cart item details.
*   `PUT /pop/cart/item/:itemId` - Update specific POP cart item options/quantity.
*   `DELETE /pop/cart/item/:itemId` - Delete an item from the POP cart.
*   `PUT /pop/cart/item/batch-delete` - Batch delete multiple items from the POP cart.
*   `PUT /pop/cart/quantity` - Update POP cart item quantity.

#### `BOWonderCartWebService` (Back Office, prefix `/bo/cart`)
*   `GET /bo/cart` - Retrieve detailed App cart details for Back Office operators.

#### `BOWebCartWebService` (Back Office, prefix `/bo/web-cart`)
*   `GET /bo/web-cart` - Retrieve detailed Web cart details for Back Office operators.

### Asynchronous Tasks & Message Consumers
The service does not contain internal scheduler configurations (such as `@Scheduled` annotations). Periodic tasks are driven entirely by incoming Kafka messages:

1.  **`CleanDataMessageHandler`**
    *   **Trigger:** Consumes `ConsumerCleanDataMessage` from `consumer-clean-data` topic.
    *   **Action:** Runs low-priority, batch-oriented SQL cleanup queries to delete carts, items, carry-over histories, and reorder records older than 1 month.
2.  **`CourierSupplyHealthTriggerMessageHandler`**
    *   **Trigger:** Consumes `CourierSupplyHealthTriggerMessage` from `courier-supply-health-trigger-message` topic.
    *   **Action:** Reactively triggers an immediate evaluation of the user's cart against courier capacities, fetching details from replica databases and external web services and fanning out results to the downstream delivery supply service.
3.  **`CourierSupplyHealthTriggerPeriodicallyMessageHandler`**
    *   **Trigger:** Consumes `CourierSupplyHealthTriggerPeriodicallyMessage` from `courier-supply-health-trigger-periodically-message` topic.
    *   **Action:** Calls notification services to retrieve lists of active online users (app and web), and fans out trigger messages sequentially into Kafka for each online user to check their courier supply status.

---

## 2. Database Access Patterns

The application interacts with a relational MySQL database via `core.framework`'s generic persistence layer (`Repository<T>`) and direct JDBC executions (`Database` bean).

### Tables Accessed
*   **App Cart:** `carts`, `cart_items`, `cart_menu_item_options`, `cart_bundle_choices`, `cart_bundle_choice_options`.
*   **Web Cart:** `web_carts`, `web_cart_items`, `web_cart_menu_item_options`, `web_cart_bundle_choices`, `web_cart_bundle_choice_options`.
*   **POP Cart:** `pop_carts`, `pop_cart_items`, `pop_cart_menu_item_options`, `pop_cart_bundle_choices`, `pop_cart_bundle_choice_options`.
*   **Maintenance Logs:** `cart_carry_over_histories`, `reorder_cart_created_records`.

### Query Types
*   **Select Queries:** Predominantly simple CRUD queries on individual tables via primary key `id` or uniquely indexed `owner_id`. No standard SQL relational `JOIN` queries are defined in the service.
*   **Batch Delete Queries:** Implemented in `CleanDataMessageHandler` using raw SQL queries to fetch IDs older than a timestamp and execute batch deletions:
    ```sql
    select id from web_carts where updated_time <= ? limit ?
    delete from web_carts where id in (...)
    ```

### Indexes & Schema Structures
All cart tables are configured under the `InnoDB` storage engine.
*   **Primary Keys:** Explicitly defined on `id` across all tables.
*   **Unique Keys:** Unique indices exist on `owner_id` for main tables:
    *   `carts` -> `UNIQUE INDEX uk_carts_owner_id (owner_id)`
    *   `web_carts` -> `UNIQUE INDEX uk_web_carts_owner_id (owner_id)`
    *   `pop_carts` -> `UNIQUE INDEX uk_carts_owner_id (owner_id)`
*   **Foreign Keys:** Parent-child associations (e.g. searching options by `cart_item_id`) do not have database-enforced foreign keys or corresponding indices defined in the SQL migration files, which could cause full table scans on child tables in production.

### Database Connection and Replica Configuration
*   **Primary Connection:** Configured via `sys.jdbc.url`. An explicit query execution timeout of 5 seconds is configured: `db().timeout(Duration.ofSeconds(5))`.
*   **Replica Connection:** Configured via `sys.jdbc.replicaUrl` and bound to `@Named("replicaDB")`. Read-only, low-priority operations (specifically the `CourierSupplyHealthTriggerMessageHandler` and underlying data fetchers) are annotated with `@Named("replicaDB")` to offload primary database traffic.
*   **Connection Pool:** The application does not specify connection pool parameters (such as minimum/maximum pool sizes or connection idle limits) in its properties files, relying entirely on the underlying `core.framework` defaults.
*   **Slow SQL Suppression:** The codebase systematically calls `Database.suppressSlowSQLWarning(true)` before executing multiple transactional queries in cart-related services, restoring it via `Database.suppressSlowSQLWarning(false)` afterward. This masks slow queries from automated system alerts.

---

## 3. Kafka Usage

Kafka is utilized as the primary mechanism for decoupled, asynchronous messaging and background job triggering.

### Kafka Configuration
*   **Broker Address:** Configured via `sys.kafka.uri`.
*   **Consumer Control:** The flag `app.enableKafkaConsumer` allows disabling consumer subscriptions completely in development/local environments (`app.enableKafkaConsumer=false`).
*   **Polling & Concurrency:** Concurrency is configured in the module via `kafka().concurrency(CommonConfig.CONCURRENCY)`. Max poll limits are explicitly set: `kafka().maxPoll(CommonConfig.MAX_POLL_SIZE, CommonConfig.MAX_POLL_BYTES)`.
*   **Blocking poll loops:** None visible. Message deserialization and polling are handled entirely by `core.framework`.

### Topics Produced
*   `delivery-courier-supply-health-check` (via `DeliverySupplyTopic.DELIVERY_COURIER_SUPPLY_HEALTH_CHECK`): Sends computed courier supply requests (`CourierSupplyHealthRequestMessage`) downstream.
*   `consumer-clean-data` (via `CommonTopic.CONSUMER_CLEAN_DATA`): Registered as a fallback publisher to bypass framework setup validation errors.

### Topics Consumed
*   `consumer-clean-data` (via `CommonTopic.CONSUMER_CLEAN_DATA`) -> `CleanDataMessageHandler`
*   `wonder-configuration-updated` (via `WonderConfigurationTopics.WONDER_CONFIGURATION_UPDATED`) -> `WonderConfigurationUpdatedMessageHandler`
*   `corporate-order-client-changed` (via `CorporateOrderClientTopics.CORPORATE_ORDER_CLIENT_CHANGED`) -> `CorporateOrderClientChangedMessageHandler`
*   `courier-supply-health-trigger-message` (via `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC`) -> `CourierSupplyHealthTriggerMessageHandler`
*   `courier-supply-health-trigger-periodically-message` (via `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_PERIODICALLY_TOPIC`) -> `CourierSupplyHealthTriggerPeriodicallyMessageHandler`

---

## 4. Caching Strategy

The service implements a multi-tiered caching approach using Redis as the centralized cache provider.

### Cache Regions & TTL Configuration
The following cache regions are registered at startup in `CartModule.java`:
1.  `DeliveryFeeConfigurationCache` (12 Hours) - Delivery fee tables.
2.  `ServiceFeeConfigurationCache` (12 Hours) - General service fee parameters.
3.  `HDRFastPassConfigurationCache` (12 Hours) - Fast pass availability/configs.
4.  `HDRTipConfigurationCache` (12 Hours) - Tip suggestion configs.
5.  `HDRTipConfigurationHardcodedValueCache` (12 Hours) - Tip hardcoded fallback rules.
6.  `CorporateOrderFeeAndDiscountCache` (12 Hours) - Fees/discounts for corporate customers.
7.  `WonderSpotServiceFeeConfigurationCache` (12 Hours) - Service fees specific to locations.
8.  `CustomerCache` (10 Minutes) - Customer details (phone, membership status, name).
9.  `CourierSupplyHealthRequestMessage.Pickup` (1 Day) - Pickup location address data.
10. `CustomerAddressCache` (1 Day) - User delivery address details.
11. `RestaurantCache` (1 Day) - Restaurant and Brand name mapping.

### Caching Patterns
*   **Cache-Aside Pattern:** Implemented in `CourierSupplyHealthDataFetcher` via `cache.get(key, loader)`. Misses fallback to a synchronous API call to external web services, and the results are populated back into Redis.
*   **Reactive Invalidation:** Local cache is evicted reactively when `WonderConfigurationUpdatedMessage` or `CorporateOrderClientChangedMessage` are consumed, ensuring near-instant configuration updates.
*   **Idempotency & Rate Throttling:** Direct Redis key-value operations are used in Kafka handlers to prevent concurrent processing of the same user.
    *   Throttles rapid triggers using a short-lived Redis key: `courier:supply:health:trigger:<ownerId>` with custom expiration (`app.courierSupplyHealthTriggerCache.durationOfSeconds` which defaults to 10 seconds).
    *   Throttles periodic sweeps using `courier_supply:user:<ownerId>` with a 1-minute expiration.
*   **Graceful Degradation:** The service relies on Redis as a hard dependency. A Redis connection failure or crash will result in exceptions thrown during cart lookups and Kafka message processing.

---

## 5. Resilience Configuration

The service utilizes a combination of standard `core.framework` client bindings and a custom wrapper layer `lib.clb.CLBConfig` (Circuit Breaker Client) to configure outbound REST clients.

### Resilience Implementations
*   **Explicit Connect/Read Timeout & Fallback:**
    Only configured on Back Office GoogleBot restaurant integrations:
    *   `PublishedMenuItemWebService` -> Timeout: 5,000 milliseconds (`Timeout.MILLISECOND_5000`), fallback: `GoogleBotPublishedMenuItemWebServiceFallback`.
    *   `PublishedBundleItemWebService` -> Timeout: 5,000 milliseconds (`Timeout.MILLISECOND_5000`), fallback: `GoogleBotPublishedBundleItemWebServiceFallback`.
*   **Implicit Timeout Settings (Major Risk):**
    All other outbound clients are initialized via standard `config(CLBConfig.class).client(interface, url)` or `api().client(interface, url)` calls.
    *   No connect/read timeouts are explicitly set in code.
    *   No custom fallback handlers or circuit breakers are configured on critical, user-facing synchronous calls.
    *   The service relies entirely on unknown implicit defaults within `CLBConfig` or the host container, making it highly susceptible to thread/resource pool exhaustion if a downstream system suffers from high latency or slow responses.

---

## 6. External Dependencies

The `wonder-cart-service` plays a central coordination role and makes synchronous HTTP calls to numerous downstream services to construct and price cart structures.

### Critical Path Dependencies (User-Facing / Synchronous)
*   **`restaurant-service-v2`** (via `PublishedRestaurantWebService`, `PublishedMenuItemWebService`, `PublishedBundleItemWebService`) - Retrieves restaurant brand details, menus, and item templates.
*   **`customer-service`** (via `AliasUserWebService`, `CustomerWebService`, `CustomerAddressWebService`, `VisitorWebService`, `VisitorAddressWebService`, `CustomerCreditCardWebService`, `CustomerMembershipWebService`) - Retrieves customer profiles, address geolocations, and memberships.
*   **`order-service`** (via `PriceWebService`) - Calculates cart price.
*   **`wonder-setting-service`** (via `ConfigurationWebService`, `HDRFeeConfigurationWebService`, `HDRTipConfigurationWebService`, `CorporateOrderClientWebService`, `WonderSpotFeeConfigurationWebService`, `FeeConfigurationWebService`) - Retrieves active service fees, taxes, corporate client mappings, and tips.
*   **`customer-wallet-service`** (via `CustomerWalletWebService`) - Syncs customer credits and wallet balances.
*   **`marketing-service`** (via `PromotionWebService`, `MembershipProductWebService`, `SegmentWebService`) - Validates promotion codes and eligibility.
*   **`search-service`** (via `SearchWebService`) - Performs search indexing of cart views.
*   **`kitchen-management-service`** (via `HDRWebService`) - Coordinates Heated Delivery Restaurant configs.

### Asynchronous/Background Dependencies (Kafka Consumer Path)
*   **`wonder-app-notification-service`** (via `AppOnlineUserWebService`) - Fetches online app users.
*   **`wonder-web-notification-service`** (via `WebOnlineUserWebService`) - Fetches online web users.
*   **`blueapron-product-service`** (via `BACycleItemWebService`, `BAGlobalSettingWebService`) - Retrieves Blue Apron product data.

---

## 7. Scalability Signals

*   **Stateless Execution:** The service contains no local disk writes or in-memory operational states. Carts are entirely backed by the database and cached in Redis. The service layer is horizontally scalable.
*   **Decoupled Triggers:** Background periodic workflows (such as online user scans) are triggered via fanned-out Kafka messages rather than in-process cron schedulers, allowing even distribution across nodes.
*   **JVM-Local Semaphore Concurrency Constraint (Not Replica-Safe):**
    `SemaphoreExecutorUtil` uses standard `java.util.concurrent.Semaphore` initialized via `app.semaphore.permits` (configured as `5` in dev).
    *   This semaphore throttles the concurrency of `CourierSupplyHealthTriggerMessageHandler` reactively.
    *   Because it resides in the JVM's local memory, scaling the service to $N$ replicas multiplies the concurrency ceiling to $5 \times N$. If the intent of the semaphore is to protect downstream systems (like `DeliverySupplyService`) from overload, horizontal scaling breaks this capacity guardrail.
*   **Un-batched Sequential Outbound HTTP Loop:**
    In `CourierSupplyHealthDataFetcher.fetchRestaurantInfos(Set<String> restaurantIds)`, the code loops through the list of restaurant IDs. If there are multiple restaurant IDs in the set and they are not cached, it performs synchronous, blocking HTTP calls sequentially:
    ```java
    for (String restaurantId : restaurantIds) {
        RestaurantCache cacheObj = restaurantCache.get(cacheKey, k -> {
            ...
            GetPublishedRestaurantBaseInfoResponse info = publishedRestaurantWebService.getBaseInfo(restaurantId, request);
            ...
        });
    }
    ```
    On a cold cache, a cart containing items from $M$ different restaurants will trigger $M$ sequential outbound HTTP queries, inflating transaction latency.
*   **Suppression of Telemetry Signals:**
    Systematic use of `Database.suppressSlowSQLWarning(true)` inside primary read/write methods prevents the framework from logging slow database queries. This blind spot hides potential bottlenecks from SRE automated warning engines.

---

## Key Scalability Signals

1.  **Non-Replica-Safe Concurrency Throttle:** The use of JVM-local `Semaphore` instances in `SemaphoreExecutorUtil` means concurrency limits scale linearly with the number of replicas ($N \times \text{limit}$). This defeats the purpose of downstream overload protection when scaling out. A distributed semaphore (e.g. Redisson/Redis-based) is required.
2.  **Systematic Suppression of Slow SQL Warnings:** The extensive use of `Database.suppressSlowSQLWarning(true)` across App, Web, and POP cart services hides slow relational queries from framework diagnostics and automated SRE telemetry tools, risking silent performance degradation under load.
3.  **Missing Explicit Connect/Read Timeouts on Downstream REST Clients:** Apart from Back Office GoogleBot clients, no other external REST interfaces configure explicit timeouts or circuit breakers. The service is highly vulnerable to cascading thread pool exhaustion if any core dependency (such as `restaurant-service` or `customer-service`) exhibits high latency.
4.  **Sequential HTTP Query Loop in Cache Misses:** The `CourierSupplyHealthDataFetcher` executes outbound blocking REST calls sequentially within a loop on cache misses. For multiple distinct un-cached restaurant IDs, this introduces an $O(M)$ sequential delay that blocks message consumption.
5.  **Missing Database Indexes on Foreign Key Columns:** Child tables such as `cart_items` and `cart_menu_item_options` lack indices on foreign key join columns (e.g. `cart_id`, `owner_id`), causing expensive table scans on cart load/update flows.
