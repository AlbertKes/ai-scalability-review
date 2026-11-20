| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| auto_ship_cart_fulfillment_events | 264791 | 30067.02 | 2052.08 | 32119.09 |
| auto_ship_cart_item_ingredients | 8215839 | 3808.11 | 3358.33 | 7166.44 |
| auto_ship_cart_fulfillment_outboxes | 6515318 | 2499.25 | 3050.58 | 5549.83 |
| auto_ship_cart_items | 1851924 | 3769.02 | 1622.47 | 5391.48 |
| auto_ship_carts | 1187793 | 2146.05 | 3070.84 | 5216.89 |
| auto_ship_delivery_addresses | 1243516 | 2724.02 | 2424.67 | 5148.69 |
| simulation_auto_ship_cart_items | 1863265 | 1009.00 | 1011.98 | 2020.98 |
| auto_ship_cart_experiment_results | 3026655 | 1057.33 | 350.98 | 1408.31 |
| auto_ship_cart_skip_reasons | 2356945 | 1044.98 | 312.00 | 1356.98 |
| auto_ship_cart_original_deliveries | 4637429 | 1113.55 | 0.00 | 1113.55 |
| auto_ship_fulfillment_errors | 1070737 | 657.89 | 361.38 | 1019.27 |
| auto_ship_refresh_delivery_option_tasks | 78121 | 747.02 | 18.06 | 765.08 |
| auto_ship_create_cart_tasks | 184509 | 620.02 | 33.16 | 653.17 |
| simulation_auto_ship_customizations | 610295 | 256.00 | 318.00 | 574.00 |
| simulation_auto_ship_carts | 632134 | 248.00 | 320.00 | 568.00 |
| simulation_auto_ship_plans | 671784 | 219.00 | 341.00 | 560.00 |
| auto_ship_auth_cart_tasks | 78197 | 520.02 | 20.14 | 540.16 |
| auto_ship_generate_order_tasks | 77366 | 443.02 | 28.20 | 471.22 |
| auto_ship_delivery_day_shift | 380193 | 185.00 | 77.47 | 262.47 |
| auto_ship_forter_infos | 301958 | 158.00 | 71.52 | 229.52 |

- **Largest table**: `auto_ship_cart_fulfillment_events` at 31.37 GB (264791 rows)
- **Tables above 10 GB**: 1
- **Total of the top 20 tables**: 70.44 GB

[Source: MySQL (rfprodv2-ba-consumer-db/ba_auto_ship) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='ba_auto_ship' ORDER BY (data_length+index_length) DESC LIMIT 20]
