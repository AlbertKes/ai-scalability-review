# Infrastructure Context — wonder-cart-service (uat)

Local infra repository root: `/Users/albertke/IdeaProjects/03Wonder/infra`

This document was assembled by selecting only the infra files relevant to **wonder-cart-service**. Cite
values from it as `[Source: code → <path shown in the heading>]`.

## Selection Summary

- Kubernetes manifests: 6 of 82 files included
- AKS node pools (Terraform): 5 of 64 files included
- MySQL Flexible Server (Terraform): 2 of 64 files included
- MySQL database (Terraform): 5 of 29 files included

No files were skipped or truncated.

## Kubernetes manifests

### `infra/uat/app/consumer/kube/resource/190-hpa.yml`

```
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: apns-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: apns-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ba-customer-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ba-customer-service
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ba-marketing-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ba-marketing-service
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-delivery-zone-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-delivery-zone-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: credit-card-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: credit-card-service
  minReplicas: 2
  maxReplicas: 6
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: customer-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: customer-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: customer-survey-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: customer-survey-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: customer-wallet-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: customer-wallet-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: etl-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: etl-service
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: grubhub-integration-feed-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: grubhub-integration-feed-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: grubhub-integration-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: grubhub-integration-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: grubhub-restaurant-feed-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: grubhub-restaurant-feed-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: image-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: image-service
  minReplicas: 2
  maxReplicas: 6
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: marketing-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: marketing-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: mobile-api
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mobile-api
  minReplicas: 5
  maxReplicas: 15
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
          value: 4
          periodSeconds: 10
    scaleDown:
      stabilizationWindowSeconds: 3600
      policies:
        - type: Pods
          value: 1
          periodSeconds: 180
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: pop-api
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: pop-api
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: restaurant-recommendation-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: restaurant-recommendation-service
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: restaurant-service-v2
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: restaurant-service-v2
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: search-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: search-service
#    prod   minReplicas  config 10
  minReplicas: 10
  maxReplicas: 30
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
          value: 4
          periodSeconds: 10
    scaleDown:
      stabilizationWindowSeconds: 3600
      policies:
        - type: Pods
          value: 1
          periodSeconds: 180

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
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
---
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: wonder-setting-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: wonder-setting-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: wonder-web-api
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: wonder-web-api
  minReplicas: 5
  maxReplicas: 15
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
          value: 4
          periodSeconds: 10
    scaleDown:
      stabilizationWindowSeconds: 3600
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
    azure.workload.identity/client-id: d6c762da-5475-4389-b9d1-5ecdaf9d5066
  name: marketing-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 10f4f6c9-b474-48ca-b99a-0e565b12e237
  name: wonder-setting-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: ed56d44c-4113-41ce-86ca-feb6412f7af4
  name: image-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 5fe7650f-834e-47bc-b4cc-b4fd35b5fa74
  name: marketing-site
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 4cfd257c-3d3b-443f-aa37-7987094788ed
  name: merchandising-site
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: a54dbc34-59d6-4ae5-a553-80ae091f7455
  name: wonder-setting-site
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: b7779c40-0439-4a9f-b304-0a721e888902
  name: wonder-spot-site
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 6f712e22-9596-445d-9893-24d1e0340ad5
  name: wonder-web-api
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: bd341db7-05e7-42c5-a10f-6ebc995af844
  name: wonder-portal-site
  namespace: uat-consumer
---
# OMS apps migrated to ArgoCD (WD-3991) — ServiceAccounts removed:
# order-search-service, customer-service-site, fulfillment-service,
# order-number-service, order-service, tax-service, dbw-order-service,
# gift-card-order-service, payment-service, zendesk-integration-service,
# oms-task-service, forter-integration-service, decagon-integration-service
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 53dc9cc8-168f-4e90-aede-e298455af544
  name: mobile-api
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: ba0fca24-019f-405d-8d38-926ed59ceb0a
  name: credit-card-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 94151b95-0cbb-41ce-bbd2-9ef4ee105e5f
  name: customer-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 35ce1e36-b24b-453b-814c-1cfcfd69d607
  name: etl-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: ab491aeb-b7ba-4deb-92fb-26cd15c34f8d
  name: user-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 1d833bc8-6ed6-499a-a84b-89eb0156aaaf
  name: wonder-cart-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: da347862-55b3-4cee-9761-6dbe58baefe4
  name: customer-wallet-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 46a8bccc-0b13-4d3c-a635-f92febd25776
  name: customer-survey-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 6527150b-e5b7-4bb8-aa4a-271570cec8f1
  name: pop-api
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 2bb43cab-f9fa-4814-b428-13b88a7ba012
  name: wonder-app-task-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 3dd6ac43-030f-4084-a3be-b9b25fdaaae8
  name: grubhub-integration-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: aad8d50a-3b95-4e04-a79c-b74ea8640de7
  name: apns-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 64e6ae61-4a32-42de-b882-98a5aea2cea9
  name: consumer-delivery-zone-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 40010386-78b0-4289-92f0-0f40b1f91ef8
  name: restaurant-service-v2
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 1d2fd366-9bac-4c49-949a-bd4146bb1039
  name: grubhub-restaurant-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: e3d1b31d-e2b2-491c-9c85-a080c3e4e94c
  name: restaurant-recommendation-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 3a2fa66c-f672-4f48-87f7-5e58fb1f14d0
  name: wonder-portal-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 71c3ea79-d9dc-4ba4-a1d4-ebb52440fd1d
  name: wonder-portal-cms-site
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 463c453a-5a17-4973-85a3-3b80c6b2cd23
  name: local-restaurant-status-calculator
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: d14f7764-4b02-4091-adc4-3a733ab4b820
  name: search-service
  namespace: uat-consumer
---
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: 05b925cd-fcdb-499c-b9d3-dcc84b902b60
  name: wonder-create-integration-service
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-consumer-delivery-zone-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-consumer-delivery-zone-service
  minReplicas: 4
  maxReplicas: 12
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
          value: 4
          periodSeconds: 10
    scaleDown:
      stabilizationWindowSeconds: 3600
      policies:
        - type: Pods
          value: 1
          periodSeconds: 180
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-credit-card-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-credit-card-service
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
          value: 2
          periodSeconds: 10
    scaleDown:
      stabilizationWindowSeconds: 3600
      policies:
        - type: Pods
          value: 1
          periodSeconds: 180
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-customer-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-customer-service
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-customer-survey-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-customer-survey-service
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-customer-wallet-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-customer-wallet-service
  minReplicas: 3
  maxReplicas: 9
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 120
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-grubhub-integration-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-grubhub-integration-service
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-grubhub-restaurant-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-grubhub-restaurant-service
  minReplicas: 6
  maxReplicas: 18
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
          value: 6
          periodSeconds: 10
    scaleDown:
      stabilizationWindowSeconds: 3600
      policies:
        - type: Pods
          value: 1
          periodSeconds: 180
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-marketing-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-marketing-service
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-restaurant-service-v2
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-restaurant-service-v2
  minReplicas: 6
  maxReplicas: 18
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
          value: 6
          periodSeconds: 10
    scaleDown:
      stabilizationWindowSeconds: 3600
      policies:
        - type: Pods
          value: 1
          periodSeconds: 180
---
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
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-wonder-setting-service
  namespace: uat-consumer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: consumer-wonder-setting-service
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

### `infra/uat/app/infra/env/08-aks-workload-identity.tf`

```
locals {
  workloadIdentityFile = yamldecode(file("./workload_identity/aks_user_managed_identity.yml"))

  workloadIdentityList = flatten([
    for identity, config in local.workloadIdentityFile : [
      for federated_subject in config.subjects : {
        identity_name     = identity
        federated_subject = federated_subject
      }
    ]
  ])

  workloadIdentityTags = {
    for identity, config in local.workloadIdentityFile : identity => {
      pillar = lookup(config, "pillar", null)
      system = lookup(config, "system", null)
    }
  }
}

