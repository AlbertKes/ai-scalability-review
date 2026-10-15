# Infrastructure Context — mobile-api (prod)

Local infra repository root: `/Users/albertke/IdeaProjects/03Wonder/infra`

This document was assembled by selecting only the infra files relevant to **mobile-api**, and from
the shared ones only the parts that name it. Cite values from it as
`[Source: code → <path shown in the heading>]`.

## Selection Summary

- Kubernetes manifests: 6 of 108 files included
- AKS node pools (Terraform): 1 of 60 files included
- MySQL Flexible Server (Terraform): 2 of 60 files included
- MySQL database (Terraform): 2 of 29 files included

### Skipped or trimmed files

- infra/prod/app/consumer/kube/resource/199-service-account.yml (kept 1 of 42 YAML documents — the rest belong to other services)
- infra/prod/app/consumer/kube/resource/200-consumer-hybrid-hpa.yml (kept 2 of 26 YAML documents — the rest belong to other services)
- infra/prod/app/consumer/kube/resource/55-mobile-search-api.yml (kept 1 of 2 YAML documents — the rest belong to other services)
- infra/prod/app/consumer/kube/resource/59-wonder-app-notification-service.yml (kept 1 of 3 YAML documents — the rest belong to other services)

If a configuration value you need is in one of these files, read it directly from the
infra repository root above.

## Kubernetes manifests

### `infra/prod/app/consumer/kube/resource/199-service-account.yml`

```
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: c7d7ae29-2542-44ee-8333-4980dcf1b98c
  name: mobile-api
  namespace: prod-consumer
```

### `infra/prod/app/consumer/kube/resource/200-consumer-hybrid-hpa.yml`

```
apiVersion: datadoghq.com/v1alpha1
kind: DatadogMetric
metadata:
  name: mobile-api-datadog-metric
  namespace: prod-consumer
spec:
  query: >
    throughput(
      sum:trace.undertow_http.request.hits{
        env:prod,
        service:mobile-api
      }.as_count()
    )
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: mobile-api
  namespace: prod-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mobile-api
  minReplicas: 5
  maxReplicas: 15
  metrics:
    - type: External
      external:
        metric:
          name: datadogmetric@prod-consumer:mobile-api-datadog-metric
        target:
          type: AverageValue
          averageValue: 60
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 50
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      selectPolicy: Max
      policies:
        - type: Percent
          value: 100
          periodSeconds: 10
    scaleDown:
      stabilizationWindowSeconds: 3600
      selectPolicy: Min
      policies:
        - type: Pods
          value: 1
          periodSeconds: 180
```

### `infra/prod/app/consumer/kube/resource/55-mobile-api.yml`

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mobile-api
  namespace: prod-consumer
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
  replicas: 5
  selector:
    matchLabels:
      app: mobile-api
  revisionHistoryLimit: 10
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 10
  template:
    metadata:
      labels:
        app: mobile-api
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: mobile-api
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: mobile-api
          matchLabelKeys:
            - pod-template-hash
        - maxSkew: 1
          topologyKey: "kubernetes.io/hostname"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: mobile-api
          matchLabelKeys:
            - pod-template-hash
      nodeSelector:
        rfpool: apppool
      volumes:
        - name: tls
          secret:
            secretName: wonder-core-ng-tls-secret
      containers:
        - name: mobile-api
          image: rfprodv2acr.azurecr.io/wonder-consumer-project/mobile-api:latest
          volumeMounts:
            - mountPath: /opt/app/tls
              name: tls
              readOnly: true
          env:
            - name: JAVA_OPTS
              value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:MaxMetaspaceSize=256m -Xss256k -XX:G1PeriodicGCInterval=900k"
            - name: DD_TAGS
              value: pillar:growth,team:consumer,framework:core-ng
            - name: APP_GRUBHUBACCESSCONTROL
              value: all
            - name: SHUTDOWN_DELAY_IN_SEC
              value: "5"
          envFrom:
            - configMapRef:
                name: mobile-api-config
          ports:
            - containerPort: 8080 # To use custom prob path, container port is required by AGIC. refer to https://docs.microsoft.com/en-us/azure/application-gateway/ingress-controller-add-health-probes
          readinessProbe:
            httpGet:
              path: /health-check
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /liveness-probe
              port: 8080
              scheme: HTTP
            initialDelaySeconds: 90
            periodSeconds: 10
            timeoutSeconds: 1
            successThreshold: 1
            failureThreshold: 3
          resources:
            requests:
              cpu: 2
              memory: 4Gi
            limits:
              cpu: 4
              memory: 8Gi
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
  name: mobile-api
  namespace: prod-consumer
