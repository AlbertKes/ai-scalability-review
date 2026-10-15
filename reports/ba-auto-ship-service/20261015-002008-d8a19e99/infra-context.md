# Infrastructure Context — ba-auto-ship-service (prod)

Local infra repository root: `/Users/albertke/IdeaProjects/03Wonder/infra`

This document was assembled by selecting only the infra files relevant to **ba-auto-ship-service**, and from
the shared ones only the parts that name it. Cite values from it as
`[Source: code → <path shown in the heading>]`.

## Selection Summary

- Kubernetes manifests: 5 of 34 files included
- AKS node pools (Terraform): 1 of 60 files included

### Skipped or trimmed files

- infra/prod/app/baconsumer/kube/resource/102-service-account.yml (kept 1 of 21 YAML documents — the rest belong to other services)
- infra/prod/app/baconsumer/kube/resource/110-hpa.yml (kept 1 of 17 YAML documents — the rest belong to other services)
- infra/prod/app/baconsumer/kube/resource/18-1-ba-auto-ship-task-service.yml (kept 1 of 2 YAML documents — the rest belong to other services)
- infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-simulation-service.yml (kept 1 of 2 YAML documents — the rest belong to other services)

If a configuration value you need is in one of these files, read it directly from the
infra repository root above.

## Kubernetes manifests

### `infra/prod/app/baconsumer/kube/resource/102-service-account.yml`

```
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: a0942958-cd5e-4fa9-9cfb-c5a90c9cb3fd
  name: ba-auto-ship-service
  namespace: prod-ba-consumer
```

### `infra/prod/app/baconsumer/kube/resource/110-hpa.yml`

```
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ba-auto-ship-service
  namespace: prod-ba-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ba-auto-ship-service
  minReplicas: 3
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 80

  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60  # Only when the metric meet the conditions throughout the entire time window, scaleUp can be considered
      policies:
        - type: Pods
          value: 1
          periodSeconds: 60  # scale up one pod every 60s
    scaleDown:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 1
          periodSeconds: 60  # scale down one pod every 1 min
```

### `infra/prod/app/baconsumer/kube/resource/18-1-ba-auto-ship-task-service.yml`

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ba-auto-ship-task-service
  namespace: prod-ba-consumer
  labels:
    wonder.com/repository: blueapron-project
    wonder.com/team: ba-consumer
    wonder.com/group: app
    wonder.com/owner: miller
    wonder.com/language: java
    wonder.com/framework: core-ng
    wonder.com/api-spec: core-ng
    wonder.com/jira-project-key: BLUE
  annotations:
    wonder.com/pager-duty-service: "[TECH] Blue Apron 2.0"
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ba-auto-ship-task-service
  revisionHistoryLimit: 10
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 10
  template:
    metadata:
      labels:
        app: ba-auto-ship-task-service
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: ba-auto-ship-service
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: ba-auto-ship-task-service
          matchLabelKeys:
            - pod-template-hash
        - maxSkew: 1
          topologyKey: "kubernetes.io/hostname"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: ba-auto-ship-task-service
          matchLabelKeys:
            - pod-template-hash
      nodeSelector:
        rfpool: apppool
      containers:
        - name: ba-auto-ship-task-service
          image: rfprodv2acr.azurecr.io/blueapron-project/ba-auto-ship-service:latest
          volumeMounts:
            - mountPath: /opt/app/tls
              name: tls
              readOnly: true
          env:
            - name: JAVA_OPTS
              value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:MaxMetaspaceSize=128m -Xss256k -XX:G1PeriodicGCInterval=900k"
            - name: DD_TAGS
              value: pillar:growth,team:ba-consumer,framework:core-ng
            - name: APP_ENABLEKAFKA
              value: "true"
            - name: APP_FREESHIPPING_THRESHOLD_ABTESTING_ENABLED
              value: "false"
          envFrom:
            - configMapRef:
                name: ba-auto-ship-service-config
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
            initialDelaySeconds: 20
            periodSeconds: 10
            timeoutSeconds: 1
            successThreshold: 1
            failureThreshold: 3

          resources:
            requests:
              cpu: 2
              memory: 1.5Gi
            limits:
              cpu: 4
              memory: 3Gi
      volumes:
        - name: tls
          secret:
            secretName: wonder-core-ng-tls-secret
