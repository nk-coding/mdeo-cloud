# mdeo Kubernetes Deployment

This OpenTofu configuration deploys the full **mdeo** application stack on Kubernetes.

> **Prerequisite:** The cluster-level operators (NGINX Gateway Fabric + CloudNativePG) must already be installed. Run [`../cluster_setup`](../cluster_setup) first if you have not done so.

## Architecture Overview

| Category | Services |
|---|---|
| **Frontend** | `workbench` (nginx, port 80) |
| **Backend** | `backend` (Spring Boot, port 8080) |
| **Plugin Services** | `service-metamodel`, `service-model`, `service-script`, `service-model-transformation`, `service-config`, `service-config-optimization`, `service-config-mdeo` (Node.js, port 3000 each) |
| **Execution Services** | `script-execution` (8081), `model-transformation-execution` (8082), `optimizer-execution` (8083) |
| **Databases** | `postgres-backend`, `postgres-script`, `postgres-model-transformation`, `postgres-optimizer` (CloudNativePG clusters) |
| **Gateway** | Kubernetes Gateway API (`Gateway` + `HTTPRoute`) using the `nginx` GatewayClass installed by `cluster_setup` |

## File Structure

```
infra/k8s/mdeo/
├── main.tf                 # Providers, namespace, shared locals
├── variables.tf            # All input variables
├── postgres.tf             # 4x PostgreSQL cluster definitions (CNPG CRDs pre-installed by cluster_setup)
├── backend.tf              # Backend Spring Boot deployment + service
├── services.tf             # 8 TS/nginx services using locals + for_each
├── execution-services.tf   # 3 execution services using locals + for_each
├── gateway.tf              # Gateway + HTTPRoutes
└── README.md               # This file
```

## Input Variables

| Variable | Type | Description | Default |
|---|---|---|---|
| `kubeconfig` | `string` | Path to the kubeconfig file | `./kubeconfig.yaml` |
| `namespace` | `string` | Kubernetes namespace for all mdeo resources | `mdeo` |
| `storage_class` | `string` (nullable) | Storage class for PVCs (null = cluster default) | `null` |
| `image_registry` | `string` | Container image registry | `ghcr.io` |
| `image_owner` | `string` | Registry owner/organisation (**required**) | — |
| `app_version` | `string` | Docker image tag to deploy | `latest` |
| `app_endpoint` | `string` | Public base URL (e.g. `http://localhost`) | `http://localhost` |
| `admin_username` | `string` | Backend admin username | `admin` |
| `admin_password` | `string` | Backend admin password (**required**, sensitive) | — |
| `database_user` | `string` | PostgreSQL username for all databases | `mdeo` |
| `execution_timeout_ms` | `number` | Execution timeout in milliseconds | `300000` |
| `session_max_idle_seconds` | `number` | Max session idle time in seconds | `3600` |
| `session_max_absolute_seconds` | `number` | Max absolute session lifetime in seconds | `86400` |
| `max_langium_instances` | `number` | Max Langium worker instances per service | `5` |
| `gateway_class_name` | `string` | GatewayClass name (e.g. `nginx`, `cilium`, `traefik`) | `nginx` |

## Deployment Steps

### 1. Apply cluster_setup first

```bash
cd infra/k8s/cluster_setup && tofu apply && cd -
```

### 2. Copy kubeconfig

```bash
cp ~/.kube/config infra/k8s/mdeo/kubeconfig.yaml
```

### 3. Create a `terraform.tfvars` file

```hcl
# infra/k8s/mdeo/terraform.tfvars

image_owner    = "myorg"            # required – your GitHub org / registry owner
admin_password = "change-me-now"   # required – sensitive

# Optional overrides
app_version        = "v1.2.3"
app_endpoint       = "https://mdeo.example.com"
namespace          = "mdeo"
storage_class      = "standard"
gateway_class_name = "nginx"        # change if using a different controller
```

### 4. Initialize

```bash
cd infra/k8s/mdeo

tofu init
# or: terraform init
```

### 5. Preview the plan

```bash
tofu plan -var-file=terraform.tfvars
# or: terraform plan -var-file=terraform.tfvars
```

### 6. Apply

```bash
tofu apply -var-file=terraform.tfvars
# or: terraform apply -var-file=terraform.tfvars
```

The deployment will:
1. Create the `mdeo` namespace
2. Provision 4 PostgreSQL clusters (one per service group)
3. Deploy all backend, plugin, and execution services
4. Configure a `Gateway` and `HTTPRoute` resources

> **Note:** The first `apply` may take several minutes while the CNPG operator initialises the database clusters.

## Accessing the Application

### Local / Development (port-forward)

```bash
kubectl -n nginx-gateway port-forward service/nginx-gateway-fabric 8080:80
# Then open: http://localhost:8080
```

### Production

Set `app_endpoint = "https://your-domain.com"` in your `tfvars`.  
The Gateway will automatically configure an HTTPS listener.  
You must provide a TLS secret named `mdeo-tls` in the `mdeo` namespace:

```bash
kubectl -n mdeo create secret tls mdeo-tls \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key
```

Or use **cert-manager** to automate certificate provisioning.

## Database Passwords

All database passwords are generated automatically by the `random_password` resource and stored in Kubernetes secrets of type `kubernetes.io/basic-auth`. Retrieve them with:

```bash
kubectl -n mdeo get secret postgres-backend-credentials \
  -o jsonpath='{.data.password}' | base64 -d
```

Passwords are stored in the Terraform state. Use a **secure remote backend** in production.

## Cleanup

```bash
tofu destroy -var-file=terraform.tfvars
# or: terraform destroy -var-file=terraform.tfvars
```

This removes all mdeo resources including the namespace, databases, deployments, `Gateway`, and `HTTPRoute` resources. The cluster-level operators (`nginx-gateway` / `cnpg-system` namespaces) are managed by `../cluster_setup` and are **not** removed here.

## Troubleshooting

### Gateway not routing traffic

Verify the GatewayClass exists and the controller is running:

```bash
kubectl get gatewayclass nginx
kubectl get pods -n nginx-gateway
```

If the GatewayClass is missing, re-apply `../cluster_setup`.

### CNPG Cluster CRD not found at plan time

This usually means `../cluster_setup` has not been applied yet. Run it first and verify the CNPG operator pod is `Running` in `cnpg-system`.

### Check pod status

```bash
kubectl -n mdeo get pods
kubectl -n mdeo describe pod <pod-name>
kubectl -n mdeo logs <pod-name>
```
