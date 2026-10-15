# Service Code Analysis — Business Context Extraction: mobile-api

This document provides a factual, detailed architectural and code-level context extraction for **mobile-api**, synthesized from the application source code (`frontend/mobile-api`, `frontend/mobile-api-db-migration`, `frontend/mobile-api-domain`, and `frontend/mobile-api-interface`).

---

## 1. API Surface

The `mobile-api` service acts as the primary Backend-for-Frontend (BFF) and API aggregation gateway for the consumer mobile applications (iOS and Android). It exposes REST endpoints via the `core-ng` web service framework across 45 WebService interfaces, plus custom HTTP route controllers for image resizing and internal database migrations.

### 1.1 REST Endpoints Inventory

A total of **265 REST endpoints** are defined and exposed by the service:

#### Account Management (`AccountWebService`)
| Method | Path | Method Signature |
| :--- | :--- | :--- |
| `GET` | `/account/profile` | `ProfileView getProfile()` |
| `PUT` | `/v2/account/profile` | `void updateProfileV2(UpdateProfileRequest request)` |
| `PUT` | `/account/update-email` | `void updateEmail(UpdateEmailRequest request)` |
| `GET` | `/account/preferences` | `GetPreferencesResponse getPreferences()` |
| `PUT` | `/account/preferences` | `void updatePreferences(UpdatePreferencesRequest request)` |
| `GET` | `/account/settings` | `GetSettingsResponse settings()` |
| `PUT` | `/account/settings` | `void updateSettings(UpdateSettingsRequest request)` |
| `PUT` | `/account/settings/marketing-text` | `void updateMarketingText(UpdateMarketingTextSettingRequest request)` |
| `GET` | `/account/credit-history` | `SearchCreditHistoryResponse searchCreditHistory(SearchCreditHistoryRequest request)` |
| `GET` | `/account/allergen` | `ListAccountAllergenResponse listAccountAllergen()` |
| `PUT` | `/account/allergen` | `void updateAccountAllergen(UpdateAccountAllergenRequest request)` |
| `GET` | `/account/tags` | `ListTagResponse listTags()` |
| `POST` | `/account/tag` | `void addTag(AddTagRequest request)` |
| `POST` | `/account/delete-profile` | `void deleteAccount()` |
| `GET` | `/account/corporate-client` | `ListCorporateClientResponse listCorporateClient()` |
| `PUT` | `/account/corporate-client/switch` | `void switchCorporateClient(SwitchCorporateClientRequest request)` |
| `PUT` | `/account/corporate-client/leave` | `void leaveCorporateClient(LeaveCorporateClientRequest request)` |
| `PUT` | `/v2/account/corporate-client/join` | `JoinCorporateClientResponseV2 joinCorporateClientV2(JoinCorporateClientRequestV2 request)` |
| `GET` | `/account/corporate-client/:code` | `GetCorporateClientResponse getCorporateClient(@PathParam("code") String code)` |
| `GET` | `/account/corporate-client/status/:code` | `CheckCorporateClientStatusResponse checkCorporateClientStatus(@PathParam("code") String code)` |

#### Address & Geolocation (`AddressWebService`, `MapWebService`)
| Method | Path | Method Signature |
| :--- | :--- | :--- |
| `GET` | `/address/delivery-zone` | `CheckDeliveryZoneResponse checkDeliveryZone(CheckDeliveryZoneRequest request)` |
| `GET` | `/address/time-zone` | `GetTimeZoneResponse getTimeZone(GetTimeZoneRequest request)` |
| `GET` | `/v2/address` | `ListAddressResponseV2 listAddressV2()` |
| `POST` | `/v2/address` | `CreateAddressResponseV2 createAddressV2(CreateAddressRequestV2 request)` |
| `PUT` | `/v2/address/:id` | `UpdateAddressResponseV2 updateAddressV2(@PathParam("id") String id, UpdateAddressRequestV2 request)` |
| `DELETE` | `/address/:id` | `void deleteAddress(@PathParam("id") String id)` |
| `PUT` | `/address-check` | `CheckAddressResponse checkAddress(CheckAddressRequest request)` |
| `PUT` | `/address-change` | `ChangeAddressResponse changeAddress(ChangeAddressRequest request)` |
| `PUT` | `/address/reverse-geo-code` | `ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request)` |
| `PUT` | `/v2/map/autocomplete` | `AutocompleteResponseV2 autocompleteV2(AutocompleteRequest request)` |
| `GET` | `/map/place/:id` | `GetPlaceResponse getPlace(@PathParam("id") String id)` |
| `PUT` | `/map/query-place` | `QueryPlaceResponse queryPlace(QueryPlaceRequest request)` |

#### Authentication, User & Session (`SessionWebService`, `DeviceWebService`, `WonderUserWebService`)
| Method | Path | Method Signature |
| :--- | :--- | :--- |
| `POST` | `/v2/session` | `CreateSessionResponse createSessionV2(CreateSessionRequestV2 request)` |
| `GET` | `/session` | `GetSessionResponse getSession()` |
| `PUT` | `/session/zip-code` | `UpdateSessionZipCodeResponse updateZipCode(UpdateSessionZipCodeRequest request)` |
| `GET` | `/session/address` | `GetSessionAddressResponse getAddress()` |
| `PUT` | `/session/leave-corporate-client` | `void leaveCorporateClient()` |
| `PUT` | `/session/switch-corporate-client` | `void switchCorporateClient(SessionSwitchCorporateClientRequest request)` |
| `POST` | `/session/apply-promotion-code` | `ApplyPromotionCodeResponse applyPromotionCode(ApplyPromotionCodeRequest request)` |
| `PUT` | `/device/messaging-token` | `void updateMessagingToken(UpdateMessagingTokenRequest request)` |
| `POST` | `/user/verify-phone-request` | `SendVerificationCodeResponse sendVerificationCode(SendVerificationCodeRequest request)` |
| `POST` | `/user/verify-phone` | `VerifyCodeResponse verifyPhone(VerifyCodeRequest request)` |
| `POST` | `/v2/user/verify-phone-request` | `SendVerificationCodeResponse sendVerificationCodeV2(SendVerificationCodeRequest request)` |
| `POST` | `/v2/user/verify-phone` | `VerifyCodeResponse verifyPhoneV2(VerifyCodeRequest request)` |
| `POST` | `/v3/user/register` | `UserRegisterResponseV2 registerV3(UserRegisterRequestV2 request)` |
| `PUT` | `/v4/user/login` | `UserLoginResponseV3 loginV4(UserLoginRequestV3 request)` |
| `PUT` | `/v3/user/login-by-phone` | `UserLoginResponseV3 loginByPhoneV3(UserLoginByPhoneRequestV2 request)` |
| `POST` | `/user/login-by-apple-token` | `UserLoginResponseV3 loginByAppleToken(UserLoginByAppleTokenRequest request)` |
| `POST` | `/user/login-by-google-token` | `UserLoginResponseV3 loginByGoogleToken(UserLoginByGoogleTokenRequest request)` |
| `PUT` | `/user/logout` | `UserLogoutResponse logout()` |
| `PUT` | `/user/send-password-reset-email` | `void sendPasswordResetEmail(SendPasswordResetEmailRequest request)` |
| `PUT` | `/user/check-password-reset-token` | `CheckPasswordResetTokenResponse checkPasswordResetToken(CheckPasswordResetTokenRequest request)` |
| `PUT` | `/user/reset-password` | `void resetPassword(ResetPasswordRequest request)` |
| `PUT` | `/user/change-password` | `void changePassword(ChangePasswordRequest request)` |
| `PUT` | `/v2/user/ba/reset-password` | `void baUserResetPasswordV2(BAUserResetPasswordRequest request)` |
| `PUT` | `/user/password/existence` | `CheckPasswordExistenceResponse checkPasswordExistence()` |
| `PUT` | `/user/check` | `CheckUserResponse checkUser(CheckUserRequest request)` |
| `PUT` | `/user/password/create` | `void createPassword(CreatePasswordRequest request)` |
| `PUT` | `/user/reset-password-by-email` | `void resetPasswordByEmail(UserResetPasswordByEmailRequest request)` |
| `PUT` | `/user/ba/reset-password-to-login` | `UserLoginResponseV3 baUserResetPasswordToLogin(BAUserResetPasswordToLoginRequest request)` |
| `PUT` | `/user/deletion-requested-check` | `UserDeletionRequestedCheckResponse checkDeletionRequested(UserDeletionRequestedCheckRequest request)` |