spec:
  ports:
    - port: 8080
  selector:
    app: mobile-api
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: mobile-api
  namespace: prod-consumer
  annotations:
    appgw.ingress.kubernetes.io/appgw-ssl-certificate: "CloudflareOriginServerCert"
spec:
  ingressClassName: azure-application-gateway-cloudflare
  rules:
    - host: mobile-api.remarkablefoods.net
      http:
        paths:
          - path: /message
            pathType: Prefix
            backend:
              service:
                name: wonder-app-notification-service
                port:
                  number: 8080

          - path: /search
            pathType: Prefix
            backend:
              service:
                name: mobile-search-api
                port:
                  number: 8080

          - path: /
            pathType: Prefix
            backend:
              service:
                name: mobile-api
                port:
                  number: 8080

```

### `infra/prod/app/consumer/kube/resource/55-mobile-search-api.yml`

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mobile-search-api
  namespace: prod-consumer
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
      app: mobile-search-api
  revisionHistoryLimit: 10
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 10
  template:
    metadata:
      labels:
        app: mobile-search-api
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: mobile-api
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: mobile-search-api
          matchLabelKeys:
            - pod-template-hash
        - maxSkew: 1
          topologyKey: "kubernetes.io/hostname"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: mobile-search-api
          matchLabelKeys:
            - pod-template-hash
      nodeSelector:
        rfpool: apppool
      volumes:
        - name: tls
          secret:
            secretName: wonder-core-ng-tls-secret
      containers:
        - name: mobile-search-api
          image: rfprodv2acr.azurecr.io/wonder-consumer-project/mobile-api:latest
          volumeMounts:
            - mountPath: /opt/app/tls
              name: tls
              readOnly: true
          env:
            - name: JAVA_OPTS
              value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:MaxMetaspaceSize=256m -Xss256k -XX:G1PeriodicGCInterval=900k"
            - name: DD_TAGS
              value: pillar:growth,team:consumer,framework:core-ng
            - name: APP_GRUBHUBACCESSCONTROL
              value: all
            - name: SHUTDOWN_DELAY_IN_SEC
              value: "5"
          envFrom:
            - configMapRef:
                name: mobile-api-config
          ports:
            - containerPort: 8080 # To use custom prob path, container port is required by AGIC. refer to https://docs.microsoft.com/en-us/azure/application-gateway/ingress-controller-add-health-probes
          readinessProbe:
            httpGet:
              path: /health-check
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /liveness-probe
              port: 8080
              scheme: HTTP
            initialDelaySeconds: 90
            periodSeconds: 10
            timeoutSeconds: 1
            successThreshold: 1
            failureThreshold: 3
          resources:
            requests:
              cpu: 2
              memory: 4Gi
            limits:
              cpu: 4
              memory: 8Gi
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

### `infra/prod/app/consumer/kube/resource/59-wonder-app-notification-service.yml`

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wonder-app-notification-service
  namespace: prod-consumer
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
      app: wonder-app-notification-service
  revisionHistoryLimit: 10
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 10
  template:
    metadata:
      labels:
        app: wonder-app-notification-service
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: mobile-api
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: wonder-app-notification-service
          matchLabelKeys:
            - pod-template-hash
        - maxSkew: 1
          topologyKey: "kubernetes.io/hostname"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: wonder-app-notification-service
          matchLabelKeys:
            - pod-template-hash
      nodeSelector:
        rfpool: apppool
      volumes:
        - name: tls
          secret:
            secretName: wonder-core-ng-tls-secret
      containers:
        - name: wonder-app-notification-service
          image: rfprodv2acr.azurecr.io/wonder-consumer-project/wonder-app-notification-service:latest
          volumeMounts:
            - mountPath: /opt/app/tls
              name: tls
              readOnly: true
          env:
            - name: JAVA_OPTS
              value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:MaxMetaspaceSize=256m -Xss256k -XX:G1PeriodicGCInterval=900k"
            - name: DD_TAGS
              value: pillar:growth,team:consumer,framework:core-ng
            - name: SHUTDOWN_DELAY_IN_SEC
              value: "5"
          ports:
            - containerPort: 8080 # To use custom prob path, container port is required by AGIC. refer to https://docs.microsoft.com/en-us/azure/application-gateway/ingress-controller-add-health-probes
          readinessProbe:
            httpGet:
              path: /health-check
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /liveness-probe
              port: 8080
              scheme: HTTP
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

### `infra/prod/app/consumer/kube/resource/kafka-consumer/55-mobile-api-kafka-consumer.yml`

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mobile-api-kafka-consumer
  namespace: prod-consumer
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
      app: mobile-api-kafka-consumer
  revisionHistoryLimit: 10
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 10
  template:
    metadata:
      labels:
        app: mobile-api-kafka-consumer
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: mobile-api
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: mobile-api-kafka-consumer
          matchLabelKeys:
            - pod-template-hash
        - maxSkew: 1
          topologyKey: "kubernetes.io/hostname"
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: mobile-api-kafka-consumer
          matchLabelKeys:
            - pod-template-hash
      nodeSelector:
        rfpool: apppool
      volumes:
        - name: tls
          secret:
            secretName: wonder-core-ng-tls-secret
      containers:
        - name: mobile-api-kafka-consumer
          image: rfprodv2acr.azurecr.io/wonder-consumer-project/mobile-api:latest
          volumeMounts:
            - mountPath: /opt/app/tls
              name: tls
              readOnly: true
          env:
            - name: JAVA_OPTS
              value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:MaxMetaspaceSize=256m -Xss256k -XX:G1PeriodicGCInterval=900k"
            - name: DD_TAGS
              value: pillar:growth,team:consumer,framework:core-ng
            - name: APP_GRUBHUBACCESSCONTROL
              value: all
            - name: SHUTDOWN_DELAY_IN_SEC
              value: "5"
            - name: APP_ENABLEKAFKACONSUMER
              value: "true"
          envFrom:
            - configMapRef:
                name: mobile-api-config
          ports:
            - containerPort: 8080 # To use custom prob path, container port is required by AGIC. refer to https://docs.microsoft.com/en-us/azure/application-gateway/ingress-controller-add-health-probes
          readinessProbe:
            httpGet:
              path: /health-check
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /health-check
              port: 8080
              scheme: HTTP
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
              cpu: 3
              memory: 3Gi
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

## MySQL Flexible Server (Terraform)

### `infra/prod/app/infra/env/16-mysql-flexible-wonder-db.tf`

```
resource "random_password" "mysql_flexible_wonder_db_password" {
  length  = 16
  special = true
}

