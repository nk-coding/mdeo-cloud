variable "kubeconfig" {
  type        = string
  description = "Path to the kubeconfig file used to connect to the cluster"
  default     = "./kubeconfig.yaml"
}

variable "namespace" {
  type        = string
  description = "Kubernetes namespace to deploy all mdeo resources into"
  default     = "mdeo"
}

variable "storage_class" {
  type        = string
  description = "Storage class to use for persistent volumes (null = cluster default)"
  nullable    = true
  default     = null
}

variable "image_registry" {
  type        = string
  description = "Container image registry"
  default     = "ghcr.io"
}

variable "image_owner" {
  type        = string
  description = "Registry owner/organisation (e.g. 'myorg' → images pulled from ghcr.io/myorg/mdeo-<service>)"
  default     = "nk-coding"
}

variable "app_version" {
  type        = string
  description = "Docker image tag to deploy (e.g. 'latest', 'v1.2.3')"
  default     = "latest"
}

variable "app_endpoint" {
  type        = string
  description = "Public base URL of the application (e.g. 'http://localhost' or 'https://mdeo.example.com')"
  default     = "http://localhost"
}

variable "admin_username" {
  type        = string
  description = "Backend admin username"
  default     = "admin"
}

variable "admin_password" {
  type        = string
  description = "Backend admin password"
  sensitive   = true
}

variable "database_user" {
  type        = string
  description = "PostgreSQL username used for all databases"
  default     = "mdeo"
}

variable "execution_timeout_ms" {
  type        = number
  description = "Timeout in milliseconds for script/model-transformation/optimizer execution"
  default     = 300000
}

variable "session_max_idle_seconds" {
  type        = number
  description = "Maximum idle time (in seconds) before a backend session expires"
  default     = 3600
}

variable "session_max_absolute_seconds" {
  type        = number
  description = "Absolute maximum session lifetime in seconds"
  default     = 86400
}

variable "max_langium_instances" {
  type        = number
  description = "Maximum number of Langium worker instances per JS service"
  default     = 5
}
