# Service Code Analysis — wonder-cart-service

This document provides a factual and detailed technical summary of the `wonder-cart-service`, including its interface and database access patterns, extracted directly from the source code for the purpose of a scalability review.

---

## 1. API Surface

The service exposes REST endpoints for managing shopping carts across multiple client interfaces (App, Web, POP, Back Office). It also consumes and processes Kafka messages.

### REST Endpoints
Primarily synchronous, user-facing endpoints are located on the critical path of cart modifications and checkout.

#### A. Mobile App Clients (`AppCartWebService` & `WonderCartItemWebService`)
- `POST /cart`: Creates a new app cart.
- `DELETE /cart`: Deletes an app cart.
- `DELETE /v2/cart`: Deletes an app cart with concurrent checks.
- `DELETE /cart/delete-all`: Empties all items in the cart.
- `PUT /cart/address`: Updates the cart's delivery address ID.
- `PUT /cart/payment`: Updates the credit card and payment method.
- `PUT /cart/utensils`: Updates utensil selection preferences.
- `PUT /V2/cart/tip`: Updates the tips structure (type, value, amount).
- `PUT /cart/donation`: Updates donation flag status.
- `GET /cart/summary`: Retrieves cart summary information.
- `PUT /cart`: Searches and fetches cart details.
- `PUT /v2/cart/get`: Gets detailed cart response.
- `GET /cart/check`: Checks the cart's overall status.
- `PUT /cart/transfer`: Transfers cart from visitor/guest to a registered user.
- `PUT /cart/clean-address`: Clears the delivery address.
- `GET /cart/address/:ownerId`: Retrieves delivery address for a given owner ID.
- `PUT /cart/promotion`: Applies or updates a promotion code.
- `PUT /cart/hdr`: Updates Heaters/Delivery Restaurant (HDR) identifier or address ID.
- `GET /cart/brand-category`: Retrieves categories of brands in the cart.
- `GET /cart/hdr`: Retrieves HDR info for the cart.
- `GET /cart/restaurant`: Retrieves restaurant information.
- `PUT /cart/fast-pass`: Updates fast-pass selection.
- `PUT /cart/schedule-time-slot`: Updates the selected schedule delivery time slot.
- `PUT /cart/carry-over`: Carries over cart items from a prior session.
- `PUT /cart/carry-over/preview`: Previews cart carry-over items.
- `PUT /cart/clean-time-slot`: Clears the scheduled delivery time slot.
- `POST /cart/record-reorder-cart`: Records a cart reorder event.
- `PUT /cart/basic-info`: Updates basic cart info.
- `PUT /cart/inventory-key/get`: Generates/retrieves an inventory key.
- `POST /cart/item`: Adds an item to the app cart.
- `POST /cart/item/reorder/batch-add`: Adds a batch of items from a previous order.
- `GET /cart/item/:itemId`: Retrieves details for a specific item.
- `PUT /cart/item/:itemId`: Updates customization options or quantity for a cart item.
- `DELETE /cart/item/:itemId`: Deletes a specific cart item.
- `PUT /cart/item/batch-delete`: Batch deletes multiple items.
- `PUT /cart/quantity`: Updates cart item quantity.