#### Cart (`WonderCartWebService`)
| Method | Path | Method Signature |
| :--- | :--- | :--- |
| `GET` | `/v7/cart` | `GetWonderCartResponseV7 getV7()` |
| `POST` | `/v2/cart/menu-item` | `AddCartItemResponse addMenuItemV2(AddCartMenuItemRequestV2 request)` |
| `POST` | `/v3/cart/bundle-item` | `AddCartItemResponse addBundleItemV3(AddCartBundleItemRequestV2 request)` |
| `PUT` | `/v3/cart/menu-item/:itemId` | `UpdateCartItemResponse updateMenuItemV3(@PathParam("itemId") String itemId, UpdateCartMenuItemRequestV2 request)` |
| `PUT` | `/v3/cart/bundle-item/:itemId` | `UpdateCartItemResponse updateBundleItemV3(@PathParam("itemId") String itemId, UpdateCartBundleItemRequestV2 request)` |
| `DELETE` | `/v7/cart/item/:itemId` | `DeleteCartItemResponse deleteCartItemV7(@PathParam("itemId") String itemId)` |
| `DELETE` | `/v3/cart` | `void clearV3()` |
| `PUT` | `/v5/cart/reorder` | `ReorderResponse reorderV5(ReorderRequest request)` |
| `PUT` | `/v3/cart/dining-option` | `SwitchCartDiningOptionResponse switchDiningOptionV3(SwitchCartDiningOptionRequest request)` |
| `PUT` | `/v3/cart/corporate-client` | `SwitchCorporateClientResponse switchCorporateClient(SwitchCartCorporateClientRequest request)` |

#### Checkout & Ordering (`WonderCheckoutWebService`)
| Method | Path | Method Signature |
| :--- | :--- | :--- |
| `GET` | `/v4/checkout` | `GetCheckoutResponseV4 getV4()` |
| `PUT` | `/v8/checkout/check` | `CheckCheckoutResponseV8 checkV8(CheckCheckoutRequestV8 request)` |
| `PUT` | `/v8/checkout/order/check` | `CheckPlaceOrderResponseV8 checkOrderV8(CheckPlaceOrderRequestV8 request)` |
| `POST` | `/v3/checkout/order` | `PlaceOrderResponse placeOrderV3(PlaceOrderRequestV2 request)` |
| `PUT` | `/v3/checkout/refresh` | `RefreshCheckoutResponseV3 refreshV3()` |
| `PUT` | `/checkout/donation` | `void updateDonation(UpdateCustomerOrderDonationRequest request)` |
| `PUT` | `/v3/checkout/eta` | `GetCheckoutOrderETAResponse getCheckoutOrderETAV3(GetCheckoutOrderETARequest request)` |
| `PUT` | `/v2/checkout/schedule-order-time-slot` | `QueryScheduleOrderTimeSlotResponse queryTimeSlotV2(QueryScheduleOrderTimeSlotRequest request)` |

#### Order Fulfillment, Status & History (`WonderOrderWebService`, `WonderGeneralOrderWebService`, `WonderOrderRatingWebService`, `MarketplaceOrderWebService`)
| Method | Path | Method Signature |
| :--- | :--- | :--- |
| `GET` | `/v4/order/:orderId` | `GetOrderResponseV4 getV4(@PathParam("orderId") String orderId)` |
| `GET` | `/v4/order/:orderId/status` | `GetOrderStatusResponse getStatusV4(@PathParam("orderId") String orderId)` |
| `GET` | `/v2/order` | `ListOrderResponseV2 listV2(ListOrderRequest request)` |
| `GET` | `/v3/order/active` | `ListActiveOrderResponseV2 listActiveOrdersV3()` |
| `GET` | `/v2/order/past` | `ListPastOrderResponseV2 listPastOrdersV2(ListPastOrderRequest request)` |
| `GET` | `/order/route/:orderId` | `GetOrderRouteResponse getRoute(@PathParam("orderId") String orderId)` |
| `GET` | `/order/receipt/:orderId` | `GetOrderReceiptResponse getReceipt(@PathParam("orderId") String orderId)` |
| `POST` | `/order/:orderId/tip` | `void addTip(@PathParam("orderId") String orderId, AddOrderTipRequest request)` |
| `GET` | `/order/:orderId/modify-history` | `ListOrderModifyHistoryResponse listModifyHistory(@PathParam("orderId") String orderId)` |
| `GET` | `/order/general/active` | `ListGeneralActiveOrderResponse listGeneralActiveOrders()` |
| `GET` | `/order/general/past` | `ListGeneralPastOrderResponse listGeneralPastOrders(ListGeneralPastOrderRequest request)` |
| `POST` | `/order/rating` | `void rateOrder(OrderRatingRequest request)` |
| `GET` | `/order/:orderId/rating` | `GetOrderRatingResponse getOrderRating(@PathParam("orderId") String orderId)` |
| `GET` | `/v2/order/marketplace/past` | `ListMarketplacePastOrderResponse listPastOrders(ListMarketplacePastOrderRequest request)` |
| `GET` | `/v2/order/marketplace/:orderId` | `GetMarketplaceOrderDetailResponse getOrderDetail(@PathParam("orderId") String orderId)` |