resource "azurerm_mysql_flexible_server" "wonder" {
  name                         = "${lower(var.env)}-flexible-wonder-db"
  location                     = var.location
  resource_group_name          = azurerm_resource_group.resource_group.name
  administrator_login          = "dbadmin"
  administrator_password       = random_password.mysql_flexible_wonder_db_password.result
  sku_name                     = "MO_Standard_E16ds_v4"
  version                      = "8.4"
  zone                         = "1"
  backup_retention_days        = 7
  geo_redundant_backup_enabled = true

  high_availability {
    mode                      = "SameZone"
    standby_availability_zone = "1"
  }

  maintenance_window {
    day_of_week  = 2
    start_hour   = 9
    start_minute = 0
  }

  storage {
    auto_grow_enabled   = true
    io_scaling_enabled  = true
    log_on_disk_enabled = true
    size_gb             = 500
  }

  tags = merge(local.tags.datadog, local.tags.consumer)

  identity {
    identity_ids = [
      azurerm_user_assigned_identity.mysql_aad_identity.id
    ]
    type = "UserAssigned"
  }

  lifecycle {
    ignore_changes = [
      storage[0].size_gb
    ]
  }
}

resource "azurerm_mysql_flexible_server_active_directory_administrator" "wonder_aad_admin" {
  server_id   = azurerm_mysql_flexible_server.wonder.id
  identity_id = azurerm_user_assigned_identity.mysql_aad_identity.id
  login       = "Terraform"
  object_id   = data.azuread_application.terraform.client_id
  tenant_id   = data.azurerm_client_config.current.tenant_id
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_timezone" {
  name                = "time_zone"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "+0:00"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_character_set_server" {
  name                = "character_set_server"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "UTF8MB4"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_wait_timeout" {
  name                = "wait_timeout"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "28800"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_binlog_row_image" {
  name                = "binlog_row_image"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "FULL"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_slow_query_log" {
  name                = "slow_query_log"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "ON"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_long_query_time" {
  name                = "long_query_time"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "5"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_audit_log_enabled" {
  name                = "audit_log_enabled"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "ON"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_audit_log_events" {
  name                = "audit_log_events"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "CONNECTION,DML,DDL,ADMIN"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_audit_log_exclude_users" {
  name                = "audit_log_exclude_users"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "sp-*,azure_superuser"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_log_queries_not_using_indexes" {
  name                = "log_queries_not_using_indexes"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "OFF"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_log_output" {
  name                = "log_output"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "FILE"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_sql_mode" {
  name                = "sql_mode"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = ""
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_disable_secure_transport" {
  name                = "require_secure_transport"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "OFF"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_binlog_retain_7_days" {
  name                = "binlog_expire_logs_seconds"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder.name
  value               = "604800"
}