#### B. Web Clients (`WebCartWebService` & `WebCartItemWebService`)
- `POST /web-cart`: Creates a web cart.
- `DELETE /web-cart`: Deletes a web cart.
- `DELETE /web-cart/delete-with-concurrent-check`: Deletes with concurrent check.
- `DELETE /web-cart/delete-all`: Empties all items.
- `PUT /web-cart/address`: Updates delivery address.
- `PUT /web-cart/payment`: Updates payment information.
- `PUT /web-cart/utensils`: Updates utensil preferences.
- `PUT /web-cart/tip`: Updates tip amount.
- `PUT /web-cart/donation`: Updates donation choice.
- `GET /web-cart/summary`: Retrieves web cart summary.
- `PUT /web-cart`: Searches and fetches cart details.
- `PUT /v2/web-cart/get`: Gets detailed web cart response.
- `GET /web-cart/restaurant`: Retrieves restaurant details.
- `GET /web-cart/check`: Checks web cart status.
- `PUT /web-cart/transfer`: Transfers guest web cart.
- `PUT /web-cart/clean-address`: Clears web cart address.
- `GET /web-cart/address/:ownerId`: Gets delivery address.
- `PUT /web-cart/promotion`: Applies a promo code.
- `PUT /web-cart/hdr`: Updates web cart HDR info.
- `PUT /web-cart/hdr-and-address`: Updates web cart HDR and address IDs.
- `GET /web-cart/hdr`: Retrieves HDR info.
- `GET /web-cart/brand-category`: Retrieves brand categories.
- `PUT /web-cart/fast-pass`: Updates fast pass selection.
- `GET /web-cart/restaurant-id`: Retrieves associated restaurant ID.
- `PUT /web-cart/schedule-time-slot`: Updates delivery scheduling slot.
- `PUT /web-cart/carry-over`: Carries over web cart items.
- `PUT /web-cart/carry-over/preview`: Previews carry over.
- `POST /web-cart/item`: Adds an item.
- `GET /web-cart/item/:itemId`: Retrieves item details.
- `PUT /web-cart/item/:itemId`: Updates item options.
- `DELETE /web-cart/item/:itemId`: Deletes a specific item.
- `PUT /web-cart/item/batch-delete`: Batch deletes items.
- `PUT /web-cart/item/quantity`: Updates quantity.

#### C. POP Clients (`POPCartWebService` & `POPCartItemWebService`)
- `POST /pop/cart`: Creates a POP cart.
- `DELETE /pop/cart`: Deletes a POP cart.
- `DELETE /pop/cart/concurrent-check`: Deletes with concurrent check.
- `DELETE /pop/cart/delete-all`: Empties POP cart.
- `PUT /pop/cart/utensils`: Updates utensil choices.
- `GET /pop/cart/summary`: Retrieves summary.
- `PUT /pop/cart/refresh`: Refreshes POP cart.
- `GET /pop/cart`: Retrieves POP cart.
- `GET /pop/cart/hdr`: Gets associated HDR info.
- `PUT /pop/cart/promotion`: Applies promo codes.
- `PUT /pop/cart/phone`: Updates POP cart contact phone number.
- `POST /pop/cart/item`: Adds a POP cart item.
- `GET /pop/cart/item/:itemId`: Retrieves POP item.
- `PUT /pop/cart/item/:itemId`: Updates POP item.
- `DELETE /pop/cart/item/:itemId`: Deletes POP item.
- `PUT /pop/cart/item/batch-delete`: Batch deletes items.
- `PUT /pop/cart/quantity`: Updates POP item quantity.

#### D. Back Office Clients (`BOWonderCartWebService` & `BOWebCartWebService`)
- `GET /bo/cart`: Fetches app cart details for Back Office operators.
- `GET /bo/web-cart`: Fetches web cart details for Back Office operators.

---

### Message Consumers & Asynchronous/Batch Jobs
The service has no JVM-level `@Scheduled` background runners. Decoupled batch and periodic workloads are driven entirely via incoming Kafka messages:

1. **`CleanDataMessageHandler`**
   - **Trigger Topic:** `CommonTopic.CONSUMER_CLEAN_DATA` (`consumer-clean-data`)
   - **Action:** Runs asynchronous batch cleanups on a primary master database, purging records older than 1 month from `carts`, `cart_items`, and carry-over history tables.
   - **Frequency:** Driven by periodic messaging scheduled outside the JVM.

2. **`CourierSupplyHealthTriggerPeriodicallyMessageHandler`**
   - **Trigger Topic:** `courier-supply-health-trigger-periodically-message`
   - **Action:** Gathers list of online active users from external notification service APIs (`AppOnlineUserWebService` and `WebOnlineUserWebService`) and self-publishes a `CourierSupplyHealthTriggerMessage` for every active user.
   - **Frequency:** External periodic cron.

3. **`CourierSupplyHealthTriggerMessageHandler`**
   - **Trigger Topic:** `courier-supply-health-trigger-message`
   - **Action:** Real-time reactive processing. Compiles extensive cart contents, customer metadata, and pickup location details to publish `CourierSupplyHealthRequestMessage` for downstream supply engine consumption.

---

## 2. Database Access Patterns

The application connects to a MySQL database configured in `sys.properties`.

