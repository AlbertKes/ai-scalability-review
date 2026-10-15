| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| auto_ship_cart_fulfillment_events | 1381162 | 36053.02 | 2618.56 | 38671.58 |
| auto_ship_cart_items | 1979941 | 3785.02 | 1621.50 | 5406.52 |
| auto_ship_cart_fulfillment_outboxes | 7008122 | 2387.25 | 2924.58 | 5311.83 |
| auto_ship_carts | 1376051 | 2154.05 | 3102.92 | 5256.97 |
| auto_ship_delivery_addresses | 1254697 | 2731.02 | 2435.09 | 5166.11 |
| simulation_auto_ship_cart_items | 1863265 | 1009.00 | 1011.98 | 2020.98 |
| auto_ship_cart_experiment_results | 3026655 | 1057.33 | 350.98 | 1408.31 |
| auto_ship_cart_skip_reasons | 2386857 | 1044.98 | 312.00 | 1356.98 |
| auto_ship_cart_original_deliveries | 4762527 | 1113.55 | 0.00 | 1113.55 |
| auto_ship_fulfillment_errors | 1071034 | 657.89 | 361.38 | 1019.27 |
| auto_ship_refresh_delivery_option_tasks | 74580 | 816.03 | 16.06 | 832.09 |
| auto_ship_create_cart_tasks | 208260 | 702.02 | 33.17 | 735.19 |
| auto_ship_auth_cart_tasks | 62952 | 559.02 | 16.11 | 575.13 |
| simulation_auto_ship_customizations | 610295 | 256.00 | 318.00 | 574.00 |
| simulation_auto_ship_carts | 632134 | 248.00 | 320.00 | 568.00 |
| simulation_auto_ship_plans | 671784 | 219.00 | 341.00 | 560.00 |
| auto_ship_generate_order_tasks | 68415 | 459.02 | 27.19 | 486.20 |
| auto_ship_delivery_day_shift | 380636 | 185.00 | 77.47 | 262.47 |
| auto_ship_forter_infos | 239229 | 130.00 | 60.48 | 190.48 |
| auto_ship_cart_cancel_reasons | 283069 | 129.00 | 37.77 | 166.77 |

- **Largest table**: `auto_ship_cart_fulfillment_events` at 37.77 GB (1381162 rows)
- **Tables above 10 GB**: 1
- **Total of the top 20 tables**: 70.00 GB

[Source: MySQL (rfprodv2-ba-consumer-db/ba_auto_ship) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='ba_auto_ship' ORDER BY (data_length+index_length) DESC LIMIT 20]
