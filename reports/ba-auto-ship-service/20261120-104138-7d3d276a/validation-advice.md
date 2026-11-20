## Check 8 — Recommendation Soundness: FAIL

### Findings

**[PASS]** 5 items verified: Recommendation 1 (Implement Table Archiving and Purging for Fulfillment Events), Recommendation 5 (Right-Size Container Resource Requests to Reclaim Cluster Capacity), Executive Summary Finding 1 (Unbounded MySQL Fulfillment Event Table Growth), Executive Summary Finding 4 (Elevated Kafka Consumer Lag on Rebalance Events), Executive Summary Finding 5 (Over-Allocated Pod CPU and Memory Requests)
  - Reported: `Claims regarding fulfillment event table growth (>10 GB limit), Kafka consumer lag peaks (1756 msgs), and container CPU/memory request right-sizing`
  - Actual: `Verified against MySQL table telemetry (31.37 GB), Datadog Kafka lag metrics, and K8s deployment manifests (2 CPU / 1.5Gi request vs 0.0378 cores / 543.76 MiB usage)` [Source: mysql-tables.md, metrics.md, and code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-service.yml]
  - Dimension: Persistence
  - Detail: all matched.

**[FAIL]** Executive Summary Finding 2 (Severe MongoDB Query Targeting Spikes on Cleanup)
  - Reported: `Collection cleanup operations trigger extreme query targeting degradation with a peak of 2090.72 documents scanned per returned document due to a dropped timestamp index on the tracking collection.`
  - Actual: `Index on created_time is actively maintained with 90-day TTL; BLUE-6977 dropped the unexpired index specifically to recreate it with expiration in migration Main.java` [Source: code → backend/ba-auto-ship-service-mongo-migration/src/main/java/Main.java:L96 and backend/ba-auto-ship-service-mongo-migration/src/main/java/app/ba/autoship/script/AutoShipCartItemTrackingsScript.java:L13]
  - Dimension: Persistence
  - Detail: Premise contradicted by primary source (Rule 8a). The report claims cleanup operations run unindexed because AutoShipCartItemTrackingsScript.java dropped the timestamp index. However, AutoShipCartItemTrackingsScript only dropped the legacy index created_time_1 as part of ticket BLUE-6977 so Main.java could recreate it with a 90-day TTL (mongo.createIndex("auto_ship_cart_item_trackings", ascending("created_time"), new IndexOptions().expireAfter(90L, TimeUnit.DAYS))). The collection is not unindexed. Furthermore, query targeting ratio (2090.72) is a cluster-level shared metric for prod-blueapron-main, not scoped to this collection or service (Rule 8f).

**[FAIL]** Recommendation 2: Re-establish Missing Index on MongoDB Tracking Collection (High Priority)
  - Reported: `Dropping the created_time index in AutoShipCartItemTrackingsScript.java forces AutoShipCartItemTrackingCleanupService.java to execute unindexed collection scans with Filters.lt("created_time", cutoffTime) across 44536174 documents... Action: Add an explicit index on { created_time: 1 } for collection auto_ship_cart_item_trackings in BAMongoModule.java`
  - Actual: `TTL index on created_time already exists in mongo migration; BAMongoModule does not manage Mongo indexes in core-ng` [Source: code → backend/ba-auto-ship-service-mongo-migration/src/main/java/Main.java:L96 and backend/ba-auto-ship-service/src/main/java/app/ba/autoship/BAMongoModule.java:L35]
  - Dimension: Persistence
  - Detail: Fails 8a (premise contradicted by primary source), 8c (action already implemented), 8f (shared cluster metric), and 8g (wrong framework idiom). Main.java:L96 already creates ascending("created_time") with 90-day expiration; AutoShipCartItemTrackingsScript.java dropped the index only to recreate it with the TTL option. Additionally, 44,536,174 documents is the entire ba-auto-ship database object count, not this collection. In core-ng, Mongo indexes are managed in the companion migration module Main.java, not in BAMongoModule.java.