# gtid_mode changes between modes can only be done one step at a time in ascending order (ex. OFF -> OFF_PERMISSIVE -> ON_PERMISSIVE -> ON)
# So, the following two configurations will be done on Azure Portal
# See: https://docs.microsoft.com/en-us/azure/mysql/concepts-read-replicas#global-transaction-identifier-gtid
#resource "azurerm_mysql_flexible_server_configuration" "wonder_db_enforce_gtid_consistency" {
#  name                = "enforce_gtid_consistency"
#  resource_group_name = azurerm_resource_group.resource_group.name
#  server_name         = azurerm_mysql_flexible_server.wonder.name
#  value               = "ON"
#}
#
#resource "azurerm_mysql_flexible_server_configuration" "wonder_db_turn_gtid_mode_on" {
#  name                = "gtid_mode"
#  resource_group_name = azurerm_resource_group.resource_group.name
#  server_name         = azurerm_mysql_flexible_server.wonder.name
#  value               = "ON"
#}

resource "azurerm_management_lock" "mysql_flexible_wonder" {
  name       = "${azurerm_mysql_flexible_server.wonder.name}-delete-lock"
  scope      = azurerm_mysql_flexible_server.wonder.id
  lock_level = "CanNotDelete"
}

resource "azurerm_log_analytics_workspace" "mysql_flexible_wonder_log" {
  name                         = "${var.env}-mysql-flexible-wonder-log"
  location                     = var.location
  resource_group_name          = azurerm_resource_group.resource_group.name
  sku                          = "PerGB2018"
  local_authentication_enabled = false
  tags                         = local.tags.consumer
}

