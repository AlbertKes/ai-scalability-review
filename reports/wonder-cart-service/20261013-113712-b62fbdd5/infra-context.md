# Infrastructure Context — wonder-cart-service (uat)

Local infra repository root: `/Users/albertke/IdeaProjects/03Wonder/infra`

This document was assembled by selecting only the infra files relevant to **wonder-cart-service**, and from
the shared ones only the parts that name it. Cite values from it as
`[Source: code → <path shown in the heading>]`.

## Selection Summary

- Kubernetes manifests: 6 of 82 files included
- AKS node pools (Terraform): 2 of 64 files included
- MySQL Flexible Server (Terraform): 1 of 64 files included
- MySQL database (Terraform): 5 of 29 files included

### Skipped or trimmed files

- infra/uat/app/consumer/kube/resource/190-hpa.yml (kept 1 of 23 YAML documents — the rest belong to other services)
- infra/uat/app/consumer/kube/resource/199-service-account.yml (kept 1 of 30 YAML documents — the rest belong to other services)
- infra/uat/app/consumer/kube/resource/consumer-service/99-consumer-service-hpa.yml (kept 1 of 12 YAML documents — the rest belong to other services)

If a configuration value you need is in one of these files, read it directly from the
infra repository root above.

## Kubernetes manifests

### `infra/uat/app/consumer/kube/resource/190-hpa.yml`

```
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: wonder-cart-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: wonder-cart-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 100
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 2
          periodSeconds: 10
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Pods
          value: 1
          periodSeconds: 180
```

### `infra/uat/app/consumer/kube/resource/199-service-account.yml`

```
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 1d833bc8-6ed6-499a-a84b-89eb0156aaaf
  name: wonder-cart-service
  namespace: uat-consumer
```

### `infra/uat/app/consumer/kube/resource/22-wonder-cart-service.yml`

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wonder-cart-service
  namespace: uat-consumer
  labels:
    wonder.com/repository: wonder-consumer-project
    wonder.com/team: consumer
    wonder.com/group: app
    wonder.com/owner: dale
    wonder.com/language: java
    wonder.com/framework: core-ng
    wonder.com/api-spec: core-ng
    wonder.com/jira-project-key: CON
  annotations:
    wonder.com/pager-duty-service: "[TECH] Consumer"
spec:
  replicas: 2
  selector:
    matchLabels:
      app: wonder-cart-service
  revisionHistoryLimit: 10
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 10
  template:
    metadata:
      labels:
        app: wonder-cart-service
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: wonder-cart-service
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: wonder-cart-service
          matchLabelKeys:
            - pod-template-hash
      nodeSelector:
        rfpool: apppool
      volumes:
        - name: tls
          secret:
            secretName: wonder-core-ng-tls-secret
      containers:
        - name: wonder-cart-service
          image: ftiuatacr.azurecr.io/wonder-consumer-project/wonder-cart-service:latest
          volumeMounts:
            - mountPath: /opt/app/tls
              name: tls
              readOnly: true
          env:
            - name: JAVA_OPTS
              value: "-XX:InitialRAMPercentage=75 -XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:MaxGCPauseMillis=400 -XX:InitiatingHeapOccupancyPercent=45  -Xss512k  -XX:MaxMetaspaceSize=256m -Dlog4j2.formatMsgNoLookups=true"
            - name: DD_TAGS
              value: pillar:growth,team:consumer,framework:core-ng
            - name: SHUTDOWN_DELAY_IN_SEC
              value: "5"
          envFrom:
            - configMapRef:
                name: wonder-cart-service-config
          readinessProbe:
            httpGet:
              path: /health-check
              port: 8443
              scheme: HTTPS
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /liveness-probe
              port: 8443
              scheme: HTTPS
            initialDelaySeconds: 90
            periodSeconds: 10
            timeoutSeconds: 1
            successThreshold: 1
            failureThreshold: 3
          resources:
            requests:
              cpu: 1
              memory: 1Gi
            limits:
              cpu: 2
              memory: 2Gi
          lifecycle:
            preStop:
              exec:
                command:
                  - /bin/sh
                  - -c
                  - |
                    echo 'Executing preStop'
                    sleep 10
---
apiVersion: v1
kind: Service
metadata:
  name: wonder-cart-service
  namespace: uat-consumer
