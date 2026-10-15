# Service Code Analysis — Business Context Extraction: wonder-cart-service

## 1. API Surface

The `wonder-cart-service` application exposes REST endpoints organized across eight web service interfaces and two custom controller routes. The service also registers message handlers consuming Kafka topics.

### REST Endpoints

#### 1. App Cart Web Service (`AppCartWebService` -> `WonderAppCartWebServiceImpl`)
Handles cart operations for the native mobile consumer application (iOS and Android).

| HTTP Method | Path | Method Name | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/cart` | `create` | Creates a new cart for a user or visitor. |
| `DELETE` | `/cart` | `delete` | Deletes a cart by owner ID and cart ID. |
| `DELETE` | `/v2/cart` | `deleteWithConcurrentCheck` | Deletes a cart verifying owner ID and cart ID match in a single atomic transaction. |
| `DELETE` | `/cart/delete-all` | `deleteAllCart` | Deletes all carts and item records across the entire database. |
| `PUT` | `/cart/address` | `updateAddress` | Updates delivery address and HDR ID on a cart. |
| `PUT` | `/cart/payment` | `updatePayment` | Updates payment method associated with the cart. |
| `PUT` | `/cart/utensils` | `updateUtensils` | Updates utensil requirement flag on the cart. |
| `PUT` | `/V2/cart/tip` | `updateTipV2` | Updates tip percentage or custom tip amount on the cart. |
| `PUT` | `/cart/donation` | `updateDonation` | Updates round-up donation selection on the cart. |
| `GET` | `/cart/summary` | `summary` | Retrieves a calculated pricing summary for the cart. |
| `PUT` | `/cart` | `search` | Searches carts with pagination (`skip`, `limit`) and HDR filters. |
| `PUT` | `/v2/cart/get` | `getV2` | Retrieves complete cart data with menu details, options, pricing, and promotions. |
| `GET` | `/cart/check` | `check` | Validates cart state and item availability. |
| `PUT` | `/cart/transfer` | `transfer` | Transfers cart ownership (e.g. from visitor to registered customer). |
| `PUT` | `/cart/clean-address` | `cleanAddress` | Clears delivery address from a cart. |
| `GET` | `/cart/address/:ownerId` | `getCartAddress` | Retrieves the stored cart delivery address for an owner. |
| `PUT` | `/cart/promotion` | `updatePromotion` | Applies or removes promotion code on the cart. |
| `PUT` | `/cart/hdr` | `updateCartHDROrAddressId` | Updates HDR facility ID and delivery address ID. |
| `GET` | `/cart/brand-category` | `getBrandCategory` | Retrieves brand category associated with the cart. |
| `GET` | `/cart/hdr` | `getHDR` | Retrieves HDR facility metadata associated with the cart. |
| `GET` | `/cart/restaurant` | `getRestaurant` | Retrieves restaurant metadata for restaurants present in the cart. |
| `PUT` | `/cart/fast-pass` | `updateFastPass` | Updates fast-pass selection flag on the cart. |
| `PUT` | `/cart/schedule-time-slot` | `updateScheduleTimeSlot` | Updates scheduled delivery time slot on the cart. |
| `PUT` | `/cart/carry-over` | `carryOver` | Carries over items from an existing cart to a new slot or service day. |
| `PUT` | `/cart/carry-over/preview`| `carryOverPreview` | Previews the impact of carrying over a cart. |
| `PUT` | `/cart/clean-time-slot` | `cleanTimeSlot` | Clears scheduled delivery time slot from the cart. |
| `POST` | `/cart/record-reorder-cart`| `recordReorderCart` | Records reorder event tracking for cart recreation. |
| `PUT` | `/cart/basic-info` | `updateCartBasicInfo` | Updates basic cart attributes. |
| `PUT` | `/cart/inventory-key/get` | `getInventoryKey` | Retrieves inventory validation keys for cart items. |

#### 2. App Cart Item Web Service (`WonderCartItemWebService` -> `WonderCartItemWebServiceImpl`)
Handles individual cart item and customization mutations for the native mobile consumer application.

| HTTP Method | Path | Method Name | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/cart/item` | `addItem` | Adds a menu item or bundle item to the cart. |
| `POST` | `/cart/item/reorder/batch-add` | `batchAddItems` | Batch adds multiple items into a cart during reorder flows. |
| `GET` | `/cart/item/:itemId` | `getItem` | Retrieves detailed customizations and options for a specific cart item. |
| `PUT` | `/cart/item/:itemId` | `updateItem` | Updates options, choices, or special instructions on an item. |
| `DELETE` | `/cart/item/:itemId` | `deleteItem` | Removes an item from the cart. |
| `PUT` | `/cart/item/batch-delete` | `batchDeleteItems` | Deletes multiple cart items in a single request. |
| `PUT` | `/cart/quantity` | `updateQuantity` | Updates quantity for multiple items in the cart. |

#### 3. Web Cart Web Service (`WebCartWebService` -> `WebCartWebServiceImpl`)
Handles cart operations for the web platform frontend application.