resource "azurerm_monitor_diagnostic_setting" "mysql_flexible_wonder_diagnostic_setting" {
  name                       = "${var.env}-mysql-flexible-wonder-diagnostic"
  target_resource_id         = azurerm_mysql_flexible_server.wonder.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.mysql_flexible_wonder_log.id

  enabled_log {
    category = "MySqlSlowLogs"
  }
  enabled_log {
    category = "MySqlAuditLogs"
  }
  metric {
    category = "AllMetrics"
    enabled  = false
  }
}

resource "azurerm_mysql_flexible_server" "wonder_replica_v4" {
  name                = "${azurerm_mysql_flexible_server.wonder.name}-replica-v4"
  resource_group_name = azurerm_resource_group.resource_group.name
  location            = var.location
  # create_mode         = "Replica"
  source_server_id = azurerm_mysql_flexible_server.wonder.id

  administrator_login          = azurerm_mysql_flexible_server.wonder.administrator_login
  geo_redundant_backup_enabled = azurerm_mysql_flexible_server.wonder.geo_redundant_backup_enabled
  sku_name                     = azurerm_mysql_flexible_server.wonder.sku_name
  version                      = azurerm_mysql_flexible_server.wonder.version
  zone                         = "3"
  backup_retention_days        = azurerm_mysql_flexible_server.wonder.backup_retention_days

  maintenance_window {
    day_of_week  = azurerm_mysql_flexible_server.wonder.maintenance_window[0].day_of_week
    start_hour   = azurerm_mysql_flexible_server.wonder.maintenance_window[0].start_hour
    start_minute = azurerm_mysql_flexible_server.wonder.maintenance_window[0].start_minute
  }

  storage {
    auto_grow_enabled   = azurerm_mysql_flexible_server.wonder.storage[0].auto_grow_enabled
    io_scaling_enabled  = azurerm_mysql_flexible_server.wonder.storage[0].io_scaling_enabled
    log_on_disk_enabled = azurerm_mysql_flexible_server.wonder.storage[0].log_on_disk_enabled
    size_gb             = azurerm_mysql_flexible_server.wonder.storage[0].size_gb
  }

  tags = merge(local.tags.datadog, local.tags.consumer)

  identity {
    identity_ids = [
      azurerm_user_assigned_identity.mysql_aad_identity.id
    ]
    type = "UserAssigned"
  }
  lifecycle {
    ignore_changes = [
      storage[0].size_gb
    ]
  }
}

resource "azurerm_monitor_diagnostic_setting" "mysql_flexible_wonder_replica_v4_diagnostic_setting" {
  name                       = "${var.env}-mysql-flexible-wonder-replica-v4-diagnostic"
  target_resource_id         = azurerm_mysql_flexible_server.wonder_replica_v4.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.mysql_flexible_wonder_log.id

  enabled_log {
    category = "MySqlSlowLogs"
  }
  enabled_log {
    category = "MySqlAuditLogs"
  }
  metric {
    category = "AllMetrics"
    enabled  = false
  }
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_replica_audit_log_enabled" {
  name                = "audit_log_enabled"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder_replica_v4.name
  value               = "ON"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_replica_audit_log_events" {
  name                = "audit_log_events"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder_replica_v4.name
  value               = "CONNECTION,DML,DDL,ADMIN"
}

resource "azurerm_mysql_flexible_server_configuration" "wonder_db_replica_audit_log_exclude_users" {
  name                = "audit_log_exclude_users"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.wonder_replica_v4.name
  value               = "sp-*,azure_superuser"
}