spec:
  ports:
    - port: 443
      targetPort: 8443
  selector:
    app: wonder-cart-service
---
apiVersion: v1
kind: Service
metadata:
  name: wonder-cart-service-headless
  namespace: uat-consumer
spec:
  clusterIP: None
  selector:
    app: wonder-cart-service
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  annotations:
    acme.cert-manager.io/http01-edit-in-place: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-internal"
    cert-manager.io/duration: 26280h
    cert-manager.io/renew-before: 720h
    nginx.org/ssl-services: wonder-cart-service
    nginx.org/redirect-to-https: "true"
  name: wonder-cart-service-internal-f5
  namespace: uat-consumer
spec:
  ingressClassName: nginx-f5-internal
  rules:
    - host: wonder-cart-service.uat-consumer.wonder.internal
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: wonder-cart-service
                port:
                  number: 443
  tls:
    - hosts:
        - wonder-cart-service.uat-consumer.wonder.internal
      secretName: wonder-cart-service-wonder-tls

```

### `infra/uat/app/consumer/kube/resource/consumer-service/22-consumer-wonder-cart-service.yml`

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: consumer-wonder-cart-service
  namespace: uat-consumer
  labels:
    wonder.com/repository: wonder-consumer-project
    wonder.com/team: consumer
    wonder.com/group: app
    wonder.com/owner: dale
    wonder.com/language: java
    wonder.com/framework: core-ng
    wonder.com/api-spec: core-ng
    wonder.com/jira-project-key: CON
  annotations:
    wonder.com/pager-duty-service: "[TECH] Consumer"
spec:
  replicas: 3
  selector:
    matchLabels:
      app: consumer-wonder-cart-service
  revisionHistoryLimit: 10
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 10
  template:
    metadata:
      labels:
        app: consumer-wonder-cart-service
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: wonder-cart-service
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: consumer-wonder-cart-service
          matchLabelKeys:
            - pod-template-hash
      nodeSelector:
        rfpool: apppool
      volumes:
        - name: tls
          secret:
            secretName: wonder-core-ng-tls-secret
      containers:
        - name: consumer-wonder-cart-service
          image: ftiuatacr.azurecr.io/wonder-consumer-project/wonder-cart-service:latest
          volumeMounts:
            - mountPath: /opt/app/tls
              name: tls
              readOnly: true
          env:
            - name: JAVA_OPTS
              value: "-XX:InitialRAMPercentage=75 -XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:MaxGCPauseMillis=400 -XX:InitiatingHeapOccupancyPercent=45  -Xss512k  -XX:MaxMetaspaceSize=256m -Dlog4j2.formatMsgNoLookups=true"
            - name: DD_TAGS
              value: pillar:growth,team:consumer,framework:core-ng
            - name: SHUTDOWN_DELAY_IN_SEC
              value: "5"
          envFrom:
            - configMapRef:
                name: wonder-cart-service-config
          readinessProbe:
            httpGet:
              path: /health-check
              port: 8443
              scheme: HTTPS
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /liveness-probe
              port: 8443
              scheme: HTTPS
            initialDelaySeconds: 90
            periodSeconds: 10
            timeoutSeconds: 1
            successThreshold: 1
            failureThreshold: 3
          resources:
            requests:
              cpu: 1
              memory: 2Gi
            limits:
              cpu: 2
              memory: 4Gi
          lifecycle:
            preStop:
              exec:
                command:
                  - /bin/sh
                  - -c
                  - |
                    echo 'Executing preStop'
                    sleep 10
---
apiVersion: v1
kind: Service
metadata:
  name: consumer-wonder-cart-service-headless
  namespace: uat-consumer
spec:
  clusterIP: None
  selector:
    app: consumer-wonder-cart-service

```

### `infra/uat/app/consumer/kube/resource/consumer-service/99-consumer-service-hpa.yml`

```
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-wonder-cart-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-wonder-cart-service
  minReplicas: 3
  maxReplicas: 9
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 50
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 3
          periodSeconds: 10
    scaleDown:
      stabilizationWindowSeconds: 3600
      policies:
        - type: Pods
          value: 1
          periodSeconds: 180
```