#### Discovery, Search & Menus (`MobileSearchWebService`, `WonderRestaurantWebService`, `WonderDishWebService`, `WonderDishWebServiceV2`, `WonderMenuItemWebService`, `WonderFeaturedCollectionWebService`, `WonderHDRWebService`, etc.)
| Method | Path | Method Signature |
| :--- | :--- | :--- |
| `PUT` | `/search/v3` | `WonderSearchResponseV3 searchV3(WonderSearchRequestV3 request)` |
| `PUT` | `/search/type-ahead/v2` | `WonderTypeAheadResponseV2 typeAheadV2(WonderTypeAheadRequestV2 request)` |
| `PUT` | `/search/cuisine-filter` | `WonderCuisineFilterQueryResponse cuisineFilter(WonderCuisineFilterQueryRequest request)` |
| `PUT` | `/restaurant/v2` | `SearchRestaurantResponseV2 searchRestaurantV2(SearchRestaurantRequestV2 request)` |
| `PUT` | `/restaurant/:id` | `GetRestaurantResponse get(@PathParam("id") String id, GetRestaurantRequest request)` |
| `GET` | `/restaurant/v2/:id/about-page` | `GetAboutPageResponse getAboutPageV2(@PathParam("id") String id)` |
| `PUT` | `/dish` | `WonderDishResponse getDishes(WonderDishRequest request)` |
| `PUT` | `/dish/search` | `SearchDishesResponse searchDishes(SearchDishesRequest request)` |
| `GET` | `/dish/dish-module` | `WonderDishModuleResponse getDishModules()` |
| `PUT` | `/v2/dish` | `WonderDishResponseV2 getDishesV2(WonderDishRequestV2 request)` |
| `PUT` | `/menu-item/:id` | `WonderGetMenuItemResponse get(@PathParam("id") String id, WonderGetMenuItemRequest request)` |
| `GET` | `/featured-collections` | `ListFeaturedCollectionWithItemResponse listFeaturedCollections()` |
| `GET` | `/featured-collections/:id` | `GetFeaturedCollectionResponse getFeaturedCollection(@PathParam("id") String id)` |
| `PUT` | `/wonder-hdr/search` | `SearchHDRResponse searchHDR(SearchHDRRequest request)` |
| `GET` | `/wonder-hdr/:hdrId` | `GetHDRResponse getHDR(@PathParam("hdrId") String hdrId)` |
| `PUT` | `/V3/home-page` | `MobileHomePageResponseV3 getHomePageV3(MobileHomePageRequestV3 request)` |

#### App Store Ratings, Settings, Payments, Support & Notifications
- `WonderAppStoreRatingWebService`: `GET /app-store-rating/prompt-action`, `PUT /app-store-rating/prompt-action`, `PUT /app-store-rating/prompt-trigger-consumed`.
- `CreditCardWebService` & `PaymentWebService`: Credit card addition, deletion, setup intents, Braintree client tokens, payment provider selector.
- `WonderSettingWebService`: `GET /v2/setting`.
- `VersionWebService`: `GET /version` (App version check and upgrade enforcement).
- `WonderMessageWebService`: `POST /message/pull` (deprecated), `POST /message/v2/pull` (polling for real-time order and marketing notifications).
- `WonderSupportWebService`: Zendesk support article queries.
- `WonderSelfServiceWebService` & `WonderTipWebService`: Self-service refund and tip configuration.

#### Dynamic Images (`ImageModule`)
| Method | Path | Method Signature |
| :--- | :--- | :--- |
| `GET` | `/image/:size/:key(*)` | `ImageController::execute` (Resizes and proxies image assets) |

#### Database Migration Routes (`MigrateModule`)
| Method | Path | Method Signature |
| :--- | :--- | :--- |
| `PUT` | `/_app/migrate-session-expired-time` | `MigrateSessionExpireTimeController` |
| `POST` | `/_app/migrate-session-nickname` | `MigrateSessionNicknameController` |
| `POST` | `/_app/migrate-login-histories` | `MigrateLoginHistoryController` |
| `GET` | `/_app/migrate-version-configuration` | `MigrateVersionConfigurationController` |
| `PUT` | `/_app/migrate-non-city-address` | `MigrateNonCityAddressController` |
| `PUT` | `/_app/migrate-wonder-marketing-setting-initialization` | `MigrateWonderMarketingSettingInitializationController` |

---

### 1.2 Critical Path Endpoints (Synchronous, User-Facing)

The critical user journeys depend directly on the following synchronous paths:

1. **App Launch & Initialization**:
   - `GET /version`: Checks version compatibility and mandatory update requirements.
   - `POST /v2/session`: Generates/retrieves user session, initializes visitor identity, and validates hardware device ID.
   - `PUT /session/zip-code`, `PUT /address-check`, `PUT /address-change`: Resolves address serviceable status, delivery zone, and available delivery options.
2. **Catalog Discovery & Search**:
   - `PUT /V3/home-page`: Loads homepage carousels, active announcements, and featured collections.
   - `PUT /restaurant/v2`: Fetches restaurants for the delivery zone or pickup HDR.
   - `PUT /search/v3` & `PUT /search/type-ahead/v2`: Real-time text search and typeahead across dishes and restaurants.
   - `PUT /dish` & `PUT /v2/dish`: Loads dish collections, cuisine filters, and pricing.
   - `PUT /menu-item/:id`: Loads individual menu item options, modifiers, allergens, and inventory.
3. **Cart Operations**:
   - `GET /v7/cart`: Evaluates cart state, price calculations, and line items.
   - `POST /v2/cart/menu-item` & `PUT /v3/cart/menu-item/:itemId`: Mutates items and triggers inventory checks.
4. **Checkout & Order Placement**:
   - `PUT /v8/checkout/check`: Validates fee rules, promotional codes, delivery ETA windows, and delivery address.
   - `PUT /v8/checkout/order/check`: Pre-flight verification before order charging.
   - `POST /v3/checkout/order`: Synchronously calls payment gateways, fraud detection (Forter), and the order management service (`order-service`).
5. **Real-time Order Tracking**:
   - `GET /v4/order/:orderId/status`: Order fulfillment lifecycle status polling.
   - `POST /message/v2/pull`: Periodic client polling for in-app updates and notification banners.