resource "azurerm_private_endpoint" "mysql_wonder_pep" {
  name                          = "${azurerm_mysql_flexible_server.wonder.name}-pep"
  location                      = var.location
  resource_group_name           = azurerm_resource_group.resource_group.name
  subnet_id                     = azurerm_subnet.app.id
  custom_network_interface_name = "${azurerm_mysql_flexible_server.wonder.name}-pep-nic"

  private_service_connection {
    name                           = "${azurerm_mysql_flexible_server.wonder.name}-pep"
    is_manual_connection           = false
    private_connection_resource_id = azurerm_mysql_flexible_server.wonder.id
    subresource_names              = ["mysqlServer"]
  }

  private_dns_zone_group {
    name                 = "default"
    private_dns_zone_ids = [azurerm_private_dns_zone.mysql_privatelink_server.id]
  }

  tags = local.tags.consumer
}

resource "azurerm_private_endpoint" "mysql_wonder_replica_v4_pep" {
  name                          = azurerm_mysql_flexible_server.wonder_replica_v4.name
  location                      = var.location
  resource_group_name           = azurerm_resource_group.resource_group.name
  subnet_id                     = azurerm_subnet.app.id
  custom_network_interface_name = "${azurerm_mysql_flexible_server.wonder_replica_v4.name}-nic"

  private_service_connection {
    name                           = azurerm_mysql_flexible_server.wonder_replica_v4.name
    is_manual_connection           = false
    private_connection_resource_id = azurerm_mysql_flexible_server.wonder_replica_v4.id
    subresource_names              = ["mysqlServer"]
  }

  private_dns_zone_group {
    name                 = "default"
    private_dns_zone_ids = [azurerm_private_dns_zone.mysql_privatelink_server.id]
  }

  tags = local.tags.consumer
}

```

### `infra/prod/app/infra/env/98-import.tf`

```
# DEV-953
import {
  id = "/subscriptions/3c275d02-525c-4e0d-b6e9-e45ae88f113b/resourceGroups/RFProdV2/providers/Microsoft.DBforMySQL/flexibleServers/rfprodv2-flexible-wonder-db-replica-v4"
  to = azurerm_mysql_flexible_server.wonder_replica_v4
}

import {
  id = "/subscriptions/3c275d02-525c-4e0d-b6e9-e45ae88f113b/resourceGroups/RFProdV2/providers/Microsoft.Network/privateEndpoints/rfprodv2-flexible-wonder-db-replica-v4"
  to = azurerm_private_endpoint.mysql_wonder_replica_v4_pep
}

# These role grants were manually removed due to failing Terraform applys in the wake
# of the Azure CDN outage and subsequent resource manager API performance degredation.
# https://wonder.atlassian.net/browse/KT-2892
removed {
  from = azurerm_role_assignment.Rfprodv2storage_StorageBlobDataReader_DavidGiliotti
  lifecycle {
    destroy = false
  }
}

# https://wonder.atlassian.net/browse/KT-2892
removed {
  from = azurerm_role_assignment.Rfprodv2storage_StorageBlobDataReader_JenniferWang
  lifecycle {
    destroy = false
  }
}

# https://wonder.atlassian.net/browse/KT-2892
removed {
  from = azurerm_role_assignment.Rfprodv2storage_StorageBlobDataReader_KunalThorat
  lifecycle {
    destroy = false
  }
}

# https://wonder.atlassian.net/browse/KT-2892
removed {
  from = azurerm_role_assignment.Rfprodv2ladlestorage_StorageBlobDataReader_SahilVora
  lifecycle {
    destroy = false
  }
}

# https://wonder.atlassian.net/browse/WD-1952
moved {
  from = module.azure-rbac-management.azurerm_role_assignment.rbac-builtin-service-principal-role-assignment["sp-/subscriptions/3c275d02-525c-4e0d-b6e9-e45ae88f113b:c61f8443-8ba1-4a41-9fa8-ec3a42a0356f:Microsoft Defender for Cloud Servers Scanner Resource Provider-VM Scanner Operator"]
  to   = module.azure-rbac-management.azurerm_role_assignment.rbac-builtin-service-principal-role-assignment["sp-/subscriptions/3c275d02-525c-4e0d-b6e9-e45ae88f113b:c61f8443-8ba1-4a41-9fa8-ec3a42a0356f:Microsoft Defender for Cloud Servers Scanner Resource Provider-Defender Agentless VM Scan"]
}