resource "azurerm_user_assigned_identity" "user_assigned_identity" {
  for_each = {
    for identity, config in local.workloadIdentityFile : identity => identity
  }
  name                = "${var.env}-${each.value}"
  location            = var.location
  resource_group_name = azurerm_resource_group.resource_group.name
  tags = merge(
    {
      department = "devops"
      team       = "devops"
      dept       = "eng"
    },
    local.workloadIdentityTags[each.value].pillar != null ? { pillar = local.workloadIdentityTags[each.value].pillar } : {},
    local.workloadIdentityTags[each.value].system != null ? { system = local.workloadIdentityTags[each.value].system } : {}
  )
}

resource "azurerm_federated_identity_credential" "federated_identity_credential" {
  for_each = {
    for identity in local.workloadIdentityList : "${identity.identity_name}-${identity.federated_subject}" => identity
  }
  name                = "${var.env}-${each.value.identity_name}"
  resource_group_name = azurerm_resource_group.resource_group.name
  audience            = ["api://AzureADTokenExchange"]
  issuer              = azurerm_kubernetes_cluster.aks_v2.oidc_issuer_url
  parent_id           = azurerm_user_assigned_identity.user_assigned_identity[each.value.identity_name].id
  subject             = "system:serviceaccount:${each.value.federated_subject}"
}