```

### `infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-service.yml`

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ba-auto-ship-service
  namespace: prod-ba-consumer
  labels:
    wonder.com/repository: blueapron-project
    wonder.com/team: ba-consumer
    wonder.com/group: app
    wonder.com/owner: miller
    wonder.com/language: java
    wonder.com/framework: core-ng
    wonder.com/api-spec: core-ng
    wonder.com/jira-project-key: BLUE
  annotations:
    wonder.com/pager-duty-service: "[TECH] Blue Apron 2.0"
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ba-auto-ship-service
  revisionHistoryLimit: 10
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 10
  template:
    metadata:
      labels:
        app: ba-auto-ship-service
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: ba-auto-ship-service
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: ba-auto-ship-service
          matchLabelKeys:
            - pod-template-hash
        - maxSkew: 1
          topologyKey: "kubernetes.io/hostname"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: ba-auto-ship-service
          matchLabelKeys:
            - pod-template-hash
      nodeSelector:
        rfpool: apppool
      containers:
        - name: ba-auto-ship-service
          image: rfprodv2acr.azurecr.io/blueapron-project/ba-auto-ship-service:latest
          volumeMounts:
            - mountPath: /opt/app/tls
              name: tls
              readOnly: true
          env:
            - name: JAVA_OPTS
              value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:MaxMetaspaceSize=512m -Xss256k -XX:G1PeriodicGCInterval=900k"
            - name: DD_TAGS
              value: pillar:growth,team:ba-consumer,framework:core-ng
            - name: APP_BAWEBURL
              value: https://blueapron.com
            - name: APP_ENABLEKAFKA
              value: "false"
            - name: APP_FREESHIPPING_THRESHOLD_ABTESTING_ENABLED
              value: "false"
          envFrom:
            - configMapRef:
                name: ba-auto-ship-service-config
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
            initialDelaySeconds: 20
            periodSeconds: 10
            timeoutSeconds: 1
            successThreshold: 1
            failureThreshold: 3

          resources:
            requests:
              cpu: 2
              memory: 1.5Gi
            limits:
              cpu: 4
              memory: 3Gi
      volumes:
        - name: tls
          secret:
            secretName: wonder-core-ng-tls-secret
---
apiVersion: v1
kind: Service
metadata:
  name: ba-auto-ship-service
  namespace: prod-ba-consumer
spec:
  ports:
    - name: "https"
      port: 443
      targetPort: 8443
    - name: "http"
      port: 8080
      targetPort: 8080
  selector:
    app: ba-auto-ship-service

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  annotations:
    acme.cert-manager.io/http01-edit-in-place: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-internal"
    cert-manager.io/duration: 26280h
    cert-manager.io/renew-before: 720h
    nginx.org/ssl-services: ba-auto-ship-service
    nginx.org/redirect-to-https: "true"
  name: ba-auto-ship-service-internal-f5
  namespace: prod-ba-consumer
spec:
  ingressClassName: nginx-f5-internal
  rules:
    - host: ba-auto-ship-service.prod-ba-consumer.wonder.internal
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: ba-auto-ship-service
                port:
                  number: 443
  tls:
    - hosts:
        - ba-auto-ship-service.prod-ba-consumer.wonder.internal
      secretName: ba-auto-ship-service-wonder-tls

```

### `infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-simulation-service.yml`

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ba-auto-ship-simulation-service
  namespace: prod-ba-consumer
  labels:
    wonder.com/repository: blueapron-project
    wonder.com/team: ba-consumer
    wonder.com/group: app
    wonder.com/owner: miller
    wonder.com/language: java
    wonder.com/framework: core-ng
    wonder.com/api-spec: core-ng
    wonder.com/jira-project-key: BLUE
  annotations:
    wonder.com/pager-duty-service: "[TECH] Blue Apron 2.0"
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ba-auto-ship-simulation-service
  revisionHistoryLimit: 10
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 10
  template:
    metadata:
      labels:
        app: ba-auto-ship-simulation-service
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: ba-auto-ship-service
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: ba-auto-ship-simulation-service
          matchLabelKeys:
            - pod-template-hash
        - maxSkew: 1
          topologyKey: "kubernetes.io/hostname"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: ba-auto-ship-simulation-service
          matchLabelKeys:
            - pod-template-hash
      nodeSelector:
        rfpool: apppool
      containers:
        - name: ba-auto-ship-simulation-service
          image: rfprodv2acr.azurecr.io/blueapron-project/ba-auto-ship-service:latest
          volumeMounts:
            - mountPath: /opt/app/tls
              name: tls
              readOnly: true
          env:
            - name: JAVA_OPTS
              value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:MaxMetaspaceSize=128m -Xss256k -XX:G1PeriodicGCInterval=900k"
            - name: DD_TAGS
              value: pillar:growth,team:ba-consumer,framework:core-ng
            - name: APP_ENABLEKAFKA
              value: "false"
            - name: APP_SIMULATIONMODE
              value: "true"
          envFrom:
            - configMapRef:
                name: ba-auto-ship-service-config
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
            initialDelaySeconds: 20
            periodSeconds: 10
            timeoutSeconds: 1
            successThreshold: 1
            failureThreshold: 3

          resources:
            requests:
              cpu: 2
              memory: 1.5Gi
            limits:
              cpu: 4
              memory: 3Gi
      volumes:
        - name: tls
          secret:
            secretName: wonder-core-ng-tls-secret
```

## AKS node pools (Terraform)

### `infra/prod/app/infra/env/12-aks.tf`

```
locals {
  aks_name                = "${var.env}AKSCluster"
  aks_kubelet_sp_name     = "${var.env}AKSCluster-agentpool"
  platform_key_vault_name = "${lower(var.env)}-platform-kv"
}

data "azurerm_key_vault" "platform_key_vault" {
  name                = local.platform_key_vault_name
  resource_group_name = azurerm_resource_group.resource_group.name
}