import {
  id = "/subscriptions/3c275d02-525c-4e0d-b6e9-e45ae88f113b/resourceGroups/RFProdV2/providers/Microsoft.Storage/storageAccounts/rfprodv2uatdataexchange/blobServices/default/containers/blueapron"
  to = module.storage.azurerm_storage_container.containers["uat_data_exchange.blueapron"]
}

# [WD-4312] Containers existed in Azure with publicAccess=container; import so
# Terraform does not recreate them, then lock down to private.
import {
  to = module.storage.azurerm_storage_container.containers["storage.ship-forecast-override"]
  id = "/subscriptions/3c275d02-525c-4e0d-b6e9-e45ae88f113b/resourceGroups/RFProdV2/providers/Microsoft.Storage/storageAccounts/rfprodv2storage/blobServices/default/containers/ship-forecast-override"
}

import {
  to = module.storage.azurerm_storage_container.containers["storage.ship-picklist"]
  id = "/subscriptions/3c275d02-525c-4e0d-b6e9-e45ae88f113b/resourceGroups/RFProdV2/providers/Microsoft.Storage/storageAccounts/rfprodv2storage/blobServices/default/containers/ship-picklist"
}

# [KT-3262] Containers were manually created; import so Terraform does not recreate them
import {
  to = azurerm_cosmosdb_sql_container.extensiv-data
  id = "/subscriptions/3c275d02-525c-4e0d-b6e9-e45ae88f113b/resourceGroups/RFProdV2/providers/Microsoft.DocumentDB/databaseAccounts/rfprodv2-main/sqlDatabases/KitchenDB/containers/extensiv-data"
}

import {
  to = azurerm_cosmosdb_sql_container.extensiv-data-lease
  id = "/subscriptions/3c275d02-525c-4e0d-b6e9-e45ae88f113b/resourceGroups/RFProdV2/providers/Microsoft.DocumentDB/databaseAccounts/rfprodv2-main/sqlDatabases/KitchenDB/containers/extensiv-data-lease"
}

```

## MySQL database (Terraform)

### `infra/prod/app/infra/mysql/01-variables.tf`

```
variable "resource_group_name" {
  type    = string
  default = "RFProdV2"
}

provider "sops" {}

data "sops_file" "ck_secrets" {
  source_file = "${path.module}/secrets/ck-db.enc.json"
}

data "sops_file" "main_secrets" {
  source_file = "${path.module}/secrets/main-db.enc.json"
}

data "sops_file" "wonder_secrets" {
  source_file = "${path.module}/secrets/wonder-db.enc.json"
}

data "sops_file" "df_secrets" {
  source_file = "${path.module}/secrets/df-db.enc.json"
}

data "sops_file" "hdr_secrets" {
  source_file = "${path.module}/secrets/hdr-db.enc.json"
}

data "sops_file" "wonder_development_secrets" {
  source_file = "${path.module}/secrets/wonder-development-db.enc.json"
}

data "sops_file" "wonderworks_secrets" {
  source_file = "${path.module}/secrets/wonderworks-db.enc.json"
}

data "sops_file" "shipping_secrets" {
  source_file = "${path.module}/secrets/shipping-db.enc.json"
}

data "sops_file" "pantry_secrets" {
  source_file = "${path.module}/secrets/pantry-db.enc.json"
}

data "sops_file" "platform_services_secrets" {
  source_file = "${path.module}/secrets/platform-services-db.enc.json"
}

data "sops_file" "delivery_secrets" {
  source_file = "${path.module}/secrets/delivery-db.enc.json"
}

