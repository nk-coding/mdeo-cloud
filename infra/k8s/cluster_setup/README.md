# mdeo Cluster Setup

This OpenTofu configuration installs the **cluster-level operators** that the mdeo deployment depends on. Apply this **once per cluster** before running the mdeo deployment in [`../mdeo`](../mdeo).

## What it installs

| Component | Namespace | Purpose |
|---|---|---|
| **NGINX Gateway Fabric** | `nginx-gateway` | Kubernetes Gateway API controller; creates the `nginx` GatewayClass |
| **CloudNativePG operator** | `cnpg-system` | Registers `postgresql.cnpg.io` CRDs; manages PostgreSQL clusters |

## Prerequisites

- [OpenTofu](https://opentofu.org/docs/intro/install/) ≥ 1.6 **or** [Terraform](https://www.terraform.io/downloads.html) ≥ 1.5
- Access to a running Kubernetes cluster with a valid `kubeconfig`
- The [Gateway API CRDs](https://gateway-api.sigs.k8s.io/) installed on the cluster (NGINX Gateway Fabric requires them):

```bash
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.1/standard-install.yaml
```

## Input Variables

| Variable | Type | Description | Default |
|---|---|---|---|
| `kubeconfig` | `string` | Path to the kubeconfig file | `./kubeconfig.yaml` |

## Usage

### 1. Copy kubeconfig

```bash
cp ~/.kube/config infra/k8s/cluster_setup/kubeconfig.yaml
```

### 2. Initialize

```bash
cd infra/k8s/cluster_setup

# OpenTofu
tofu init

# or Terraform
terraform init
```

### 3. Apply

```bash
tofu apply
# or
terraform apply
```

No `tfvars` file is needed — the only variable is `kubeconfig`, which defaults to `./kubeconfig.yaml`.

After a successful apply, verify that both the GatewayClass and the CNPG operator are ready:

```bash
kubectl get gatewayclass nginx
# Expected: ACCEPTED = True

kubectl get pods -n cnpg-system
# Expected: cnpg-* pod Running
```

You can now proceed to deploy mdeo in [`../mdeo`](../mdeo).

## Cleanup

```bash
tofu destroy
# or
terraform destroy
```

> **Note:** Destroying cluster_setup will remove the NGINX Gateway Fabric and CNPG operator. If the mdeo deployment is still running, its PostgreSQL clusters will lose their operator and the Gateway will stop working. Always destroy `../mdeo` first.