---

### 1.3 Batch and Scheduled Jobs

1. **`cleanup-local-delivery-zone-cache`**:
   - **Type**: Framework scheduled task (`schedule().fixedRate(...)`).
   - **Target**: `DeliveryZoneService.CleanupLocalDeliveryZoneCacheJob`.
   - **Frequency**: Every **30 minutes**.
   - **Purpose**: Iterates over the in-memory `LocalCacheStore` on each pod and evicts expired delivery zone metadata entries.
2. **`redis.pool::refresh`**:
   - **Type**: Background maintenance task (`context.backgroundTask().scheduleWithFixedDelay(...)`).
   - **Frequency**: Every **5 minutes**.
   - **Purpose**: Verifies Redis connection pool liveness and trims idle sockets across pods.
3. **Internal Data Migrations (Manual / Ad-Hoc)**:
   - Several long-running tasks can be triggered asynchronously via `MigrateModule` endpoints using internal `Executor` threads (e.g. `migrate-wonder-marketing-setting-init-task`, `migrate-customer-address`, `migrate-login-histories-task`).

---

## 2. Database Access Patterns

### 2.1 MySQL Database Access

The application connects to a MySQL database named `mobile` using the `core-ng` database abstraction layer.

#### Connection Pool Configuration
- **Host / URL**: Configured via `sys.jdbc.url` (`jdbc:mysql://...`).
- **Connection Pool Sizing**: Not explicitly overridden in application code or property files (`sys.jdbc.poolSize` is omitted). It inherits default `core-ng` defaults (minimum 2 connections, maximum pool size typically 50 connections per pod, connection checkout timeout 30 seconds).

#### Tables Accessed & Query Patterns
The service accesses 14 domain entity tables:

1. **`sessions`** (`Session.java`):
   - **Primary Key**: `token` (VARCHAR(50)).
   - **Indexes**:
     - `ix_sessions_state_latitude_longitude_expired_time` (`state`, `latitude`, `longitude`, `expired_time`)
     - `ix_sessions_address_id` (`address_id`)
     - `ix_corporate_client_id` (`corporate_client_id`)
     - `ix_sessions_user_id_expired_time` (`user_id`, `expired_time`)
     - `ix_sessions_last_access_time` (`last_access_time`)
   - **Query Types**:
     - `SELECT`: Key lookups (`sessionRepository.get(token)`), range queries by `user_id = ? AND expired_time > ?`, `address_id = ?`, and `corporate_client_id = ?`.
     - `INSERT`: New sessions inserted via `sessionRepository.insert(session)`.
     - `UPDATE` / `PARTIAL UPDATE`: Heavily updated on user actions (`addressId`, `zipCode`, `lastAccessTime`, `expiredTime`, `corporateClientId`).
     - `DELETE` / `BATCH DELETE`: Executed on user logout (`delete(token)`) or customer deletion events (`batchDelete(tokenList)`).
2. **`devices`** (`Device.java`):
   - **Primary Key**: `id` (VARCHAR(50)).
   - **Indexes**: Primary key only.
   - **Query Types**: `SELECT` by `id` (`deviceRepository.get(deviceId)`), batch select via `in("id", deviceIds)`, `INSERT` for new device registrations, `UPDATE` for push messaging tokens.
3. **`clients`** (`Client.java`):
   - **Primary Key**: `id` (VARCHAR(50)).
   - **Query Types**: `SELECT` by `id` via `clientRepository.get(clientId)`. Read-only static client API key registry.
4. **`login_histories`** (`LoginHistory.java`):
   - **Primary Key**: `id` (VARCHAR(50)).
   - **Indexes**: `ix_login_histories_user_id` (`user_id`).
   - **Query Types**: `SELECT` with `user_id = ? LIMIT 1`, `DELETE FROM login_histories WHERE user_id = ?` (in migration controller).
5. **`user_first_time_login_histories`** (`UserFirstTimeLoginHistory.java`):
   - **Primary Key**: `user_id` (VARCHAR(50)).
   - **Query Types**: `SELECT` by primary key `user_id`, `INSERT IGNORE` via background executor on user login.
6. **`account_device_histories`** (`AccountDeviceHistory.java`):
   - **Primary Key**: `id` (VARCHAR(50)).
   - **Indexes**: `uk_account_device_histories_user_id_device_unique_id` (`user_id`, `device_unique_id`).
   - **Query Types**: `SELECT` with `user_id = ? AND device_unique_id = ?`, `INSERT` on initial session bind.
7. **`restaurant_active_enrollments`** (`RestaurantActiveEnrollment.java`):
   - **Primary Key**: `id` (VARCHAR(50)).
   - **Indexes**: `uk_restaurant_active_enrollments_unique_id` (`unique_id`), `ix_restaurant_active_enrollments_device_id` (`device_id`), `ix_restaurant_active_enrollments_restaurant_id` (`restaurant_id`).
   - **Query Types**: `SELECT` count query with `restaurant_id = ? AND device_id = ? AND hdr_id = ? AND notify_time IS NULL`, `INSERT IGNORE`, `UPDATE ... WHERE restaurant_id = ? AND hdr_id = ?`.
8. **`app_store_ratings`** (`AppStoreRating.java`):
   - **Primary Key**: `user_id` (VARCHAR(50)).
   - **Query Types**: `SELECT` by PK, `INSERT`, `UPDATE` prompt timestamps and count.
9. **`app_store_rating_prompt_triggers`** (`AppStoreRatingPromptTrigger.java`):
   - **Primary Key**: `id` (VARCHAR(50)).
   - **Indexes**: `ix_user_id_trigger` (`user_id`, `prompt_trigger`).
   - **Query Types**: `SELECT` with `user_id = ? AND prompt_trigger = ?`, `INSERT`, `UPDATE`.
10. **`app_search_variant_configurations`** (`AppSearchVariantConfiguration.java`):
    - **Primary Key**: `id` (VARCHAR(50)).
    - **Query Types**: `SELECT` by PK (`app-search-variant-configuration`).
11. **`latest_version_configurations`** (`LatestVersionConfiguration.java`):
    - **Primary Key**: `platform` (VARCHAR(20)).
    - **Query Types**: `SELECT` by PK (`IOS` / `ANDROID`), `INSERT IGNORE`, `UPDATE`.
12. **`previous_version_configurations`** (`PreviousVersionConfiguration.java`):
    - **Primary Key**: `(version, platform)`.
    - **Query Types**: `SELECT` by compound PK, `INSERT IGNORE`, `UPDATE`.
13. **`verified_phones`** (`VerifiedPhone.java`):
    - **Primary Key**: `session_token` (VARCHAR(50)).
    - **Query Types**: `SELECT` by PK, `INSERT`, `DELETE` on verification completion.