data "sops_file" "ba_consumer_secrets" {
  source_file = "${path.module}/secrets/ba-consumer-db.enc.json"
}

data "sops_file" "product_catalog_secrets" {
  source_file = "${path.module}/secrets/product-catalog-db.enc.json"
}

data "sops_file" "oms_secrets" {
  source_file = "${path.module}/secrets/oms-db.enc.json"
}

locals {
  ckDb                       = jsondecode(file("./configs/ck-db-config.json"))
  ckDbSecrets                = jsondecode(data.sops_file.ck_secrets.raw)
  mainDb                     = jsondecode(file("./configs/main-db-config.json"))
  mainDbSecrets              = jsondecode(data.sops_file.main_secrets.raw)
  wonderDb                   = jsondecode(file("./configs/wonder-db-config.json"))
  wonderDbSecrets            = jsondecode(data.sops_file.wonder_secrets.raw)
  dfDb                       = jsondecode(file("./configs/df-db-config.json"))
  dfDbSecrets                = jsondecode(data.sops_file.df_secrets.raw)
  hdrDb                      = jsondecode(file("./configs/hdr-db-config.json"))
  hdrDbSecrets               = jsondecode(data.sops_file.hdr_secrets.raw)
  wonderDevelopmentDb        = jsondecode(file("./configs/wonder-development-db-config.json"))
  wonderDevelopmentDbSecrets = jsondecode(data.sops_file.wonder_development_secrets.raw)
  wonderworksDb              = jsondecode(file("./configs/wonderworks-db-config.json"))
  wonderworksDbSecrets       = jsondecode(data.sops_file.wonderworks_secrets.raw)
  shippingDb                 = jsondecode(file("./configs/shipping-db-config.json"))
  shippingDbSecrets          = jsondecode(data.sops_file.shipping_secrets.raw)
  pantryDb                   = jsondecode(file("./configs/pantry-db-config.json"))
  pantryDbSecrets            = jsondecode(data.sops_file.pantry_secrets.raw)
  platformServicesDb         = jsondecode(file("./configs/platform-services-db-config.json"))
  platformServicesDbSecrets  = jsondecode(data.sops_file.platform_services_secrets.raw)
  deliveryDb                 = jsondecode(file("./configs/delivery-db-config.json"))
  deliveryDbSecrets          = jsondecode(data.sops_file.delivery_secrets.raw)
  baConsumerDb               = jsondecode(file("./configs/ba-consumer-db-config.json"))
  baConsumerDbSecrets        = jsondecode(data.sops_file.ba_consumer_secrets.raw)
  productCatalogDb           = jsondecode(file("./configs/product-catalog-db-config.json"))
  productCatalogDbSecrets    = jsondecode(data.sops_file.product_catalog_secrets.raw)
  omsDb                      = jsondecode(file("./configs/oms-db-config.json"))
  omsDbSecrets               = jsondecode(data.sops_file.oms_secrets.raw)
}

```

### `infra/prod/app/infra/mysql/02-wonder-db.tf`

```
module "wonder_db" {
  source = "./modules/db"

  resource_group        = var.resource_group_name
  db_host               = local.wonderDb.db_host
  master_username       = local.wonderDb.master_username
  db_migration_user     = local.wonderDb.db_migration_user
  db_migration_password = local.wonderDbSecrets.db_migration_password
  databases             = local.wonderDb.databases
}

module "wonder_db_users" {
  source = "./modules/user"

  db_host          = local.wonderDb.db_host
  master_username  = local.wonderDb.master_username
  users            = local.wonderDbSecrets.db_users
  db_user_grants   = local.wonderDb.db_user_grants
  aad_users        = local.wonderDb.aad_users
  sensitive_tables = local.wonderDb.sensitive_tables
  databases        = local.wonderDb.databases
}

moved {
  from = module.wonder_db_users.mysql_grant.aad_user_grant
  to   = module.wonder_db_users.mysql_grant.service_grants
}

```
