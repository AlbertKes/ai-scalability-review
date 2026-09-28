| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| pop_cart_items | 8265 | 5.02 | 4.09 | 9.11 |
| cart_items | 4944 | 5.02 | 4.03 | 9.05 |
| cart_menu_item_options | 4946 | 4.02 | 3.27 | 7.28 |
| pop_cart_menu_item_options | 7115 | 4.02 | 2.17 | 6.19 |
| web_cart_menu_item_options | 1673 | 3.02 | 2.23 | 5.25 |
| web_cart_items | 1411 | 2.02 | 2.03 | 4.05 |
| carts | 3158 | 3.02 | 1.02 | 4.03 |
| pop_carts | 5964 | 2.03 | 1.36 | 3.39 |
| cart_bundle_item_choices | 1024 | 1.02 | 1.27 | 2.28 |
| cart_bundle_item_choice_options | 526 | 1.02 | 0.23 | 1.25 |
| web_carts | 1307 | 1.02 | 0.14 | 1.16 |
| cart_carry_over_histories | 300 | 1.02 | 0.00 | 1.02 |
| web_cart_bundle_item_choice_options | 238 | 0.16 | 0.16 | 0.31 |
| web_cart_bundle_item_choices | 159 | 0.08 | 0.11 | 0.19 |
| pop_cart_bundle_item_choices | 0 | 0.02 | 0.03 | 0.05 |
| pop_cart_bundle_item_choice_options | 0 | 0.02 | 0.03 | 0.05 |
| flyway_schema_history | 110 | 0.02 | 0.02 | 0.03 |
| reorder_cart_created_records | 4 | 0.02 | 0.02 | 0.03 |

- **Largest table**: `pop_cart_items` at 0.01 GB (8265 rows)
- **Tables above 10 GB**: 0
- **Total of the top 18 tables**: 0.05 GB

[Source: MySQL (ftiuat-flexible-consumer-db/wonder_cart) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='wonder_cart' ORDER BY (data_length+index_length) DESC LIMIT 20]