| HTTP Method | Path | Method Name | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/web-cart` | `create` | Creates a new web cart. |
| `DELETE` | `/web-cart` | `delete` | Deletes a web cart. |
| `DELETE` | `/web-cart/delete-with-concurrent-check` | `deleteWithConcurrentCheck` | Deletes web cart verifying owner and cart ID atomically. |
| `DELETE` | `/web-cart/delete-all` | `deleteAllCart` | Deletes all web carts and item records across the database. |
| `PUT` | `/web-cart/address` | `updateAddress` | Updates delivery address on a web cart. |
| `PUT` | `/web-cart/payment` | `updatePayment` | Updates payment method on a web cart. |
| `PUT` | `/web-cart/utensils` | `updateUtensils` | Updates utensil requirement flag on a web cart. |
| `PUT` | `/web-cart/tip` | `updateTip` | Updates tip percentage or custom tip on a web cart. |
| `PUT` | `/web-cart/donation` | `updateDonation` | Updates round-up donation on a web cart. |
| `GET` | `/web-cart/summary` | `summary` | Retrieves calculated pricing summary for a web cart. |
| `PUT` | `/web-cart` | `search` | Searches web carts with pagination (`skip`, `limit`). |
| `PUT` | `/v2/web-cart/get` | `getV2` | Retrieves complete web cart data with full item and pricing context. |
| `GET` | `/web-cart/restaurant` | `getCartRestaurant` | Retrieves restaurant metadata for web cart items. |
| `GET` | `/web-cart/check` | `check` | Validates web cart items and restaurant availability. |
| `PUT` | `/web-cart/transfer` | `transfer` | Transfers web cart ownership from visitor to registered customer. |
| `PUT` | `/web-cart/clean-address` | `cleanAddress` | Clears delivery address from web cart. |
| `GET` | `/web-cart/address/:ownerId` | `getCartAddress` | Retrieves stored web cart address for an owner. |
| `PUT` | `/web-cart/promotion` | `updatePromotion` | Applies or removes promotion code on web cart. |
| `PUT` | `/web-cart/hdr` | `updateCartHDR` | Updates HDR facility ID on web cart. |
| `PUT` | `/web-cart/hdr-and-address` | `updateHDROrAddressId` | Updates HDR facility ID and delivery address on web cart. |
| `GET` | `/web-cart/hdr` | `getHDR` | Retrieves HDR metadata for web cart. |
| `GET` | `/web-cart/brand-category` | `getBrandCategory` | Retrieves brand category for web cart. |
| `PUT` | `/web-cart/fast-pass` | `updateFastPass` | Updates fast-pass flag on web cart. |
| `GET` | `/web-cart/restaurant-id` | `getRestaurantIds` | Retrieves distinct restaurant IDs present in web cart. |
| `PUT` | `/web-cart/schedule-time-slot` | `updateScheduleTimeSlot` | Updates scheduled delivery time slot on web cart. |
| `PUT` | `/web-cart/carry-over` | `carryOver` | Carries over items in a web cart to a new slot. |
| `PUT` | `/web-cart/carry-over/preview` | `carryOverPreview` | Previews carry-over impact on web cart items. |

#### 4. Web Cart Item Web Service (`WebCartItemWebService` -> `WebCartItemWebServiceImpl`)
Handles item mutations for the web platform frontend application.

| HTTP Method | Path | Method Name | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/web-cart/item` | `addItem` | Adds a menu item or bundle item to web cart. |
| `GET` | `/web-cart/item/:itemId` | `getItem` | Retrieves detailed customizations for a web cart item. |
| `PUT` | `/web-cart/item/:itemId` | `updateItem` | Updates options, choices, or instructions on a web cart item. |
| `DELETE` | `/web-cart/item/:itemId` | `deleteItem` | Removes an item from web cart. |
| `PUT` | `/web-cart/item/batch-delete` | `batchDeleteItems` | Batch deletes multiple items from web cart. |
| `PUT` | `/web-cart/item/quantity` | `updateQuantity` | Updates quantity of items in web cart. |

#### 5. Point-of-Purchase Cart Web Service (`POPCartWebService` -> `POPCartWebServiceImpl`)
Handles cart operations for physical Point-of-Purchase (kiosks and in-person terminals).

| HTTP Method | Path | Method Name | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/pop/cart` | `create` | Creates a new POP cart. |
| `DELETE` | `/pop/cart` | `delete` | Deletes a POP cart. |
| `DELETE` | `/pop/cart/concurrent-check` | `deleteWithConcurrentCheck` | Deletes a POP cart verifying owner and cart ID atomically. |
| `DELETE` | `/pop/cart/delete-all` | `deleteAllCart` | Deletes all POP carts and item records across the database. |
| `PUT` | `/pop/cart/utensils` | `updateUtensils` | Updates utensil flag on POP cart. |
| `GET` | `/pop/cart/summary` | `summary` | Retrieves pricing summary for POP cart. |
| `PUT` | `/pop/cart/refresh` | `refresh` | Refreshes POP cart state and pricing. |
| `GET` | `/pop/cart` | `get` | Retrieves full POP cart details. |
| `GET` | `/pop/cart/hdr` | `getHDR` | Retrieves HDR facility metadata for POP cart. |
| `PUT` | `/pop/cart/promotion` | `updatePromotion` | Updates promotion code on POP cart. |
| `PUT` | `/pop/cart/phone` | `updatePhone` | Updates customer phone number on POP cart. |

#### 6. Point-of-Purchase Cart Item Web Service (`POPCartItemWebService` -> `POPCartItemWebServiceImpl`)
Handles item mutations for physical Point-of-Purchase carts.

| HTTP Method | Path | Method Name | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/pop/cart/item` | `addItem` | Adds an item to POP cart. |
| `GET` | `/pop/cart/item/:itemId` | `getItem` | Retrieves single item details from POP cart. |
| `PUT` | `/pop/cart/item/:itemId` | `updateItem` | Updates options on a POP cart item. |
| `DELETE` | `/pop/cart/item/:itemId` | `deleteItem` | Removes an item from POP cart. |
| `PUT` | `/pop/cart/item/batch-delete` | `batchDeleteItems` | Batch deletes items from POP cart. |
| `PUT` | `/pop/cart/quantity` | `updateQuantity` | Updates quantity of items in POP cart. |

#### 7. Back Office Mobile Cart Web Service (`BOWonderCartWebService` -> `BOWonderCartWebServiceImpl`)
Exposes back-office retrieval of mobile cart state.

| HTTP Method | Path | Method Name | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/bo/cart` | `get` | Retrieves mobile cart state for back-office administrators. |

#### 8. Back Office Web Cart Web Service (`BOWebCartWebService` -> `BOWebCartWebServiceImpl`)
Exposes back-office retrieval of web cart state.

| HTTP Method | Path | Method Name | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/bo/web-cart` | `get` | Retrieves web cart state for back-office administrators. |

#### 9. Custom Controller Routes
Direct HTTP routes registered in module initialization.