14. **`autocomplete_weird_data`** (`AutoCompleteWiredData.java`):
    - Table dropped via migration `R__autocomplete_weird_data.sql`. Entity repository registered in `MobileAPIApp.java` but unreferenced in application logic.

---

### 2.2 Large Scans, Suppressed Warnings & Unbounded Queries

1. **Suppressed Slow SQL Warnings**:
   - In `CustomerUpdatedMessageHandler.java`:
     ```java
     Database.suppressSlowSQLWarning(true);
     List<Session> sessions = sessionRepository.select("user_id = ? AND expired_time > ?", userId, ZonedDateTime.now());
     Database.suppressSlowSQLWarning(false);
     ```
     The developers explicitly suppressed the framework slow SQL warning on this query, indicating frequent slow execution warnings under load.
   - In `WonderSpotService.java`:
     ```java
     database.suppressSlowSQLWarning(true);
     Query<Session> query = sessionRepository.select();
     query.where("corporate_client_id = ?", corporateClientId);
     ```
2. **Unbounded Query in Address Migration**:
   - In `MigrateNonCityAddressService.java`:
     ```java
     Query<Session> select = sessionRepository.select();
     select.where("address_id IS NULL");
     select.where("city IS NULL");
     select.where("state IS NOT NULL");
     List<Session> sessions = select.fetch();
     ```
     This query performs an **unbounded full table scan** across the `sessions` table without a `LIMIT` clause, pulling all matching records directly into memory.
3. **N+1 Query Pattern in Migration**:
   - In `MigrateLoginHistoryController.java`:
     For each user ID in a 300-record batch, the code executes:
     1. `SELECT ... FROM login_histories WHERE user_id = ? ORDER BY created_time ASC LIMIT 1`
     2. `INSERT INTO user_first_time_login_histories`
     3. `DELETE FROM login_histories WHERE user_id = ?`
     This produces **900 individual SQL executions per batch**.
4. **Iterative Update Loop on Sessions**:
   - In `WonderSpotService.resetCorporateClientIdInSession`: Queries sessions in batches of 300 by `corporate_client_id`, then loops over each entity executing individual single-row `sessionRepository.update(session)` calls instead of a batch update statement.

---

### 2.3 MongoDB / Atlas
**N/A**. The `mobile-api` service does not connect directly to MongoDB. (All references to MongoDB in the codebase describe downstream services like `search-service`).

---

## 3. Kafka Usage

### 3.1 Topics Produced To

The service publishes events to **6 business topics**, plus an infrastructure action logging topic:

| Topic Name | Message Class | Publishing Module / Service |
| :--- | :--- | :--- |
| `customer-profile-updated` | `CustomerProfileUpdatedMessage` | `UserModule.java` (`UserService`) |
| `send-wonder-app-notification` | `SendWonderAppNotificationMessage` | `WonderMessageModule.java` (`WonderMessageService`) |
| `session-address-updated` | `SessionAddressUpdatedMessage` | `SessionModule.java` (`SessionAddressService`) |
| `customer-browse-location-update-message` | `CustomerBrowseLocationUpdateMessage` | `SessionModule.java` (`CustomerBrowseLocationService`) |
| `sync-customer-attribute` | `SyncCustomerAttributeMessage` | `AddressModule.java` (`AddressChangeService`) |
| `oos-cart-nearby-inventory-calculation` | `OOSCartNearbyInventoryCalculationMessage` | `CartModule.java` (`CartItemService`) |
| Action Log Appender | `core.framework.log.message.ActionLogMessage` | `sys.log.appender` (`log-kafka-bootstrap.log-kafka:9092`) |

---

### 3.2 Topics Consumed From

The application defines a `KafkaConsumerModule` with subscriptions to **11 topics**:

| Topic Name | Message Class | Handler Class |
| :--- | :--- | :--- |
| `wait-list-user-address-serviceable` | `WaitListUserAddressServiceableMessage` | `WaitListUserAddressServiceableMessageHandler` |
| `customer-updated` | `CustomerUpdatedMessage` | `CustomerUpdatedMessageHandler` |
| `consumer-delivery-zone-updated-event` | `ConsumerDeliveryZoneUpdatedMessage` | `DeliveryZoneUpdatedMessageHandler` |
| `restaurant-whitelist-changed-topic` | `ConsumerRestaurantWhitelistChangedMessage` | `WhitelistChangedMessageHandler` |
| `wonder-configuration-updated-message` | `WonderConfigurationUpdatedMessage` | `WonderConfigurationUpdatedMessageHandler` |
| `wonder-user-updated-topic` | `WonderUserUpdatedMessage` | `WonderUserUpdatedMessageHandler` |
| `customer-deleted` | `CustomerDeletedMessage` | `CustomerDeletedMessageHandler` |
| `customer-profile-updated` | `CustomerProfileUpdatedMessage` | `CustomerProfileUpdatedMessageHandler` |
| `coming-soon-restaurant-goes-live` | `ComingSoonRestaurantGoesLiveMessage` | `ComingSoonRestaurantGoesLiveMessageHandler` |
| `customer-address-updated` | `CustomerAddressUpdatedMessage` | `CustomerAddressUpdatedMessageHandler` |
| `corporate-order-client-changed-topic` | `CorporateOrderClientChangedMessage` | `CorporateOrderClientChangedMessageHandler` |

#### Consumer Status and Configuration:
- **Consumer Disabled**: In all environment configuration files (`conf/prod/resources/app.properties`, `conf/uat/`, `conf/dev/`, `src/main/resources/app.properties`), the consumer is disabled:
  ```properties
  app.enableKafkaConsumer=false
  ```
  Consequently, `KafkaConsumerModule` is **not loaded at runtime** in production.
- **Consumer Group ID**: If enabled, core-ng assigns the default application name `mobile-api` as the consumer group ID.
- **Concurrency & Polling**:
  - `kafka().maxPoll(CommonConfig.MAX_POLL_SIZE, CommonConfig.MAX_POLL_BYTES)`: `MAX_POLL_SIZE = 30`, `MAX_POLL_BYTES = 1,048,576` (1 MB).
  - `kafka().concurrency(CommonConfig.CONCURRENCY)`: Configured via common library constants.
- **Blocking `poll()` Loops**: **None**. The framework handles polling internally with non-blocking worker thread pools dispatching to `MessageHandler.handle(key, message)`.

---

## 4. Caching Strategy

