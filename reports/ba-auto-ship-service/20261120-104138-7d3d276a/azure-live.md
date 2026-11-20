## 8. Infrastructure Live State (Azure MCP)

### MySQL Flexible Server
- **Server Name**: rfprodv2-ba-consumer-db [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Location**: eastus2 [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Version**: 8.4 [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **SKU**: Standard_E8ds_v4 [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **StorageSizeGB**: 312 [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **BackupRetentionDays**: 15 [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **GeoRedundantBackup**: Enabled [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **max_connections**: 5461 [Source: Azure MCP → mysql_server_param_get(param: max_connections, server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **slow_query_log**: ON [Source: Azure MCP → mysql_server_param_get(param: slow_query_log, server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **long_query_time**: 10 [Source: Azure MCP → mysql_server_param_get(param: long_query_time, server: rfprodv2-ba-consumer-db, resource-group: Wonder-Platform-Prod)]
- **Replicas**: rfprodv2-ba-consumer-db-replica [Source: Azure MCP → mysql_list(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]

### AKS Cluster & Node Pool
- **Cluster Name**: RFProdV2AKSCluster [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Node Pool Name**: apppoolv5 [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **VM Size**: Standard_D16s_v4 [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Min Count**: 18 [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Max Count**: 42 [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Current Count**: 37 [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Enable Auto Scaling**: true [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Mode**: User [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]

```json
{ "mysql_max_connections": 5461 }
```
