# Service Code Analysis — Business Context Extraction: ba-auto-ship-service

## 1. API Surface

The service `ba-auto-ship-service` is implemented on the Core-NG Java framework and deployed across three Kubernetes workloads:
- `ba-auto-ship-service`: Handles synchronous REST API traffic (`APP_ENABLEKAFKA=false`, autoscaled via HPA with 3 to 5 replicas).
- `ba-auto-ship-task-service`: Consumes asynchronous Kafka events and processes batch operations (`APP_ENABLEKAFKA=true`, fixed at 3 replicas).
- `ba-auto-ship-simulation-service`: Executes heavy offline auto-ship assignment simulations (`app.simulationMode=true`, fixed at 1 replica).

### REST Endpoints Exposed

#### Customer & WebService Endpoints (defined in `ba-auto-ship-service-interface`)
- **BACustomerAutoShipWebService**
  - `GET /auto-ship/customer/:userId/order-usage-info` — Fetches customer auto-ship order usage and spend history.
  - `PUT /auto-ship/customer/:userId/cart` — Lists cart summaries across delivery cycles.
  - `GET /auto-ship/customer/:userId/cart/upcoming` — Lists upcoming editable auto-ship carts.
  - `GET /auto-ship/customer/:userId/cart/upcoming-count` — Returns count of upcoming active carts.
  - `PUT /v2/auto-ship/customer/:userId/cart/:cartId` — Retrieves full cart details for a specific cycle.
  - `PUT /auto-ship/v2/customer/:userId/cart/:cartId/summary` — Retrieves lightweight cart summary for attribution.
  - `PUT /v3/auto-ship/customer/:userId/cart/:cartId/preview-item` — Previews cart state when adding/removing candidate items.
  - `PUT /auto-ship/customer/:userId/cart/:cartId/view` — Marks a cart as viewed by customer (transitions item build mode).
  - `PUT /auto-ship/v2/customer/:userId/cart/:cartId/item` — Updates cart items (item swaps, servings, quantities).
  - `PUT /v2/auto-ship/customer/:userId/cart/:cartId/preview` — Previews price, tax, and discounts for a cart.
  - `PUT /v3/auto-ship/customer/:userId/cart/:cartId/full-detail` — Full detail preview including recommendations and price breakdown.
  - `PUT /auto-ship/customer/:userId/cart/:cartId/skip` — Skips a scheduled auto-ship cart.
  - `PUT /auto-ship/customer/:userId/cart/by-delivery-date/skip` — Skips cart matching a specific delivery date.
  - `PUT /auto-ship/customer/:userId/cart/:cartId/confirm` — Customer manually confirms cart contents before cutoff.
  - `PUT /auto-ship/customer/:userId/cart/:cartId/delivery-date` — Updates cart delivery date (triggers sibling delivery adjustments).
  - `PUT /auto-ship/customer/:userId/cart/:cartId/delivery-timeslot` — Updates delivery timeslot window.
  - `PUT /auto-ship/customer/:userId/cart/:cartId/delivery-address` — Updates delivery address for cart and active plan.
  - `PUT /auto-ship/customer/:userId/cart/:cartId/payment` — Updates payment method on cart and propagates to active plan.
  - `PUT /auto-ship/customer/:userId/cart/:cartId/promotion` — Updates or validates promotion code applied to cart.
  - `PUT /auto-ship-cart/customer/:userId/:cartId/check-3ds` — Evaluates whether cart requires 3D Secure verification.
  - `PUT /auto-ship-cart/customer/:userId/:cartId/failed-3ds` — Records 3DS authentication failure.
  - `PUT /auto-ship-cart/customer/:userId/:cartId/allow-continue-charge` — Unblocks charging after customer passes 3DS challenge.
  - `PUT /auto-ship/customer/:userId/cart/in-progress-check` — Checks if any carts are currently mid-flight in order generation.
  - `PUT /auto-ship/customer/:userId/check-credit-card-usage` — Checks if a payment method is bound to active auto-ship plans.
  - `GET /auto-ship/customer/:userId/next-delivery-cart` — Retrieves the earliest upcoming delivery cart.
  - `GET /auto-ship/customer/:userId/legacy-phone` — Retrieves legacy customer default phone number.
  - `GET /auto-ship/customer/:userId/cart/:cartId/historical-items` — Retrieves historical items from past orders for repeat purchases.
  - `GET /auto-ship-cart/customer/:userId/cart/item-in-sibling-weeks` — Checks if specific recipe items appear in adjacent delivery weeks.
  - `PUT /auto-ship-cart/customer/:userId/cart/enable-auto-apply` — Enables/disables auto-application of credits and promotions.
- **BAAutoShipWebService**
  - `POST /auto-ship/estimate-price` — Estimates weekly subscription price based on protein and serving preferences.
  - `GET /auto-ship/cancel-reason` — Lists active plan cancellation reason options.
  - `POST /auto-ship/enroll` — Enrolls customer in an auto-ship subscription plan.
  - `GET /auto-ship/:userId/plan` — Retrieves customer's current active auto-ship plan.
  - `PUT /auto-ship/:userId/promotion` — Updates plan-level promotion code.
  - `PUT /auto-ship/plan/:planId` — Updates plan customizations (meal kit / prepared meal counts, proteins).
  - `PUT /auto-ship/plan/:planId/cancel` — Cancels active auto-ship plan.
  - `PUT /auto-ship/plan/:planId/cancel-reason` — Updates cancellation reason survey data.
  - `PUT /auto-ship/plan/list-by-user` — Lists historical and active plans by user ID.
- **BAAutoShipWinbackWebService**
  - `PUT /auto-ship/winback-promotion/offer` — Evaluates and offers winback promotion to churned subscribers.
  - `PUT /auto-ship/winback-promotion/claim` — Claims winback promotion code for re-enrollment.
  - `PUT /auto-ship/winback-promotion/check` — Checks status and validity of winback promotion.
- **BACustomerAutoShipRecommendationAlgorithmWebService**
  - `GET /auto-ship/customer/:userId/recommendation-algorithm` — Fetches active ML recommendation algorithm assignment for user.
- **BAAutoShipCartWebService**
  - `GET /auto-ship/cart/skip-reason` — Lists skip reasons for cart pause flow.
- **BAAutoShipDeliveryAddressWebService**
  - `PUT /auto-ship/delivery-address/check-usage` — Verifies if address is currently assigned to active plans.
  - `PUT /auto-ship/delivery-address/unbind` — Unbinds address from auto-ship plans.
  - `PUT /auto-ship/delivery-address` — Creates or updates delivery address.
  - `PUT /auto-ship/delivery-address/list-non-city` — Lists addresses missing municipality/city data (data hygiene).
  - `PUT /auto-ship/delivery-address/list-too-long-address-name` — Lists addresses exceeding length limits.
  - `PUT /auto-ship/delivery-address/:id/city` — Updates address city name.
  - `PUT /auto-ship/delivery-address/:id/address-line` — Updates address line details.
- **BOBAAutoShipAssignmentConfigWebService**
  - `PUT /bo/auto-ship/assignment-config` — Updates assignment algorithm capacity configuration.
  - `GET /bo/auto-ship/assignment-config` — Fetches current assignment parameters.
  - `GET /bo/auto-ship/assignment-config/list` — Lists historical assignment configurations.
  - `GET /bo/auto-ship/soft-cap-buffer-config` — Fetches soft capacity buffer configurations.
  - `GET /bo/auto-ship/soft-available-multiplier-config` — Fetches soft inventory multiplier configurations.
  - `PUT /bo/auto-ship/soft-available-multiplier-config` — Updates soft inventory multipliers.
- **BOAssignmentSimulationWebService**
  - `GET /bo/auto-ship/assignment-simulation/check-running` — Checks if simulation execution is currently in progress.
  - `GET /bo/auto-ship/assignment-simulation/list` — Lists historical simulation runs.
  - `PUT /bo/auto-ship/assignment-simulation/check` — Checks status of a specific simulation job.
  - `PUT /bo/auto-ship/assignment-simulation/trigger` — Triggers new assignment simulation run.
