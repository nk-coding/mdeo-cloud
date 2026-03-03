variable "kubeconfig" {
  type        = string
  description = "Path to the kubeconfig file used to connect to the cluster"
  default     = "./kubeconfig.yaml"
}
