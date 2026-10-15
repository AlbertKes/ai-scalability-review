| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| cart_menu_item_options | 159009 | 175.02 | 106.48 | 281.50 |
| web_cart_menu_item_options | 123440 | 100.02 | 93.41 | 193.42 |
| cart_items | 61927 | 121.25 | 44.23 | 165.48 |
| web_cart_items | 48973 | 55.02 | 40.19 | 95.20 |
| cart_bundle_item_choice_options | 34363 | 33.09 | 23.14 | 56.23 |
| cart_bundle_item_choices | 29787 | 33.08 | 23.14 | 56.22 |
| web_cart_bundle_item_choice_options | 26252 | 20.08 | 19.11 | 39.19 |
| carts | 39090 | 28.06 | 10.05 | 38.11 |
| web_cart_bundle_item_choices | 25639 | 18.06 | 18.05 | 36.11 |
| web_carts | 26466 | 16.06 | 12.05 | 28.11 |
| cart_carry_over_histories | 28765 | 21.02 | 0.00 | 21.02 |
| reorder_cart_created_records | 36129 | 8.06 | 2.02 | 10.08 |
| pop_cart_items | 167 | 0.13 | 0.06 | 0.19 |
| pop_cart_menu_item_options | 197 | 0.11 | 0.03 | 0.14 |
| pop_cart_bundle_item_choices | 54 | 0.05 | 0.03 | 0.08 |
| pop_carts | 108 | 0.05 | 0.02 | 0.06 |
| pop_cart_bundle_item_choice_options | 11 | 0.02 | 0.03 | 0.05 |
| flyway_schema_history | 111 | 0.02 | 0.02 | 0.03 |

- **Largest table**: `cart_menu_item_options` at 0.27 GB (159009 rows)
- **Tables above 10 GB**: 0
- **Total of the top 18 tables**: 1.00 GB

[Source: MySQL (rfprodv2-flexible-wonder-db/wonder_cart) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='wonder_cart' ORDER BY (data_length+index_length) DESC LIMIT 20]