### `infra/uat/app/consumer/kube/resource/kafka-consumer/22-wonder-cart-service-kafka-consumer.yml`

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wonder-cart-service-kafka-consumer
  namespace: uat-consumer
  labels:
    wonder.com/repository: wonder-consumer-project
    wonder.com/team: consumer
    wonder.com/group: app
    wonder.com/owner: dale
    wonder.com/language: java
    wonder.com/framework: core-ng
    wonder.com/api-spec: core-ng
    wonder.com/jira-project-key: CON
  annotations:
    wonder.com/pager-duty-service: "[TECH] Consumer"
spec:
  replicas: 2
  selector:
    matchLabels:
      app: wonder-cart-service-kafka-consumer
  revisionHistoryLimit: 10
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 10
  template:
    metadata:
      labels:
        app: wonder-cart-service-kafka-consumer
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: wonder-cart-service
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: wonder-cart-service-kafka-consumer
          matchLabelKeys:
            - pod-template-hash
      nodeSelector:
        rfpool: apppool
      volumes:
        - name: tls
          secret:
            secretName: wonder-core-ng-tls-secret
      containers:
        - name: wonder-cart-service-kafka-consumer
          image: ftiuatacr.azurecr.io/wonder-consumer-project/wonder-cart-service:latest
          volumeMounts:
            - mountPath: /opt/app/tls
              name: tls
              readOnly: true
          env:
            - name: JAVA_OPTS
              value: "-XX:InitialRAMPercentage=75 -XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:MaxGCPauseMillis=400 -XX:InitiatingHeapOccupancyPercent=45  -Xss512k  -XX:MaxMetaspaceSize=256m -Dlog4j2.formatMsgNoLookups=true"
            - name: DD_TAGS
              value: pillar:growth,team:consumer,framework:core-ng
            - name: SHUTDOWN_DELAY_IN_SEC
              value: "5"
            - name: APP_ENABLEKAFKACONSUMER
              value: "true"
          envFrom:
            - configMapRef:
                name: wonder-cart-service-config
          readinessProbe:
            httpGet:
              path: /health-check
              port: 8443
              scheme: HTTPS
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /health-check
              port: 8443
              scheme: HTTPS
            initialDelaySeconds: 90
            periodSeconds: 10
            timeoutSeconds: 1
            successThreshold: 1
            failureThreshold: 3
          resources:
            requests:
              cpu: 1
              memory: 1Gi
            limits:
              cpu: 2
              memory: 2Gi
          lifecycle:
            preStop:
              exec:
                command:
                  - /bin/sh
                  - -c
                  - |
                    echo 'Executing preStop'
                    sleep 10

```

## AKS node pools (Terraform)

### `infra/uat/app/infra/env/02-provider.tf`

```
provider "azurerm" {
  subscription_id = var.subscription_id
  features {}
}
provider "azurerm" {
  subscription_id = "3c275d02-525c-4e0d-b6e9-e45ae88f113b"
  features {}
  alias = "prod"
}

provider "azuread" {
}

provider "sops" {}

data "sops_file" "secrets" {
  source_file = "${path.module}/secrets.enc.json"
}

resource "azurerm_resource_group" "resource_group" {
  name     = var.env
  location = var.location

  tags = {
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "platform"
  }
}

data "azurerm_client_config" "current" {}

data "azurerm_kubernetes_cluster" "aks" {
  name                = azurerm_kubernetes_cluster.aks_v2.name
  resource_group_name = azurerm_resource_group.resource_group.name
  depends_on = [
    azurerm_kubernetes_cluster.aks_v2
  ]
}

data "azurerm_key_vault" "main" {
  name                = "ftiuatkv"
  resource_group_name = azurerm_resource_group.resource_group.name
}

data "azurerm_key_vault" "platform_kv" {
  name                = "ftiuat-platform-kv"
  resource_group_name = azurerm_resource_group.resource_group.name
}

provider "kubernetes" {
  host                   = data.azurerm_kubernetes_cluster.aks.kube_admin_config.0.host
  client_certificate     = base64decode(data.azurerm_kubernetes_cluster.aks.kube_admin_config.0.client_certificate)
  client_key             = base64decode(data.azurerm_kubernetes_cluster.aks.kube_admin_config.0.client_key)
  cluster_ca_certificate = base64decode(data.azurerm_kubernetes_cluster.aks.kube_admin_config.0.cluster_ca_certificate)
}

```

### `infra/uat/app/infra/env/07-aks-v2.tf`

```
locals {
  aks_name            = "${var.env}AKSClusterV2"
  aks_kubelet_sp_name = "${var.env}AKSClusterV2-agentpool"
}