| HTTP Method | Path | Controller Class | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/app/migrate-cart-bundle-item-choice` | `MigrateCartBundleItemChoiceController` | Triggers asynchronous background database migration for bundle item choices. |
| `GET` | `/liveness-probe` | `PodLivenessProbeController` | Kubernetes pod liveness health check probe. |

### Critical Path Endpoints

The following synchronous, user-facing endpoints sit directly on the critical path of the consumer checkout and ordering journey:
- **Full Cart Retrieval & Pricing Calculation:**
  - `PUT /v2/cart/get`
  - `PUT /v2/web-cart/get`
  - `GET /cart/summary`
  - `GET /web-cart/summary`
  - `GET /pop/cart`
  - `GET /pop/cart/summary`
  *Impact:* Invoked every time a consumer opens the cart screen or modifies cart contents. These endpoints perform multi-table database queries across cart headers, items, customization options, and bundle selections, while orchestrating synchronous outbound RPC calls to `price-service`, `promotion-service`, `restaurant-service-v2`, `customer-service`, and `wonder-setting-service`.
- **Item Add & Mutation Endpoints:**
  - `POST /cart/item`, `PUT /cart/item/:itemId`, `DELETE /cart/item/:itemId`, `PUT /cart/quantity`
  - `POST /web-cart/item`, `PUT /web-cart/item/:itemId`, `DELETE /web-cart/item/:itemId`, `PUT /web-cart/item/quantity`
  - `POST /pop/cart/item`, `PUT /pop/cart/item/:itemId`, `DELETE /pop/cart/item/:itemId`, `PUT /pop/cart/quantity`
  *Impact:* Synchronously validate item availability with upstream restaurant catalogs, perform relational database operations within transactions, and fire downstream `CartSyncInterceptor` logic to publish async Kafka sync events.
- **Checkout Metadata Mutations:**
  - `PUT /cart/address`, `PUT /cart/payment`, `PUT /cart/tip`, `PUT /cart/promotion`, `PUT /cart/schedule-time-slot`, `PUT /cart/fast-pass` (and web equivalents).
  *Impact:* Synchronously invoked during the checkout funnel steps before order placement.

### Batch, Scheduled Jobs, and Message Consumers

- **No Application Cron / In-Process `@Scheduled` Timers:**
  The application code does not declare scheduled cron tasks via framework schedulers (except for a 30-second host IP refresh job registered inside the client-side load balancing library: `clb:refresh-all-host-ip`).
- **Kafka-Driven Periodic & Batch Processing:**
  1. `CleanDataMessageHandler` (Topic: `CommonTopic.CONSUMER_CLEAN_DATA`):
     - Triggered externally by scheduled Kafka messages.
     - Spawns four background executor tasks (`clean-app-cart`, `clean-web-cart`, `clean-cart-carry-over-history`, `clean-reorder-cart-created-records`) that execute unbounded `while(true)` loops deleting expired carts (older than one month) in batches of 100 rows.
  2. `CourierSupplyHealthTriggerPeriodicallyMessageHandler` (Topic: `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_PERIODICALLY_TOPIC`):
     - Triggered periodically via Kafka.
     - Invokes external notification web services (`appOnlineUserWebService.getOnlineUsers()` and `webOnlineUserWebService.getOnlineUsers()`), then iterates in background executor threads over all active online users, checking Redis and publishing Kafka trigger messages per user.
  3. `CourierSupplyHealthTriggerMessageHandler` (Topic: `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC`):
     - Consumes cart mutation trigger events, acquires a semaphore permit, queries the read replica database, fetches menu/restaurant details, and emits delivery supply check messages.
  4. Ad-Hoc Migration Route (`POST /app/migrate-cart-bundle-item-choice`):
     - Batch iterates over all `cart_bundle_item_choices` rows using `skip(skip).limit(500)` pagination, querying `publishedMenuItemWebService` and executing `batchUpsert`.

---

## 2. Database Access Patterns

### MySQL Access

The service uses relational MySQL as its primary persistent store. Two database connections are configured in `CartServiceApp.java`: the primary database (`db()`) and a read replica (`db("replicaDB")`).

#### Tables Accessed and Entity Mappings

The service accesses 17 active entity tables and 2 database views:

| Table Name | Entity Class | Access Types | Key Columns / Filtering |
| :--- | :--- | :--- | :--- |
| `carts` | `Cart` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `UNIQUE KEY (owner_id)` |
| `cart_items` | `CartItem` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_id, updated_time)`, `INDEX (owner_id, restaurant_id)` |
| `cart_menu_item_options` | `CartMenuItemOption` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_item_id)`, `INDEX (owner_id)`, `INDEX (cart_id)` |
| `cart_bundle_item_choices` | `CartBundleItemChoice` | `SELECT`, `INSERT`, `UPDATE`, `DELETE`, `BATCH UPSERT` | `PRIMARY KEY (id)`, `INDEX (cart_item_id)`, `INDEX (owner_id)`, `INDEX (cart_id)` |
| `cart_bundle_item_choice_options` | `CartBundleItemChoiceOption` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_bundle_item_choice_id)`, `INDEX (cart_item_id)`, `INDEX (owner_id)`, `INDEX (cart_id)` |
| `reorder_cart_created_records` | `ReorderCartCreatedRecord` | `SELECT`, `INSERT`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (ix_created_time)` |
| `cart_carry_over_histories` | `CartCarryOverHistory` | `SELECT`, `INSERT`, `DELETE` | `PRIMARY KEY (id)` |
| `web_carts` | `WebCart` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `UNIQUE KEY (owner_id)` |
| `web_cart_items` | `WebCartItem` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_id, updated_time)`, `INDEX (owner_id, restaurant_id)` |
| `web_cart_menu_item_options` | `WebCartMenuItemOption` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_item_id)`, `INDEX (owner_id)`, `INDEX (cart_id)` |
| `web_cart_bundle_item_choices` | `WebCartBundleItemChoice` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_item_id)`, `INDEX (owner_id)`, `INDEX (cart_id)` |
| `web_cart_bundle_item_choice_options` | `WebCartBundleItemChoiceOption` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_bundle_item_choice_id)`, `INDEX (cart_item_id)`, `INDEX (owner_id)`, `INDEX (cart_id)` |
| `pop_carts` | `POPCart` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `UNIQUE KEY (owner_id)` |
| `pop_cart_items` | `POPCartItem` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_id, updated_time)`, `INDEX (owner_id, restaurant_id)` |
| `pop_cart_menu_item_options` | `POPCartMenuItemOption` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_item_id)`, `INDEX (owner_id)`, `INDEX (cart_id)` |
| `pop_cart_bundle_item_choices` | `POPCartBundleItemChoice` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_item_id)`, `INDEX (owner_id)`, `INDEX (cart_id)` |
| `pop_cart_bundle_item_choice_options` | `POPCartBundleItemChoiceOption` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `PRIMARY KEY (id)`, `INDEX (cart_bundle_item_choice_id)`, `INDEX (cart_item_id)`, `INDEX (owner_id)`, `INDEX (cart_id)` |

*Database Views:*
- `SearchCartView`: Used by `CartService.search()` to project joined aggregation results.
- `SearchWebCartView`: Used by `WebCartService.search()` to project joined aggregation results.

#### Large Scans and JOINs Without Indexed Filters

