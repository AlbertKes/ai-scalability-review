| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| cart_menu_item_options | 202293 | 175.02 | 109.64 | 284.66 |
| web_cart_menu_item_options | 171032 | 104.02 | 95.55 | 199.56 |
| cart_items | 81624 | 121.28 | 44.25 | 165.53 |
| web_cart_items | 62280 | 55.02 | 39.25 | 94.27 |
| cart_bundle_item_choice_options | 36508 | 33.11 | 22.14 | 55.25 |
| cart_bundle_item_choices | 30143 | 30.09 | 22.14 | 52.23 |
| web_cart_bundle_item_choice_options | 29824 | 20.08 | 19.14 | 39.22 |
| carts | 43383 | 28.09 | 10.06 | 38.16 |
| web_cart_bundle_item_choices | 24716 | 16.06 | 16.11 | 32.17 |
| web_carts | 32900 | 16.08 | 11.05 | 27.13 |
| cart_carry_over_histories | 38180 | 27.02 | 0.00 | 27.02 |
| reorder_cart_created_records | 43655 | 9.06 | 3.02 | 12.08 |
| pop_cart_menu_item_options | 205 | 0.11 | 0.09 | 0.20 |
| pop_cart_items | 169 | 0.13 | 0.06 | 0.19 |
| pop_cart_bundle_item_choices | 52 | 0.05 | 0.03 | 0.08 |
| pop_carts | 107 | 0.05 | 0.02 | 0.06 |
| pop_cart_bundle_item_choice_options | 27 | 0.02 | 0.03 | 0.05 |
| flyway_schema_history | 113 | 0.02 | 0.02 | 0.03 |

- **Largest table**: `cart_menu_item_options` at 0.28 GB (202293 rows)
- **Tables above 10 GB**: 0
- **Total of the top 18 tables**: 1.00 GB

[Source: MySQL (rfprodv2-flexible-wonder-db/wonder_cart) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='wonder_cart' ORDER BY (data_length+index_length) DESC LIMIT 20]