- **BOBAAutoShipAssignmentWebService**
  - `PUT /bo/ba-auto-ship/assignment/simulate` — Simulates item assignment against projected inventory.
  - `POST /bo/ba-auto-ship/assignment/specific-cycle` — Manually triggers assignment for a delivery cycle.
  - `PUT /bo/ba-auto-ship/reassign-summary` — Summarizes re-assignment metrics and impacted subscribers.
  - `PUT /bo/ba-auto-ship/reassign-statistics` — Aggregates shortage/OOS re-assignment results.
  - `PUT /bo/ba-auto-ship/reassign-notify-statistics` — Statistics on customer notifications sent post-reassignment.
  - `PUT /bo/ba-auto-ship/reassign-affected-user-detail` — Lists individual customers impacted by recipe swaps.
  - `POST /bo/ba-auto-ship/notify-reassign-affected-users` — Triggers batch email notifications for re-assigned customers.
  - `POST /bo/ba-auto-ship/refresh-delivery-options` — Triggers refresh of fulfillment carrier delivery dates.
- **BOBAAutoShipCartAddressWebService**
  - `GET /bo/ba-auto-ship/cart-address/:cartId` — Retrieves delivery address snapshot bound to cart.
- **BOBAAutoShipCartWebService**
  - `PUT /bo/ba-auto-ship/cart/aggregate-by-menu-item` — Aggregates demand across fulfillment centers for menu items.
  - `PUT /bo/ba-auto-ship/cart/search-by-menu-item` — Searches carts containing a specific menu item.
  - `PUT /bo/ba-auto-ship/cart/escalate-check` — Checks if cart meets escalation criteria.
  - `PUT /bo/ba-auto-ship/cart/escalate` — Escalates cart to customer operations queue.
- **BOBAAutoShipFulfillmentErrorWebService**
  - `PUT /bo/ba-auto-ship/fulfillment-error/:id/investigation-findings` — Records findings for fulfillment errors.
  - `PUT /bo/ba-auto-ship/fulfillment-errors` — Queries fulfillment error records with filtering.
- **BOBAAutoShipPriceSettingWebService**
  - `GET /auto-ship/product-line-price-range` — Lists allowed price bands per product line.
  - `PUT /auto-ship/product-line-price-range/batch` — Updates product line price boundaries.
- **BOBAAutoShipRemoveCartItemWebService**
  - `PUT /bo/ba-auto-ship/remove-cart-item/dish` — Ops tool to remove deprecated or unavailable dish across all carts.
- **BOBACustomerAutoShipWebService**
  - `PUT /bo/ba-auto-ship/customer/:userId/cancel` — Ops cancellation of customer plan.
  - `PUT /bo/ba-auto-ship/customer/search` — Back-office search for auto-ship plans.
  - `GET /bo/ba-auto-ship/cycle/:cycleId/cart-item-statistics` — Item demand statistics per FC for a cycle.
  - `PUT /bo/ba-auto-ship/cycle/:cycleId/cart-report` — Summary report of active, confirmed, skipped, and converted carts.
- **BOToolAutoShipCartWebService**
  - `POST /bo/tool/auto-ship/reassign-unqualified-cart` — Triggers re-assignment of carts failing minimum pricing or serving qualification.
- **BAAutoShipMigrationWebService**
  - `PUT /ba/auto-ship/migration/get-item-unviewed-quantity` — Migration utility querying unviewed quantities.

#### HTTP Controllers (Internal Ops routes in `BAAutoShipApp.java`)
- `POST /_app/ba-auto-ship/customer-ba-subtotal-init` — Initializes customer subtotal and savings cache.
- `POST /_app/ba-auto-ship/manually-auth-cart` — Manually triggers authorization batch run.
- `POST /_app/ba-auto-ship/manually-generate-order` — Manually triggers order generation batch run.
- `GET /_app/ba-auto-ship/manually-auth-cart/:cartId` — Single cart auth debug route (dev/uat only).
- `GET /_app/ba-auto-ship/manually-generate-order/:cartId` — Single cart order placement debug route (dev/uat only).
- `POST /_app/ba-auto-ship/adjust` — Processes post-order adjustments.
- `POST /_app/ba-auto-ship/create-fulfillment` — Manually pushes fulfillment orders to FR.
- `POST /_app/ba-auto-ship/fulfilment-event` — Publishes manual fulfillment events.
- `POST /_app/ba-auto-ship/fulfilment-recreate` — Re-creates canceled/failed fulfillment records.
- `POST /_app/ba-auto-ship/manually-assignment` — Triggers manual auto-assignment for a cycle.
- `POST /_app/ba-auto-ship/manually-assign-exceeded-price-cart` — Re-evaluates carts exceeding maximum price limits.
- `POST /_app/ba-auto-ship/init-cart-item-product-line` — Data patch initializing product lines on cart items.
- `POST /_app/ba-auto-ship/rebalance-manually-start-next-batch` — Manually advances rebalance batch pool.
- `POST /_app/ba-auto-ship/add-rebalance-event` — Injects rebalance trigger event.
- `POST /_app/ba-auto-ship/check-has-active-ba-subscription-customers` — Scans active subscriber flag.
- `POST /_app/ba-auto-ship/remove-dish-cycle` — Configures dish skip list for cycle.
- `POST /_app/ba-auto-ship/migrate-fbm-auto-ship-plan-delivery-day-of-week` — One-time delivery day migration.
- `POST /_app/ba-auto-ship/import-fbm-shifted-zip-codes` — Imports zip codes requiring shifted delivery schedules.
- `POST /_app/ba-auto-ship/manually-create-cart` — Manually generates future carts for active plans.
- `POST /_app/ba-auto-ship/fix-cancel-cart` — Fixes cancelled carts out of sync with active plan status.
- `GET /_app/ba-auto-ship/promotion-code-init` — Initializes promotion code tracking state.
- `POST /_app/ba-auto-ship/savings-init` — Recalculates customer savings aggregate metrics.
- `POST /_app/ba-auto-ship/refresh-cart-delivery-date` — Batch refreshes cart delivery dates after calendar shifts.
- `POST /_app/ba-auto-ship/clean-up` — Triggers retention cleanup across completed task tables.
- `POST /_app/ba-auto-ship/cleanup-cart-item-tracking` — Purges expired MongoDB cart item trackings.

---

### Critical Path Endpoints
The following endpoints are synchronous and on the critical path of subscriber user-facing operations:
1. **Cart Lifecycle & Customization**:
   - `PUT /v2/auto-ship/customer/:userId/cart/:cartId` (cart load)
   - `PUT /auto-ship/v2/customer/:userId/cart/:cartId/item` (swap/add/remove meal kit & prepared dishes)
   - `PUT /v3/auto-ship/customer/:userId/cart/:cartId/preview-item` (live serving & price calculations)
   - `PUT /auto-ship/customer/:userId/cart/:cartId/confirm` (customer manual cart confirmation)
   - `PUT /auto-ship/customer/:userId/cart/:cartId/skip` (cart skipping / pausing)
2. **Subscription Lifecycle**:
   - `POST /auto-ship/estimate-price` (onboarding pricing calculation)
   - `POST /auto-ship/enroll` (subscription signup transaction)
   - `GET /auto-ship/:userId/plan` (account plan dashboard)
   - `PUT /auto-ship/plan/:planId` (updating subscriber weekly preference parameters)
   - `PUT /auto-ship/plan/:planId/cancel` (subscription churn flow)
3. **Delivery & Checkout Attributes**:
   - `PUT /auto-ship/customer/:userId/cart/:cartId/delivery-date`
   - `PUT /auto-ship/customer/:userId/cart/:cartId/delivery-address`
   - `PUT /auto-ship/customer/:userId/cart/:cartId/payment`
   - `PUT /auto-ship-cart/customer/:userId/:cartId/check-3ds` & `PUT /auto-ship-cart/customer/:userId/:cartId/allow-continue-charge` (payment authorization recovery)

---

### Batch and Scheduled Jobs