data "azuread_application" "terraform" {
  # display_name = "Terraform"
  client_id = "229e9df9-1877-4ca9-b310-bd77ed961bc5"
}

resource "azuread_application_federated_identity_credential" "Terraform_DevAzureAgentPool" {
  application_id = data.azuread_application.terraform.id
  display_name   = "Terraform-UatAzureAgentPool"
  audiences      = ["api://AzureADTokenExchange"]
  issuer         = azurerm_kubernetes_cluster.aks_v2.oidc_issuer_url
  subject        = "system:serviceaccount:azure-agent-pool:azure-agent-pool-v3"
}

resource "azurerm_role_assignment" "UatDevops_PrivateDNSZoneContributor_WonderInternal" {
  for_each = toset([
    "UatDevops-InternalIngressExternalDNS"
  ])
  scope                = azurerm_private_dns_zone.wonderinternal.id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Private DNS Zone Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UatHDR_StorageBlobDataContributor_FTIUatStorage" {
  for_each = toset([
    "UatHDR-HDRPortal",
    "UatHDR-DeliveryZoneSite"
  ])
  scope                = module.storage.storage_accounts["storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UatWonderWorks_StorageBlobDataContributor_FTIUatStorage" {
  for_each = toset([
    "UatWonderWorks-WonderWorksFileService",
    "UatWonderWorks-WonderWorksOfficialBosite",
    "UatWonderWorks-WonderWorksSite"
  ])
  scope                = module.storage.storage_accounts["storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UatPlatformServices_Contributor_FTIUatAcr" {
  for_each = toset([
    "UatPlatformServices-WonderAzureManagementService"
  ])
  scope                = azurerm_container_registry.acr.id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UatHDR_Contributor_FTIUatAcr" {
  for_each = toset([
    "UatHDR-DataRestore"
  ])
  scope                = azurerm_container_registry.acr.id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UATPlatformServices_StorageBlobDataContributor_FTIUatStorage" {
  for_each = toset([
    "UatPlatformServices-ApiDocService",
    "UatPlatformServices-ApiHub",
    "UatPlatformServices-MySQLMonitor",
    "UatPlatformServices-WonderAzureManagementService"
  ])
  scope                = module.storage.storage_accounts["storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UATPlatformServices_StorageBlobDataContributor_FTIUatPantryArchiving" {
  for_each = toset([
    "UatPlatformServices-DataArchivingService",
    "UatPlatformServices-DataLifecycleService"
  ])
  scope                = module.storage.storage_accounts["pantry_archiving"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UATPlatformServices_StorageBlobDataContributor_FTIUatKDSArchiving" {
  for_each = toset([
    "UatPlatformServices-DataArchivingService",
    "UatPlatformServices-DataLifecycleService"
  ])
  scope                = module.storage.storage_accounts["kds_archiving"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UATPlatformServices_StorageBlobDataContributor_FTIUatHDRArchiving" {
  for_each = toset([
    "UatPlatformServices-DataArchivingService",
    "UatPlatformServices-DataLifecycleService"
  ])
  scope                = module.storage.storage_accounts["hdr_archiving"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UATPlatformServices_StorageBlobDataContributor_FTIUatBAArchiving" {
  for_each = toset([
    "UatPlatformServices-DataArchivingService",
    "UatPlatformServices-DataLifecycleService"
  ])
  scope                = module.storage.storage_accounts["ba_archiving"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UATBAConsumer_StorageBlobDataContributor_FTIUatStorage" {
  for_each = toset([
    "UatBAConsumer-BAProductService",
    "UatBAConsumer-BAMerchSite",
    "UatBAConsumer-BAImageService",
    "UatBAConsumer-BAMerchDataRestore"
  ])
  scope                = module.storage.storage_accounts["storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UATConsumer_StorageBlobDataContributor_FTIUatBoStorage" {
  for_each = toset([
    "UatConsumer-WonderPortalSite"
  ])
  scope                = module.storage.storage_accounts["bo_storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UATConsumer_StorageBlobDataContributor_FTIUatStorage" {
  for_each = toset([
    "UatConsumer-ImageService",
    "UatConsumer-MarketingService",
    "UatConsumer-MerchandisingSite",
    "UatConsumer-MarketingSite",
    "UatConsumer-WonderSettingSite",
    "UatConsumer-WonderSpotSite",
    "UatConsumer-WonderWebApi",
    "UatConsumer-CustomerServiceSite",
    "UatConsumer-DecagonIntegrationService",
    "UatConsumer-OrderService",
    "UatConsumer-RestaurantServiceV2"
  ])
  scope                = module.storage.storage_accounts["storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UATConsumer_StorageBlobDataContributor_FTIUatConsumerSettingServiceStorage" {
  for_each = toset([
    "UatConsumer-WonderSettingService"
  ])
  scope                = module.storage.storage_accounts["consumer_setting_service"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UatHDRDelivery_StorageBlobDataContributor_FTIUatBoStorage" {
  for_each = toset([
    "UatHDRDelivery-CourierTaskService",
    "UatHDRDelivery-DeliveryTaskV2Service",
    "UatHDRDelivery-DeliveryAgentService"
  ])
  scope                = module.storage.storage_accounts["bo_storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UatMasterData_StorageBlobDataContributor_FTIUatStorage" {
  for_each = toset([
    "UatMasterData-RecipeSite",
    "UatMasterData-DataRestore",
    "UatMasterData-FileService",
    "UatMasterData-InternalRecipeService",
    "UatMasterData-MongoDump",
    "UatMasterData-MasterDataAgentService"
  ])
  scope                = module.storage.storage_accounts["storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UatMasterData_StorageBlobDataContributor_FTIUatCookBookStorage" {
  for_each = toset([
    "UatMasterData-RecipeSite",
    "UatMasterData-DataRestore",
    "UatMasterData-FileService",
    "UatMasterData-InternalRecipeService",
    "UatMasterData-MasterDataAgentService"
  ])
  scope                = module.storage.storage_accounts["cookbook_storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "UatBAConsumer_StorageBlobDataContributor_FTIUatMainEventhubNamespaceStorage" {
  for_each = toset([
    "UatBAConsumer-FRIntegrationService"
  ])
  scope                = azurerm_storage_account.main_eventhub_namespace_storage.id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

resource "azurerm_role_assignment" "PlatformKV_UatWonderCoreNGExternalSecret_SecretsUser" {
  scope                = data.azurerm_key_vault.platform_kv.id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity["Uat-WonderCoreNGExternalSecret"].principal_id
  role_definition_name = "Key Vault Secrets User"
  description          = var.terraformDesc
}

resource "azurerm_role_assignment" "PlatformKV_UatWonderCoreNGExternalSecret_CertificatesUser" {
  scope                = data.azurerm_key_vault.platform_kv.id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity["Uat-WonderCoreNGExternalSecret"].principal_id
  role_definition_name = "Key Vault Certificate User"
  description          = var.terraformDesc
}

data "azuread_application" "capacity_test_terraform" {
  client_id = "18ebaced-34fd-446b-85da-8bfc4dbe85d1"
}

resource "azuread_application_federated_identity_credential" "Terraform_CapacityTestManagerService" {
  display_name   = "Terraform-CapacityTestManagerService"
  application_id = data.azuread_application.capacity_test_terraform.id
  audiences      = ["api://AzureADTokenExchange"]
  issuer         = azurerm_kubernetes_cluster.aks_v2.oidc_issuer_url
  subject        = "system:serviceaccount:uat-capacity-test:capacity-test-manager-service"
}

resource "azurerm_role_assignment" "PlatformKV_UatArgoCDRepoServer_CryptoUser" {
  scope                = data.azurerm_key_vault.main.id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity["Uat-ArgoCDRepoServer"].principal_id
  role_definition_name = "Key Vault Crypto User"
  description          = var.terraformDesc
}

resource "azurerm_role_assignment" "UatKafkaToBq_StorageBlobDataReader_FTIUatStorage" {
  scope                = module.storage.storage_accounts["storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity["UatKafkaToBq"].principal_id
  role_definition_name = "Storage Blob Data Reader"
  description          = var.terraformDesc
}

resource "azurerm_role_assignment" "UatSouschef_StorageBlobDataContributor_FTIUatStorage" {
  for_each = toset([
    "UatSouschefRecipeConsumer",
    "UatSouschefWorker"
  ])
  scope                = module.storage.storage_accounts["storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

# COPS-5890: productcatalog GraphQL uploads to productcatalog-imports / productcatalog-images
resource "azurerm_role_assignment" "UatSupplyChainProductCatalog_StorageBlobDataContributor_FTIUatProductCatalog" {
  for_each = toset([
    "UatSupplyChainProductCatalog-Api"
  ])
  scope                = module.storage.storage_accounts["productcatalog_storage"].id
  principal_id         = azurerm_user_assigned_identity.user_assigned_identity[each.key].principal_id
  role_definition_name = "Storage Blob Data Contributor"
  description          = var.terraformDesc
  depends_on = [
    azurerm_user_assigned_identity.user_assigned_identity
  ]
}

```

### `infra/uat/app/infra/env/21-aks-rbac.tf`

```
locals {
  clusterBuiltinRoleAssignmentFile   = yamldecode(file("./rbac/aks_rbac_cluster_builtin_role_assignment.yml"))
  namespaceBuiltinRoleAssignmentFile = yamldecode(file("./rbac/aks_rbac_namespace_builtin_role_assignment.yml"))
  namespaceRoleAssignmentFile        = yamldecode(file("./rbac/aks_rbac_namespace_role_assignment.yml"))
}

module "aks-rbac-management" {
  source                      = "../../../../modules/aks-rbac-management"
  kubernetes_cluster_id       = azurerm_kubernetes_cluster.aks_v2.id
  cluster_builtin_role_data   = local.clusterBuiltinRoleAssignmentFile
  namespace_builtin_role_data = local.namespaceBuiltinRoleAssignmentFile
  namespace_role_data         = local.namespaceRoleAssignmentFile
  terraformDesc               = var.terraformDesc
}

```

### `infra/uat/app/infra/env/loadgenerator_vm.tf`

```
data "azurerm_key_vault" "load_generator_platform_kv" {
  name                = "${lower(var.env)}-platform-kv"
  resource_group_name = azurerm_resource_group.resource_group.name
}

resource "tls_private_key" "load_generator" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "azurerm_key_vault_secret" "load_generator_private_key" {
  name         = "${var.env}-load-generator-ssh-private-key"
  key_vault_id = data.azurerm_key_vault.load_generator_platform_kv.id

  # Keep the private key out of Terraform state by using the write-only API.
  value_wo         = tls_private_key.load_generator.private_key_pem
  value_wo_version = 1
}

module "load_generator" {
  source = "../../../../modules/azure-vm-compute"

  vm_name             = "${var.env}LoadGenerator"
  resource_group_name = azurerm_resource_group.resource_group.name
  location            = var.location
  subnet_id           = azurerm_subnet.app.id
  vm_size             = "Standard_F8s_v2"
  admin_username      = "azureuser"
  ssh_public_key      = tls_private_key.load_generator.public_key_openssh

  tags = {
    env        = var.logicalEnv
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "platform"
    purpose    = "load-generator"
  }
}

output "load_generator_private_key_secret_name" {
  description = "Key Vault secret name containing the generated private key for the load generator VM"
  value       = azurerm_key_vault_secret.load_generator_private_key.name
}

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

### `infra/uat/app/infra/env/24-mysql-flexible-ba-consumer-server.tf`

```
resource "random_password" "ba_consumer_db_password" {
  length  = 16
  special = true
}

resource "azurerm_mysql_flexible_server" "ba_consumer" {
  name                = "${lower(var.env)}-flexible-ba-consumer-db"
  resource_group_name = azurerm_resource_group.resource_group.name
  location            = var.location

  administrator_login    = "dbadmin"
  administrator_password = random_password.ba_consumer_db_password.result
  sku_name               = "MO_Standard_E8ds_v4"
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
    system     = "consumer-ba"
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

resource "azurerm_private_endpoint" "ba_consumer_db" {
  name                = "${azurerm_mysql_flexible_server.ba_consumer.name}-pep"
  location            = var.location
  resource_group_name = azurerm_resource_group.resource_group.name
  subnet_id           = azurerm_subnet.app.id

  private_service_connection {
    name                           = "${azurerm_mysql_flexible_server.ba_consumer.name}-psc"
    is_manual_connection           = false
    private_connection_resource_id = azurerm_mysql_flexible_server.ba_consumer.id
    subresource_names              = ["mysqlServer"]
  }

  private_dns_zone_group {
    name                 = "ba-consumer-db-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.mysql_privatelink_server.id]
  }

  tags = {
    env        = var.env
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "growth"
    system     = "consumer-ba"
  }
}

resource "azurerm_mysql_flexible_server_active_directory_administrator" "ba_consumer_aad_admin" {
  server_id   = azurerm_mysql_flexible_server.ba_consumer.id
  identity_id = azurerm_user_assigned_identity.mysql_main_aad_identity.id
  login       = "Terraform"
  object_id   = data.azuread_application.terraform.client_id
  tenant_id   = data.azurerm_client_config.current.tenant_id
}

resource "azurerm_mysql_flexible_server_firewall_rule" "ba_consumer_db_allow_office_access" {
  for_each            = var.mysql_flexible_db_access_allowed_cidr
  name                = each.key
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  start_ip_address    = each.value.start
  end_ip_address      = each.value.end
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_timezone" {
  name                = "time_zone"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "+0:00"
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_wait_timeout" {
  name                = "wait_timeout"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "28800"
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_binlog_row_image" {
  name                = "binlog_row_image"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "FULL"
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_slow_query_log" {
  name                = "slow_query_log"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "ON"
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_long_query_time" {
  name                = "long_query_time"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "10"
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_audit_log_enabled" {
  name                = "audit_log_enabled"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "ON"
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_audit_log_events" {
  name                = "audit_log_events"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "GENERAL"
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_character_set_server" {
  name                = "character_set_server"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "UTF8MB4"
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_log_queries_not_using_indexes" {
  name                = "log_queries_not_using_indexes"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "OFF"
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_sql_mode" {
  name                = "sql_mode"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = ""
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_disable_secure_transport" {
  name                = "require_secure_transport"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "OFF"
}

resource "azurerm_mysql_flexible_server_configuration" "ba_consumer_db_binlog_retain_7_days" {
  name                = "binlog_expire_logs_seconds"
  resource_group_name = azurerm_resource_group.resource_group.name
  server_name         = azurerm_mysql_flexible_server.ba_consumer.name
  value               = "604800"
}

resource "azurerm_management_lock" "mysql_flexible_ba_consumer" {
  name       = "${azurerm_mysql_flexible_server.ba_consumer.name}-delete-lock"
  scope      = azurerm_mysql_flexible_server.ba_consumer.id
  lock_level = "CanNotDelete"
}

resource "azurerm_mysql_flexible_server" "ba_consumer_replica" {
  name                = "${azurerm_mysql_flexible_server.ba_consumer.name}-replica"
  resource_group_name = azurerm_resource_group.resource_group.name
  location            = var.location
  create_mode         = "Replica"
  source_server_id    = azurerm_mysql_flexible_server.ba_consumer.id

  administrator_login          = azurerm_mysql_flexible_server.ba_consumer.administrator_login
  geo_redundant_backup_enabled = azurerm_mysql_flexible_server.ba_consumer.geo_redundant_backup_enabled
  sku_name                     = azurerm_mysql_flexible_server.ba_consumer.sku_name
  version                      = azurerm_mysql_flexible_server.ba_consumer.version
  zone                         = "3"
  backup_retention_days        = azurerm_mysql_flexible_server.ba_consumer.backup_retention_days

  depends_on = [
    azurerm_mysql_flexible_server.ba_consumer
  ]
  tags = {
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "growth"
    system     = "consumer-ba"
  }

  identity {
    type = "UserAssigned"
    identity_ids = [
      azurerm_user_assigned_identity.mysql_main_aad_identity.id
    ]
  }

  lifecycle {
    ignore_changes = [
      storage
    ]
  }
}

resource "azurerm_private_endpoint" "ba_consumer_replica_db" {
  name                = "${azurerm_mysql_flexible_server.ba_consumer_replica.name}-pep"
  location            = var.location
  resource_group_name = azurerm_resource_group.resource_group.name
  subnet_id           = azurerm_subnet.app.id

  private_service_connection {
    name                           = "${azurerm_mysql_flexible_server.ba_consumer_replica.name}-psc"
    is_manual_connection           = false
    private_connection_resource_id = azurerm_mysql_flexible_server.ba_consumer_replica.id
    subresource_names              = ["mysqlServer"]
  }

  private_dns_zone_group {
    name                 = "ba-consumer-db-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.mysql_privatelink_server.id]
  }

  tags = {
    env        = var.env
    department = "devops"
    team       = "devops"
    dept       = "eng"
    pillar     = "growth"
    system     = "consumer-ba"
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