### Tables Accessed & Query Types
- **Tables:** `carts`, `cart_items`, `web_carts`, `web_cart_items`, `pop_carts`, `pop_cart_items`, `cart_bundle_item_choices`, `cart_bundle_item_choice_options`, `cart_menu_item_options`, `web_cart_bundle_item_choices`, `web_cart_bundle_item_choice_options`, `web_cart_menu_item_options`, `pop_cart_bundle_item_choices`, `pop_cart_menu_item_options`, `cart_carry_over_histories`, and `reorder_cart_created_records`.
- **Primary / Index Queries:**
  - `carts` is queried via `owner_id = ?` (Unique Index `uk_carts_owner_id`) or primary key `id`.
  - `cart_items` is queried using `cart_id = ?` (covered by index `ix_cart_items_cart_id_updated_time`) or `owner_id = ? AND restaurant_id = ?` (Index `ix_cart_items_owner_id_restaurant_id`).
- **Data Mutation Queries:** Row-level single updates and inserts for user cart items, customized choices, and scheduling.
- **Batch Deletions:** Executed inside `CleanDataMessageHandler` to purge records older than a month.

### Read Replicas
- Read replica configuration is specified under `sys.jdbc.replicaUrl`.
- Performance-critical read operations on background paths—specifically within `CourierSupplyHealthTriggerMessageHandler`—explicitly inject and query read replica repository beans (`@Inject @Named("replicaDB") Repository<Cart>...`) to divert read load away from the primary write master.

### Connection Pool Configuration
- There is **no explicit configuration** of database connection pool sizing, idle limits, or wait timeouts in `app.properties` or `sys.properties`. The application relies completely on core.framework's default JDBC configuration.

### N+1 or Unbounded Queries
- In `CourierSupplyHealthTriggerMessageHandler`, multiple separate queries are executed sequentially to fetch options and choices (`dataHelper.cartBundleItemChoiceMap(...)`, `dataHelper.cartBundleItemChoiceOptionMap(...)`, `dataHelper.cartItemOptionMap(...)`) for a single `ownerId`. While bulk loaded per owner, they result in consecutive database hits for each health trigger.
- Unbounded queries or table scans are prevented as queries are strictly filtered by unique indices or mapped parent IDs.

---

## 3. Kafka Usage

### Kafka Topics Consumed From
- `consumer-clean-data` (from `CommonTopic.CONSUMER_CLEAN_DATA`)
- `wonder-configuration-updated` (from `WonderConfigurationTopics.WONDER_CONFIGURATION_UPDATED`)
- `corporate-order-client-changed` (from `CorporateOrderClientTopics.CORPORATE_ORDER_CLIENT_CHANGED`)
- `courier-supply-health-trigger-message` (from `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC`)
- `courier-supply-health-trigger-periodically-message` (from `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_PERIODICALLY_TOPIC`)

### Kafka Topics Produced To
- `consumer-clean-data` (during app initialization)
- `delivery-courier-supply-health-check` (from `DeliverySupplyTopic.DELIVERY_COURIER_SUPPLY_HEALTH_CHECK`): Publishes health requests containing user cart payloads.

### Consumer Group and Concurrency Configuration
- Max poll thresholds and concurrency settings are configured via framework setup:
  - `kafka().maxPoll(CommonConfig.MAX_POLL_SIZE, CommonConfig.MAX_POLL_BYTES)`
  - `kafka().concurrency(CommonConfig.CONCURRENCY)`
- The real-time trigger message consumer concurrency is governed by a local in-process semaphore thread pool:
  - `SemaphoreExecutorUtil.submitWithSemaphoreFromConfig` (permits controlled by `app.semaphore.permits` in `app.properties` which defaults to `5`).

### Blocking Poll Loops
- N/A. Polling and partition thread allocation loops are completely encapsulated by the framework.

---

## 4. Caching Strategy

The service leverages a Redis cluster configured via `sys.redis.host` and standard core.framework cache regions.

### Cache Regions, TTLs, and Eviction Policies
Cache regions are registered during application startup in `CartModule.java`:

| Cache Region Class | TTL (Time-To-Live) | Eviction / Load Policy |
| :--- | :--- | :--- |
| `DeliveryFeeConfigurationCache` | **12 Hours** | Cache-Aside (`cache.get(key, loader)`) |
| `ServiceFeeConfigurationCache` | **12 Hours** | Cache-Aside |
| `HDRFastPassConfigurationCache` | **12 Hours** | Cache-Aside |
| `HDRTipConfigurationCache` | **12 Hours** | Cache-Aside |
| `HDRTipConfigurationHardcodedValueCache` | **12 Hours** | Cache-Aside |
| `CorporateOrderFeeAndDiscountCache` | **12 Hours** | Evicted explicitly via `CorporateOrderClientChangedMessageHandler` |
| `WonderSpotServiceFeeConfigurationCache` | **12 Hours** | Cache-Aside |
| `CustomerCache` | **10 Minutes** | Cache-Aside (queries `CustomerWebService`) |
| `CourierSupplyHealthRequestMessage.Pickup` | **1 Day (24h)** | Cache-Aside (queries `HDRWebService`) |
| `CustomerAddressCache` | **1 Day (24h)** | Cache-Aside (queries `CustomerAddressWebService`) |
| `RestaurantCache` | **1 Day (24h)** | Cache-Aside (queries `PublishedRestaurantWebService`) |

### Direct Key-Value Rate Limiting (Idempotency)
Direct Redis key manipulation is utilized in the message handlers to enforce rate-limiting:
- `courier:supply:health:trigger:%s` (TTL = `app.courierSupplyHealthTriggerCache.durationOfSeconds` defaults to **10 seconds**). This enforces de-duplication of supply check requests.
- `CourierSupplyHealthTriggerPeriodicallyMessageHandler.USER_KEY + ownerId` (TTL = **1 minute**). Prevents redundant evaluations for active users during periodic cycles.

### Graceful Degradation
All cache regions implement a "cache-aside" pattern with a supplier fallback block. On a cache miss, the system transparently fetches up-to-date data from MySQL or the respective HTTP API client. However, if the Redis cache is entirely offline, the service is vulnerable to cascading latency spikes and downstream API thread pool exhaustion.

---

## 5. Resilience Configuration

Resilience limits are handled in `APIClientModule.java` via the `CLBConfig` framework.

### Configured Circuit Breakers, Retries, and Timeouts
Explicit configurations only exist for GoogleBot-oriented checkout fallbacks:
- `GoogleBotPublishedMenuItemWebService` & `GoogleBotPublishedBundleItemWebService` (targeting head-of-line search crawls) are bound with an explicit connect/read timeout of **5000 milliseconds** (`Timeout.MILLISECOND_5000`) and fallbacks `GoogleBotPublishedMenuItemWebServiceFallback` and `GoogleBotPublishedBundleItemWebServiceFallback`.

### Missing Resilience and Default Cascade Risks
- All remaining critical downstream API clients (including `PublishedRestaurantWebService`, `CustomerWebService`, `CustomerAddressWebService`, `SearchWebService`, etc.) are declared via simple URL bindings `config(CLBConfig.class).client(..., serviceURL)` in `APIClientModule.java`.
- There are **no explicit read or connect timeout configurations**, nor any explicit circuit breakers or retries defined for these clients. They rely implicitly on library/framework defaults, presenting an operational risk of system-wide thread pool exhaustion if a critical downstream dependency slows down.

---

## 6. External Dependencies

The application acts as a central cart-orchestrator and performs synchronous downstream calls during the checkout and preparation lifecycle.