#### 1. In-Process Scheduled Job (runs within `ba-auto-ship-service`)
- **scheduler-liveness-check-job**:
  - **Frequency**: Every 1 minute (`schedule().fixedRate("scheduler-liveness-check-job", ..., Duration.ofMinutes(1))`).
  - **Handler**: `SchedulerLivenessCheckJob` -> `SchedulerLivenessMonitorService`.
  - **Behavior**: Probes `blueapron-scheduler-service` over HTTP (`/liveness-probe`). Runs in all replicas guarded by Redis distributed lock `scheduler-liveness-monitor:lock`. Fires Slack alerts if scheduler downtime exceeds 30 minutes.

#### 2. Externally Orchestrated Jobs (triggered by `blueapron-scheduler-service` via Kafka)
Scheduling is centralized in `blueapron-scheduler-service` to ensure single-instance triggering across Kubernetes replicas:
- **auto-ship-fulfillment-trigger-job**:
  - **Frequency**: Every 1 second (`Duration.ofSeconds(1)`).
  - **Topic**: `auto-ship-fulfillment-trigger-message`.
  - **Action**: Polls `auto_ship_cart_fulfillment_outboxes` and publishes pending fulfillment messages.
- **auto-ship-reassign-notification-trigger-job**:
  - **Frequency**: Every 3 seconds (`Duration.ofSeconds(3)`).
  - **Topic**: `auto-ship-reassign-notification-trigger-message`.
  - **Action**: Scans and sends email notifications for items reassigned due to OOS.
- **ba-auto-ship-task-batch-check-job**:
  - **Frequency**: Every 60 seconds (`Duration.ofSeconds(60)` in prod).
  - **Topic**: `ba-auto-ship-task-batch-check-message`.
  - **Action**: Checks and starts next batches for cart creation, auth, and order generation.
- **ba-auto-ship-rebalance-trigger-job**:
  - **Frequency**: Every 1 minute (`Duration.ofMinutes(1)`).
  - **Topic**: `auto-ship-rebalance-trigger-message`.
  - **Action**: Evaluates fulfillment center capacities and initiates rebalance batching.
- **ba-auto-ship-remove-dish-trigger-job**:
  - **Frequency**: Every 1 minute (`Duration.ofMinutes(1)`).
  - **Topic**: `auto-ship-remove-dish-trigger-message`.
  - **Action**: Processes batches for dish removals triggered by ops.
- **ba-auto-ship-task-trigger-job**:
  - **Frequency**: Hourly at minute 0 (`0 0 * * * ?`).
  - **Topic**: `ba-auto-ship-task-trigger-message`.
  - **Action**: Scans plans and creates carts, triggers auth for carts approaching cutoff, triggers order generation for past-cutoff carts.
- **ba-auto-ship-task-check-job**:
  - **Frequency**: Every 15 minutes (`Duration.ofMinutes(15)`).
  - **Topic**: `ba-auto-ship-task-check-message`.
  - **Action**: Detects stuck batch tasks (pending timeout 10m, processing timeout 5m) and fails or retries them.
- **ba-auto-ship-fulfillment-stuck-recovery-job**:
  - **Frequency**: Every 15 minutes (`Duration.ofMinutes(15)`).
  - **Topic**: `auto-ship-fulfillment-stuck-recovery-message`.
  - **Action**: Recovers outbox records stuck in `PROCESSING` longer than 90 minutes.
- **ba-auto-ship-fulfillment-event-sent-result-check-job**:
  - **Frequency**: Every 30 minutes (`Duration.ofMinutes(30)`).
  - **Topic**: `auto-ship-fulfillment-event-sent-result-check-message`.
  - **Action**: Verifies that fulfillment events sent to FR integration received matching tracking responses.
- **ba-auto-ship-issue-order-check-job**:
  - **Frequency**: Every 30 minutes (`Duration.ofMinutes(30)`).
  - **Topic**: `auto-ship-issue-order-check-message`.
  - **Action**: Reconciles converted carts against generated orders in OMS, reporting discrepancy counts to Slack.
- **ba-auto-ship-rebalance-check-job**:
  - **Frequency**: Every 1 hour (`Duration.ofHours(1)`).
  - **Topic**: `auto-ship-rebalance-check-message`.
  - **Action**: Evaluates cycle-level inventory against target FC allocations.
- **ba-auto-ship-notification-trigger-job**:
  - **Frequency**: Daily at 09:00 New York time (`0 0 9 * * ?`).
  - **Topic**: `auto-ship-notification-message`.
  - **Action**: Sends cutoff reminder and order confirmation notifications.
- **ba-auto-ship-cart-created-notification-trigger-job**:
  - **Frequency**: Weekly on Monday at 09:00 New York time (`0 0 9 ? * MON`).
  - **Topic**: `auto-ship-cart-created-notification-message`.
  - **Action**: Sends notifications alerting subscribers that future carts (3 weeks ahead) have been generated.

---

## 2. Database Access Patterns

### MySQL Configuration & Database Details
- **Target DB**: Azure MySQL Flexible Server `rfprodv2-ba-consumer-db.mysql.database.azure.com:3306/ba_auto_ship`.
- **Database Connection Pool**: Managed via Core-NG `RetryDBConfig` and `db().repository(...)`.
  - Default Core-NG pool size: minimum 2, maximum 50 connections. No custom overrides configured in `BADBModule.java` or `sys.properties`.
  - Authentication: Azure Managed Identity (`useSSL=true`, user `iam/azure/aad-ba-auto-ship-service`).

### MySQL Tables Accessed and Query Types

#### 1. Core Domain Tables
- `auto_ship_carts` (Repository: `AutoShipCart.class`):
  - **Size**: ~1.38M rows (5.26 GB).
  - **Operations**:
    - `SELECT`: `get` by ID, `select()` filtered by `auto_ship_plan_id`, `user_id`, `cycle_id`, `cutoff_time`, `status`, and `delivery_date`.
    - `INSERT`: `insert` during cart generation.
    - `UPDATE`: `update`, `partialUpdate` (status, items subtotal, viewed state, delivery date, timeslot, payment method, 3DS flags, version optimistic lock).
- `auto_ship_cart_items` (Repository: `AutoShipCartItem.class`):
  - **Size**: ~1.98M rows (5.41 GB).
  - **Operations**:
    - `SELECT`: `select()` by `auto_ship_cart_id`, `menu_item_id`, `cycle_id`.
    - `INSERT` / `DELETE`: `batchInsert` on cart generation / modification; `batchDelete` when removing or swapping dishes.
- `auto_ship_plans` (Repository: `AutoShipPlan.class`):
  - **Operations**: `SELECT` by `user_id` and `status = ACTIVE`; `INSERT` on enrollment; `UPDATE` on customization change or cancellation.
- `auto_ship_plan_customizations` (Repository: `AutoShipPlanCustomization.class`):
  - **Operations**: `SELECT` by `plan_id`; `INSERT`; `UPDATE` when customer changes meal kit / pre-made quantities or dietary protein preferences.
- `auto_ship_delivery_addresses` (Repository: `AutoShipDeliveryAddress.class`):
  - **Size**: ~1.25M rows (5.17 GB).
  - **Operations**: `SELECT` by `auto_ship_cart_id` or `user_id`; `INSERT`; `UPDATE`; `DELETE` when replacing address.
- `auto_ship_cart_skip_reasons` (Repository: `AutoShipCartSkipReason.class`):
  - **Size**: ~2.39M rows (1.36 GB).
  - **Operations**: `SELECT`, `INSERT` on skip, `DELETE` on unskip.
- `auto_ship_forter_infos` (Repository: `AutoShipForterInfo.class`):
  - **Operations**: `SELECT`, `INSERT`, `UPDATE` recording fraud decision tokens.
- `auto_ship_plan_cancel_reasons` & `auto_ship_cart_cancel_reasons`:
  - **Operations**: `SELECT`, `INSERT` recording subscriber churn rationale.
- `auto_ship_promotion_usages` (Repository: `AutoShipPromotionUsage.class`):
  - **Operations**: `SELECT` and `INSERT` ensuring single-use promos are enforced.
- `customer_ba_subtotals_and_savings` (Repository: `CustomerBASubtotalAndSaving.class`):
  - **Operations**: `SELECT`, `INSERT`, `UPDATE` caching cumulative subscriber savings.
- `auto_ship_winback_promotions`:
  - **Operations**: `SELECT`, `INSERT`, `UPDATE` managing winback lifecycle state.
