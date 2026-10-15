| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| sessions | 2112802 | 862.09 | 1804.73 | 2666.83 |
| account_device_histories | 1620797 | 352.97 | 317.00 | 669.97 |
| devices | 2500983 | 443.30 | 0.00 | 443.30 |
| user_first_time_login_histories | 1162718 | 324.95 | 0.00 | 324.95 |
| login_histories_con_24123 | 958672 | 181.00 | 131.98 | 312.98 |
| app_store_ratings | 1319458 | 165.56 | 0.00 | 165.56 |
| restaurant_active_enrollments | 12420 | 8.50 | 11.09 | 19.59 |
| app_store_rating_prompt_triggers | 26621 | 6.55 | 6.56 | 13.11 |
| device_app_attest | 5246 | 2.52 | 0.00 | 2.52 |
| verified_phones | 11189 | 2.41 | 0.00 | 2.41 |
| flyway_schema_history | 35 | 0.02 | 0.02 | 0.03 |
| login_histories | 0 | 0.02 | 0.02 | 0.03 |
| latest_version_configurations | 2 | 0.02 | 0.00 | 0.02 |
| previous_version_configurations | 233 | 0.02 | 0.00 | 0.02 |
| order_numbers | 1 | 0.02 | 0.00 | 0.02 |
| clients | 2 | 0.02 | 0.00 | 0.02 |
| app_search_variant_configurations | 0 | 0.02 | 0.00 | 0.02 |

- **Largest table**: `sessions` at 2.60 GB (2112802 rows)
- **Tables above 10 GB**: 0
- **Total of the top 17 tables**: 4.51 GB

[Source: MySQL (rfprodv2-flexible-wonder-db/mobile) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='mobile' ORDER BY (data_length+index_length) DESC LIMIT 20]
