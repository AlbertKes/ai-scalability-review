## 8. Infrastructure Live State (Azure MCP)

### MySQL Flexible Server (`rfprodv2-flexible-wonder-db`)
- **Server Name**: `rfprodv2-flexible-wonder-db` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **Location**: `eastus2` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **Version**: `8.4` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **SKU**: `Standard_E16ds_v4` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **StorageSizeGB**: `1841` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **BackupRetentionDays**: `7` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **GeoRedundantBackup**: `Enabled` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **max_connections**: `2731` [Source: Azure MCP → mysql_server_param_get(param=max_connections, server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **slow_query_log**: `ON` [Source: Azure MCP → mysql_server_param_get(param=slow_query_log, server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **long_query_time**: `5` [Source: Azure MCP → mysql_server_param_get(param=long_query_time, server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **Replicas / Servers List**: `rfprodv2-flexible-wonder-db-replica-v4` (and other servers in group) [Source: Azure MCP → mysql_list(resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]

### AKS Cluster & Node Pool
- **Cluster Name**: `RFProdV2AKSCluster` [Source: Azure MCP → aks_cluster_get(resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **Kubernetes Version**: `1.34.8` [Source: Azure MCP → aks_cluster_get(resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **Selected Node Pool (`apppoolv5`)**:
  - Name: `apppoolv5` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - vmSize: `Standard_D16s_v4` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - minCount: `18` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - maxCount: `42` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - count: `37` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - enableAutoScaling: `true` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - mode: `User` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]

```json
{ "mysql_max_connections": 2731 }
```