- `auto_ship_delivery_day_shifts`, `fbm_shifted_zip_codes`, `auto_ship_cart_original_deliveries`:
  - **Operations**: `SELECT`, `INSERT`, `UPDATE` managing holiday/carrier calendar shifts.

#### 2. Task & Batch Execution Tables
- `auto_ship_create_cart_task_batches`, `auto_ship_create_cart_tasks`, `auto_ship_create_cart_batch_pools`:
  - **Operations**: `INSERT`, `batchInsertIgnore`, `batchDelete`, `UPDATE` tracking cart generation batches.
- `auto_ship_auth_cart_task_batches`, `auto_ship_auth_cart_tasks`:
  - **Operations**: `INSERT`, `batchInsertIgnore`, `UPDATE` (`status = PROCESSING`, `completed_task = completed_task + 1`).
- `auto_ship_generate_order_task_batches`, `auto_ship_generate_order_tasks`:
  - **Operations**: `INSERT`, `batchInsertIgnore`, `UPDATE` (`status = PROCESSING`, `completed_task = completed_task + 1`).
- `auto_ship_reassign_unqualified_cart_item_tasks`, `auto_ship_reassign_unqualified_cart_item_task_batches`:
  - **Operations**: `INSERT`, `UPDATE` with `SELECT ... FOR UPDATE` row locks, `DELETE` on batch completion.
- `auto_ship_remove_dish_cart_item_tasks`, `auto_ship_remove_dish_cart_item_task_batches`:
  - **Operations**: `INSERT`, `UPDATE`, `batchDelete`.
- `auto_ship_refresh_delivery_option_tasks`, `auto_ship_refresh_delivery_option_task_batches`:
  - **Operations**: `INSERT`, `batchInsertIgnore`, `UPDATE`.
- `auto_ship_notification_tasks`, `auto_ship_cart_created_notification_tasks`, `auto_ship_reassign_notification_tasks`:
  - **Operations**: `INSERT`, `UPDATE`.

#### 3. Outbox & Event Tables
- `auto_ship_cart_fulfillment_outboxes`:
  - **Size**: ~7.01M rows (5.31 GB).
  - **Operations**: `SELECT` polling pending items (`status = AVAILABLE` limit 5), `UPDATE` transition to `PENDING` / `PROCESSING` / `SUCCESS`.
- `auto_ship_cart_fulfillment_events`:
  - **Size**: ~1.38M rows (38.67 GB — largest table in database).
  - **Operations**: `INSERT`, `UPDATE`, `SELECT` recording complete payload sent to FR.
- `auto_ship_cart_rebalance_outboxes`, `auto_ship_cart_rebalance_events`:
  - **Operations**: Outbox polling and event recording for inventory rebalances.
- `auto_ship_cart_remove_dish_outboxes`, `auto_ship_cart_remove_dish_events`:
  - **Operations**: Outbox pattern for dish removal events.

#### 4. Simulation Tables
- `assignment_simulations`, `simulation_inventory_results`, `simulation_auto_ship_carts`, `simulation_auto_ship_cart_items`, `simulation_auto_ship_plans`, `simulation_auto_ship_plan_customizations`, `simulation_auto_ship_assignment_tasks`:
  - **Operations**: Heavy batch inserts, updates, and reads during simulation runs (isolated to simulation pod).

---

### Large Scans, Complex JOINs, and Slow Queries

1. **Parallel STRAIGHT_JOIN Aggregations in `BOBAAutoShipCartItemQueryService`**:
   The endpoint `GET /bo/ba-auto-ship/cycle/:cycleId/cart-item-statistics` executes four large aggregation queries in parallel across 4 executor threads:
   ```sql
   SELECT ci.menu_item_id, c.fulfillment_center_code,
          SUM(CASE ci.selected_servings WHEN 4 THEN ci.quantity * 2 ELSE ci.quantity END) AS quantity
   FROM auto_ship_carts c
   STRAIGHT_JOIN auto_ship_cart_items ci ON c.id = ci.auto_ship_cart_id
   WHERE c.cycle_id = ? AND c.status = ? [AND c.viewed = ? AND c.item_build_type = ?]
   GROUP BY ci.menu_item_id, c.fulfillment_center_code
   ORDER BY ci.menu_item_id
   ```
   With ~1.38M carts and ~1.98M cart items, running 4 parallel straight joins without a multi-column covering index causes substantial temporary table allocations, file sorts, and severe CPU spikes on Azure MySQL.
2. **Explicitly Suppressed Slow Query in `BAAutoShipMigrationService`**:
   In `BAAutoShipMigrationService.getUnviewedQuantity`, the code wraps the query with:
   ```java
   Database.suppressSlowSQLWarning(true);
   var results = database.select(querySql, ItemQuantityView.class, params.toArray());
   Database.suppressSlowSQLWarning(false);
   ```
   The query performs an unindexed join across `auto_ship_cart_items` and `auto_ship_carts` filtering on `(i.cycle_id, c.cycle_id, c.status, c.viewed)` with grouping on `menu_item_id` and `fulfillment_center_code`.
3. **Subquery Scan in `searchPendingAuth`**:
   In `AutoShipCartQueryService.searchPendingAuth`, pending authorization scans execute:
   ```sql
   SELECT ... FROM auto_ship_carts
   WHERE status IN ('ACTIVE', 'CONFIRMED')
     AND cutoff_time <= ? AND cutoff_time > ?
     AND auto_ship_cart_number > ?
     AND auth_result IS NULL
     AND EXISTS (SELECT 1 FROM auto_ship_cart_items i WHERE i.auto_ship_cart_id = auto_ship_carts.id)
   ORDER BY auto_ship_cart_number ASC LIMIT 800
   ```
   The correlated `EXISTS` subquery forces MySQL to evaluate child item existence row-by-row for qualifying cart batches.
4. **Three-Way JOINs with Offset-Based Pagination in `AutoShipCartQueryServicePart2`**:
   Methods `listUnviewdCarts` and `listUnqualifiedCarts` join `auto_ship_carts a INNER JOIN auto_ship_plans b ON ... INNER JOIN auto_ship_plan_customizations c ON ...` using `LIMIT ?, ?`. As `skip` increments during large cycle sweeps, MySQL scans and discards preceding rows, degrading performance linearly with cycle size.

---

### MongoDB / Atlas Database Access Patterns

- **Target DB**: MongoDB Atlas `prod-blueapron-main-pl-0.e9cxtw.mongodb.net/ba-auto-ship`.
- **Read Preference**: Secondary read preference configured (`readPreference=secondary`, `readPreferenceTags=nodeType:ELECTABLE`).

#### Collections Accessed (15 Collections)
1. `auto_ship_cart_item_trackings`: Audit trail of item additions, swaps, and removals.
2. `auto_ship_plan_changelogs`: Historical modifications to subscriber plans.
3. `auto_ship_plan_customization_trackings`: Historical modifications to plan customizations.
4. `autoship_classification_price_mapping`: Dietary classification price settings.
5. `product_line_price_range`: Minimum and maximum pricing thresholds per product line.
6. `auto_ship_reassign_cart_item_trackings`: Tracking logs for items reassigned during shortage/OOS.
7. `auto_ship_cart_auth_fail_reasons`: Log of authorization failure codes and decline reasons.
8. `manually_trigger_create_cart_trackings`: Records manual cycle cart creation triggers.
9. `auto_ship_cart_reassign_results`: Reassignment outcomes per subscriber cart.
10. `auto_ship_fulfillment_event_sent_result_trackings`: Tracks handshake and status of events dispatched to FR.
11. `auto_ship_issue_order_trackings`: Tracks order placement outcomes from auto-ship carts.
12. `auto_ship_has_active_ba_subscription_customers`: Fast lookup collection for active subscriber status.
13. `auto_ship_cart_delivery_date_refresh_results`: Tracks results of bulk delivery date shifts.
14. `auto_ship_assign_skip_dish_cycles`: Singleton document holding cycles configured to skip dishes.
15. `auto_ship_experiment_groups`: Customer experiment bucket allocations.