The service utilizes two Redis instances and an in-memory JVM cache:
1. **Primary Redis** (`sys.redis.host=redis`): Used by `core-ng`'s built-in `cache()` system.
2. **Dedicated Cache Redis** (`sys.cache.host=mobile-cache`): Bound under name `"cacheRedis"` with a strict 1-second socket timeout (`redis.timeout(Duration.ofSeconds(1))`), used for high-frequency dish views and filter caching.
3. **Local JVM Cache**: An in-memory store in `DeliveryZoneService`.

### 4.1 Cache Regions and TTLs

| Cache Class / Key Pattern | Store | TTL | Fallback / Graceful Degradation |
| :--- | :--- | :--- | :--- |
| `Session.class` | Primary Redis | 30 minutes | Falls back to MySQL `sessions` table lookup (`findActiveSession`). |
| `LoginUser.class` | Primary Redis | 30 minutes | Falls back to downstream HTTP call: `userWebService.get(userId)`. |
| `Client.class` | Primary Redis | 30 minutes | Falls back to MySQL `clients` table lookup. |
| `DeliveryZoneCache.class` | Primary Redis | 12 hours | Falls back to `boDeliveryZoneWebService.getDeliveryZone(id)`; on failure, returns dummy `"unknown"`. |
| `HDRWhitelistCache.class` | Primary Redis | 24 hours | Falls back to `whitelistWebService.getWhitelist()`. |
| `ETAConfigurationCache.class` | Primary Redis | 12 hours | Falls back to `etaConfigurationWebService.get()`. |
| `OrderItemCacheV3.class` | Primary Redis | 12 hours | Falls back to `orderWebService.get(orderId)`. |
| `OrderItemSourceMetadataCache.class` | Primary Redis | 30 minutes | Falls back to order service queries. |
| `HDROOSItemCache.class` | Primary Redis | 5 minutes | Falls back to inventory service lookups. |
| `WonderLocalRestaurantDisplayRuleCache.class` | Primary Redis | 24 hours | Falls back to setting service. |
| `WonderSettingConfigurationCache.class` | Primary Redis | 12 hours | Falls back to `configurationWebService.get()`. |
| `DeliveryFeeConfigurationCache.class` | Primary Redis | 12 hours | Falls back to setting service. |
| `ServiceFeeConfigurationCache.class` | Primary Redis | 12 hours | Falls back to setting service. |
| `HDRTipConfigurationCache.class` | Primary Redis | 12 hours | Falls back to setting service. |
| `HDRTipConfigurationHardcodedValueCache.class` | Primary Redis | 12 hours | Falls back to setting service. |
| `HDRFastPassConfigurationCache.class` | Primary Redis | 12 hours | Falls back to setting service. |
| `WonderSpotServiceFeeConfigurationCache.class` | Primary Redis | 12 hours | Falls back to setting service. |
| `FeeVisibilityConfigurationCache.class` | Primary Redis | 12 hours | Falls back to setting service. |
| `CorporateClientOrderFeeCache.class` | Primary Redis | 12 hours | Falls back to setting service. |
| `NutritionConfigurationCache.class` | Primary Redis | 12 hours | Falls back to setting service. |
| `OrderBrandCategoryCache.class` | Primary Redis | 12 hours | Falls back to restaurant service. |
| `SpecialHandlingInfoCache.class` | Primary Redis | 1 minute | Falls back to downstream service. |
| `AuthenticationUserCache.class` | Primary Redis | 1 minute | Used for rate limiting authentication attempts. |
| `AuthenticationAttemptsCache.class` | Primary Redis | 6 minutes | Used for brute-force lockouts. |
| `GiftCardOrderCheckoutCache.class` | Primary Redis | 24 hours | Falls back to gift card service. |
| `ListFeaturedCollectionWithItemResponse.class` | Primary Redis | 5 minutes | Falls back to `featuredCollectionWebService.listFeaturedCollectionsWithItems()`. |
| `GetFeaturedCollectionResponse.class` | Primary Redis | 5 minutes | Falls back to `featuredCollectionWebService.get(id)`. |
| `dish-item-view:{hdrId}:{itemId}` | Dedicated Cache Redis | 1 minute | Recomputes item views from menu and inventory data. |
| `hp-dish-filter-item:{hdrId}:{diningOption}:{name}` | Dedicated Cache Redis | 1 minute | Re-filters homepage dish items. |
| `dlp-dish-filter-item:{hdrId}:{diningOption}:{name}` | Dedicated Cache Redis | 1 minute | Re-filters dish landing page items. |
| `DeliveryZoneService.localCacheStore` | JVM Memory | 30 minutes | In-process cache of delivery zone IDs to names. |

### 4.2 Degradation on Cache Miss
All caches registered with `cache().add(...)` use the read-through pattern `cache.get(key, loader)`. If a key is evicted or misses, the loader executes synchronously against MySQL or downstream HTTP services. If Redis fails completely:
- Dedicated `"cacheRedis"` falls back to `EmptyRedis` if unconfigured.
- Primary Redis failures throw Redis exceptions or bubble up to the caller unless explicitly guarded in wrapper code.

---

## 5. Resilience Configuration

### 5.1 Circuit Breaker Instances
**0 (None)**.
There are **no true circuit breaker state machines** (such as Resilience4j, Sentinel, or Hystrix) implemented in `mobile-api`.
- Although outbound clients use `CLBConfig` (Client-Side Load Balancing) and `EnhancedAPIConfig`, their fallback mechanism (`APIInvocationHandler`) only acts as an **exception catch-and-dispatch interceptor**.
- It does **not** track error rates, has no `OPEN`, `HALF-OPEN`, or `CLOSED` states, and **cannot short-circuit** requests. Under sustained downstream degradation, all requests continue to wait for timeouts and retries.

---

### 5.2 Retry Policies and Timeouts

| Client Library | Connect Timeout | Default Read Timeout | Max Retries | Retry Wait Interval |
| :--- | :--- | :--- | :--- | :--- |
| `CLBConfig` | 2,000 ms | 20,000 ms (overridden per client) | 2 | 100 ms |
| `EnhancedAPIConfig` | 1,000 ms | 20,000 ms (overridden per client) | 2 | 100 ms |
| Standalone `HTTPClient` | 2,000 ms | 25,000 ms | 3 | 500 ms |

---

### 5.3 Missing Circuit-Breakers on Critical External Calls

1. **Order Placement (`OrderWebService.placeOrderV2`, `GrubhubOrderWebService.checkoutOrder`)**:
   - Timeouts are set to **15,000 ms** (`order-service`) and **20,000 ms** (`grubhub-integration-service`), with 2 retries.
   - If downstream services experience latency spikes, requests will hang for **30 to 60 seconds**, rapidly exhausting Undertow server threads across all pods.
