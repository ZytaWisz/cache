# Kubernetes Deployment Guide — cache-app (Spring Boot + Kafka + Redis)

## Overview

This guide walks you through deploying the **cache-app** Spring Boot application on Kubernetes.

### What gets deployed

| Component   | Image                    | Purpose                              |
|-------------|--------------------------|--------------------------------------|
| `cache-app` | `cache-app:latest`       | Spring Boot REST API                 |
| `kafka`     | `apache/kafka:latest`    | Message broker (KRaft, single node)  |
| `redis`     | `redis:7-alpine`         | Cache backend (Spring Cache)         |

### File structure created

```
k8s/
├── namespace.yaml
├── kafka/
│   ├── kafka-deployment.yaml
│   └── kafka-service.yaml
├── redis/
│   ├── redis-deployment.yaml
│   └── redis-service.yaml
└── app/
    ├── app-configmap.yaml
    ├── app-deployment.yaml
    └── app-service.yaml
```

---

## Prerequisites

- Docker installed and running
- `kubectl` installed and configured to point at your cluster
- A running Kubernetes cluster — options:
  - **Local**: [Docker Desktop](https://docs.docker.com/desktop/kubernetes/) (enable Kubernetes in settings), [minikube](https://minikube.sigs.k8s.io/), or [kind](https://kind.sigs.k8s.io/)
  - **Cloud**: GKE, EKS, AKS

Verify your cluster is reachable:
```bash
kubectl cluster-info
```

---

## Step 1 — Code changes already applied

The following source files were updated to read configuration from environment variables instead of hardcoded `localhost` values. **No further code changes are needed.**

### `application.yaml`
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}       # env var, falls back to localhost
      port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${KAFKA_CONSUMER_GROUP_ID:consumer-cache-group}
```

### `KafkaProducerConfig.java` & `KafkaConsumerConfig.java`
Both now inject `${spring.kafka.bootstrap-servers}` via `@Value` instead of hardcoding `"localhost:9092"`.

> ✅ The app still works locally without any env vars set (defaults kick in).

---

## Step 2 — Build the JAR

```bash
./mvnw clean package -DskipTests
```

This produces `target/cache-0.0.1-SNAPSHOT.jar`.

---

## Step 3 — Build the Docker image

```bash
docker build -t cache-app:latest .
```

### If using minikube
Load the image directly into minikube's Docker daemon so it doesn't need a registry:
```bash
minikube image load cache-app:latest
```

### If using Docker Desktop Kubernetes
The image is already available — no extra step needed.

### If using a remote cluster (GKE / EKS / AKS)
Push the image to a container registry first:
```bash
# Example with Docker Hub
docker tag cache-app:latest <your-dockerhub-username>/cache-app:latest
docker push <your-dockerhub-username>/cache-app:latest
```
Then update `k8s/app/app-deployment.yaml`:
```yaml
image: <your-dockerhub-username>/cache-app:latest
imagePullPolicy: Always
```

---

## Step 4 — Create the namespace

```bash
kubectl apply -f k8s/namespace.yaml
```

Verify:
```bash
kubectl get namespace cache-app
```

---

## Step 5 — Deploy Kafka

```bash
kubectl apply -f k8s/kafka/kafka-deployment.yaml
kubectl apply -f k8s/kafka/kafka-service.yaml
```

Wait for Kafka to be ready:
```bash
kubectl rollout status deployment/kafka -n cache-app
```

Check the pod is running:
```bash
kubectl get pods -n cache-app -l app=kafka
```

---

## Step 6 — Deploy Redis

```bash
kubectl apply -f k8s/redis/redis-deployment.yaml
kubectl apply -f k8s/redis/redis-service.yaml
```

Wait for Redis to be ready:
```bash
kubectl rollout status deployment/redis -n cache-app
```

---

## Step 7 — Deploy the Spring Boot application

Apply the ConfigMap first (it holds the env vars the app reads):
```bash
kubectl apply -f k8s/app/app-configmap.yaml
```

Then deploy the app and its service:
```bash
kubectl apply -f k8s/app/app-deployment.yaml
kubectl apply -f k8s/app/app-service.yaml
```

Wait for the app to be ready:
```bash
kubectl rollout status deployment/cache-app -n cache-app
```

---

## Step 8 — Verify everything is running

```bash
kubectl get all -n cache-app
```

Expected output (all pods should show `Running`):
```
NAME                             READY   STATUS    RESTARTS   AGE
pod/cache-app-xxxxx              1/1     Running   0          1m
pod/kafka-xxxxx                  1/1     Running   0          3m
pod/redis-xxxxx                  1/1     Running   0          2m

NAME                TYPE        CLUSTER-IP     PORT(S)
service/cache-app   NodePort    10.x.x.x       8080:3xxxx/TCP
service/kafka       ClusterIP   10.x.x.x       9092/TCP,29092/TCP
service/redis       ClusterIP   10.x.x.x       6379/TCP
```

---

## Step 9 — Access the application

### Docker Desktop / kind
```bash
kubectl port-forward service/cache-app 8080:8080 -n cache-app
```
Then open: http://localhost:8080

### minikube
```bash
minikube service cache-app -n cache-app
```
minikube will open the URL automatically.

### Cloud (LoadBalancer)
Change `type: NodePort` to `type: LoadBalancer` in `k8s/app/app-service.yaml`, re-apply, then:
```bash
kubectl get service cache-app -n cache-app
# Use the EXTERNAL-IP shown
```

---

## Step 10 — Test the API

### Create a customer
```bash
curl -X POST http://localhost:8080/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "alice@example.com"}'
```
Expected: returns the new customer's `id` (e.g. `1`)

### Get a customer (first call hits DB, subsequent calls hit Redis cache)
```bash
curl http://localhost:8080/api/customer/1
```

### Get all customers
```bash
curl http://localhost:8080/api/customers
```

### Verify Kafka message was produced
Check the app logs — you should see the consumer print the event:
```bash
kubectl logs -l app=cache-app -n cache-app --tail=50
```
Look for: `Received message: CUSTOMER CREATED: ID: 1, name: Alice, email: alice@example.com.`

---

## Troubleshooting

### Pod stuck in `Pending`
```bash
kubectl describe pod <pod-name> -n cache-app
```
Usually means insufficient resources. Check `kubectl describe nodes`.

### App pod keeps restarting (`CrashLoopBackOff`)
```bash
kubectl logs <cache-app-pod-name> -n cache-app
```
Common causes:
- Kafka not ready yet → app retries on startup; wait a minute and it should stabilise
- Wrong image name → check `image:` in `app-deployment.yaml`
- Health probe failing → the app uses `/actuator/health` which requires `spring-boot-starter-actuator`; if not present, remove the probes from `app-deployment.yaml`

### Kafka connection refused
Verify the Kafka pod is running and the service resolves:
```bash
kubectl exec -it <cache-app-pod-name> -n cache-app -- sh
# inside the pod:
nslookup kafka
```

### Check ConfigMap values
```bash
kubectl describe configmap cache-app-config -n cache-app
```

---

## Tear down

Remove everything in the namespace:
```bash
kubectl delete namespace cache-app
```

---

## Quick reference — all commands in order

```bash
# 1. Build
./mvnw clean package -DskipTests
docker build -t cache-app:latest .

# 2. (minikube only) load image
minikube image load cache-app:latest

# 3. Deploy
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/kafka/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/app/

# 4. Watch pods come up
kubectl get pods -n cache-app -w

# 5. Forward port (Docker Desktop / kind)
kubectl port-forward service/cache-app 8080:8080 -n cache-app
```