#### Indexing and TTL Policies
- **TTL Indexes Configured** (via `ba-auto-ship-service-mongo-migration/Main.java`):
  - `auto_ship_cart_item_trackings`: `created_time` (expire after 90 days).
  - `auto_ship_reassign_cart_item_trackings`: `created_time` (expire after 90 days).
  - `auto_ship_cart_reassign_results`: `comms_notified_time` (expire after 30 days).
  - `auto_ship_fulfillment_event_sent_result_trackings`: `expired_at` (expire after 0 seconds).
  - `auto_ship_issue_order_trackings`: `expired_at` (expire after 0 seconds).
  - `auto_ship_cart_delivery_date_refresh_results`: `expired_time` (expire after 0 seconds).
- **Secondary Index Coverage**:
  - `auto_ship_plan_changelogs`: `{auto_ship_plan_id: 1}`, `{user_id: 1, created_time: -1}`, `{auto_ship_plan_id: 1, event: -1}`.
  - `auto_ship_cart_reassign_results`: `{action_id: 1}`, `{cycle_id: 1, user_id: 1}`, `{cycle_id: 1, reassign_type: 1, comms_notified_time: 1}`.
  - `auto_ship_fulfillment_event_sent_result_trackings`: `{sent_time: -1, received_time: 1, is_notified: 1}`.

#### Aggregation Pipelines
1. **Notification Statistics Pipeline (`AutoShipReassignResultService.notifyStatistics`)**:
   Runs on `auto_ship_cart_reassign_results` to calculate communication status:
   - `$match`: Matches `action_ids` and `reassign_type`.
   - `$group`: Groups by `auto_ship_cart_id`, evaluating `$max` conditional flags for `$ifNull(comms_notified_time)`.
   - `$group`: Groups by `_id: null` to compute `pending_notify_count` and `already_notify_count`.
2. **Affected User Statistics Pipeline (`AutoShipReassignResultService.affectedUserStatistics`)**:
   Runs on `auto_ship_cart_reassign_results`:
   - `$match`: Matches `action_id IN (...)`.
   - `$group`: First stage groups by `{action_id, auto_ship_cart_id}` to compute distinct customer impacts.
   - `$group`: Second stage groups by `action_id` computing `affected_user_count = $sum: 1`.
   - `$project` & `$sort`: Formats final count and notification state.

#### Update Patterns in MongoDB
- **Full Document Replacement (`replace` / `bulkReplace`)**:
  - `AutoShipFulfillmentEventSentResultTrackingService` (`replace`).
  - `BAAutoShipFulfillmentEventSentResultCheckMessageHandler` (`bulkReplace`).
  - `BAAutoShipIssueOrderCheckMessageHandler` (`bulkReplace`).
  - `AutoShipAssignSkipDishCycleService` (`replace`).
- **Targeted `$set` Updates (`update`)**:
  - `AutoShipIssueOrderTrackingService`: Uses `Updates.combine(Updates.set("status", ...), Updates.set("reason", ...))`.
  - `AutoShipCartMigrationService`: Uses `Updates.set("user_id", ...)`.
- **Append-Only Inserts**: `auto_ship_plan_changelogs`, `auto_ship_cart_item_trackings`, `auto_ship_reassign_cart_item_trackings`.

---

### N+1 Query Patterns and Unbounded Queries

1. **MySQL Row-Lock Contention on Batch Completion Updates**:
   For every single task processed in a batch (batches are 800 to 1,000 tasks each):
   - In `AutoShipAuthCartTaskService.authCart` and `AutoShipGenerateOrderTaskService.generateOrder`:
     Every task worker executes:
     ```sql
     UPDATE auto_ship_auth_cart_task_batches
     SET completed_task = completed_task + 1, updated_time = ?, updated_by = ?
     WHERE id = ?
     ```
     When multiple worker threads or pods process messages concurrently, up to 1,000 tasks concurrently contend for the exclusive row lock on the single parent batch record. In `BAAutoShipRebalanceEventMessageHandler.java:103`, deadlocks were explicitly encountered and mitigated by injecting `SELECT ... FOR UPDATE`, but `AutoShipAuthCartTaskService` and `AutoShipGenerateOrderTaskService` execute bare updates without serial lock acquisition.
2. **Sibling Cart Synchronization Loop (`AutoShipCartSiblingSyncService`)**:
   When a customer modifies delivery details (e.g. delivery date, address, or payment method) on one cart:
   - `listEditable(userId)` queries all upcoming active/confirmed/skipped carts for the subscriber.
   - The service iterates through every sibling cart and asynchronously submits individual update transactions (`database.beginTransaction()`), triggering N separate database writes and version checks per user action.
3. **High-Frequency 1-Second Outbox Query Loop**:
   The handler `BAAutoShipFulfillmentTriggerMessageHandler` receives a trigger message every 1 second:
   - Executes `findNormal(outboxConcurrency)` and `findRetry(outboxConcurrency)` against `auto_ship_cart_fulfillment_outboxes`.
   - With 7M rows in this table, persistent 1-second polling continuously consumes database connection pool slots.

---

## 3. Kafka Usage

Kafka messaging is configured in `MessagePublishModule.java` and `MessageSubscribeModule.java`.

### Kafka Topics Produced To (14 Topics)
- `ba-auto-ship-auto-assign-cart-message`: Dispatched to trigger cart item assignment for a subscriber plan.
- `ba-auto-ship-generate-order-message`: Dispatched when a cart passes cutoff to generate an OMS order.
- `ba-auto-ship-auth-cart-message`: Dispatched prior to cutoff to pre-authorize payment via payment gateway / Forter.
- `auto-ship-notification-message`: Cutoff reminder notification events.
- `auto-ship-cart-created-notification-message`: Notification events alerting customers that new future carts were built.
- `auto-ship-fulfillment-event-message`: Pushes confirmed cart fulfillment payloads to `fr-integration-service`.
- `ba-auto-ship-reassign-cart-item-message`: Dispatched to reassign shortage or out-of-stock items in active carts.
- `ba-auto-ship-update-plan-message`: Broadcasts subscriber plan preference updates.
- `auto-ship-reassign-notification-trigger-message`: Dispatches batch customer email alerts after recipe swaps.
- `ba-customer-auto-ship-event-message`: Broadcasts customer auto-ship lifecycle events (enrollment, cancellation).
- `auto-ship-rebalance-event-message`: Dispatched per cart to execute FC load rebalancing.
- `auto-ship-remove-dish-event-message`: Dispatched per cart to remove an ops-deprecated dish.
- `ba-auto-ship-refresh-delivery-option-message`: Dispatched to re-calculate delivery carrier options.
- `ba-plan-updated-message`: Broadcasts plan updates to marketing and analytics consumers.

---

### Kafka Topics Consumed From (25 Topics)

#### 1. Batch & Task Schedulers (consumed via default group)
- `ba-auto-ship-task-trigger-message` -> `BAAutoShipTaskTriggerMessageHandler`
- `ba-auto-ship-task-check-message` -> `BAAutoShipTaskCheckMessageHandler`
- `ba-auto-ship-task-batch-check-message` -> `BAAutoShipTaskBatchCheckMessageHandler`
- `ba-auto-ship-reassign-cart-item-message` -> `BAAutoShipReAssignCartItemMessageHandler`
- `ba-auto-ship-update-plan-message` -> `BAAutoShipUpdatePlanMessageHandler`
- `auto-ship-issue-order-check-message` -> `BAAutoShipIssueOrderCheckMessageHandler`
- `auto-ship-fulfillment-trigger-message` -> `BAAutoShipFulfillmentTriggerMessageHandler`
- `auto-ship-rebalance-check-message` -> `BAAutoShipRebalanceCheckMessageHandler`
- `auto-ship-rebalance-trigger-message` -> `BAAutoShipRebalanceTriggerMessageHandler`
- `auto-ship-remove-dish-trigger-message` -> `BAAutoShipRemoveDishTriggerMessageHandler`
- `auto-ship-fulfillment-stuck-recovery-message` -> `BAAutoShipFulfillmentStuckRecoveryMessageHandler`
- `auto-ship-fulfillment-event-sent-result-check-message` -> `BAAutoShipFulfillmentEventSentResultCheckMessageHandler`