2. **Unbounded Timeout on `BODeliveryZoneWebService`**:
   - `BODeliveryZoneWebService` is registered via `config(CLBConfig.class).client(...)` without an explicit timeout override or a fallback bean. It inherits the 20-second default timeout.

---

## 6. External Dependencies

The `mobile-api` service communicates with **34 downstream HTTP services** exposing over 65 client interfaces:

| Service / Dependency | Base Property Key | Configured Client Interfaces | Connect Timeout | Read Timeout |
| :--- | :--- | :--- | :--- | :--- |
| **Customer Service** | `app.consumer.customer.serviceURL` | `CustomerWebService`, `CustomerPreferenceWebService`, `VisitorPreferenceWebService`, `CustomerCreditWebService`, `CustomerAllergenWebservice`, `CustomerTagWebService`, `CustomerAddressWebService`, `VisitorAddressWebService` | 2,000 ms | 1,500 ms |
| | | `CustomerMembershipWebService`, `VisitorWebService`, `CustomerPrimaryAddressWebService`, `CustomerPaymentWebService`, `VisitorAllergenWebService`, `VerificationCodeWebService`, `WaitListUserWebService`, `CustomerOrderDonationWebService`, `CustomerChallengeWebService`, `AliasUserWebService`, `MarketingSettingInitializationWebService` | 2,000 ms | 5,000 ms |
| | | `CustomerCreditCardWebService` | 2,000 ms | 10,000 ms |
| **Customer Wallet Service** | `app.consumer.customerWallet.serviceURL` | `CustomerWalletWebService`, `GiftCardWebService`, `CustomerBlueApronWebService` | 2,000 ms | 5,000 ms |
| **User Service** | `app.consumer.user.serviceURL` | `UserWebService` | 2,000 ms | 5,000 ms |
| **Customer Survey Service** | `app.consumer.customerSurvey.serviceURL` | `OrderRatingWebService`, `BlueApronOrderRatingWebService`, `GeneralOrderRatingWebService` | 2,000 ms | 5,000 ms |
| **Search Service** | `app.consumer.search.serviceURL` | `RestaurantSearchWebService` | 2,000 ms | 5,000 ms |
| | | `DishSearchWebService`, `DishSearchWebServiceV2` | 2,000 ms | 1,500 ms |
| **Restaurant Recommendation** | `app.consumer.restaurantRecommendation.serviceURL`| `RestaurantRecWebService` | 2,000 ms | 5,000 ms |
| **Restaurant Service V2** | `app.consumer.restaurant.serviceURL` | `PublishedMenuItemWebService`, `PublishedRestaurantWebService`, `AllergenWebService`, `ComingSoonRestaurantConfigurationWebService`, `SmartMerchandisingWebService` | 2,000 ms | 1,500 ms |
| | | `PublishedBundleItemWebService`, `PublishedItemWebService`, `MenuItemNutritionWebService`, `MenuItemWebService`, `HybridPublishedMenuItemWebService`, `HybridPublishedRestaurantWebService`, `HybridPublishedItemWebService`, `RestaurantWebService`, `BundleItemWebService`, `IngredientWebService`, `CuisineWebService`, `SpotlightTagWebService`, `RLPFilterWebService`, `RestaurantCarouselWebService`, `FeaturedCollectionWebService` | 2,000 ms | 5,000 ms |
| **Wonder Setting Service** | `app.consumer.wonderSetting.serviceURL` | `ConfigurationWebService`, `ETAConfigurationWebService` | 2,000 ms | 1,500 ms |
| | | `DonationOrganizationWebService`, `HDRFeeConfigurationWebService`, `FeeConfigurationWebService`, `AlcoholConfigurationWebService`, `CorporateOrderClientWebService`, `HDRTipConfigurationWebService`, `CaloriesSodiumWhitelistUserWebService`, `NutritionConfigurationWebService`, `WonderLocalRestaurantDisplayRuleWebService`, `WonderSpotFeeConfigurationWebService` | 2,000 ms | 5,000 ms |
| **Marketing Service** | `app.consumer.marketing.serviceURL` | `AdWebService`, `RLPMessageConfigurationWebService` | 2,000 ms | 1,500 ms |
| | | `ContentConfigurationWebService`, `PromotionWebService`, `MembershipProductWebService`, `ZoneExpansionMessageWebService`, `ReferralProgramConfigurationWebService`, `ChallengeWebService`, `SegmentWebService` | 2,000 ms | 5,000 ms |
| **Consumer Delivery Zone** | `app.consumer.consumerDeliveryZone.serviceURL` | `DeliveryZoneWebService` | 2,000 ms | 1,500 ms |
| | | `BODeliveryZoneWebService` | 2,000 ms | **20,000 ms** (default, no override) |
| **Wonder Cart Service** | `app.consumer.wonderCart.serviceURL` | `AppCartWebService`, `WebCartWebService`, `WonderCartItemWebService` | 2,000 ms | 5,000 ms |
| **APNs Service** | `app.consumer.apns.serviceURL` | `APNsWebService` | 2,000 ms | 3,000 ms |
| **Grubhub Restaurant Service** | `app.consumer.grubhubRestaurant.serviceURL` | `WonderLocalRestaurantWebService`, `GrubhubLocalRestaurantWebService`, `WonderLocalMenuItemWebService` | 2,000 ms | 20,000 ms |
| **Grubhub Integration Service**| `app.consumer.grubhubIntegration.serviceURL` | `GrubhubRestaurantWebService`, `GrubhubCartWebService`, `GrubhubOrderWebService`, `GrubhubOfferWebService` | 2,000 ms | 20,000 ms |
| | | `GrubhubOAuthWebService` | 2,000 ms | 500 ms |
| **ETL Service** | `app.consumer.etl.serviceURL` | `ETLCustomerWebService`, `ETLOrderWebService` | 2,000 ms | 1,500 ms |
| | | `ETLRestaurantWebService`, `ETLRestaurantBrandWebService` | 2,000 ms | 5,000 ms |
| **Image Service** | `app.image.serviceURL` | `ImageWebService` | 2,000 ms | 5,000 ms |
| **Blue Apron Auto Ship** | `app.ba.autoShip.serviceURL` | `BACustomerAutoShipWebService` | 1,000 ms | 800 ms |
| **Blue Apron Membership** | `app.ba.membership.serviceURL` | `BACustomerMembershipWebService` | 1,000 ms | 800 ms |
| **Blue Apron Product** | `app.ba.product.serviceURL` | `BAGlobalSettingWebService` | 1,000 ms | 800 ms |
| **Order Service** | `app.order.serviceURL` | `PriceWebService`, `OrderWebService`, `BOBrazeWebService`, `FirstPlaceOrderWebService`, `ConsumerOrderWebService`, `OrderModifyHistoryWebService`, `OrderChargeTrackingWebService`, `SelfServiceWebService`, `LocalGrubhubOrderWebService` | 1,000 ms | 15,000 ms |
| **Delivery Task V2** | `app.deliveryTaskV2.serviceURL` | `TaskWebService` | 1,000 ms | 500 ms |
| **Tax Service** | `app.tax.serviceURL` | `TaxWebService` | 1,000 ms | 5,000 ms |
| **Forter Integration** | `app.forterIntegration.serviceURL` | `ForterIntegrationWebService` | 1,000 ms | 1,200 ms |
| **Order ETA Service** | `app.orderETA.serviceURL` | `HDROrderETAWebService` | 1,000 ms | Configured via `app.hdrFallbackETA.timeOut` |
| **Kitchen Order Service (KOS)**| `app.kitchenOrder.serviceURL` | `TimeSlotWebService` | 1,000 ms | Configured via `app.hdrFallbackScheduleOrder.timeOut` |
| **Kitchen Management (KMS)** | `app.kitchenManagement.serviceURL` | `WhitelistWebService` | 1,000 ms | 1,500 ms |
| **Gift Card Order Service** | `app.giftCardOrder.serviceURL` | `GiftCardOrderWebService` | 1,000 ms | 10,000 ms |
| **Order Search Service** | `app.orderSearch.serviceURL` | `OrderSearchWebService` | 1,000 ms | 5,000 ms |
| **Order Issue Service** | `app.orderIssue.serviceURL` | `MPOrderIssueWebService`, `MPOrderWebService` | 1,000 ms | 5,000 ms |
| **Order Number Service** | `app.orderNumber.serviceURL` | `OrderNumberWebService` | 1,000 ms | 5,000 ms |
| **Payment Service** | `app.payment.serviceURL` | `PaymentMethodWebService` | 1,000 ms | 500 ms |
| | | `GrubhubPaymentMethodWebService`, `BraintreeWebService`, `PaymentServiceProviderSelectorWebService` | 1,000 ms | 3,000 ms |
| **Menu Item Inventory** | `app.menuItemInventory.serviceURL` | `HDRRestaurantInventoryWebService` | 1,000 ms | Configured via `app.menuItemInventory.timeoutDuration` |
| **Zendesk Integration** | `app.zendeskIntegration.serviceURL` | `AppZendeskWebService` | 1,000 ms | 5,000 ms |
| **Mapbox API / Google Maps** | External HTTPS SaaS | `PlaceService`, `GMapPlaceService` | 2,000 ms | 25,000 ms (via standalone HTTPClient) |

