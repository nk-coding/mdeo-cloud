resource "helm_release" "nginx_gateway_fabric" {
  name             = "nginx-gateway-fabric"
  repository       = "https://helm.nginx.com/stable"
  chart            = "nginx-gateway-fabric"
  namespace        = "nginx-gateway"
  create_namespace = true
  wait             = true

  set {
    name  = "nginxGateway.image.pullPolicy"
    value = "Always"
  }
}

resource "kubernetes_manifest" "gateway_class" {
  depends_on = [helm_release.nginx_gateway_fabric]

  manifest = {
    apiVersion = "gateway.networking.k8s.io/v1"
    kind       = "GatewayClass"
    metadata = {
      name = "nginx"
      labels = {
        "app.kubernetes.io/part-of" = "mdeo"
      }
    }
    spec = {
      controllerName = "gateway.nginx.org/nginx-gateway-controller"
    }
  }
}

resource "kubernetes_manifest" "gateway" {
  depends_on = [kubernetes_manifest.gateway_class]

  manifest = {
    apiVersion = "gateway.networking.k8s.io/v1"
    kind       = "Gateway"
    metadata = {
      name      = "mdeo-gateway"
      namespace = local.ns
      labels = {
        "app.kubernetes.io/name"    = "mdeo-gateway"
        "app.kubernetes.io/part-of" = "mdeo"
      }
    }
    spec = {
      gatewayClassName = "nginx"
      listeners = concat(
        [
          {
            name     = "http"
            port     = 80
            protocol = "HTTP"
            allowedRoutes = {
              namespaces = {
                from = "Same"
              }
            }
          }
        ],
        startswith(var.app_endpoint, "https://") ? [
          {
            name     = "https"
            port     = 443
            protocol = "HTTPS"
            tls = {
              mode = "Terminate"
              certificateRefs = [
                {
                  kind      = "Secret"
                  name      = "mdeo-tls"
                  namespace = local.ns
                }
              ]
            }
            allowedRoutes = {
              namespaces = {
                from = "Same"
              }
            }
          }
        ] : []
      )
    }
  }
}

resource "kubernetes_manifest" "http_route_workbench" {
  depends_on = [kubernetes_manifest.gateway]

  manifest = {
    apiVersion = "gateway.networking.k8s.io/v1"
    kind       = "HTTPRoute"
    metadata = {
      name      = "workbench"
      namespace = local.ns
      labels = {
        "app.kubernetes.io/name"    = "workbench"
        "app.kubernetes.io/part-of" = "mdeo"
      }
    }
    spec = {
      parentRefs = [
        {
          name      = "mdeo-gateway"
          namespace = local.ns
        }
      ]
      rules = [
        {
          matches = [
            {
              path = {
                type  = "PathPrefix"
                value = "/"
              }
            }
          ]
          backendRefs = [
            {
              name = kubernetes_service_v1.js["workbench"].metadata[0].name
              port = 80
            }
          ]
        }
      ]
    }
  }
}

resource "kubernetes_manifest" "http_route_backend" {
  depends_on = [kubernetes_manifest.gateway]

  manifest = {
    apiVersion = "gateway.networking.k8s.io/v1"
    kind       = "HTTPRoute"
    metadata = {
      name      = "backend"
      namespace = local.ns
      labels = {
        "app.kubernetes.io/name"    = "backend-route"
        "app.kubernetes.io/part-of" = "mdeo"
      }
    }
    spec = {
      parentRefs = [
        {
          name      = "mdeo-gateway"
          namespace = local.ns
        }
      ]
      rules = [
        {
          matches = [
            {
              path = {
                type  = "PathPrefix"
                value = "/api"
              }
            }
          ]
          backendRefs = [
            {
              name = kubernetes_service_v1.backend.metadata[0].name
              port = 8080
            }
          ]
        }
      ]
    }
  }
}