#### 2. Task Execution Consumers (isolated consumer groups)
- `ba-auto-ship-auth-cart-message` (Group `auth-cart`) -> `BAAutoShipAuthCartMessageHandler`
- `ba-auto-ship-generate-order-message` (Group `generate-order`) -> `BAAutoShipGenerateOrderMessageHandler`
- `ba-auto-ship-auto-assign-cart-message` (Group `auto-assign-cart`) -> `BAAutoShipAutoAssignCartMessageHandler`
- `auto-ship-fulfillment-event-message` (Group `fulfillment-event`) -> `BAAutoShipFulfillmentEventMessageHandler`
- `auto-ship-rebalance-event-message` (Group `rebalance-event`) -> `BAAutoShipRebalanceEventMessageHandler`
- `auto-ship-remove-dish-event-message` & `ba-auto-ship-refresh-delivery-option-message` (Group `remove-dish-event`) -> `BAAutoShipRemoveDishEventMessageHandler` / `BAAutoShipRefreshDeliveryOptionMessageHandler`
- `ba-customer-auto-ship-event-message` (Group `customer-auto-ship-event`) -> `BACustomerAutoShipEventMessageHandler`
- `auto-ship-notification-message`, `auto-ship-cart-created-notification-message`, `auto-ship-reassign-notification-trigger-message` (Group `notification`) -> Respective notification handlers

#### 3. External Domain Event Consumers (consumed via default group & fr-update-event)
- `blue-apron-clean-data-message` -> Purges data on test/ops cleanup.
- `blue-apron-brand-updated-message` -> Updates local brand asset caches.
- `blue-apron-global-setting-updated-message` -> Updates pricing and cutoff global caches.
- `blue-apron-hide-item-message` -> Marks hidden dishes and triggers cart reassignment.
- `blue-apron-item-removed-from-menu-message` -> Triggers dish removal workflow from affected carts.
- `blue-apron-order-setting-updated-message` -> Evicts order setting cache.
- `menu-item-classification-updated` -> Evicts dietary classification cache.
- `ba-menu-item-inventory-updated-message` -> Evaluates OOS items and queues shortage reassignments.
- `oms-order-event` -> Records order state changes from OMS.
- `customer-updated` & `wonder-user-updated` -> Syncs subscriber user profile changes.
- `ba-cycle-item-sku-swap-message` -> Triggers swap tasks when recipes change SKUs.
- `fr-fulfillment-progress-update` & `fr-package-update` (Group `fr-update-event`) -> Updates tracking carrier details.

---

### Consumer Group Configuration & Concurrency

| Consumer Group | Topics Subscribed | Concurrency (prod) | Max Poll Records | Max Poll Bytes | Long Delay Alert Threshold |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Default** (`ba-auto-ship-service`) | Trigger topics, domain events, external updates | 1 | 100 | 3 MB | Default (core-ng) |
| **auth-cart** | `ba-auto-ship-auth-cart-message` | 1 (`task.authCart.KafkaConcurrency`) | 100 | 3 MB | 10 minutes |
| **generate-order** | `ba-auto-ship-generate-order-message` | 1 (`task.generateOrder.KafkaConcurrency`) | 100 | 3 MB | 10 minutes |
| **auto-assign-cart** | `ba-auto-ship-auto-assign-cart-message` | 1 (`task.createCart.KafkaConcurrency`) | 50 | 3 MB | 10 minutes |
| **fulfillment-event** | `auto-ship-fulfillment-event-message` | 1 (`task.fulfillmentEvent.KafkaConcurrency`) | 50 | 3 MB | 10 minutes |
| **rebalance-event** | `auto-ship-rebalance-event-message` | 1 (`task.rebalanceEvent.KafkaConcurrency`) | 50 (`rebalanceMaxPollRecords`) | 3 MB | 10 minutes |
| **remove-dish-event** | `auto-ship-remove-dish-event-message`, `ba-auto-ship-refresh-delivery-option-message` | 1 (`task.removeDishEvent.KafkaConcurrency`) | 50 | 3 MB | 10 minutes |
| **fulfillment-stuck-recovery** | `auto-ship-fulfillment-stuck-recovery-message` | 1 | 50 | 3 MB | Default |
| **fulfillment-event-sent-result-check** | `auto-ship-fulfillment-event-sent-result-check-message` | 1 | 50 | 3 MB | Default |
| **notification** | Notification & reassign trigger topics | 3 | 100 | 3 MB | 10 minutes |
| **customer-auto-ship-event** | `ba-customer-auto-ship-event-message` | 3 | 50 | 3 MB | 10 minutes |
| **fr-update-event** | `fr-fulfillment-progress-update`, `fr-package-update` | 10 (`task.frUpdateEvent.KafkaConcurrency`) | 50 (`maxPollRecords`) | 3 MB | 10 minutes |

### Blocking `poll()` Loops
Core-NG manages polling loops asynchronously within the framework's message listener threads. However:
- Handlers invoke synchronous, multi-second remote HTTP operations (e.g. `authCartService.systemAuthCart` calls Forter and BAOrderWebService; `generateOrderService.generateOrder` calls OrderWebService to place orders).
- If external dependencies experience elevated latency, the message handler thread is blocked, delaying the consumption of subsequent messages and triggering Kafka consumer group rebalances if execution exceeds max poll intervals.

---

## 4. Caching Strategy

The caching layer uses Redis as a shared distributed store along with selective in-process JVM local caches, configured in `CacheModule.java`.

### Cache Regions and Configuration

#### 1. Distributed Redis Caches
- `CycleCache.class`: TTL = 24 Hours (`Duration.ofDays(1)`). Key prefix `cache:ba-auto-ship-service:cycle`. Stores active cycle definitions.
- `UpcomingCycleCache.class`: TTL = 1 Hour (`Duration.ofHours(1)`). Caches upcoming available delivery cycles.
- `SpecificCycleCache.class`: TTL = 1 Hour (`Duration.ofHours(1)`). Caches individual cycle details.
- `DesiredCycleCache.class`: TTL = 24 Hours (`Duration.ofHours(24)`). Caches subscriber cycle range.
- `CycleItemCache.class`: TTL = 1 Hour (`Duration.ofHours(1)`). Caches cycle recipe items and servings.
- `CycleProductCache.class`: TTL = 1 Hour (`Duration.ofHours(1)`). Caches family meal and prepared meal product listings per facility.
- `OrderSettingCache.class`: TTL = 12 Hours (`Duration.ofHours(12)`). Caches general ordering parameters.
- `PricingSettingCache.class`: TTL = 12 Hours (`Duration.ofHours(12)`). Caches pricing tier rules.
- `CutoffSettingCache.class`: TTL = 12 Hours (`Duration.ofHours(12)`). Caches cutoff offset rules.
- `AutoShipBrandAssetCache.class`: TTL = 12 Hours (`Duration.ofHours(12)`). Caches brand metadata.
- `AutoShipPlanScanNumberCache.class`: TTL = 6 Hours (`Duration.ofHours(6)`). Stores cursor positions (`plan-scan-number` and `plan-scan-number-for-specific-cycle`) for batch plan scanning.
- `SimulationAutoShipPlanScanNumberCache.class`: TTL = 1 Hour (`Duration.ofHours(1)`). Cursor for assignment simulations.
- `MenuItemClassificationListCache.class`: TTL = 24 Hours (`Duration.ofHours(24)`). Caches dietary classification mappings.
- `ZipCodeMappingCache.class`: TTL = 24 Hours (`Duration.ofHours(24)`). Caches postal code to FC and timezone mappings.
- `ExperimentGroupCache.class`: TTL = 7 Days (`Duration.ofDays(7)`). Caches customer experimentation bucketing.
- `BAFacilityConfigCache.class`: TTL = 5 Minutes (`Duration.ofMinutes(5)`). Caches facility operational configurations.

#### 2. Local In-Memory JVM Caches (`.local()`)
- `FRCustomerSegmentCache.class`: TTL = 1 Hour (`Duration.ofHours(1)`). Registered via `addLocal(...)`. Stores employee segment and whitelist flags in local JVM heap.