---

## 7. Scalability Signals

### 7.1 Singletons and In-Process State Across Replicas
1. **`DeliveryZoneService.localCacheStore`**:
   - `DeliveryZoneService` holds an in-memory `LocalCacheStore` instance inside the JVM heap.
   - It caches delivery zone names for 30 minutes.
   - Changes in zone configuration or updates via Kafka do not invalidate this local memory store across replicas. Replicas serve stale zone metadata until their local 30-minute timer expires.
2. **`MapService.tempSwitchFlag`**:
   - `private volatile Boolean tempSwitchFlag = Boolean.FALSE;` is held in memory to switch between Mapbox and Google Maps during API outages.
   - When triggered, it flips to `TRUE` only on the local pod, leaving other replicas querying the failing map provider until they independently encounter errors.
3. **`MobileSearchService.CUISINES` Static Lists**:
   - Static `ArrayList` instances initialized upon service instantiation. While not thread-safe if mutated, they remain read-only during request processing.

---

### 7.2 Scheduled Tasks and Concurrency Guards
1. **`cleanup-local-delivery-zone-cache`**:
   - Scheduled on each pod every 30 minutes.
   - Because it cleans only the pod's local in-memory `LocalCacheStore`, it does not require distributed locking.
2. **Migration Tasks Triggered via HTTP**:
   - Endpoints in `MigrateModule` (e.g. `/_app/migrate-login-histories`, `/_app/migrate-non-city-address`, `/_app/migrate-wonder-marketing-setting-initialization`) submit background tasks directly to `Executor`.
   - **None of these migration jobs use distributed locks** (e.g., Redis or DB locks). If invoked concurrently or triggered simultaneously across multiple replicas, parallel execution would cause race conditions and redundant database load.

---

### 7.3 Stateful Operations and Local Storage
- **File System Writes**: **None**. The service does not write to the local container disk.
- **Local In-Memory Cache**: Pods maintain separate local cache stores that drift out of sync with each other and with the shared Redis database.

---

## Key Scalability Signals

The top 5 architectural scalability concerns identified in `mobile-api` are:

1. **Absence of True Circuit Breakers on High-Timeout External Dependencies**:
   The service relies on exception-catch fallback interception without a circuit breaker state machine. Upstream calls to `order-service` (15s timeout) and `grubhub-integration-service` (20s timeout) allow 2 retries. In the event of downstream service degradation, caller threads remain blocked for 30–60 seconds. Under production traffic (min 5, max 15 pods), Undertow worker thread exhaustion will cascade across all pods within seconds.
2. **High Write and Scan Contention on MySQL `sessions` Table**:
   The `sessions` table handles heavy write traffic (created, updated, and partially updated on every user address change, cart operation, and login event). Table scans and slow range queries (`user_id = ? AND expired_time > ?`, `corporate_client_id = ?`) have already required developers to explicitly suppress slow SQL warnings (`Database.suppressSlowSQLWarning(true)`). Peak traffic surges create severe lock contention and connection pool pressure.
3. **Disabled Kafka Consumers Causing Stale Caches**:
   `app.enableKafkaConsumer=false` is hardcoded across all deployment profiles. Eleven event handlers designed to invalidate caches upon domain events (such as `ConsumerDeliveryZoneUpdatedMessage`, `CustomerUpdatedMessage`, and `WhitelistChangedMessage`) remain inactive. This forces the service to rely strictly on time-based expiration (up to 12–24 hours) for critical configuration updates.
4. **Replica Cache Drift via Pod-Local In-Memory Caches**:
   `DeliveryZoneService` caches zone names in a local JVM `LocalCacheStore` for 30 minutes. Because invalidations are not propagated between replicas, different pods can return inconsistent delivery zone names for the same user sessions.
5. **Unguarded Asynchronous Data Migrations with Unbounded Queries**:
   The `MigrateModule` controllers expose background migration tasks that execute unbounded queries (`sessionRepository.select()` fetching all rows into heap memory) and lack distributed synchronization. If invoked while multiple pods are active, they risk exhausting database connections and causing out-of-memory errors.