1. **Cart Search Queries (`CartService.java`, `WebCartService.java`):**
   ```sql
   SELECT cart.owner_id, cart.cart_type, cart.id cart_id, cart_item.restaurant_id, COUNT(cart_item.id) item_count, cart.hdr_id, cart.dining_option 
   FROM carts cart 
   LEFT JOIN cart_items cart_item ON cart.id = cart_item.cart_id 
   WHERE cart_item.restaurant_id IS NOT NULL 
   GROUP BY cart.owner_id, cart.id, cart_item.restaurant_id 
   ORDER BY cart.created_time ASC 
   LIMIT ?, ?;
   ```
   - **Index Analysis:** Neither `carts` nor `web_carts` has an index on `created_time`. The `ORDER BY cart.created_time ASC` combined with `LEFT JOIN` and `GROUP BY` forces MySQL to generate a temporary table and execute an unindexed filesort across the table.
   - **Slow SQL Suppression:** The application code explicitly suppresses framework performance warnings around this query via `Database.suppressSlowSQLWarning(true)` and `Database.suppressSlowSQLWarning(false)`.
   - **Unbounded Count:** Prior to the query, `response.total = cartRepository.select().count()` executes a full table `SELECT COUNT(*)` on `carts` / `web_carts`.
2. **Item Ordering in Cart Context Hydration (`CartContextService.java`, `WebCartContextService.java`):**
   ```java
   Database.suppressSlowSQLWarning(true);
   List<CartItem> items = cartItemRepository.select("cart_id=? ORDER BY created_time ASC", cart.id);
   var cartBundleItemChoices = cartBundleItemChoiceRepository.select("cart_id=? ORDER BY seq ASC", cart.id);
   var cartBundleItemChoiceOptions = cartBundleItemChoiceOptionRepository.select("cart_id=? ORDER BY seq ASC", cart.id);
   Database.suppressSlowSQLWarning(false);
   ```
   - **Index Analysis:** The index on `cart_items` is `ix_cart_items_cart_id_updated_time (cart_id, updated_time)`. There is no composite index on `(cart_id, created_time)`. Similarly, `cart_bundle_item_choices` has `(cart_id)` but no index on `(cart_id, seq)`. These orderings force filesorts, requiring explicit slow SQL suppression in application code on the user critical path.
3. **Data Cleanup Full Table Scans (`CleanDataMessageHandler.java`):**
   ```sql
   SELECT id FROM web_carts WHERE updated_time <= ? LIMIT 100;
   SELECT id FROM carts WHERE updated_time <= ? LIMIT 100;
   SELECT id FROM cart_carry_over_histories WHERE updated_time <= ? LIMIT 100;
   ```
   - Neither `carts`, `web_carts`, nor `cart_carry_over_histories` contains an index on `updated_time`.
   - As a result, each iteration of the cleanup loop performs a full table scan against these tables.

#### Connection Pool Configuration

- **Database Timeout:** Explicitly configured in `CartServiceApp.java` as:
  `db().timeout(Duration.ofSeconds(5));`
- **Pool Sizing:** No custom connection pool parameters (`minSize`, `maxSize`, checkout timeouts, idle timeouts) are specified in `sys.properties`, `app.properties`, or application code. The application relies entirely on core-ng default pool settings.
- **Replica Database:** The read replica (`db("replicaDB")`) is initialized with URLs and credentials from `sys.jdbc.replicaUrl`, `sys.jdbc.user`, and `sys.jdbc.password`. It registers entity repositories for `Cart`, `CartItem`, `WebCart`, `WebCartItem`, and option/bundle tables to offload query load from the primary writer.

### MongoDB / Atlas Access

**N/A** — `wonder-cart-service` does not utilize MongoDB or Atlas; all persistence is handled via MySQL and Redis.

### N+1 Query Patterns and Unbounded Queries

1. **Unbounded Total Table Truncation (`deleteAllCart`):**
   In `DeleteCartService.java`, `DeleteWebCartService.java`, and `DeletePOPCartService.java`:
   ```java
   Database.suppressSlowSQLWarning(true);
   try (Transaction transaction = database.beginTransaction()) {
       database.execute(Strings.format("DELETE FROM {}", Cart.class.getAnnotation(Table.class).name()));
       database.execute(Strings.format("DELETE FROM {}", CartItem.class.getAnnotation(Table.class).name()));
       database.execute(Strings.format("DELETE FROM {}", CartMenuItemOption.class.getAnnotation(Table.class).name()));
       database.execute(Strings.format("DELETE FROM {}", CartBundleItemChoice.class.getAnnotation(Table.class).name()));
       database.execute(Strings.format("DELETE FROM {}", CartBundleItemChoiceOption.class.getAnnotation(Table.class).name()));
       transaction.commit();
   }
   Database.suppressSlowSQLWarning(false);
   ```
   Exposed via REST endpoints (`DELETE /cart/delete-all`, `DELETE /web-cart/delete-all`, `DELETE /pop/cart/delete-all`). Executes unconstrained `DELETE FROM <table>` on all core transactional tables in a single transaction, locking all rows and filling undo logs.
2. **Unthrottled Cleanup While Loop (`CleanDataMessageHandler.java`):**
   Runs a non-blocking `while(true)` loop executing `SELECT id ... LIMIT 100` followed by `DELETE FROM ... WHERE cart_id IN (...)` across 5 tables simultaneously for App and Web carts without pauses or sleep intervals, creating heavy sustained I/O load.
3. **Multi-Step Cart Hydration Queries (Grouped Batch Fetches):**
   In `CartContextService` and `WebCartContextService`, the application avoids classic per-item row-by-row N+1 queries by fetching all items for a `cart_id`, collecting all options via `cartItemOptionMap(ownerId, items)`, and grouping in-memory. However, fetching a single cart still requires 5 sequential SQL queries (`carts`, `cart_items`, `cart_bundle_item_choices`, `cart_bundle_item_choice_options`, `cart_menu_item_options`).
4. **Ad-Hoc Migration Deep Offset Scan (`MigrateCartBundleItemChoiceController.java`):**
   Performs pagination with `skip(skip).limit(500)` ordering by `created_time ASC` over `cart_bundle_item_choices`. For large table sizes, high offset values (`OFFSET N`) without indexed pagination cause deep scan overhead.

---

## 3. Kafka Usage

### Topics Produced To

