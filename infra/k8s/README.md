# mdeo Terraform/OpenTofu Kubernetes Deployment

This directory contains the Terraform/OpenTofu configuration for deploying the full **mdeo** system on Kubernetes.
The configuration is fully compatible with both the `terraform` and `tofu` CLIs.

## Architecture Overview

The deployment includes the following components:

| Category | Services |
|---|---|
| **Frontend** | `workbench` (nginx, port 80) |
| **Backend** | `backend` (Spring Boot, port 8080) |
| **Plugin Services** | `service-metamodel`, `service-model`, `service-script`, `service-model-transformation`, `service-config`, `service-config-optimization`, `service-config-mdeo` (Node.js, port 3000 each) |
| **Execution Services** | `script-execution` (8081), `model-transformation-execution` (8082), `optimizer-execution` (8083) |
| **Databases** | `postgres-backend`, `postgres-script`, `postgres-model-transformation`, `postgres-optimizer` (via CloudNativePG) |
| **Gateway** | NGINX Gateway Fabric (Kubernetes Gateway API) |

## Prerequisites

- [OpenTofu](https://opentofu.org/docs/intro/install/) ≥ 1.6 **or** [Terraform](https://www.terraform.io/downloads.html) ≥ 1.5 installed
- Access to a running Kubernetes cluster (e.g. k3s, kind, EKS, GKE, AKS)
- A valid `kubeconfig` file for accessing the cluster
- The **CloudNativePG** operator will be automatically installed by this configuration — no pre-installation needed
- The **NGINX Gateway Fabric** controller will also be automatically installed

## File Structure

```
infra/k8s/
├── main.tf                 # Providers, namespace, shared locals
├── variables.tf            # All input variables
├── postgres.tf             # CNPG operator + 4x PostgreSQL cluster definitions
├── backend.tf              # Backend Spring Boot deployment + service
├── services.tf             # 8 TS/nginx services using locals + for_each
├── execution-services.tf   # 3 execution services using locals + for_each
├── gateway.tf              # NGINX Gateway Fabric, GatewayClass, Gateway, HTTPRoutes
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

## Deployment Steps

### 1. Copy kubeconfig

Place your cluster's kubeconfig at `infra/k8s/kubeconfig.yaml`, or set the `kubeconfig` variable to its path.

```bash
cp ~/.kube/config infra/k8s/kubeconfig.yaml
```

### 2. Create a `terraform.tfvars` file

```hcl
# infra/k8s/terraform.tfvars

image_owner    = "myorg"            # required – your GitHub org / registry owner
admin_password = "change-me-now"   # required – sensitive

# Optional overrides
app_version  = "v1.2.3"
app_endpoint = "https://mdeo.example.com"
namespace    = "mdeo"
storage_class = "standard"
```

### 3. Initialize

```bash
cd infra/k8s

# OpenTofu
tofu init

# or Terraform
terraform init
```

### 4. Preview the plan

```bash
tofu plan -var-file=terraform.tfvars
# or
terraform plan -var-file=terraform.tfvars
```

### 5. Apply

```bash
tofu apply -var-file=terraform.tfvars
# or
terraform apply -var-file=terraform.tfvars
```

Confirm the prompt to proceed.  
The deployment will:
1. Install the **CloudNativePG** operator in the `cnpg-system` namespace
2. Install the **NGINX Gateway Fabric** controller in the `nginx-gateway` namespace
3. Create the `mdeo` namespace
4. Provision 4 PostgreSQL clusters (one per service group)
5. Deploy all backend, services, and execution services
6. Configure Gateway API routing

> **Note:** The first `apply` may take several minutes while the CNPG operator initialises and the database clusters reach healthy status.

## Accessing the Application

### Local / Development (port-forward)

```bash
# Forward the gateway HTTP listener to localhost:8080
kubectl -n mdeo port-forward service/workbench 8080:80

# Then open:
open http://localhost:8080
```

Alternative – port-forward the Gateway itself (if using NGINX Gateway Fabric's service):

```bash
kubectl -n nginx-gateway port-forward service/nginx-gateway-fabric 8080:80
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

## Gateway API vs Classic Ingress

This configuration uses the **Kubernetes Gateway API** (`gateway.networking.k8s.io`) rather than the classic Ingress resource. Key differences:

| Feature | Classic Ingress | Gateway API |
|---|---|---|
| API stability | Stable but limited | GA since Kubernetes 1.28 |
| Route types | `Ingress` only | `HTTPRoute`, `GRPCRoute`, `TCPRoute`, … |
| Multi-protocol | Limited | First-class support |
| Traffic splitting | Annotation-based (vendor-specific) | Native `backendRefs` weights |
| Cross-namespace routing | Not supported | Supported via `ReferenceGrant` |
| Role separation | Single resource | `GatewayClass` (infra) / `Gateway` (ops) / `Route` (dev) |

The Gateway API provides a cleaner separation of concerns and is the recommended approach for new deployments. NGINX Gateway Fabric is the reference implementation used here.

## Database Passwords

All database passwords are generated automatically by the `random_password` Terraform resource and stored in Kubernetes secrets of type `kubernetes.io/basic-auth`. You can retrieve them with:

```bash
# Example: backend database password
kubectl -n mdeo get secret postgres-backend-credentials \
  -o jsonpath='{.data.password}' | base64 -d
```

Passwords are stored in the Terraform state. Make sure to use a **secure remote backend** (e.g. S3 + encryption, Terraform Cloud) in production.

## Cleanup

```bash
# OpenTofu
tofu destroy -var-file=terraform.tfvars

# or Terraform
terraform destroy -var-file=terraform.tfvars
```

This will remove all resources including the namespace, databases, deployments, and Gateway configuration.  
The CNPG and NGINX Gateway namespaces (`cnpg-system`, `nginx-gateway`) will also be cleaned up.

## Troubleshooting

### CNPG CRD not ready

If `tofu plan` fails with `Error: resource type not found`, the CNPG CRDs may not yet be installed. Run `tofu apply` with a targeted apply first:

```bash
tofu apply -target=helm_release.cnpg_operator -var-file=terraform.tfvars
tofu apply -var-file=terraform.tfvars
```

### Gateway CRDs not ready

Similarly for Gateway API CRDs:

```bash
tofu apply -target=helm_release.nginx_gateway_fabric -var-file=terraform.tfvars
tofu apply -var-file=terraform.tfvars
```

### Check pod status

```bash
kubectl -n mdeo get pods
kubectl -n mdeo describe pod <pod-name>
kubectl -n mdeo logs <pod-name>
```
