resource "azurerm_data_factory_pipeline" "sftp_share_to_archive" {
  name            = "SFTP-share-to-archive"
  data_factory_id = azurerm_data_factory.primary.id

  annotations         = []
  concurrency         = 1
  folder              = "SFTP-share-to-archive"
  parameters          = {}
  resource_group_name = var.resource_group
  variables = {
    "SFTPShareNames" = ""
  }
  activities_json = jsonencode(
    [
      {
        dependsOn = []
        name      = "Set Share Names"
        type      = "SetVariable"
        typeProperties = {
          value        = var.sftp_shares
          variableName = "SFTPShareNames"
        }
        userProperties = []
      },
      {
        dependsOn = [
          {
            activity = "Set Share Names"
            dependencyConditions = [
              "Succeeded",
            ]
          },
        ]
        name = "ForEach Share"
        type = "ForEach"
        typeProperties = {
          activities = [
            {
              dependsOn = []
              inputs = [
                {
                  parameters = {
                    sharename = {
                      type  = "Expression"
                      value = "@item()"
                    }
                  }
                  referenceName = "SFTPShare"
                  type          = "DatasetReference"
                },
              ]
              name = "Move sftp share to archive"
              outputs = [
                {
                  parameters = {
                    sharename = {
                      type  = "Expression"
                      value = "@item()"
                    }
                  }
                  referenceName = "SFTPArchive"
                  type          = "DatasetReference"
                },
              ]
              policy = {
                retry                  = 0
                retryIntervalInSeconds = 300
                secureInput            = false
                secureOutput           = false
                timeout                = "0.03:00:00"
              }
              type = "Copy"
              typeProperties = {
                enableStaging = false
                logSettings = {
                  copyActivityLogSettings = {
                    enableReliableLogging = false
                    logLevel              = "Info"
                  }
                  enableCopyActivityLog = true
                  logLocationSettings = {
                    linkedServiceName = {
                      referenceName = "SFTPArchive"
                      type          = "LinkedServiceReference"
                    }
                    path = "sftparchive-logs"
                  }
                }
                preserve = [
                  "Attributes",
                ]
                sink = {
                  storeSettings = {
                    copyBehavior = "PreserveHierarchy"
                    type         = "AzureBlobStorageWriteSettings"
                  }
                  type = "BinarySink"
                }
                skipErrorFile = {
                  dataInconsistency = false
                }
                source = {
                  formatSettings = {
                    compressionProperties = null
                    type                  = "BinaryReadSettings"
                  }
                  storeSettings = {
                    deleteFilesAfterCompletion = true
                    recursive                  = true
                    type                       = "AzureFileStorageReadSettings"
                  }
                  type = "BinarySource"
                }
                validateDataConsistency = true
              }
              userProperties = [
                {
                  name  = "Source"
                  value = "/"
                },
                {
                  name  = "Destination"
                  value = "sftparchive/@{item()}/"
                },
              ]
            },
          ]
          items = {
            type  = "Expression"
            value = "@variables('SFTPShareNames')"
          }
        }
        userProperties = []
      },
    ]
  )

  depends_on = [
    azurerm_data_factory_dataset_binary.sftp_share,
    azurerm_data_factory_dataset_binary.sftp_archive
  ]

  timeouts {}
}