1. `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC` (`courier-supply-health-trigger-message`):
   - Message Bean: `CourierSupplyHealthTriggerMessage`
   - Key: `ownerId`
   - Publishers: `CartSyncPublisherService` (invoked via `CartSyncInterceptor` after cart mutations, and `CourierSupplyHealthTriggerPeriodicallyMessageHandler`).
2. `DeliverySupplyTopic.DELIVERY_COURIER_SUPPLY_HEALTH_CHECK` (`delivery-courier-supply-health-check`):
   - Message Bean: `CourierSupplyHealthRequestMessage`
   - Key: `cart.id`
   - Publisher: `CourierSupplyHealthTriggerMessageHandler`.
3. `CommonTopic.CONSUMER_CLEAN_DATA` (`consumer-clean-data`):
   - Message Bean: `ConsumerCleanDataMessage`
   - Publisher registered in `KafkaConsumerModule.java` (not actively published by service logic; used to satisfy framework initialization requirements).

### Topics Consumed From

| Topic Name / Constant | Message Bean Class | Handler Class | Concurrency | Max Poll |
| :--- | :--- | :--- | :--- | :--- |
| `CommonTopic.CONSUMER_CLEAN_DATA` (`consumer-clean-data`) | `ConsumerCleanDataMessage` | `CleanDataMessageHandler` | `CommonConfig.CONCURRENCY` | `CommonConfig.MAX_POLL_SIZE`, `CommonConfig.MAX_POLL_BYTES` |
| `WonderConfigurationTopics.WONDER_CONFIGURATION_UPDATED` (`wonder-configuration-updated`) | `WonderConfigurationUpdatedMessage` | `WonderConfigurationUpdatedMessageHandler` | `CommonConfig.CONCURRENCY` | `CommonConfig.MAX_POLL_SIZE`, `CommonConfig.MAX_POLL_BYTES` |
| `CorporateOrderClientTopics.CORPORATE_ORDER_CLIENT_CHANGED` (`corporate-order-client-changed`) | `CorporateOrderClientChangedMessage` | `CorporateOrderClientChangedMessageHandler` | `CommonConfig.CONCURRENCY` | `CommonConfig.MAX_POLL_SIZE`, `CommonConfig.MAX_POLL_BYTES` |
| `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_MESSAGE_TOPIC` (`courier-supply-health-trigger-message`) | `CourierSupplyHealthTriggerMessage` | `CourierSupplyHealthTriggerMessageHandler` | `CommonConfig.CONCURRENCY` | `CommonConfig.MAX_POLL_SIZE`, `CommonConfig.MAX_POLL_BYTES` |
| `WonderCartTopics.COURIER_SUPPLY_HEALTH_TRIGGER_PERIODICALLY_TOPIC` (`courier-supply-health-trigger-periodically-message`) | `CourierSupplyHealthTriggerPeriodicallyMessage` | `CourierSupplyHealthTriggerPeriodicallyMessageHandler` | `CommonConfig.CONCURRENCY` | `CommonConfig.MAX_POLL_SIZE`, `CommonConfig.MAX_POLL_BYTES` |

### Consumer Group and Configuration Details

- **Consumer Enablement Switch:**
  The entire Kafka consumer pipeline is guarded by configuration in `KafkaConsumerModule.java`:
  ```java
  if (!Boolean.parseBoolean(requiredProperty("app.enableKafkaConsumer"))) {
      return;
  }
  ```
  In all standard configuration files (`conf/app.properties`, `conf/prod/resources/app.properties`, `src/main/resources/app.properties`), `app.enableKafkaConsumer=false`.
- **Consumer Group ID:**
  Derived automatically by core-ng from `LogManager.APP_NAME` (`wonder-cart-service`).
- **Poll and Concurrency Settings:**
  Configured via `CommonConfig`:
  - `kafka().maxPoll(CommonConfig.MAX_POLL_SIZE, CommonConfig.MAX_POLL_BYTES);`
  - `kafka().concurrency(CommonConfig.CONCURRENCY);`

### Blocking Poll Loops

- **Framework Consumer Polling:**
  The polling loop is abstracted internally by core-ng framework daemon threads. There are no manual or blocking `consumer.poll()` loops in application code.
- **Handler-Level Blocking Operations:**
  - `CourierSupplyHealthTriggerMessageHandler`: Blocks acquiring a permit from `SemaphoreExecutorUtil.submitWithSemaphoreFromConfig(...)` (bounded by `app.semaphore.permits=5`). Tasks executing within the semaphore block synchronously on replica database reads and three outbound HTTP calls (`PublishedBundleItemWebService`, `PublishedMenuItemWebService`, `PublishedRestaurantWebService`).
  - `CourierSupplyHealthTriggerPeriodicallyMessageHandler`: In background executor threads, performs a synchronous `for` loop over all online users, executing blocking `redis.get(USER_KEY + userId)` calls sequentially per user.

---

## 4. Caching Strategy

Caching is implemented through Redis using both core-ng framework typed `Cache<T>` abstractions and direct `Redis` client operations.

### Cache Regions and TTLs

All cache regions are registered in `CartModule.java`:

| Cache Class / Type | Key Format | Configured TTL | Source Loader / Purpose |
| :--- | :--- | :--- | :--- |
| `DeliveryFeeConfigurationCache` | Specific: `DELIVERY_FEE_CONFIGURATION:{brandCategory}:{hdrId}`<br>Global: `GLOBAL_DELIVERY_FEE_CONFIGURATION:{brandCategory}` | 12 Hours (`Duration.ofHours(12)`) | `FeeConfigurationWebService.getDeliveryFeeConfiguration` / `getGlobalDeliveryFeeConfiguration` |
| `ServiceFeeConfigurationCache` | Specific: `SERVICE_FEE_CONFIGURATION:{brandCategory}:{hdrId}`<br>Global: `GLOBAL_SERVICE_FEE_CONFIGURATION:{brandCategory}` | 12 Hours (`Duration.ofHours(12)`) | `FeeConfigurationWebService.getServiceFeeConfiguration` / `getGlobalServiceFeeConfiguration` |
| `HDRFastPassConfigurationCache` | Specific: `HDR_FAST_PASS_CONFIGURATION_CACHE:{hdrId}`<br>Global: `GLOBAL_HDR_FAST_PASS_CONFIGURATION_CACHE` | 12 Hours (`Duration.ofHours(12)`) | `HDRFeeConfigurationWebService.getFastPassConfiguration` |
| `HDRTipConfigurationCache` | Specific: `HDR_TIP_CONFIGURATION_CACHE:{hdrId}`<br>Global: `GLOBAL_HDR_TIP_CONFIGURATION_CACHE` | 12 Hours (`Duration.ofHours(12)`) | `HDRTipConfigurationWebService.getHDRTipConfiguration` / `getGlobalTipConfiguration` |
| `HDRTipConfigurationHardcodedValueCache` | `HDR_TIP_CONFIGURATION_HARDCODED_VALUE_CACHE` | 12 Hours (`Duration.ofHours(12)`) | `HDRTipConfigurationWebService.getGlobalTipConfiguration` |
| `CorporateOrderFeeAndDiscountCache` | `{corporateClientId}` | 12 Hours (`Duration.ofHours(12)`) | `CorporateOrderClientWebService.get` |
| `WonderSpotServiceFeeConfigurationCache` | Specific: `WONDER_SPOT_SERVICE_FEE_CONFIGURATION_CACHE:{corporateClientId}`<br>Global: `GLOBAL_WONDER_SPOT_SERVICE_FEE_CONFIGURATION_CACHE` | 12 Hours (`Duration.ofHours(12)`) | `WonderSpotFeeConfigurationWebService.getConfigurationByCorporate` |
| `CustomerCache` | `{customerId}` | 10 Minutes (`Duration.ofMinutes(10)`) | `CustomerWebService.get` (used in courier supply health checks) |
| `CourierSupplyHealthRequestMessage.Pickup` | `{restaurantId}` | 1 Day (`Duration.ofDays(1)`) | `PublishedRestaurantWebService.get` (pickup location details) |
| `CustomerAddressCache` | `{customerId}:{deliveryAddressId}` | 1 Day (`Duration.ofDays(1)`) | `CustomerAddressWebService.get` (customer coordinates) |
| `RestaurantCache` | `{restaurantId}` | 1 Day (`Duration.ofDays(1)`) | `PublishedRestaurantWebService.get` (restaurant address & brand) |

### Direct Redis Keys (Rate Limiting and De-duplication)

- `courier:supply:health:trigger:{ownerId}`: TTL = 10 seconds (`app.courierSupplyHealthTriggerCache.durationOfSeconds`). Checked and set in `CourierSupplyHealthTriggerMessageHandler` to prevent re-processing cart sync events within a 10-second window.
- `courier_supply:user:{ownerId}`: TTL = 1 minute (`Duration.ofMinutes(1)`). Set upon receiving individual cart events and checked by `CourierSupplyHealthTriggerPeriodicallyMessageHandler` to skip users whose cart was recently evaluated.

### Eviction Policy and Cache Degradation

- **Eviction Strategy:** Managed at the Redis instance level (no eviction policy defined in application code). Active invalidation and cache updates occur via Kafka events:
  - `WonderConfigurationUpdatedMessageHandler` consumes `WONDER_CONFIGURATION_UPDATED` events and calls `refreshCache(...)` to update fee, tip, fast pass, and delivery settings in Redis.
  - `CorporateOrderClientChangedMessageHandler` consumes `CORPORATE_ORDER_CLIENT_CHANGED` events and updates or explicitly evicts corporate discount caches (`cache.evict(...)`).
- **Graceful Degradation on Cache Miss:**
  - **No Fallback / Fail-Open on Remote Error:** When a cache miss occurs (`cache.get(key, loader)`), the service synchronously invokes the loader method calling the external web service. If the external dependency is down, throws an exception or times out, the exception propagates directly to the caller. The service does **not** degrade to cached stale values, default constants, or fallback mocks.
  - **404 NOT_FOUND Handling:** In `DeliveryFeeConfigurationCacheService` and `ServiceFeeConfigurationCacheService`, if a specific HDR configuration returns HTTP 404 (`RemoteServiceException` with code `NOT_FOUND`), the service sets `getFromGlobal = true` and falls back to loading the global configuration.

---

## 5. Resilience Configuration

### Circuit Breakers

- **Zero Circuit Breakers on Critical External Calls:**
  There are **no** circuit-breaker instances (Resilience4j, Sentinel, or similar) configured for any synchronous user-facing outbound HTTP call (`order-service`, `customer-service`, `restaurant-service-v2`, `marketing-service`, `wonder-setting-service`, `customer-wallet-service`).
- **GoogleBot Crawler Fallbacks:**
  In `APIClientModule.java`, fallback proxies are registered via `EnhancedAPIConfig` for GoogleBot crawler clients:
  - `PublishedMenuItemWebService` (`GoogleBotPublishedMenuItemWebServiceFallback`)
  - `PublishedBundleItemWebService` (`GoogleBotPublishedBundleItemWebServiceFallback`)
  *Observable Implementation Limitation:* In `GoogleBotPublishedMenuItemWebServiceFallback`, only `listSortedPublishedMenuItem` is annotated with `@Fallback`; all other methods return `null`. In `GoogleBotPublishedBundleItemWebServiceFallback`, none of the methods are annotated with `@Fallback`. Because `APIInvocationHandler.isFallbackRequired` checks for the presence of the `@Fallback` annotation, failures on non-annotated methods (such as `bulkGet`) will rethrow the exception rather than executing fallback logic.

### Retry Policies

- **Client-Side Load Balancing (`CLBConfig`):**
  Applies to services bound via `config(CLBConfig.class).client(...)`:
  - `maxRetries = 2`
  - `retryWaitTimeInMs = 100` (via `HTTPLoadBalancingInterceptor`, retrying on next resolved host IP).
- **Default Core-NG HTTP Clients:**
  For services using `api().client(...)` directly (`PriceWebService`, `BACycleItemWebService`, `BAGlobalSettingWebService`) and `wonder.internal` endpoints (`HDRWebService`), core-ng applies default retry behavior (up to 3 attempts on idempotent HTTP GET/PUT or connection drop; no retries on HTTP POST).

### Bulkhead Limits

- **In-Process Semaphore (`SemaphoreExecutorUtil`):**
  - Configured with `app.semaphore.permits = 5` (loaded from `app.properties`).
  - Limits concurrent background executions of `courier-supply-health-trigger-event-message`.
  - Implemented via a static in-process `java.util.concurrent.Semaphore`.

### Timeout Values

- **Database Timeout:** `5 seconds` (`db().timeout(Duration.ofSeconds(5))`).
- **CLB HTTP Clients (`CLBConfig`):**
  - Connect Timeout: `2 seconds` (`Duration.ofSeconds(2)`).
  - Read Timeout: `20 seconds` (`Duration.ofSeconds(20)`).
