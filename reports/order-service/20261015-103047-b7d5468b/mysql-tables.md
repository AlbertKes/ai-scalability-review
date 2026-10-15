| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| order_timelines_v2 | 183877761 | 188788.58 | 70914.16 | 259702.73 |
| order_item_options | 70119270 | 63477.72 | 24840.89 | 88318.61 |
| order_items | 59370549 | 48512.52 | 18126.88 | 66639.39 |
| order_item_components | 75013591 | 29440.84 | 22898.95 | 52339.80 |
| order_status_tracking | 113593554 | 25121.69 | 25255.00 | 50376.69 |
| order_item_ingredients | 90852504 | 31384.91 | 18020.95 | 49405.86 |
| order_charge_items | 59043166 | 32157.83 | 5859.00 | 38016.83 |
| order_item_option_value_components | 51296258 | 18969.73 | 18967.88 | 37937.61 |
| orders | 18592406 | 11748.36 | 16069.69 | 27818.05 |
| order_restaurants | 21293723 | 15717.86 | 7458.91 | 23176.77 |
| order_configs | 19167237 | 17202.83 | 0.00 | 17202.83 |
| order_payments | 22571173 | 8062.13 | 7729.95 | 15792.08 |
| order_operation_version_tracks | 46218612 | 9134.00 | 4387.00 | 13521.00 |
| order_customers | 17554005 | 5968.67 | 4842.97 | 10811.64 |
| order_charge_item_taxable_extension | 38622723 | 10563.02 | 0.00 | 10563.02 |
| order_addresses | 8897561 | 5640.59 | 4806.86 | 10447.45 |
| order_payment_transactions | 21131917 | 7192.00 | 2608.94 | 9800.94 |
| order_charges | 18474062 | 8409.63 | 1087.00 | 9496.63 |
| order_hdr_addresses | 11636809 | 5228.95 | 2654.95 | 7883.91 |
| order_bundle_item_choices | 7641834 | 4284.84 | 2395.95 | 6680.80 |

- **Largest table**: `order_timelines_v2` at 253.62 GB (183877761 rows)
- **Tables above 10 GB**: 16
- **Total of the top 20 tables**: 787.04 GB

[Source: MySQL (rfprodv2-flexible-wonder-db/order) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='order' ORDER BY (data_length+index_length) DESC LIMIT 20]