data "azurerm_key_vault_key" "aks_node_key" {
  name         = "${local.aks_name}-node-key"
  key_vault_id = data.azurerm_key_vault.platform_key_vault.id
}

resource "azurerm_log_analytics_workspace" "log_analytics_workspace" {
  name                         = "${var.env}LogWorkspace"
  location                     = var.location
  resource_group_name          = lower(azurerm_resource_group.resource_group.name)
  sku                          = "PerGB2018"
  local_authentication_enabled = false

  tags = local.tags.platform
}

resource "azurerm_kubernetes_cluster" "aks" {
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
    max_count              = 4
    node_public_ip_enabled = false
    os_disk_size_gb        = 128
    max_pods               = 50
    vnet_subnet_id         = azurerm_subnet.app.id
    zones                  = [1, 2, 3]
    node_labels = {
      wonder-env = var.env
      env        = var.logicalEnv
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

  sku_tier = "Standard"

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

  node_os_upgrade_channel = "None"

  azure_active_directory_role_based_access_control {
    admin_group_object_ids = [
      "10508151-b3b5-4d77-ba9e-68e28480f862"
    ]
    azure_rbac_enabled = true
    tenant_id          = "b8b58c09-c5f3-404d-8960-9d2ae2605573"
  }

  tags = merge(local.tags.platform, local.tags.datadog)

  lifecycle {
    ignore_changes = [
      default_node_pool.0.node_count
    ]
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "apppool_v5" {
  name                   = "apppoolv5"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks.id
  vm_size                = "Standard_D16s_v4"
  auto_scaling_enabled   = true
  min_count              = 18
  max_count              = 42
  node_public_ip_enabled = false
  os_disk_size_gb        = 256
  max_pods               = 100
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  node_labels = {
    env     = var.logicalEnv
    rfpool  = "apppool"
    datadog = var.datadogLabel
  }
  upgrade_settings {
    max_surge = 3
  }
  lifecycle {
    ignore_changes = [
      node_count
    ]
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "lab_v2" {
  name                   = "labv2"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks.id
  vm_size                = "Standard_D8s_v4"
  auto_scaling_enabled   = false
  node_count             = 1
  max_pods               = 50
  node_public_ip_enabled = false
  os_disk_size_gb        = 256
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

resource "azurerm_kubernetes_cluster_node_pool" "monitoring_v3" {
  name                   = "monitoringv3"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks.id
  vm_size                = "Standard_D8s_v4"
  auto_scaling_enabled   = true
  min_count              = 4
  max_count              = 7
  max_pods               = 50
  node_public_ip_enabled = false
  os_disk_size_gb        = 256
  os_type                = "Linux"
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  node_labels = {
    env     = var.logicalEnv
    rfpool  = "monitoring"
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

resource "azurerm_kubernetes_cluster_node_pool" "infra_v3" {
  name                   = "infrav3"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks.id
  vm_size                = "Standard_D16s_v4"
  auto_scaling_enabled   = true
  min_count              = 3
  max_count              = 4
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
    wonder-env = var.env
    env        = var.logicalEnv
    rfpool     = "infra"
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

resource "azurerm_kubernetes_cluster_node_pool" "appmidware_v2" {
  name                   = "appmidwarev2"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks.id
  vm_size                = "Standard_D8s_v4"
  auto_scaling_enabled   = true
  min_count              = 9
  max_count              = 15
  node_public_ip_enabled = false
  os_disk_size_gb        = 128
  os_type                = "Linux"
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  node_labels = {
    wonder-env = var.env
    env        = var.logicalEnv
    rfpool     = "appmidware"
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


resource "azurerm_kubernetes_cluster_node_pool" "build" {
  name                   = "build"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks.id
  vm_size                = "Standard_D8s_v4"
  auto_scaling_enabled   = true
  min_count              = 3
  max_count              = 4
  max_pods               = 50
  node_public_ip_enabled = false
  os_disk_size_gb        = 128
  os_type                = "Linux"
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1, 2, 3]
  node_labels = {
    wonder-env = var.env
    env        = var.logicalEnv
    rfpool     = "build"
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

resource "azurerm_kubernetes_cluster_node_pool" "es_warm_tier" {
  name                   = "eswarmtier"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks.id
  vm_size                = "Standard_E2as_v5"
  auto_scaling_enabled   = false
  node_count             = 2
  node_public_ip_enabled = false
  os_disk_size_gb        = 64
  max_pods               = 100
  vnet_subnet_id         = azurerm_subnet.app.id
  zones                  = [1]
  node_taints = [
    "dedicated=eswarmtier:NoSchedule"
  ]
  node_labels = {
    env     = var.logicalEnv
    rfpool  = "eswarmtier"
    datadog = var.datadogLabel
  }
  upgrade_settings {
    max_surge = 1
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "consumer_es" {
  name                   = "consumeres"
  kubernetes_cluster_id  = azurerm_kubernetes_cluster.aks.id
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
#   name       = "${azurerm_kubernetes_cluster.aks.name}-delete-lock"
#   scope      = azurerm_kubernetes_cluster.aks.id
#   lock_level = "CanNotDelete"
# }

```