#### 3. Distributed Redis Locks (`AutoShipRedisLockService`)
- Key pattern: `auto-ship-update-plan:{userId}` (TTL = 10 seconds). Locks subscriber plan during cart and plan updates.
- Key pattern: `auto-ship-notify-reassign-affected-user:{cycleId}` (TTL = 2 minutes). Locks batch notification sweeps.
- Key pattern: `scheduler-liveness-monitor:lock` (TTL = 90 seconds). Ensures single-replica execution of scheduler liveness probing.

---

### Cache Degradation and Redis Outage Behavior

1. **Standard Cache Miss**:
   Core-NG transparently invokes the provided `supplier` lambda on a cache miss, seamlessly loading the latest data from the underlying MySQL database or external microservice.
2. **Redis Outage / Auto-Fallback Status**:
   - The codebase incorporates `RedisCacheFallbackModule`, which probes Redis every 5 seconds and decorates cache stores with `FallbackCacheStore` to automatically bypass Redis during outages.
   - **CRITICAL FINDING**: In production (`conf/prod/resources/app.properties`), the feature switch is explicitly disabled:
     ```properties
     app.redisCacheFallback.enabled=false
     ```
   - Consequently, in production, if Redis is down or experiences connection timeouts, calls to `cache.get()` will throw `CacheException`, crashing user requests rather than gracefully degrading to database/service lookups.
3. **Fail-Closed Distributed Locks**:
   In `AutoShipRedisLockService.tryLock`:
   ```java
   try {
       return redis.set(key, value, expiration, true);
   } catch (Exception e) {
       logger.warn(..., "failed to acquire redis lock, treat as not acquired", e);
       return false;
   }
   ```
   If Redis is down, `tryLock` returns `false` (fail-closed). As a result, **all customer plan updates, cart confirmations, delivery address updates, and reassignment notifications are immediately rejected with HTTP 409 Conflict** during a Redis disruption.

---

## 5. Resilience Configuration

### Enhanced API Configurations & Circuit Breakers (`APIClientModule.java`)
The service extends `EnhancedAPIModule` (`app.enhancedapiclient`) to configure timeout tiers and fallbacks for select external clients:
- **ForterIntegrationWebService**:
  - Timeout: **1,200 ms**.
  - Fallback: `ForterIntegrationWebServiceFallback`. Allows cart processing to proceed in degraded mode if fraud scoring is temporarily unavailable.
- **FRFulfillmentWebService**:
  - Three configured tiers:
    - `FR150ms`: Timeout = **150 ms**, Fallback = `FRFulfillmentWebServiceFallback`.
    - `FR500ms`: Timeout = **500 ms**, Fallback = `FRFulfillmentWebServiceFallback`.
    - `FR1500ms`: Timeout = **1,500 ms**, Fallback = `FRFulfillmentWebServiceFallback`.
- **CustomerWebService**:
  - Timeout: **1,500 ms**.
  - Fallback: `CustomerWebServiceFallback`.
  - Custom HTTPClient: `connectTimeout = 1s`, `timeout = 20s`, `keepAlive = 5m`, `maxRetries = 2`, `retryWaitTime = 200ms`.
- **BARecommendationWebServiceV2**:
  - Two configured tiers:
    - `RECOMMENDATION1000ms`: Timeout = **1,000 ms**, Fallback = `BARecommendationWebServiceV2Fallback`.
    - `RECOMMENDATION1500ms`: Timeout = **1,500 ms**, Fallback = `BARecommendationWebServiceV2Fallback`.
- **SchedulerLivenessProbeClient**:
  - Connect Timeout: **1 second**.
  - Read Timeout: **5 seconds**.
  - Max Retries: **0**.

---

### Missing Circuit Breakers and Timeouts on Critical External Calls
All other downstream clients are bound using raw `api().client(...)` calls in `APIClientModule.java` without circuit breakers, bulkheads, or custom timeout definitions:
- **Order Service**: `BAOrderWebService`, `PriceWebService`, `OrderWebService` (`app.order.serviceURL`) — **Synchronous order placement and pricing critical path.**
- **Payment Service**: `PaymentServiceProviderSelectorWebService` (`app.payment.serviceURL`) — **Payment method gateway resolution.**
- **Inventory Service**: `MenuItemInventoryWebService`, `MenuItemSoftInventoryWebService`, `BOMenuItemInventoryWebService`, `BAFacilityConfigWebService` (`app.baInventory.serviceURL`) — **Inventory allocations and checks.**
- **Product Service**: `BABrandAssetWebService`, `BAGlobalSettingWebService`, `BACycleWebService`, `BACycleItemWebService`, `BACycleProductWebService`, `BAItemWebService`, `BAProductWebService`, `BOBAMenuItemWebService`, `BOCycleWebService`, `BACycleItemSkuSwapWebService`, `BlueApronOrderSettingWebService`, `BAMenuItemWebService`, `FBMWhitelistWebService` (`app.product.serviceURL`).
- **Customer Service**: `BACustomerSettingWebService`, `CustomerCreditCardWebService`, `CustomerAutoShipAddressWebService`, `CustomerCreditWebService`, `BOCustomerWebService` (`app.customer.serviceURL`).
- **Marketing Service**: `PromotionWebService`, `BAWinBackProgramWebService` (`app.marketing.serviceURL`).
- **Customer Wallet Service**: `CustomerWalletWebService` (`app.customerWallet.serviceURL`).
- **Membership Service**: `BAMembershipProductWebService`, `BACustomerMembershipWebService` (`app.baMembership.serviceURL`).
- **Shipping Service**: `BaggedDeliveryCapacityWebService` (`app.shipping.serviceURL`).
- **Setting Services**: `BAZipCodeMappingWebService`, `BOMenuItemClassificationWebService`.

**Resilience Risk**: These unconfigured clients inherit Core-NG framework defaults: **25-second connect/read timeout** and **3 retries**. If any core downstream dependency (especially `order-service` or `ba-inventory-service`) slows down under peak load, request handling threads and Kafka consumer workers will block for up to 25+ seconds per request, rapidly exhausting Undertow worker thread pools and halting event processing.

---

## 6. External Dependencies

| Downstream Service | Property Key | Client Interfaces | Configured Timeout | Circuit Breaker / Fallback |
| :--- | :--- | :--- | :--- | :--- |
| **Forter Integration Service** | `app.forterIntegration.serviceURL` | `ForterIntegrationWebService` | 1,200 ms | Yes (`ForterIntegrationWebServiceFallback`) |
| **FR Integration Service** | `app.frIntegration.serviceURL` | `FRFulfillmentWebService` | 150 ms / 500 ms / 1,500 ms | Yes (`FRFulfillmentWebServiceFallback`) |
| **Customer Service** | `app.customer.serviceURL` | `CustomerWebService` | 1,500 ms (Connect: 1s, Timeout: 20s) | Yes (`CustomerWebServiceFallback`) |
| **Recommendation Service** | `app.baRecommendation.serviceURL` | `BARecommendationWebServiceV2` | 1,000 ms / 1,500 ms | Yes (`BARecommendationWebServiceV2Fallback`) |
| **Order Service** | `app.order.serviceURL` | `BAOrderWebService`, `PriceWebService`, `OrderWebService` | Default (25s) | **None** |
| **Payment Service** | `app.payment.serviceURL` | `PaymentServiceProviderSelectorWebService` | Default (25s) | **None** |
| **Blue Apron Product Service**| `app.product.serviceURL` | `BACycleWebService`, `BACycleProductWebService`, `BAMenuItemWebService`, `BAProductWebService`, `BABrandAssetWebService`, `BAGlobalSettingWebService`, `BACycleItemWebService`, `BAItemWebService`, `BOBAMenuItemWebService`, `BOCycleWebService`, `BACycleItemSkuSwapWebService`, `BlueApronOrderSettingWebService`, `FBMWhitelistWebService` | Default (25s) | **None** |
| **BA Inventory Service** | `app.baInventory.serviceURL` | `MenuItemInventoryWebService`, `MenuItemSoftInventoryWebService`, `BOMenuItemInventoryWebService`, `SimulationMenuItemInventoryWebService`, `SimulationMenuItemSoftInventoryWebService`, `BAFacilityConfigWebService` | Default (25s) | **None** |
| **Customer Service (Sub-APIs)**| `app.customer.serviceURL` | `BACustomerSettingWebService`, `CustomerCreditCardWebService`, `CustomerAutoShipAddressWebService`, `CustomerCreditWebService`, `BOCustomerWebService` | Default (25s) | **None** |
| **BA Membership Service** | `app.baMembership.serviceURL` | `BAMembershipProductWebService`, `BACustomerMembershipWebService` | Default (25s) | **None** |
| **Marketing Service** | `app.marketing.serviceURL` | `PromotionWebService`, `BAWinBackProgramWebService` | Default (25s) | **None** |
| **Customer Wallet Service** | `app.customerWallet.serviceURL` | `CustomerWalletWebService` | Default (25s) | **None** |
| **Shipping Service** | `app.shipping.serviceURL` | `BaggedDeliveryCapacityWebService` | Default (25s) | **None** |
| **Order Search Service** | `app.orderSearch.serviceURL` | `BAAppOrderSearchWebService` | Default (25s) | **None** |
| **Order Number Service** | `app.orderNumber.serviceURL` | `OrderNumberWebService` | Default (25s) | **None** |
| **BA Setting Service** | `app.baSetting.serviceURL` | `BAZipCodeMappingWebService` | Default (25s) | **None** |
| **Wonder Setting Service** | `app.wonderSetting.serviceURL` | `BOMenuItemClassificationWebService` | Default (25s) | **None** |
| **Scheduler Service (Probe)** | `app.blueapronScheduler.serviceURL` | Custom probe via HTTPClient | Connect: 1s, Read: 5s | In-memory fallback on alert |

