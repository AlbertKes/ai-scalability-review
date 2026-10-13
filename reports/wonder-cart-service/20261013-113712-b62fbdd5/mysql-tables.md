| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| pop_cart_items | 8431 | 5.02 | 4.09 | 9.11 |
| cart_items | 5290 | 5.02 | 3.03 | 8.05 |
| cart_menu_item_options | 5026 | 4.02 | 3.05 | 7.06 |
| pop_cart_menu_item_options | 7653 | 4.02 | 2.17 | 6.19 |
| web_cart_menu_item_options | 1121 | 2.02 | 2.17 | 4.19 |
| web_cart_items | 1220 | 2.02 | 2.03 | 4.05 |
| pop_carts | 6254 | 2.03 | 1.36 | 3.39 |
| carts | 3473 | 2.02 | 1.02 | 3.03 |
| cart_bundle_item_choices | 928 | 1.02 | 1.22 | 2.23 |
| cart_bundle_item_choice_options | 474 | 1.02 | 0.19 | 1.20 |
| web_carts | 1167 | 1.02 | 0.13 | 1.14 |
| cart_carry_over_histories | 666 | 1.02 | 0.00 | 1.02 |
| web_cart_bundle_item_choice_options | 233 | 0.14 | 0.14 | 0.28 |
| web_cart_bundle_item_choices | 124 | 0.09 | 0.05 | 0.14 |
| pop_cart_bundle_item_choices | 0 | 0.02 | 0.03 | 0.05 |
| pop_cart_bundle_item_choice_options | 0 | 0.02 | 0.03 | 0.05 |
| flyway_schema_history | 110 | 0.02 | 0.02 | 0.03 |
| reorder_cart_created_records | 4 | 0.02 | 0.02 | 0.03 |

- **Largest table**: `pop_cart_items` at 0.01 GB (8431 rows)
- **Tables above 10 GB**: 0
- **Total of the top 18 tables**: 0.05 GB

[Source: MySQL (ftiuat-flexible-consumer-db/wonder_cart) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='wonder_cart' ORDER BY (data_length+index_length) DESC LIMIT 20]
