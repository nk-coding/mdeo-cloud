terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.27"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "kubernetes" {
  config_path = var.kubeconfig
}

provider "helm" {
  kubernetes {
    config_path = var.kubeconfig
  }
}

resource "kubernetes_namespace_v1" "mdeo" {
  metadata {
    name = var.namespace
    labels = {
      "app.kubernetes.io/managed-by" = "terraform"
      "app.kubernetes.io/part-of"    = "mdeo"
    }
  }
}

locals {
  ns = kubernetes_namespace_v1.mdeo.metadata[0].name

  # Internal cluster base URLs for all plugin services
  plugin_service_urls = join(",", [
    "http://service-metamodel:3000",
    "http://service-model:3000",
    "http://service-script:3000",
    "http://service-model-transformation:3000",
    "http://service-config:3000",
    "http://service-config-optimization:3000",
    "http://service-config-mdeo:3000",
  ])
}