- **Core-NG Direct API Clients:**
  - `PriceWebService` (`order-service`): No explicit timeout configured; inherits core-ng framework defaults (5s connect, up to 60s read).
  - `HDRWebService` (`kitchen-management-service`): Uses `apiConfig.client(...)` because host matches `.wonder.internal`; inherits core-ng framework defaults.
  - `BACycleItemWebService` & `BAGlobalSettingWebService`: No explicit timeout configured.
- **Fallback Client Timeout (`EnhancedAPIConfig`):** `5000 ms` (`Timeout.MILLISECOND_5000`).

---

## 6. External Dependencies

All outbound HTTP calls are configured in `APIClientModule.java`.

| Service Interface | Target Service & Property Key | Production URL | Timeouts (Connect / Read) | Resiliency / Retries |
| :--- | :--- | :--- | :--- | :--- |
| `PublishedRestaurantWebService` | Restaurant Service V2 (`app.consumer.restaurant.serviceURL`) | `https://consumer-restaurant-service-v2-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `PublishedMenuItemWebService` | Restaurant Service V2 (`app.consumer.restaurant.serviceURL`) | `https://consumer-restaurant-service-v2-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `PublishedBundleItemWebService` | Restaurant Service V2 (`app.consumer.restaurant.serviceURL`) | `https://consumer-restaurant-service-v2-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `CustomerWebService` | Customer Service (`app.consumer.customer.serviceURL`) | `https://consumer-customer-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `CustomerAddressWebService` | Customer Service (`app.consumer.customer.serviceURL`) | `https://consumer-customer-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `CustomerCreditCardWebService` | Customer Service (`app.consumer.customer.serviceURL`) | `https://consumer-customer-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `CustomerMembershipWebService` | Customer Service (`app.consumer.customer.serviceURL`) | `https://consumer-customer-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `AliasUserWebService` | Customer Service (`app.consumer.customer.serviceURL`) | `https://consumer-customer-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `VisitorWebService` | Customer Service (`app.consumer.customer.serviceURL`) | `https://consumer-customer-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `VisitorAddressWebService` | Customer Service (`app.consumer.customer.serviceURL`) | `https://consumer-customer-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `PriceWebService` | Order Service (`app.order.serviceURL`) | `https://order-service.prod-consumer.wonder.internal` | Framework Default (5s / 60s) | Framework Default (3 retries); No Circuit Breaker |
| `PromotionWebService` | Marketing Service (`app.consumer.marketing.serviceURL`) | `https://consumer-marketing-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `MembershipProductWebService`| Marketing Service (`app.consumer.marketing.serviceURL`) | `https://consumer-marketing-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `SegmentWebService` | Marketing Service (`app.consumer.marketing.serviceURL`) | `https://consumer-marketing-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `ConfigurationWebService` | Wonder Setting Service (`app.consumer.wonderSetting.serviceURL`) | `https://consumer-wonder-setting-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `HDRFeeConfigurationWebService` | Wonder Setting Service (`app.consumer.wonderSetting.serviceURL`) | `https://consumer-wonder-setting-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `HDRTipConfigurationWebService` | Wonder Setting Service (`app.consumer.wonderSetting.serviceURL`) | `https://consumer-wonder-setting-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `CorporateOrderClientWebService` | Wonder Setting Service (`app.consumer.wonderSetting.serviceURL`) | `https://consumer-wonder-setting-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `WonderSpotFeeConfigurationWebService` | Wonder Setting Service (`app.consumer.wonderSetting.serviceURL`) | `https://consumer-wonder-setting-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `FeeConfigurationWebService` | Wonder Setting Service (`app.consumer.wonderSetting.serviceURL`) | `https://consumer-wonder-setting-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `CustomerWalletWebService` | Customer Wallet Service (`app.consumer.customerWallet.serviceURL`) | `https://consumer-customer-wallet-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `SearchWebService` | Search Service (`app.consumer.search.serviceURL`) | `https://search-service-headless:8443` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `HDRWebService` | Kitchen Management (`app.kitchenManagement.serviceURL`) | `https://kitchen-management-service.prod-hdr.wonder.internal` | Framework Default (5s / 60s) | Framework Default (3 retries); Bypasses CLB; No Circuit Breaker |
| `AppOnlineUserWebService` | App Notification Service (`app.consumer.appOnlineUser.serviceURL`) | `http://wonder-app-notification-service-headless:8080` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `WebOnlineUserWebService` | Web Notification Service (`app.consumer.webOnlineUser.serviceURL`) | `http://wonder-web-notification-service-headless:8080` | 2s / 20s | CLB (2 retries, 100ms wait); No Circuit Breaker |
| `BACycleItemWebService` | Blue Apron Product Service (`app.blueApronProduct.serviceURL`) | `https://blueapron-product-service.prod-ba-consumer` | Framework Default (5s / 60s) | Framework Default (3 retries); No Circuit Breaker |
| `BAGlobalSettingWebService` | Blue Apron Product Service (`app.blueApronProduct.serviceURL`) | `https://blueapron-product-service.prod-ba-consumer` | Framework Default (5s / 60s) | Framework Default (3 retries); No Circuit Breaker |
| `PublishedMenuItemWebService` (GoogleBot) | Restaurant Service V2 (`app.restaurant.serviceURL`) | `https://restaurant-service-v2-headless:8443` | 5s / 5s | EnhancedAPIClient proxy; Fallback implementation (incomplete `@Fallback`) |
| `PublishedBundleItemWebService` (GoogleBot) | Restaurant Service V2 (`app.restaurant.serviceURL`) | `https://restaurant-service-v2-headless:8443` | 5s / 5s | EnhancedAPIClient proxy; Fallback implementation (missing `@Fallback`) |

### Missing Timeout Settings and High Timeout Vulnerabilities

- **PriceWebService Unconfigured Timeout:** `PriceWebService` is invoked synchronously on the critical checkout calculation path (`CustomerCartContext`, `WebCartContextService`, `POPCartContextService`). Because it is registered via `api().client(...)` instead of `CLBConfig`, it has no explicit connect or read timeout, leaving it vulnerable to upstream hangs up to 60 seconds.
- **Overly Generous 20-Second Read Timeout:** All clients managed through `CLBConfig` use a 20-second read timeout. For synchronous consumer HTTP requests on mobile and web carts, a 20-second timeout allows slow or hanging upstream dependencies to saturate HTTP worker thread pools, leading to thread exhaustion and complete unavailability of `wonder-cart-service`.