**[WARNING]** Executive Summary Finding 3 (Transient MongoDB Peak Read Latency Degradation)
  - Reported: `Cluster-level read operations experienced intermittent latency spikes reaching a peak of 128.87 ms during heavy query execution intervals.`
  - Actual: `Shared cluster metric max:mongodb.atlas.oplatencies.reads.avg{clustername:prod-blueapron-main} reflects entire multi-tenant Atlas cluster` [Source: Datadog API → query-metrics("max:mongodb.atlas.oplatencies.reads.avg{clustername:prod-blueapron-main}.rollup(max, 86400)")]
  - Dimension: Persistence
  - Detail: Fails 8f (shared-resource metric). The 128.87 ms read latency spike is a cluster-wide metric for prod-blueapron-main, which hosts multiple consumer domain services. It is presented as a critical finding for ba-auto-ship-service without service-level attribution.

**[WARNING]** Recommendation 3: Eliminate N+1 Entity Loading in Cart Fulfillment Handlers (Medium Priority)
  - Reported: `In BAAutoShipFulfillmentEventController.java, the fulfillment controller loops through autoShipCartIds executing 4 separate queries (cart, cartItems, familyMealChoiceValues, address) in sequential transactions per cart, and BAAutoShipGenerateOrderMessageHandler.java fetches cart items individually via cartItemQueryService.searchByCartId(cartId) rather than batch loading.`
  - Actual: `BAAutoShipFulfillmentEventController loops per cartId (valid N+1); BAAutoShipGenerateOrderMessageHandler is a single-record Kafka handler that does not call cartItemQueryService` [Source: code → backend/ba-auto-ship-service/src/main/java/app/ba/autoship/web/controller/BAAutoShipFulfillmentEventController.java:L43-L52 and backend/ba-auto-ship-service/src/main/java/app/ba/autoship/kafka/BAAutoShipGenerateOrderMessageHandler.java:L23-L28]
  - Dimension: Persistence
  - Detail: Fails 8a in part (partial premise contradiction). While BAAutoShipFulfillmentEventController.java genuinely executes sequential database queries in a loop per cart ID inside individual transactions, BAAutoShipGenerateOrderMessageHandler.java is a standard single-message Kafka consumer processing one order generation task per invocation. It does not loop over carts, nor does it inject or call cartItemQueryService.searchByCartId(cartId). The N+1 finding correctly identifies the issue in the controller, but incorrectly characterizes the Kafka message handler.

**[WARNING]** Recommendation 4: Optimize Correlated Subquery and Heavy Cart Item Aggregations (Medium Priority)
  - Reported: `Refactor searchPendingAuth in AutoShipCartQueryService.java to join against an indexed sub-selection or maintain a indexed status flag on auto_ship_carts, ensuring composite index (auto_ship_cart_id, status) is fully utilized.`
  - Actual: `No composite index (auto_ship_cart_id, status) exists; auto_ship_cart_items has no status column` [Source: code → backend/ba-auto-ship-service-db-migration/src/main/resources/db/migration/R__auto_ship_cart_items.sql:L1-L25 and backend/ba-auto-ship-service/src/main/java/app/ba/autoship/service/query/AutoShipCartQueryService.java:L174]
  - Dimension: Persistence
  - Detail: Discrepancy in proposed action (Rule 8a / Rule 8d). The finding correctly observes that searchPendingAuth uses an EXISTS subquery against auto_ship_cart_items and that aggregateByMenuItem performs a 5-dimension GROUP BY. However, the proposed action specifies 'ensuring composite index (auto_ship_cart_id, status) is fully utilized'. In the primary schema DDL, auto_ship_cart_items has no status column, and no composite index on (auto_ship_cart_id, status) exists on either table. auto_ship_cart_items already indexes auto_ship_cart_id as the leading prefix of ix_cart_id_menu_item_servings_qty.

**[SKIPPED]** Outbound HTTP client timeouts & distributed locking recommendations
  - Reported: `N/A`
  - Actual: `N/A` [Source: 2026-11-20-review.md]
  - Dimension: General
  - Detail: out of scope for now (no recommendations on explicit outbound HTTP client timeouts or distributed locking for periodic background Kafka handlers were included in the report).

### Summary
Check 8 resulted in FAIL due to invalid premises and recommendations regarding MongoDB index management. Recommendation 2 and Executive Summary Finding 2 falsely claimed that the created_time index was dropped leaving the collection unindexed, whereas ba-auto-ship-service-mongo-migration actively creates the TTL index; additionally, core-ng does not configure Mongo indexes in application modules. Recommendation 3 and Recommendation 4 contain valid performance observations (N+1 loading in fulfillment controller, correlated EXISTS subquery) but include inaccurate details (mischaracterizing a single-message Kafka consumer and citing a non-existent composite index).
