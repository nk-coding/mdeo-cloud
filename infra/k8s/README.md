# mdeo Kubernetes Infrastructure

This directory contains two independent OpenTofu configurations that together deploy the full **mdeo** system on Kubernetes.

```
infra/k8s/
├── cluster_setup/   # Cluster-level operators — apply once per cluster
└── mdeo/            # mdeo application deployment — apply per environment
```

The split keeps **cluster-wide concerns** (operator lifecycle, CRD registration) separate from **application deployment**, so the mdeo deployment can be torn down and recreated without touching the shared operators.

## Configurations

### [`cluster_setup/`](cluster_setup/)

Installs the cluster-level operators that mdeo depends on:

| Component | Namespace | Purpose |
|---|---|---|
| **NGINX Gateway Fabric** | `nginx-gateway` | Kubernetes Gateway API controller; creates the `nginx` GatewayClass |
| **CloudNativePG operator** | `cnpg-system` | Registers `postgresql.cnpg.io` CRDs; manages PostgreSQL clusters |

Apply this **once per cluster** before deploying mdeo. See [`cluster_setup/README.md`](cluster_setup/README.md) for details.

### [`mdeo/`](mdeo/)

Deploys the full mdeo application stack into the `mdeo` namespace:

- Spring Boot backend, 7 Node.js plugin services, nginx workbench
- 3 Java execution services
- 4 CloudNativePG PostgreSQL clusters
- Kubernetes `Gateway` + `HTTPRoute` resources (uses the GatewayClass from `cluster_setup`)

The `gateway_class_name` variable (default `nginx`) can be changed to use any Gateway API-compatible controller. See [`mdeo/README.md`](mdeo/README.md) for the full variable reference and deployment steps.

## Quick Start

### Prerequisites

- [OpenTofu](https://opentofu.org/docs/intro/install/) ≥ 1.6 **or** [Terraform](https://www.terraform.io/downloads.html) ≥ 1.5
- Access to a Kubernetes cluster with a valid kubeconfig
- Gateway API CRDs installed on the cluster:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.1/standard-install.yaml
```

### 1. Apply cluster_setup

```bash
cp ~/.kube/config infra/k8s/cluster_setup/kubeconfig.yaml
cd infra/k8s/cluster_setup
tofu init && tofu apply
```

### 2. Deploy mdeo

```bash
cp ~/.kube/config infra/k8s/mdeo/kubeconfig.yaml
cd infra/k8s/mdeo
tofu init && tofu apply -var-file=terraform.tfvars
```

See [`mdeo/README.md`](mdeo/README.md) for the full `terraform.tfvars` reference.

## Teardown

Always destroy in reverse order:

```bash
# 1. Remove the mdeo application
cd infra/k8s/mdeo && tofu destroy -var-file=terraform.tfvars

# 2. Remove cluster-level operators (only if no other workloads depend on them)
cd infra/k8s/cluster_setup && tofu destroy
```

## Compatibility

Both configurations are fully compatible with the `terraform` CLI (≥ 1.5) and the `tofu` CLI (≥ 1.6). Substitute `terraform` for `tofu` in any command above if preferred.