---

## 7. Scalability Signals

### Stateful Operations and In-Process State
1. **Uncoordinated Plan Cursor State in Redis**:
   `AutoShipPlanScanNumberCacheService` stores cursor keys `plan-scan-number` and `plan-scan-number-for-specific-cycle` in Redis. When `AutoShipCreateCartTaskTriggerService` scans plans to build carts:
   ```java
   long currentNumber = planScanNumberCacheService.getCurrentPlanScanNumber();
   ...
   planScanNumberCacheService.updatePlanScanNumber(currentNumber);
   ...
   planScanNumberCacheService.updatePlanScanNumber(0L);
   ```
   If two task workers execute this simultaneously, they overwrite each other's cursor positions, leading to missed plans or duplicate batch pool insertions.
2. **In-Memory Local Cache Inconsistency (`FRCustomerSegmentCache`)**:
   `FRCustomerSegmentCache` is configured as a local JVM cache (`.local()`) with a 1-hour TTL. It does not subscribe to customer update events for cache invalidation. In a multi-replica deployment, individual pods will hold differing customer segment representations for up to 1 hour after upstream updates.
3. **Simulation Pod Singleton Assumption**:
   `SimulationAutoShipAssignmentTaskRestartService` runs `onStartup(restartService::checkAndRestart)` to restart uncompleted simulations directly from MySQL. The workload relies strictly on `replicas: 1` in Kubernetes to avoid duplicate startup restarts.

---

### Scheduled Tasks and Replica Coordination
1. **Liveness Probe Concurrency Guard**:
   The in-process `SchedulerLivenessCheckJob` runs once per minute across all running pods (7 to 9 pods across the three deployments). It coordinates execution via Redis lock `scheduler-liveness-monitor:lock`. However, during a Redis outage, every pod independently drops back to in-memory tracking, causing duplicate Slack alerts.
2. **Batch Trigger Concurrency Vulnerability (Missing Distributed Locks)**:
   The batch trigger services (`AutoShipCreateCartTaskTriggerService`, `AutoShipAuthCartTaskTriggerService`, `AutoShipGenerateOrderTaskTriggerService`) rely exclusively on database checks:
   ```java
   if (authCartTaskBatchQueryService.existingProcessingBatch()) {
       return;
   }
   ```
   There is **no distributed lock** guarding the execution of `triggerTask()`. If two replicas consume or execute the trigger message concurrently, both can evaluate `existingProcessingBatch() == false`, causing both to create parent batch rows in MySQL and publish duplicate Kafka task messages for identical carts.

---

## Key Scalability Signals (Top 5 Concerns)

### 1. Severe MySQL Row Lock Contention and Deadlocks on Batch Completion Counters
- **Mechanism**: Every individual task processed within a batch (batches range from 800 to 1,000 tasks each) updates the parent batch row upon completion:
  ```sql
  UPDATE auto_ship_auth_cart_task_batches
  SET completed_task = completed_task + 1, updated_time = ?, updated_by = ?
  WHERE id = ?
  ```
- **Impact**: In `AutoShipAuthCartTaskService` and `AutoShipGenerateOrderTaskService`, these counter increments are executed without serial row locking. When multiple consumer threads or pods process batch tasks in parallel, up to 1,000 tasks simultaneously contend for the same InnoDB row lock, causing severe lock wait timeouts and deadlocks. (This failure mode was previously acknowledged in `BAAutoShipRebalanceEventMessageHandler.java:103`, where developers had to inject `SELECT ... FOR UPDATE` row locks to stop deadlocks).

### 2. Strict Batch Pipeline Serialization (`existingProcessingBatch`) Bottleneck
- **Mechanism**: All batch triggers enforce a strict single-batch check (`existingProcessingBatch()`) that aborts task initiation if any previous batch is in status `PROCESSING`. Simultaneously, production Kafka consumer concurrency for task topics is capped at **1** (`task.authCart.KafkaConcurrency=1`, `task.generateOrder.KafkaConcurrency=1`, `task.createCart.KafkaConcurrency=1`).
- **Impact**: The entire batch pipeline operates as a strictly serialized conveyor belt: 800 to 1,000 carts must be processed sequentially by a single consumer thread before the next batch can start. If subscriber volume grows or downstream services experience latency (e.g. 200 ms per order placement), a single batch of 800 orders takes over 160 seconds to complete. If a worker pod crashes or a batch gets stalled, the entire system stops initiating new batches until the 5-minute timeout recovery job runs.

### 3. Heavy Unindexed Joins and Aggregations on Multi-Million Row Tables
- **Mechanism**: Core reporting and back-office services run unindexed or straight-joined queries against high-volume tables (`auto_ship_carts` with 1.38M rows, `auto_ship_cart_items` with 1.98M rows, `auto_ship_cart_fulfillment_outboxes` with 7.01M rows).
- **Impact**: The `BOBAAutoShipCartItemQueryService` executes 4 parallel `STRAIGHT_JOIN` queries across the full cart and item tables for cycle-level item statistics. Similarly, `BAAutoShipMigrationService` explicitly suppresses slow SQL warnings (`Database.suppressSlowSQLWarning(true)`). These queries bypass indexes, force temporary disk tables, and trigger significant CPU spikes and replica lag on Azure MySQL.

### 4. Production Redis Auto-Fallback is Disabled (`app.redisCacheFallback.enabled=false`) with Fail-Closed Distributed Locks
- **Mechanism**: While a resilience fallback module (`RedisCacheFallbackModule`) was implemented to bypass Redis during outages, it is turned off in production (`conf/prod/resources/app.properties`). Concurrently, `AutoShipRedisLockService` is fail-closed.
- **Impact**: In the event of a Redis network partition, failover, or latency spike, cache reads will throw `CacheException` instead of falling back to database reads. Furthermore, all customer cart confirmations, plan updates, address updates, and reassignment notifications will fail immediately with HTTP 409 Conflict because Redis locks cannot be acquired.

### 5. Cascading Failure Risk from Unshielded External HTTP Calls (25-Second Timeout)
- **Mechanism**: Over 35 outbound WebService interfaces (including `BAOrderWebService`, `PriceWebService`, `MenuItemInventoryWebService`, and `PaymentServiceProviderSelectorWebService`) lack Enhanced API circuit breakers, bulkheads, or custom timeout configs, defaulting to Core-NG's 25-second timeout and 3 retries.
- **Impact**: If a core downstream service like `order-service` or `ba-inventory-service` degrades or becomes unresponsive during peak order generation or cutoff windows, worker threads and Kafka consumers will hang for 25+ seconds per call. This quickly exhausts thread pools, causes Kafka consumer group rebalances, and halts both synchronous user interactions and background cart processing.