---

## 7. Scalability Signals

### Singletons and In-Process State Across Multiple Replicas

- **Local JVM Semaphore (`SemaphoreExecutorUtil`):**
  Throttling for courier supply health evaluations is enforced via an in-memory static semaphore (`permits = 5`). In a multi-replica deployment, each replica maintains its own permit pool. If the service scales to $N$ pods, total concurrency grows to $5 \times N$, bypassing intended system-wide concurrency limits on downstream services and the MySQL read replica.
- **Non-Atomic Deduplication Checks:**
  In `CourierSupplyHealthTriggerMessageHandler`:
  ```java
  if (!Strings.isBlank(redis.get(String.format(COURIER_SUPPLY_HEALTH_TRIGGER_CACHE_KEY, message.ownerId)))) {
      return;
  }
  redis.set(String.format(COURIER_SUPPLY_HEALTH_TRIGGER_CACHE_KEY, message.ownerId), message.ownerId, Duration.ofSeconds(duration));
  ```
  The check (`redis.get`) and insertion (`redis.set`) are executed as two separate non-atomic commands rather than atomic `SETNX` or `SET NX PX`. Concurrent messages for the same owner processed across distinct replicas can both read empty results and execute redundant processing simultaneously.

### Scheduled and Periodic Tasks Guarding Across Replicas

- **Absence of Distributed Advisory Locks:**
  Neither the data cleaning job (`CleanDataMessageHandler`) nor the periodic user sync job (`CourierSupplyHealthTriggerPeriodicallyMessageHandler`) utilizes a distributed lock (e.g. Redis Redlock, ShedLock, or MySQL `GET_LOCK`).
- **Concurrent Fan-Out Blast Radius:**
  When `CourierSupplyHealthTriggerPeriodicallyMessage` is received, `CourierSupplyHealthTriggerPeriodicallyMessageHandler` queries all online users from `wonder-app-notification-service` and `wonder-web-notification-service`, iterating sequentially over all users and publishing individual Kafka events. If multiple replicas consume this message (or if multiple partitions are assigned across replicas), each replica runs this full list scan independently, multiplying outbound Redis lookups and Kafka message production by the replica count.
- **Ad-Hoc Migration Route Concurrency:**
  The `POST /app/migrate-cart-bundle-item-choice` endpoint launches an un-guarded background executor thread performing batched reads and upserts on `cart_bundle_item_choices`. If invoked concurrently across replicas, both replicas will execute conflicting batch upserts.

### Stateful Operations and Replica Safety

- **Local Filesystem:** No local filesystem writes or stateful disk dependencies exist.
- **Local In-Memory Caching:** There are no local in-memory caches that diverge across replicas; all caching is delegated to Redis.
- **Global Unbounded DELETE Queries:**
  The endpoints `DELETE /cart/delete-all`, `DELETE /web-cart/delete-all`, and `DELETE /pop/cart/delete-all` issue unbounded `DELETE FROM <table>` statements across core transactional tables. In a distributed multi-replica cluster, these operations cause global table locks, blocking all user traffic across all pods.

---

## Key Scalability Signals

The top five scalability concerns identified in the `wonder-cart-service` codebase are:

1. **Unindexed Full Table Scans and Filesorts in High-Frequency Query Paths:**
   - The cart search queries (`CartService.search()` and `WebCartService.search()`) perform a multi-table `LEFT JOIN` and `GROUP BY` ordered by `created_time ASC` while explicitly executing `Database.suppressSlowSQLWarning(true)` to silence framework slow-query alerts. Because neither `carts` nor `web_carts` has an index on `created_time` (only `PRIMARY KEY` on `id` and `UNIQUE KEY` on `owner_id` exist), every search request triggers a full table scan and an on-disk filesort. Furthermore, the query precedes search with an unindexed `count()` query across the entire table.
   - Background cleanup in `CleanDataMessageHandler` queries `WHERE updated_time <= ? LIMIT 100` in a `while(true)` loop. `carts`, `web_carts`, and `cart_carry_over_histories` have no index on `updated_time`, causing full table scans on every batch iteration.
   - Cart item queries in `CartContextService` order items by `created_time ASC` despite the index being `(cart_id, updated_time)`, resulting in repeated filesorts on every cart summary and cart retrieval request.
2. **Absence of Circuit Breakers and High Read Timeouts on the Critical Checkout Path:**
   - The service synchronously integrates with more than ten microservices during checkout and cart retrieval. There are **zero** circuit breakers configured for any critical user-facing dependencies (`order-service`, `customer-service`, `restaurant-service-v2`, `marketing-service`, `wonder-setting-service`).
   - The read timeout for all CLB-managed clients defaults to 20 seconds, while direct clients (`PriceWebService`, `HDRWebService`, Blue Apron) have unconfigured timeouts defaulting to 60 seconds. A degradation in an upstream dependency will hold worker threads open for 20 to 60 seconds per request, causing rapid thread pool starvation and cascading failure across all cart pods.
3. **Unguarded Periodic Background Processing and Fan-Out Blast Radius:**
   - `CourierSupplyHealthTriggerPeriodicallyMessageHandler` retrieves all online users and iterates through the entire list, issuing blocking `redis.get()` operations and publishing individual Kafka events per user. The handler has no distributed locking mechanism or leader election. In a multi-replica deployment, every replica can process this event concurrently, multiplying Redis queries and Kafka traffic by the number of active pods ($N \times \text{users}$).
4. **Local Concurrency Limits and Non-Atomic Rate Limiting Across Replicas:**
   - Concurrency for courier supply evaluations is managed by a static in-process Java `Semaphore` (`app.semaphore.permits = 5`), which does not coordinate across pods. Total concurrent load placed on downstream services scales linearly ($5 \times N$) with the horizontal pod autoscaler replica count.
   - Event deduplication in `CourierSupplyHealthTriggerMessageHandler` uses separate `redis.get()` and `redis.set()` calls instead of atomic Redis commands (`SETNX`), allowing race conditions across concurrent replicas.
5. **Unconstrained Table-Locking Deletions:**
   - The REST endpoints `/cart/delete-all`, `/web-cart/delete-all`, and `/pop/cart/delete-all` execute unbounded `DELETE FROM <table>` statements across core transactional tables (`carts`, `web_carts`, `pop_carts`, and child tables) inside open database transactions. This produces massive table and row locking, replication lag, and transaction log bloat that impacts the entire application cluster.
