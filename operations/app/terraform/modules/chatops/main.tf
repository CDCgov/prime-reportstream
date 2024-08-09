resource "azurerm_container_group" "chatops" {
  #checkov:skip=CKV2_AZURE_28: "Ensure Container Instance is configured with managed identity"
  name                = "${var.resource_prefix}-chatops"
  location            = var.location
  resource_group_name = var.resource_group
  ip_address_type     = "Public"
  os_type             = "Linux"

  container {
    name   = "chatops"
    image  = "${var.container_registry_login_server}/chatops:latest"
    cpu    = "0.5"
    memory = "1.5"

    ports {
      port     = 3000
      protocol = "TCP"
    }

    environment_variables = {
      GITHUB_REPO            = var.chatops_github_repo
      GITHUB_TARGET_BRANCHES = var.chatops_github_target_branches
    }

    secure_environment_variables = {
      SLACK_BOT_TOKEN = var.chatops_slack_bot_token
      SLACK_APP_TOKEN = var.chatops_slack_app_token
      GITHUB_TOKEN    = var.chatops_github_token
    }

    volume {
      name                 = "gh-locks"
      share_name           = "gh-locks"
      storage_account_name = var.storage_account.name
      storage_account_key  = var.storage_account.primary_access_key
      mount_path           = "/usr/app/src/.locks"
      read_only            = false
    }
  }

  image_registry_credential {
    server   = var.container_registry_login_server
    username = var.container_registry_admin_username
    password = var.container_registry_admin_password
  }
  lifecycle {
    ignore_changes = [
      tags
    ]
  }
  depends_on = [
    var.storage_account, null_resource.chatops_image
  ]
}

resource "null_resource" "chatops_image" {
  provisioner "local-exec" {
    command = <<-EOT
        cp ../../../../../.environment/chatops/help.txt ../../../../../operations/slack-boltjs-app/.help
        docker build -t slack_boltjs_app -f ../../../../../operations/slack-boltjs-app/Dockerfile.example ../../../../../operations/slack-boltjs-app --tag ${var.container_registry_login_server}/chatops:latest
        az acr login --name ${var.container_registry_login_server}
        docker push ${var.container_registry_login_server}/chatops:latest
      EOT
  }
}
