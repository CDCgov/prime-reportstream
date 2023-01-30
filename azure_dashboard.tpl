  {
    "lenses": {
      "0": {
        "order": 0,
        "parts": {
          "0": {
            "position": {
              "x": 0,
              "y": 0,
              "colSpan": 2,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [
                {
                  "name": "id",
                  "value": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                },
                {
                  "name": "Version",
                  "value": "1.0"
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/AspNetOverviewPinnedPart",
              "asset": {
                "idInputName": "id",
                "type": "ApplicationInsights"
              },
              "defaultMenuItemId": "overview"
            }
          },
          "1": {
            "position": {
              "x": 2,
              "y": 1,
              "colSpan": 1,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [
                {
                  "name": "ComponentId",
                  "value": {
                    "Name": "pdhprod-appinsights",
                    "SubscriptionId": "7d1e3999-6577-4cd5-b296-f518e5c8e677",
                    "ResourceGroup": "prime-data-hub-prod"
                  }
                },
                {
                  "name": "Version",
                  "value": "1.0"
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/ProactiveDetectionAsyncPart",
              "asset": {
                "idInputName": "ComponentId",
                "type": "ApplicationInsights"
              },
              "defaultMenuItemId": "ProactiveDetection"
            }
          },
          "2": {
            "position": {
              "x": 3,
              "y": 0,
              "colSpan": 1,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [
                {
                  "name": "ComponentId",
                  "value": {
                    "Name": "pdhprod-appinsights",
                    "SubscriptionId": "7d1e3999-6577-4cd5-b296-f518e5c8e677",
                    "ResourceGroup": "prime-data-hub-prod"
                  }
                },
                {
                  "name": "ResourceId",
                  "value": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/QuickPulseButtonSmallPart",
              "asset": {
                "idInputName": "ComponentId",
                "type": "ApplicationInsights"
              }
            }
          },
          "3": {
            "position": {
              "x": 4,
              "y": 0,
              "colSpan": 1,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [
                {
                  "name": "ComponentId",
                  "value": {
                    "Name": "pdhprod-appinsights",
                    "SubscriptionId": "7d1e3999-6577-4cd5-b296-f518e5c8e677",
                    "ResourceGroup": "prime-data-hub-prod"
                  }
                },
                {
                  "name": "TimeContext",
                  "value": {
                    "durationMs": 86400000,
                    "endTime": null,
                    "createdTime": "2018-05-04T01:20:33.345Z",
                    "isInitialTime": true,
                    "grain": 1,
                    "useDashboardTimeRange": false
                  }
                },
                {
                  "name": "Version",
                  "value": "1.0"
                },
                {
                  "name": "componentId",
                  "isOptional": true
                },
                {
                  "name": "id",
                  "isOptional": true
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/WebTestsPinnedPart",
              "asset": {
                "idInputName": "ComponentId",
                "type": "ApplicationInsights"
              }
            }
          },
          "4": {
            "position": {
              "x": 5,
              "y": 0,
              "colSpan": 1,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [
                {
                  "name": "ComponentId",
                  "value": {
                    "Name": "pdhprod-appinsights",
                    "SubscriptionId": "7d1e3999-6577-4cd5-b296-f518e5c8e677",
                    "ResourceGroup": "prime-data-hub-prod"
                  }
                },
                {
                  "name": "TimeContext",
                  "value": {
                    "durationMs": 86400000,
                    "endTime": null,
                    "createdTime": "2018-05-08T18:47:35.237Z",
                    "isInitialTime": true,
                    "grain": 1,
                    "useDashboardTimeRange": false
                  }
                },
                {
                  "name": "ConfigurationId",
                  "value": "78ce933e-e864-4b05-a27b-71fd55a6afad"
                },
                {
                  "name": "MainResourceId",
                  "isOptional": true
                },
                {
                  "name": "ResourceIds",
                  "isOptional": true
                },
                {
                  "name": "DataModel",
                  "isOptional": true
                },
                {
                  "name": "UseCallerTimeContext",
                  "isOptional": true
                },
                {
                  "name": "OverrideSettings",
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "isOptional": true
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/ApplicationMapPart",
              "settings": {},
              "asset": {
                "idInputName": "ComponentId",
                "type": "ApplicationInsights"
              }
            }
          },
          "5": {
            "position": {
              "x": 0,
              "y": 1,
              "colSpan": 3,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [],
              "type": "Extension/HubsExtension/PartType/MarkdownPart",
              "settings": {
                "content": {
                  "settings": {
                    "content": "# Usage",
                    "title": "",
                    "subtitle": ""
                  }
                }
              }
            }
          },
          "6": {
            "position": {
              "x": 3,
              "y": 1,
              "colSpan": 1,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [
                {
                  "name": "ComponentId",
                  "value": {
                    "Name": "pdhprod-appinsights",
                    "SubscriptionId": "7d1e3999-6577-4cd5-b296-f518e5c8e677",
                    "ResourceGroup": "prime-data-hub-prod"
                  }
                },
                {
                  "name": "TimeContext",
                  "value": {
                    "durationMs": 86400000,
                    "endTime": null,
                    "createdTime": "2018-05-04T01:22:35.782Z",
                    "isInitialTime": true,
                    "grain": 1,
                    "useDashboardTimeRange": false
                  }
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/UsageUsersOverviewPart",
              "asset": {
                "idInputName": "ComponentId",
                "type": "ApplicationInsights"
              }
            }
          },
          "7": {
            "position": {
              "x": 4,
              "y": 1,
              "colSpan": 3,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [],
              "type": "Extension/HubsExtension/PartType/MarkdownPart",
              "settings": {
                "content": {
                  "settings": {
                    "content": "# Reliability",
                    "title": "",
                    "subtitle": ""
                  }
                }
              }
            }
          },
          "8": {
            "position": {
              "x": 7,
              "y": 1,
              "colSpan": 1,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [
                {
                  "name": "ResourceId",
                  "value": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                },
                {
                  "name": "DataModel",
                  "value": {
                    "version": "1.0.0",
                    "timeContext": {
                      "durationMs": 86400000,
                      "createdTime": "2018-05-04T23:42:40.072Z",
                      "isInitialTime": false,
                      "grain": 1,
                      "useDashboardTimeRange": false
                    }
                  },
                  "isOptional": true
                },
                {
                  "name": "ConfigurationId",
                  "value": "8a02f7bf-ac0f-40e1-afe9-f0e72cfee77f",
                  "isOptional": true
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/CuratedBladeFailuresPinnedPart",
              "isAdapter": true,
              "asset": {
                "idInputName": "ResourceId",
                "type": "ApplicationInsights"
              },
              "defaultMenuItemId": "failures"
            }
          },
          "9": {
            "position": {
              "x": 8,
              "y": 1,
              "colSpan": 3,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [],
              "type": "Extension/HubsExtension/PartType/MarkdownPart",
              "settings": {
                "content": {
                  "settings": {
                    "content": "# Responsiveness\r\n",
                    "title": "",
                    "subtitle": ""
                  }
                }
              }
            }
          },
          "10": {
            "position": {
              "x": 11,
              "y": 1,
              "colSpan": 1,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [
                {
                  "name": "ResourceId",
                  "value": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                },
                {
                  "name": "DataModel",
                  "value": {
                    "version": "1.0.0",
                    "timeContext": {
                      "durationMs": 86400000,
                      "createdTime": "2018-05-04T23:43:37.804Z",
                      "isInitialTime": false,
                      "grain": 1,
                      "useDashboardTimeRange": false
                    }
                  },
                  "isOptional": true
                },
                {
                  "name": "ConfigurationId",
                  "value": "2a8ede4f-2bee-4b9c-aed9-2db0e8a01865",
                  "isOptional": true
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/CuratedBladePerformancePinnedPart",
              "isAdapter": true,
              "asset": {
                "idInputName": "ResourceId",
                "type": "ApplicationInsights"
              },
              "defaultMenuItemId": "performance"
            }
          },
          "11": {
            "position": {
              "x": 12,
              "y": 1,
              "colSpan": 3,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [],
              "type": "Extension/HubsExtension/PartType/MarkdownPart",
              "settings": {
                "content": {
                  "settings": {
                    "content": "# Browser",
                    "title": "",
                    "subtitle": ""
                  }
                }
              }
            }
          },
          "12": {
            "position": {
              "x": 15,
              "y": 1,
              "colSpan": 1,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [
                {
                  "name": "ComponentId",
                  "value": {
                    "Name": "pdhprod-appinsights",
                    "SubscriptionId": "7d1e3999-6577-4cd5-b296-f518e5c8e677",
                    "ResourceGroup": "prime-data-hub-prod"
                  }
                },
                {
                  "name": "MetricsExplorerJsonDefinitionId",
                  "value": "BrowserPerformanceTimelineMetrics"
                },
                {
                  "name": "TimeContext",
                  "value": {
                    "durationMs": 86400000,
                    "createdTime": "2018-05-08T12:16:27.534Z",
                    "isInitialTime": false,
                    "grain": 1,
                    "useDashboardTimeRange": false
                  }
                },
                {
                  "name": "CurrentFilter",
                  "value": {
                    "eventTypes": [
                      4,
                      1,
                      3,
                      5,
                      2,
                      6,
                      13
                    ],
                    "typeFacets": {},
                    "isPermissive": false
                  }
                },
                {
                  "name": "id",
                  "value": {
                    "Name": "pdhprod-appinsights",
                    "SubscriptionId": "7d1e3999-6577-4cd5-b296-f518e5c8e677",
                    "ResourceGroup": "prime-data-hub-prod"
                  }
                },
                {
                  "name": "Version",
                  "value": "1.0"
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/MetricsExplorerBladePinnedPart",
              "asset": {
                "idInputName": "ComponentId",
                "type": "ApplicationInsights"
              },
              "defaultMenuItemId": "browser"
            }
          },
          "13": {
            "position": {
              "x": 0,
              "y": 2,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "sessions/count",
                          "aggregationType": 5,
                          "namespace": "microsoft.insights/components/kusto",
                          "metricVisualization": {
                            "displayName": "Sessions",
                            "color": "#47BDF5"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "users/count",
                          "aggregationType": 5,
                          "namespace": "microsoft.insights/components/kusto",
                          "metricVisualization": {
                            "displayName": "Users",
                            "color": "#7E58FF"
                          }
                        }
                      ],
                      "title": "Unique sessions and users",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      },
                      "openBladeOnClick": {
                        "openBlade": true,
                        "destinationBlade": {
                          "extensionName": "HubsExtension",
                          "bladeName": "ResourceMenuBlade",
                          "parameters": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights",
                            "menuid": "segmentationUsers"
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "sessions/count",
                          "aggregationType": 5,
                          "namespace": "microsoft.insights/components/kusto",
                          "metricVisualization": {
                            "displayName": "Sessions",
                            "color": "#47BDF5"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "users/count",
                          "aggregationType": 5,
                          "namespace": "microsoft.insights/components/kusto",
                          "metricVisualization": {
                            "displayName": "Users",
                            "color": "#7E58FF"
                          }
                        }
                      ],
                      "title": "Unique sessions and users",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      },
                      "openBladeOnClick": {
                        "openBlade": true,
                        "destinationBlade": {
                          "extensionName": "HubsExtension",
                          "bladeName": "ResourceMenuBlade",
                          "parameters": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights",
                            "menuid": "segmentationUsers"
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          "14": {
            "position": {
              "x": 4,
              "y": 2,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "requests/failed",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Failed requests",
                            "color": "#EC008C"
                          }
                        }
                      ],
                      "title": "Failed requests",
                      "visualization": {
                        "chartType": 3,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      },
                      "openBladeOnClick": {
                        "openBlade": true,
                        "destinationBlade": {
                          "extensionName": "HubsExtension",
                          "bladeName": "ResourceMenuBlade",
                          "parameters": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights",
                            "menuid": "failures"
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "requests/failed",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Failed requests",
                            "color": "#EC008C"
                          }
                        }
                      ],
                      "title": "Failed requests",
                      "visualization": {
                        "chartType": 3,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      },
                      "openBladeOnClick": {
                        "openBlade": true,
                        "destinationBlade": {
                          "extensionName": "HubsExtension",
                          "bladeName": "ResourceMenuBlade",
                          "parameters": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights",
                            "menuid": "failures"
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          "15": {
            "position": {
              "x": 8,
              "y": 2,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "requests/duration",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Server response time",
                            "color": "#00BCF2"
                          }
                        }
                      ],
                      "title": "Server response time",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      },
                      "openBladeOnClick": {
                        "openBlade": true,
                        "destinationBlade": {
                          "extensionName": "HubsExtension",
                          "bladeName": "ResourceMenuBlade",
                          "parameters": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights",
                            "menuid": "performance"
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "requests/duration",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Server response time",
                            "color": "#00BCF2"
                          }
                        }
                      ],
                      "title": "Server response time",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      },
                      "openBladeOnClick": {
                        "openBlade": true,
                        "destinationBlade": {
                          "extensionName": "HubsExtension",
                          "bladeName": "ResourceMenuBlade",
                          "parameters": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights",
                            "menuid": "performance"
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          "16": {
            "position": {
              "x": 12,
              "y": 2,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "browserTimings/networkDuration",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Page load network connect time",
                            "color": "#7E58FF"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "browserTimings/processingDuration",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Client processing time",
                            "color": "#44F1C8"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "browserTimings/sendDuration",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Send request time",
                            "color": "#EB9371"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "browserTimings/receiveDuration",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Receiving response time",
                            "color": "#0672F1"
                          }
                        }
                      ],
                      "title": "Average page load time breakdown",
                      "visualization": {
                        "chartType": 3,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "browserTimings/networkDuration",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Page load network connect time",
                            "color": "#7E58FF"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "browserTimings/processingDuration",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Client processing time",
                            "color": "#44F1C8"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "browserTimings/sendDuration",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Send request time",
                            "color": "#EB9371"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "browserTimings/receiveDuration",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Receiving response time",
                            "color": "#0672F1"
                          }
                        }
                      ],
                      "title": "Average page load time breakdown",
                      "visualization": {
                        "chartType": 3,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      }
                    }
                  }
                }
              }
            }
          },
          "17": {
            "position": {
              "x": 0,
              "y": 5,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "availabilityResults/availabilityPercentage",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Availability",
                            "color": "#47BDF5"
                          }
                        }
                      ],
                      "title": "Average availability",
                      "visualization": {
                        "chartType": 3,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      },
                      "openBladeOnClick": {
                        "openBlade": true,
                        "destinationBlade": {
                          "extensionName": "HubsExtension",
                          "bladeName": "ResourceMenuBlade",
                          "parameters": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights",
                            "menuid": "availability"
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "availabilityResults/availabilityPercentage",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Availability",
                            "color": "#47BDF5"
                          }
                        }
                      ],
                      "title": "Average availability",
                      "visualization": {
                        "chartType": 3,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      },
                      "openBladeOnClick": {
                        "openBlade": true,
                        "destinationBlade": {
                          "extensionName": "HubsExtension",
                          "bladeName": "ResourceMenuBlade",
                          "parameters": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights",
                            "menuid": "availability"
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          "18": {
            "position": {
              "x": 4,
              "y": 5,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "exceptions/server",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Server exceptions",
                            "color": "#47BDF5"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "dependencies/failed",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Dependency failures",
                            "color": "#7E58FF"
                          }
                        }
                      ],
                      "title": "Server exceptions and Dependency failures",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "exceptions/server",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Server exceptions",
                            "color": "#47BDF5"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "dependencies/failed",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Dependency failures",
                            "color": "#7E58FF"
                          }
                        }
                      ],
                      "title": "Server exceptions and Dependency failures",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      }
                    }
                  }
                }
              }
            }
          },
          "19": {
            "position": {
              "x": 8,
              "y": 5,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "performanceCounters/processorCpuPercentage",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Processor time",
                            "color": "#47BDF5"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "performanceCounters/processCpuPercentage",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Process CPU",
                            "color": "#7E58FF"
                          }
                        }
                      ],
                      "title": "Average processor and process CPU utilization",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "performanceCounters/processorCpuPercentage",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Processor time",
                            "color": "#47BDF5"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "performanceCounters/processCpuPercentage",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Process CPU",
                            "color": "#7E58FF"
                          }
                        }
                      ],
                      "title": "Average processor and process CPU utilization",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      }
                    }
                  }
                }
              }
            }
          },
          "20": {
            "position": {
              "x": 12,
              "y": 5,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "exceptions/browser",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Browser exceptions",
                            "color": "#47BDF5"
                          }
                        }
                      ],
                      "title": "Browser exceptions",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "exceptions/browser",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Browser exceptions",
                            "color": "#47BDF5"
                          }
                        }
                      ],
                      "title": "Browser exceptions",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      }
                    }
                  }
                }
              }
            }
          },
          "21": {
            "position": {
              "x": 0,
              "y": 8,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "availabilityResults/count",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Availability test results count",
                            "color": "#47BDF5"
                          }
                        }
                      ],
                      "title": "Availability test results count",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "availabilityResults/count",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Availability test results count",
                            "color": "#47BDF5"
                          }
                        }
                      ],
                      "title": "Availability test results count",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      }
                    }
                  }
                }
              }
            }
          },
          "22": {
            "position": {
              "x": 4,
              "y": 8,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "performanceCounters/processIOBytesPerSecond",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Process IO rate",
                            "color": "#47BDF5"
                          }
                        }
                      ],
                      "title": "Average process I/O rate",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "performanceCounters/processIOBytesPerSecond",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Process IO rate",
                            "color": "#47BDF5"
                          }
                        }
                      ],
                      "title": "Average process I/O rate",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      }
                    }
                  }
                }
              }
            }
          },
          "23": {
            "position": {
              "x": 8,
              "y": 8,
              "colSpan": 4,
              "rowSpan": 3
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "value": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "performanceCounters/memoryAvailableBytes",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Available memory",
                            "color": "#47BDF5"
                          }
                        }
                      ],
                      "title": "Average available memory",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        }
                      }
                    }
                  }
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-prod/providers/microsoft.insights/components/pdhprod-appinsights"
                          },
                          "name": "performanceCounters/memoryAvailableBytes",
                          "aggregationType": 4,
                          "namespace": "microsoft.insights/components",
                          "metricVisualization": {
                            "displayName": "Available memory",
                            "color": "#47BDF5"
                          }
                        }
                      ],
                      "title": "Average available memory",
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideSubtitle": false
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    "metadata": {
      "model": {
        "timeRange": {
          "value": {
            "relative": {
              "duration": 24,
              "timeUnit": 1
            }
          },
          "type": "MsPortalFx.Composition.Configuration.ValueTypes.TimeRange"
        },
        "filterLocale": {
          "value": "en-us"
        },
        "filters": {
          "value": {
            "MsPortalFx_TimeRange": {
              "model": {
                "format": "local",
                "granularity": "auto",
                "relative": "1h"
              },
              "displayCache": {
                "name": "Local Time",
                "value": "Past hour"
              },
              "filteredPartIds": [
                "StartboardPart-ApplicationMapPart-f6980648-05c8-40cb-b454-8988d0cb8608",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb861a",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb861c",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb861e",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb8620",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb8622",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb8624",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb8626",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb8628",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb862a",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb862c",
                "StartboardPart-MonitorChartPart-f6980648-05c8-40cb-b454-8988d0cb862e"
              ]
            }
          }
        }
      }
    }
  }