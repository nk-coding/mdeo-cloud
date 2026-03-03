resource "kubernetes_service_v1" "backend" {
  metadata {
    name      = "backend"
    namespace = local.ns
    labels = {
      "app.kubernetes.io/name"    = "backend"
      "app.kubernetes.io/part-of" = "mdeo"
    }
  }

  spec {
    selector = {
      "app.kubernetes.io/name" = "backend"
    }

    port {
      name        = "http"
      port        = 8080
      target_port = 8080
      protocol    = "TCP"
    }
  }
}

resource "kubernetes_deployment_v1" "backend" {
  metadata {
    name      = "backend"
    namespace = local.ns
    labels = {
      "app.kubernetes.io/name"    = "backend"
      "app.kubernetes.io/part-of" = "mdeo"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        "app.kubernetes.io/name" = "backend"
      }
    }

    template {
      metadata {
        labels = {
          "app.kubernetes.io/name"    = "backend"
          "app.kubernetes.io/part-of" = "mdeo"
        }
      }

      spec {
        container {
          name              = "backend"
          image             = "${var.image_registry}/${var.image_owner}/mdeo-backend:${var.app_version}"
          image_pull_policy = "Always"

          port {
            container_port = 8080
            protocol       = "TCP"
          }

          # Database
          env {
            name  = "DATABASE_URL"
            value = "jdbc:postgresql://postgres-backend-rw:5432/mdeo"
          }
          env {
            name  = "DATABASE_USER"
            value = var.database_user
          }
          env {
            name = "DATABASE_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.db_credentials["postgres-backend"].metadata[0].name
                key  = "password"
              }
            }
          }

          # Session settings
          env {
            name  = "SESSION_MAX_IDLE_SECONDS"
            value = tostring(var.session_max_idle_seconds)
          }
          env {
            name  = "SESSION_MAX_ABSOLUTE_SECONDS"
            value = tostring(var.session_max_absolute_seconds)
          }
          env {
            name  = "CSRF_ROTATION_SECONDS"
            value = "300"
          }

          # Cookie / CORS
          env {
            name  = "COOKIE_SECURE"
            value = startswith(var.app_endpoint, "https://") ? "true" : "false"
          }
          env {
            name  = "COOKIE_SAMESITE"
            value = "Lax"
          }
          env {
            name  = "CORS_ALLOWED_HOSTS"
            value = var.app_endpoint
          }

          # Admin credentials
          env {
            name  = "ADMIN_USERNAME"
            value = var.admin_username
          }
          env {
            name  = "ADMIN_PASSWORD"
            value = var.admin_password
          }

          # Plugin URLs
          env {
            name  = "PLUGIN_BASE_URL"
            value = var.app_endpoint
          }
          env {
            name  = "INTERNAL_PLUGIN_BASE_URL"
            value = "http://workbench:80"
          }
          env {
            name  = "DEFAULT_PLUGIN_URLS"
            value = local.plugin_service_urls
          }

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            initial_delay_seconds = 120
            period_seconds        = 10
            failure_threshold     = 20
            timeout_seconds       = 10
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            initial_delay_seconds = 60
            period_seconds        = 10
            failure_threshold     = 10
            timeout_seconds       = 10
          }
        }
      }
    }
  }

  depends_on = [kubernetes_manifest.db_cluster]
}