### Downstream HTTP Clients (CLB/API Client Beans)
All clients are configured dynamically during initialization:
- `HDRWebService` (`app.kitchenManagement.serviceURL`)
- `PublishedRestaurantWebService` (`app.consumer.restaurant.serviceURL`)
- `PublishedMenuItemWebService` (`app.consumer.restaurant.serviceURL`)
- `PublishedBundleItemWebService` (`app.consumer.restaurant.serviceURL`)
- `AliasUserWebService` (`app.consumer.customer.serviceURL`)
- `CustomerWebService` (`app.consumer.customer.serviceURL`)
- `CustomerAddressWebService` (`app.consumer.customer.serviceURL`)
- `VisitorWebService` (`app.consumer.customer.serviceURL`)
- `VisitorAddressWebService` (`app.consumer.customer.serviceURL`)
- `CustomerCreditCardWebService` (`app.consumer.customer.serviceURL`)
- `CustomerMembershipWebService` (`app.consumer.customer.serviceURL`)
- `WebOnlineUserWebService` (`app.consumer.webOnlineUser.serviceURL`)
- `AppOnlineUserWebService` (`app.consumer.appOnlineUser.serviceURL`)
- `PriceWebService` (`app.order.serviceURL`)
- `ConfigurationWebService` (`app.consumer.wonderSetting.serviceURL`)
- `HDRFeeConfigurationWebService` (`app.consumer.wonderSetting.serviceURL`)
- `HDRTipConfigurationWebService` (`app.consumer.wonderSetting.serviceURL`)
- `CorporateOrderClientWebService` (`app.consumer.wonderSetting.serviceURL`)
- `WonderSpotFeeConfigurationWebService` (`app.consumer.wonderSetting.serviceURL`)
- `FeeConfigurationWebService` (`app.consumer.wonderSetting.serviceURL`)
- `CustomerWalletWebService` (`app.consumer.customerWallet.serviceURL`)
- `PromotionWebService` (`app.consumer.marketing.serviceURL`)
- `MembershipProductWebService` (`app.consumer.marketing.serviceURL`)
- `SegmentWebService` (`app.consumer.marketing.serviceURL`)
- `SearchWebService` (`app.consumer.search.serviceURL`)
- `BACycleItemWebService` (`app.blueApronProduct.serviceURL`)
- `BAGlobalSettingWebService` (`app.blueApronProduct.serviceURL`)

### Timeout Settings
- Except for GoogleBot crawler fallback APIs, **no explicit timeouts** are set for these outbound HTTP connections in the application.

---

## 7. Scalability Signals

### JVM-Local Concurrency Enforcement
- `SemaphoreExecutorUtil` uses `java.util.concurrent.Semaphore`, which is a local, in-memory concurrency construct.
- It is utilized inside `CourierSupplyHealthTriggerMessageHandler` to enforce a limit on trigger evaluations via the property `app.semaphore.permits` (set to `5` in dev).
- **Scale-Out Violation:** This semaphore is not replica-safe. If the service is scaled horizontally to `N` replicas, the actual global concurrency limit becomes `5 * N`. If the semaphore's intent is to shield downstream APIs or databases from query surges, this protection breaks under horizontal scaling.

### Stateful Operations
- The application layers are stateless: persistent states are stored in MySQL, and cached configurations and tokens are in Redis. Both components are replica-safe.

### Scheduled Task Replica Safety
- The periodic data purge (`CleanDataMessageHandler`) is triggered via a standard Kafka message subscription. Because only a single worker instance in the Kafka consumer group receives the message, it is guarded against concurrent execution across multiple replicas.

---

## Key Scalability Signals

1. **Non-Replica-Safe Semaphore for Throttling**:
   The use of an in-memory `java.util.concurrent.Semaphore` inside `SemaphoreExecutorUtil` to govern `CourierSupplyHealthTriggerMessageHandler` concurrency is a critical flaw for horizontal scalability. Scaling the service to `N` instances scales the concurrent trigger evaluation limit to `permits * N`, which can potentially bypass downstream overload protections and flood dependent services.

2. **Sequential Blocking HTTP Requests (N+1 Fetching)**:
   In `CourierSupplyHealthDataFetcher.fetchRestaurantInfos`, cache misses for multiple restaurant IDs are resolved by executing synchronous, blocking downstream requests sequentially inside a `for`-loop (`publishedRestaurantWebService.getBaseInfo`). This sequential HTTP execution path is a significant bottleneck under high trigger loads.

3. **Absence of Explicit Timeouts and Circuit Breakers**:
   The vast majority of external outbound clients (e.g. customer, search, order, pricing, wallet, and settings services) are registered without explicit connect/read timeouts or circuit breakers inside the codebase. In the event of latency spikes in any critical downstream dependency, container threads can easily become blocked, leading to cascade failures.

4. **Multiple Consecutive Database Hits Per Courier Trigger**:
   For every reactive courier supply health event, the handler performs multiple consecutive database read queries to compile bundle item options and customizable choices for the owner. At high real-time transaction volumes, this behavior results in a high query-per-second (QPS) amplification factor.
