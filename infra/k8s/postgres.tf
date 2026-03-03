resource "helm_release" "cnpg_operator" {
  name             = "cnpg"
  repository       = "https://cloudnative-pg.github.io/charts"
  chart            = "cloudnative-pg"
  namespace        = "cnpg-system"
  create_namespace = true
  wait             = true
  wait_for_jobs    = true
}

locals {
  databases = {
    "postgres-backend" = {
      cluster_name = "postgres-backend"
      size         = "1Gi"
    }
    "postgres-script" = {
      cluster_name = "postgres-script"
      size         = "1Gi"
    }
    "postgres-model-transformation" = {
      cluster_name = "postgres-model-transformation"
      size         = "1Gi"
    }
    "postgres-optimizer" = {
      cluster_name = "postgres-optimizer"
      size         = "1Gi"
    }
  }
}

resource "random_password" "db_password" {
  for_each = local.databases

  length  = 32
  special = false
}

resource "kubernetes_secret_v1" "db_credentials" {
  for_each = local.databases

  metadata {
    name      = "${each.key}-credentials"
    namespace = local.ns
    labels = {
      "app.kubernetes.io/name"    = each.key
      "app.kubernetes.io/part-of" = "mdeo"
    }
  }

  type = "kubernetes.io/basic-auth"

  data = {
    username = var.database_user
    password = random_password.db_password[each.key].result
  }
}

resource "kubernetes_manifest" "db_cluster" {
  for_each = local.databases

  depends_on = [helm_release.cnpg_operator]

  manifest = {
    apiVersion = "postgresql.cnpg.io/v1"
    kind       = "Cluster"
    metadata = {
      name      = each.value.cluster_name
      namespace = local.ns
      labels = {
        "app.kubernetes.io/name"    = each.key
        "app.kubernetes.io/part-of" = "mdeo"
      }
    }
    spec = {
      instances = 1

      storage = merge(
        { size = each.value.size },
        var.storage_class != null ? { storageClass = var.storage_class } : {}
      )

      bootstrap = {
        initdb = {
          database = "mdeo"
          owner    = var.database_user
          secret = {
            name = kubernetes_secret_v1.db_credentials[each.key].metadata[0].name
          }
        }
      }
    }
  }
}