resource "azurerm_log_analytics_workspace" "log_analytics_workspace_v2" {
  name                         = "${var.env}Workspace"
  location                     = var.location
  resource_group_name          = lower(azurerm_resource_group.resource_group.name)
  sku                          = "PerGB2018"
  local_authentication_enabled = false

  tags = {
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "platform"
  }
}

data "azurerm_key_vault_key" "aks_node_key" {
  name         = "${local.aks_name}-node-key"
  key_vault_id = data.azurerm_key_vault.platform_kv.id
}

resource "azurerm_kubernetes_cluster" "aks_v2" {
  name                = local.aks_name
  location            = var.location
  dns_prefix          = var.env
  resource_group_name = azurerm_resource_group.resource_group.name

  node_resource_group = local.aks_name
  kubernetes_version  = var.kubernetes_version

  default_node_pool {
    name                   = "systempoolv2"
    vm_size                = "Standard_D8s_v4"
    auto_scaling_enabled   = true
    min_count              = 1
    max_count              = 3
    node_public_ip_enabled = false
    os_disk_size_gb        = 128
    max_pods               = 50
    type                   = "VirtualMachineScaleSets"
    vnet_subnet_id         = azurerm_subnet.app.id
    zones                  = [1, 2, 3]
    node_labels = {
      env        = var.logicalEnv
      wonder-env = var.env
      rfpool     = "syspool"
      datadog    = var.datadogLabel
    }
    upgrade_settings {
      max_surge = 1
    }
  }

  linux_profile {
    admin_username = "aks"
    ssh_key {
      key_data = data.azurerm_key_vault_key.aks_node_key.public_key_openssh
    }
  }

  identity {
    type = "SystemAssigned"
  }

  image_cleaner_interval_hours = 48

  open_service_mesh_enabled        = false
  azure_policy_enabled             = true
  http_application_routing_enabled = false

  oidc_issuer_enabled       = true
  workload_identity_enabled = true

  network_profile {
    network_plugin = "azure"
    dns_service_ip = var.aks_networking.aks_dns_service_ip
    service_cidr   = var.aks_networking.aks_service_cidr
  }

  azure_active_directory_role_based_access_control {
    admin_group_object_ids = [
      "10508151-b3b5-4d77-ba9e-68e28480f862"
    ]
    azure_rbac_enabled = true
    tenant_id          = var.azure_ad_tenant_id
  }

  sku_tier                  = "Standard"
  node_os_upgrade_channel   = "NodeImage"
  automatic_upgrade_channel = "node-image"
  maintenance_window_node_os {
    frequency   = "Weekly"
    interval    = 1
    utc_offset  = "+00:00"
    day_of_week = "Sunday"
    start_time  = "22:00"
    start_date  = "2023-09-16T00:00:00Z"
    duration    = 5
  }

  tags = {
    env        = var.logicalEnv
    wonder-env = var.env
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "platform"
    datadog    = var.datadogLabel
  }
  lifecycle {
    ignore_changes = [
      default_node_pool.0.node_count
    ]
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "apppool_v5" {
  name                   = "apppoolv5"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks_v2.id
  vm_size                = "Standard_D16s_v4"
  auto_scaling_enabled   = true
  min_count              = 7
  max_count              = 24
  node_public_ip_enabled = false
  os_disk_size_gb        = 256
  max_pods               = 100
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  tags = {
    env        = var.logicalEnv
    wonder-env = var.env
    agentpool  = "apppool"
    dept       = "eng"
    pillar     = "platform"
    datadog    = var.datadogLabel
  }
  node_labels = {
    env     = var.logicalEnv
    rfpool  = "apppool"
    datadog = var.datadogLabel
  }
  upgrade_settings {
    max_surge = 1
  }
  lifecycle {
    ignore_changes = [
      node_count
    ]
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "monitoring_v4" {
  name                   = "monitoringv4"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks_v2.id
  vm_size                = "Standard_D8as_v5"
  auto_scaling_enabled   = false
  node_count             = 3
  max_pods               = 50
  node_public_ip_enabled = false
  os_disk_size_gb        = 128
  os_type                = "Linux"
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  tags = {
    env        = var.logicalEnv
    wonder-env = var.env
    agentpool  = "monitoring"
    dept       = "eng"
    pillar     = "platform"
    datadog    = var.datadogLabel
  }
  node_labels = {
    env     = var.logicalEnv
    rfpool  = "monitoring"
    datadog = var.datadogLabel
  }
  upgrade_settings {
    max_surge = 1
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "labv2" {
  name                   = "labv2"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks_v2.id
  vm_size                = "Standard_D8s_v4"
  auto_scaling_enabled   = false
  node_count             = 1
  max_pods               = 50
  node_public_ip_enabled = false
  os_disk_size_gb        = 128
  os_type                = "Linux"
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  node_labels = {
    env     = var.logicalEnv
    rfpool  = "lab"
    datadog = var.datadogLabel
  }
  upgrade_settings {
    max_surge = 1
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "infra_v3" {
  name                   = "infrav3"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks_v2.id
  vm_size                = "Standard_D16as_v5"
  auto_scaling_enabled   = false
  node_count             = 4
  max_pods               = 50
  node_public_ip_enabled = false
  os_disk_size_gb        = 128
  os_type                = "Linux"
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  node_taints = [
    "dedicated=infra:NoSchedule"
  ]
  node_labels = {
    env        = var.logicalEnv
    wonder-env = var.env
    rfpool     = "infra"
    datadog    = var.datadogLabel
  }
  upgrade_settings {
    max_surge = 1
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "appmidware_v2" {
  name                   = "appmidwarev2"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks_v2.id
  vm_size                = "Standard_D8s_v4"
  auto_scaling_enabled   = true
  min_count              = 7
  max_count              = 14
  max_pods               = 50
  node_public_ip_enabled = false
  os_disk_size_gb        = 128
  os_type                = "Linux"
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  node_labels = {
    env        = var.logicalEnv
    wonder-env = var.env
    rfpool     = "appmidware"
    datadog    = var.datadogLabel
  }
  upgrade_settings {
    max_surge = 1
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "build_v2" {
  name                   = "buildv2"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks_v2.id
  vm_size                = "Standard_D8as_v5"
  auto_scaling_enabled   = false
  node_count             = 3
  max_pods               = 50
  node_public_ip_enabled = false
  os_disk_size_gb        = 128
  os_type                = "Linux"
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  node_labels = {
    env        = var.logicalEnv
    wonder-env = var.env
    rfpool     = "build"
    datadog    = var.datadogLabel
  }
  upgrade_settings {
    max_surge = 1
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "consumer_es" {
  name                   = "consumeres"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks_v2.id
  vm_size                = "Standard_D16as_v5"
  auto_scaling_enabled   = true
  min_count              = 5
  max_count              = 8
  max_pods               = 50
  node_public_ip_enabled = false
  os_disk_size_gb        = 128
  os_type                = "Linux"
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  node_taints = [
    "dedicated=consumeres:NoSchedule"
  ]
  node_labels = {
    env        = var.logicalEnv
    wonder-env = var.env
    rfpool     = "consumeres"
    datadog    = var.datadogLabel
  }
  upgrade_settings {
    max_surge = 1
  }
  lifecycle {
    ignore_changes = [
      node_count
    ]
  }
}


data "azurerm_resource_group" "aks_mc_resource_group" {
  name = local.aks_name
}

data "azurerm_public_ips" "aks_public_ips" {
  resource_group_name = data.azurerm_resource_group.aks_mc_resource_group.name
  attachment_status   = "Attached"
  allocation_type     = "Static"
}

data "azurerm_user_assigned_identity" "aks_kubelet_identity" {
  name                = local.aks_kubelet_sp_name
  resource_group_name = data.azurerm_resource_group.aks_mc_resource_group.name
}

resource "azurerm_role_assignment" "allow_kubelet_identity_pull_acr" {
  principal_id         = data.azurerm_user_assigned_identity.aks_kubelet_identity.principal_id
  scope                = azurerm_resource_group.resource_group.id
  role_definition_name = "AcrPull"
  description          = var.terraformDesc
}

data "azurerm_container_registry" "prodacr" {
  name                = "rfprodv2acr"
  resource_group_name = "rfprodv2"
  provider            = azurerm.prod
}

resource "azurerm_role_assignment" "allow_kubelet_identity_pull_prod_acr" {
  principal_id         = data.azurerm_user_assigned_identity.aks_kubelet_identity.principal_id
  scope                = data.azurerm_container_registry.prodacr.id
  role_definition_name = "AcrPull"
  description          = var.terraformDesc
}

resource "azurerm_role_assignment" "allow_kubelet_identity_manage_network" {
  principal_id         = data.azurerm_user_assigned_identity.aks_kubelet_identity.principal_id
  scope                = azurerm_resource_group.resource_group.id
  role_definition_name = "Network Contributor"
  description          = var.terraformDesc
}

resource "azurerm_role_assignment" "allow_kubelet_identity_manage_identity" {
  principal_id         = data.azurerm_user_assigned_identity.aks_kubelet_identity.principal_id
  scope                = azurerm_resource_group.resource_group.id
  role_definition_name = "Managed Identity Operator"
  description          = var.terraformDesc
}

resource "azurerm_role_assignment" "allow_kubelet_identity_manage_mc_vm" {
  principal_id         = data.azurerm_user_assigned_identity.aks_kubelet_identity.principal_id
  scope                = data.azurerm_resource_group.aks_mc_resource_group.id
  role_definition_name = "Virtual Machine Contributor"
  description          = var.terraformDesc
}

data "azuread_service_principal" "aks_control_plane_sp" {
  display_name = local.aks_name
}

resource "azurerm_role_assignment" "allow_aks_control_plane_manage_network" {
  principal_id         = data.azuread_service_principal.aks_control_plane_sp.object_id
  scope                = azurerm_resource_group.resource_group.id
  role_definition_name = "Network Contributor"
  description          = var.terraformDesc
}

# resource "azurerm_management_lock" "aks" {
#   name       = "${azurerm_kubernetes_cluster.aks_v2.name}-delete-lock"
#   scope      = azurerm_kubernetes_cluster.aks_v2.id
#   lock_level = "CanNotDelete"
# }

```

## MySQL Flexible Server (Terraform)

### `infra/uat/app/infra/env/23-mysql-flexible-consumer-server.tf`

```
resource "random_password" "consumer_db_password" {
  length  = 16
  special = true
}

resource "azurerm_mysql_flexible_server" "consumer" {
  name                = "${lower(var.env)}-flexible-consumer-db"
  resource_group_name = azurerm_resource_group.resource_group.name
  location            = var.location

  administrator_login    = "dbadmin"
  administrator_password = random_password.consumer_db_password.result
  sku_name               = "GP_Standard_D8ds_v4"
  version                = "8.4"
  create_mode            = "Default"

  backup_retention_days        = 7
  geo_redundant_backup_enabled = true
  maintenance_window {
    day_of_week  = 1
    start_hour   = 5
    start_minute = 0
  }
  storage {
    auto_grow_enabled  = true
    io_scaling_enabled = true
    size_gb            = 64
  }
  tags = {
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "growth"
    system     = "consumer-wonder"
  }

  zone = "2"

  identity {
    identity_ids = [
      azurerm_user_assigned_identity.mysql_main_aad_identity.id
    ]
    type = "UserAssigned"
  }

  lifecycle {
    ignore_changes = [
      storage[0].auto_grow_enabled,
      storage[0].size_gb
    ]
  }
}

resource "azurerm_private_endpoint" "consumer_db" {
  name                = "${azurerm_mysql_flexible_server.consumer.name}-pep"
  location            = var.location
  resource_group_name = azurerm_resource_group.resource_group.name
  subnet_id           = azurerm_subnet.app.id

  private_service_connection {
    name                           = "${azurerm_mysql_flexible_server.consumer.name}-psc"
    is_manual_connection           = false
    private_connection_resource_id = azurerm_mysql_flexible_server.consumer.id
    subresource_names              = ["mysqlServer"]
  }

  private_dns_zone_group {
    name                 = "consumer-db-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.mysql_privatelink_server.id]
  }

  tags = {
    env        = var.env
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "growth"
    system     = "consumer-wonder"
  }
}

resource "azurerm_mysql_flexible_server_active_directory_administrator" "consumer_aad_admin" {
  server_id   = azurerm_mysql_flexible_server.consumer.id
  identity_id = azurerm_user_assigned_identity.mysql_main_aad_identity.id
  login       = "Terraform"
  object_id   = data.azuread_application.terraform.client_id
  tenant_id   = data.azurerm_client_config.current.tenant_id
}

resource "azurerm_mysql_flexible_server_firewall_rule" "consumer_db_allow_office_access" {
  for_each            = var.mysql_flexible_db_access_allowed_cidr
  name                = each.key
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  start_ip_address    = each.value.start
  end_ip_address      = each.value.end
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_timezone" {
  name                = "time_zone"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "+0:00"
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_wait_timeout" {
  name                = "wait_timeout"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "28800"
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_binlog_row_image" {
  name                = "binlog_row_image"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "FULL"
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_slow_query_log" {
  name                = "slow_query_log"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "ON"
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_long_query_time" {
  name                = "long_query_time"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "10"
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_audit_log_enabled" {
  name                = "audit_log_enabled"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "ON"
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_audit_log_events" {
  name                = "audit_log_events"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "GENERAL"
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_character_set_server" {
  name                = "character_set_server"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "UTF8MB4"
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_log_queries_not_using_indexes" {
  name                = "log_queries_not_using_indexes"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "OFF"
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_sql_mode" {
  name                = "sql_mode"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = ""
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_disable_secure_transport" {
  name                = "require_secure_transport"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "OFF"
}

resource "azurerm_mysql_flexible_server_configuration" "consumer_db_binlog_retain_7_days" {
  name                = "binlog_expire_logs_seconds"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.consumer.name
  value               = "604800"
}

resource "azurerm_management_lock" "mysql_flexible_consumer" {
  name       = "${azurerm_mysql_flexible_server.consumer.name}-delete-lock"
  scope      = azurerm_mysql_flexible_server.consumer.id
  lock_level = "CanNotDelete"
}

resource "azurerm_mysql_flexible_server" "consumer_replica_v2" {
  name                = "${azurerm_mysql_flexible_server.consumer.name}-replica-v2"
  resource_group_name = azurerm_resource_group.resource_group.name
  location            = var.location
  create_mode         = "Replica"
  source_server_id    = azurerm_mysql_flexible_server.consumer.id

  administrator_login          = azurerm_mysql_flexible_server.consumer.administrator_login
  geo_redundant_backup_enabled = azurerm_mysql_flexible_server.consumer.geo_redundant_backup_enabled
  sku_name                     = azurerm_mysql_flexible_server.consumer.sku_name
  version                      = azurerm_mysql_flexible_server.consumer.version
  zone                         = "3"
  backup_retention_days        = azurerm_mysql_flexible_server.consumer.backup_retention_days

  depends_on = [
    azurerm_mysql_flexible_server.consumer
  ]

  storage {
    auto_grow_enabled  = true
    io_scaling_enabled = true
  }

  tags = {
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "growth"
    system     = "consumer-wonder"
  }

  identity {
    type = "UserAssigned"
    identity_ids = [
      azurerm_user_assigned_identity.mysql_main_aad_identity.id
    ]
  }

  lifecycle {
    ignore_changes = [
      storage[0].auto_grow_enabled
    ]
  }
}

resource "azurerm_private_endpoint" "consumer_replica_v2_db" {
  name                = "${azurerm_mysql_flexible_server.consumer_replica_v2.name}-pep"
  location            = var.location
  resource_group_name = azurerm_resource_group.resource_group.name
  subnet_id           = azurerm_subnet.app.id

  private_service_connection {
    name                           = "${azurerm_mysql_flexible_server.consumer_replica_v2.name}-psc"
    is_manual_connection           = false
    private_connection_resource_id = azurerm_mysql_flexible_server.consumer_replica_v2.id
    subresource_names              = ["mysqlServer"]
  }

  private_dns_zone_group {
    name                 = "consumer-db-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.mysql_privatelink_server.id]
  }

  tags = {
    env        = var.env
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "growth"
    system     = "consumer-wonder"
  }
}

```

## MySQL database (Terraform)

### `infra/uat/app/infra/mysql/01-variables.tf`

```
variable "resource_group_name" {
  type    = string
  default = "FTIUat"
}

provider "sops" {}

data "sops_file" "secrets" {
  source_file = "${path.module}/secrets/01-main-db.enc.json"
}

data "sops_file" "consumer_secrets" {
  source_file = "${path.module}/secrets/02-consumer-db.enc.json"
}

data "sops_file" "ba_consumer_secrets" {
  source_file = "${path.module}/secrets/03-ba-consumer-db.enc.json"
}

data "sops_file" "delivery_secrets" {
  source_file = "${path.module}/secrets/04-delivery-db.enc.json"
}

data "sops_file" "product_catalog_secrets" {
  source_file = "${path.module}/secrets/05-product-catalog-db.enc.json"
}

data "sops_file" "pantry_secrets" {
  source_file = "${path.module}/secrets/06-pantry-db.enc.json"
}

data "sops_file" "hdr_secrets" {
  source_file = "${path.module}/secrets/07-hdr-db.enc.json"
}

data "sops_file" "oms_secrets" {
  source_file = "${path.module}/secrets/08-oms-db.enc.json"
}

locals {
  mainDbSecrets = jsondecode(data.sops_file.secrets.raw)
  _mainDb_raw   = jsondecode(file("./main-db-config.json"))
  _ckDb_grants  = jsondecode(file("./ck-db-user-grants.json"))

  _ck_grants_with_db = [
    for grant in local._ckDb_grants.db_user_grants : merge(grant, { db = "central_kitchen" })
  ]

  mainDb = {
    db_host           = local._mainDb_raw.db_host
    master_username   = local._mainDb_raw.master_username
    db_migration_user = local._mainDb_raw.db_migration_user
    databases         = local._mainDb_raw.databases
    aad_users         = local._mainDb_raw.aad_users
    db_user_grants    = concat(local._mainDb_raw.db_user_grants, local._ck_grants_with_db)
  }
  consumerDbSecrets       = jsondecode(data.sops_file.consumer_secrets.raw)
  consumerDb              = jsondecode(file("./consumer-db-config.json"))
  baConsumerDbSecrets     = jsondecode(data.sops_file.ba_consumer_secrets.raw)
  baConsumerDb            = jsondecode(file("./ba-consumer-db-config.json"))
  deliveryDbSecrets       = jsondecode(data.sops_file.delivery_secrets.raw)
  deliveryDb              = jsondecode(file("./delivery-db-config.json"))
  productCatalogDbSecrets = jsondecode(data.sops_file.product_catalog_secrets.raw)
  productCatalogDb        = jsondecode(file("./product-catalog-db-config.json"))
  pantryDbSecrets         = jsondecode(data.sops_file.pantry_secrets.raw)
  pantryDb                = jsondecode(file("./pantry-db-config.json"))
  hdrDbSecrets            = jsondecode(data.sops_file.hdr_secrets.raw)
  hdrDb                   = jsondecode(file("./hdr-db-config.json"))
  omsDbSecrets            = jsondecode(data.sops_file.oms_secrets.raw)
  omsDb                   = jsondecode(file("./oms-db-config.json"))
}

```

### `infra/uat/app/infra/mysql/04-consumer-db.tf`

```
module "consumer_db" {
  source = "./modules/db"

  resource_group        = var.resource_group_name
  db_host               = local.consumerDb.db_host
  master_username       = local.consumerDb.master_username
  db_migration_user     = local.consumerDb.db_migration_user
  db_migration_password = local.consumerDbSecrets.db_migration_password
  dbs                   = local.consumerDb.databases
}

```

### `infra/uat/app/infra/mysql/05-consumer-db-user.tf`

```
module "consumer_db_users" {
  source = "./modules/user"

  db_host         = local.consumerDb.db_host
  master_username = local.consumerDb.master_username
  users           = local.consumerDbSecrets.db_users
  db_user_grants  = local.consumerDb.db_user_grants
  aad_users       = local.consumerDb.aad_users
}

```

### `infra/uat/app/infra/mysql/06-ba-consumer-db.tf`

```
module "ba_consumer_db" {
  source = "./modules/db"

  resource_group        = var.resource_group_name
  db_host               = local.baConsumerDb.db_host
  master_username       = local.baConsumerDb.master_username
  db_migration_user     = local.baConsumerDb.db_migration_user
  db_migration_password = local.baConsumerDbSecrets.db_migration_password
  dbs                   = local.baConsumerDb.databases
}

```

### `infra/uat/app/infra/mysql/07-ba-consumer-db-user.tf`

```
module "ba_consumer_db_users" {
  source = "./modules/user"

  db_host         = local.baConsumerDb.db_host
  master_username = local.baConsumerDb.master_username
  users           = local.baConsumerDbSecrets.db_users
  db_user_grants  = local.baConsumerDb.db_user_grants
  aad_users       = local.baConsumerDb.aad_users
}

```
