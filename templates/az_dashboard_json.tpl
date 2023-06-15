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
                  "value": "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                },
                {
                  "name": "Version",
                  "value": "1.0"
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/MonitoredApplicationsTile",
              "asset": {
                "idInputName": "id",
                "type": "ApplicationInsights"
              }
            }
          },
          "1": {
            "position": {
              "x": 2,
              "y": 0,
              "colSpan": 2,
              "rowSpan": 1
            },
            "metadata": {
              "inputs": [
                {
                  "name": "id",
                  "value": "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                },
                {
                  "name": "Version",
                  "value": "1.0"
                }
              ],
              "type": "Extension/AppInsightsExtension/PartType/MonitoredApplicationsTile",
              "asset": {
                "idInputName": "id",
                "type": "ApplicationInsights"
              }
            }
          },
          "2": {
            "position": {
              "x": 0,
              "y": 2,
              "colSpan": 6,
              "rowSpan": 4
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "12caea22-3545-459f-8f05-9618d2003711",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "let usg_events = dynamic([\"*\"]);\nlet mainTable = union pageViews\n    | where timestamp > ago(30d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events);\nlet queryTable = mainTable;\nlet splitTable =  () {\n    queryTable\n    | extend dimension = todynamic(tostring(customDimensions[\"activeMembership\"]))\n    | evaluate bag_unpack(dimension)\n    | extend member_type = iif(isempty(memberType), \"<undefined>\", memberType)\n};\nlet cohortedTable = splitTable\n    //| where 'user_Id' != 'user_AuthenticatedId' or ('user_Id' == 'user_AuthenticatedId' and isnotempty(user_Id))\n    | summarize ['Page Views'] = count() by bin(timestamp, 1d), member_type\n    | project\n    [\"Date/Time\"] = timestamp,\n    member_type,\n    [\"Page Views\"]\n| render columnchart; \ncohortedTable\n",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "FrameControlChart",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "value": "StackedColumn",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Analytics",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${appinsights_name}",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "value": {
                    "xAxis": {
                      "name": "Date/Time",
                      "type": "datetime"
                    },
                    "yAxis": [
                      {
                        "name": "Page Views",
                        "type": "long"
                      }
                    ],
                    "splitBy": [
                      {
                        "name": "member_type",
                        "type": "string"
                      }
                    ],
                    "aggregation": "Sum"
                  },
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "value": {
                    "isEnabled": true,
                    "position": "Bottom"
                  },
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": true,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "Query": "let usg_events = dynamic([\"*\"]);\nlet mainTable = union pageViews\n    | where timestamp > ago(30d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events);\nlet queryTable = mainTable;\nlet splitTable =  () {\n    queryTable\n    | extend dimension = todynamic(tostring(customDimensions[\"activeMembership\"]))\n    | evaluate bag_unpack(dimension)\n    | extend member_type = iif(isempty(memberType), \"<undefined>\", memberType)\n};\nlet cohortedTable = splitTable\n    | where 'user_Id' != 'user_AuthenticatedId' or ('user_Id' == 'user_AuthenticatedId' and isnotempty(user_Id))\n    | summarize ['Page Views'] = count() by bin(timestamp, 1d), member_type\n    | project\n    [\"Date/Time\"] = timestamp,\n    member_type,\n    [\"Page Views\"]\n| render columnchart; \ncohortedTable\n\n",
                  "PartTitle": "Total Page Views by Member Type"
                }
              }
            }
          },
          "3": {
            "position": {
              "x": 6,
              "y": 2,
              "colSpan": 6,
              "rowSpan": 4
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "7db41bbc-9a0a-4311-9130-8493cd13c8fa",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "let usg_events = dynamic([\"*\"]);\nlet mainTable = union pageViews, customEvents, requests\n    | where timestamp > ago(1d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events);\nlet queryTable = mainTable;\nlet splitTable = () {\n    queryTable\n    | extend dimension = todynamic(tostring(customDimensions[\"activeMembership\"]))\n    | evaluate bag_unpack(dimension)\n    | extend memberType = iif(isempty(memberType), \"<undefined>\", memberType)\n};\nlet cohortedTable = splitTable\n    | where 'user_Id' != 'user_AuthenticatedId' or ('user_Id' == 'user_AuthenticatedId' and isnotempty(user_Id))\n    | where memberType != \"<undefined>\"\n    | extend byDimension = bin(timestamp, 1h)\n    | summarize metricHll = hll(session_Id) by byDimension, memberType;\nlet topSegments = cohortedTable\n    | summarize mergedMetricHll = hll_merge(metricHll) by memberType\n    | project memberType, dcount_session_Id = dcount_hll(mergedMetricHll)\n    | top 5 by dcount_session_Id desc nulls last\n    | summarize makelist(memberType);\ncohortedTable\n| extend userId = iff(memberType in (topSegments), memberType, \"Other\")\n| project byDimension, userId, metricHll\n| summarize metric = hll_merge(metricHll) by userId, byDimension\n| order by userId asc, byDimension desc\n| project [\"Date/time\"]=byDimension, [\"Membership Type\"]=userId, [\"Session Count\"] = dcount_hll(metric)\n| render columnchart\n",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "FrameControlChart",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "value": "StackedColumn",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Analytics",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${appinsights_name}",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "value": {
                    "xAxis": {
                      "name": "Date/time",
                      "type": "datetime"
                    },
                    "yAxis": [
                      {
                        "name": "Session Count",
                        "type": "long"
                      }
                    ],
                    "splitBy": [
                      {
                        "name": "Membership Type",
                        "type": "string"
                      }
                    ],
                    "aggregation": "Sum"
                  },
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "value": {
                    "isEnabled": true,
                    "position": "Bottom"
                  },
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": true,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "Query": "let usg_events = dynamic([\"*\"]);\nlet mainTable = union pageViews, customEvents\n    | where timestamp > ago(30d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events);\nlet queryTable = mainTable;\nlet splitTable = () {\n    queryTable\n    | extend dimension = todynamic(tostring(customDimensions[\"activeMembership\"]))\n    | evaluate bag_unpack(dimension)\n    | extend memberType = iif(isempty(memberType), \"<undefined>\", memberType)\n};\nlet cohortedTable = splitTable\n    | where 'user_Id' != 'user_AuthenticatedId' or ('user_Id' == 'user_AuthenticatedId' and isnotempty(user_Id))\n    | where memberType != \"<undefined>\"\n    | extend byDimension = bin(timestamp, 1d)\n    | summarize metricHll = hll(session_Id) by byDimension, memberType;\nlet topSegments = cohortedTable\n    | summarize mergedMetricHll = hll_merge(metricHll) by memberType\n    | project memberType, dcount_session_Id = dcount_hll(mergedMetricHll)\n    | top 5 by dcount_session_Id desc nulls last\n    | summarize makelist(memberType);\ncohortedTable\n| extend userId = iff(memberType in (topSegments), memberType, \"Other\")\n| project byDimension, userId, metricHll\n| summarize metric = hll_merge(metricHll) by userId, byDimension\n| order by userId asc, byDimension desc\n| project [\"Date/Time\"]=byDimension, [\"Membership Type\"]=userId, [\"Session Count\"] = dcount_hll(metric)\n| render columnchart\n\n",
                  "PartTitle": "Session Count by Member Type",
                  "Dimensions": {
                    "xAxis": {
                      "name": "Date/Time",
                      "type": "datetime"
                    },
                    "yAxis": [
                      {
                        "name": "Session Count",
                        "type": "long"
                      }
                    ],
                    "splitBy": [
                      {
                        "name": "Membership Type",
                        "type": "string"
                      }
                    ],
                    "aggregation": "Sum"
                  }
                }
              }
            }
          },
          "4": {
            "position": {
              "x": 12,
              "y": 2,
              "colSpan": 6,
              "rowSpan": 4
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "9ef4b7a4-9076-4f6a-8644-13efc1293899",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "let usg_events = dynamic([\"*\"]);let grain = iff(true, 1d, 1h);  let mainTable = union pageViews     | where timestamp > ago(90d) | where isempty(operation_SyntheticSource) | extend name =replace(\"\\n\", \"\",name) | extend name =replace(\"\\r\", \"\",name) | where '*' in (usg_events) or name in (usg_events) | where customDimensions[\"activeMembership\"] !contains \"prime-admin\" ; let resultTable = mainTable; resultTable     | make-series Users = dcountif(user_Id, 'user_Id' != 'user_AuthenticatedId' or ('user_Id' =='user_AuthenticatedId' and isnotempty(user_Id))) default = 0 on timestamp from ago(90d) to now() step grain\r| render barchart\n",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "FrameControlChart",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "value": "StackedColumn",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Analytics",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${appinsights_name}",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "value": {
                    "xAxis": {
                      "name": "timestamp",
                      "type": "datetime"
                    },
                    "yAxis": [
                      {
                        "name": "Users",
                        "type": "real"
                      }
                    ],
                    "splitBy": [],
                    "aggregation": "Sum"
                  },
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "value": {
                    "isEnabled": true,
                    "position": "Bottom"
                  },
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": true,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "Query": "let usg_events = dynamic([\"*\"]);let grain = iff(true, 1d, 1h);  let mainTable = union pageViews     | where timestamp > ago(30d) | where isempty(operation_SyntheticSource) | extend name =replace(\"\\n\", \"\",name) | extend name =replace(\"\\r\", \"\",name) | where '*' in (usg_events) or name in (usg_events) | where customDimensions[\"activeMembership\"] !contains \"prime-admin\" ; let resultTable = mainTable; resultTable     | make-series [\"Page Views\"] = dcountif(user_Id, 'user_Id' != 'user_AuthenticatedId' or ('user_Id' =='user_AuthenticatedId' and isnotempty(user_Id))) default = 0 on timestamp from ago(30d) to now() step grain\n\n",
                  "PartTitle": "Page Views Excluding 'prime-admin'",
                  "Dimensions": {
                    "xAxis": {
                      "name": "timestamp",
                      "type": "datetime"
                    },
                    "yAxis": [
                      {
                        "name": "Page Views",
                        "type": "real"
                      }
                    ],
                    "splitBy": [],
                    "aggregation": "Sum"
                  }
                }
              }
            }
          },
          "5": {
            "position": {
              "x": 18,
              "y": 2,
              "colSpan": 6,
              "rowSpan": 4
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "4c473b58-a778-417c-a144-8ba938108f7b",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "let usg_events = dynamic([\"Daily Data | Table Pagination\", \"Submissions | Table Pagination\"]);\nlet mainTable = union customEvents\n    | where timestamp > ago(90d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events)\n    | where true;\nlet byTable = mainTable;\nlet queryTable = () {\n    byTable\n    | extend dimension = customDimensions[\"tablePagination\"]\n    | extend dimension = iif(isempty(dimension), \"<undefined>\", dimension)\n};\nlet byCohortTable = queryTable\n    | project name, dimension;\nlet topSegments = byCohortTable\n    | summarize Events = count() by name, dimension\n    | summarize make_list(dimension,1000);    //Returns a dynamic JSON array of all the values of Expr in the group. Needed here.\nlet topEventMetrics = byCohortTable\n    | where dimension in (topSegments);\nlet otherEventUsers = byCohortTable\n    | where dimension !in (topSegments)\n    | extend dimension = \"Other\";\notherEventUsers\n| union topEventMetrics\n| summarize Events = count() by name, dimension\n| order by dimension asc\n| render columnchart\nwith (\n    xtitle=\"Table\",\n    ytitle=\"Counts\",\n    title=\"Pagination Usage by Table\")\n\n",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "FrameControlChart",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "value": "StackedColumn",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Pagination Usage by Table",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${appinsights_name}",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "value": {
                    "xAxis": {
                      "name": "name",
                      "type": "string"
                    },
                    "yAxis": [
                      {
                        "name": "Events",
                        "type": "long"
                      }
                    ],
                    "splitBy": [
                      {
                        "name": "dimension",
                        "type": "string"
                      }
                    ],
                    "aggregation": "Sum"
                  },
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "value": {
                    "isEnabled": true,
                    "position": "Bottom"
                  },
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": true,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "Query": "let usg_events = dynamic([\"Daily Data | Table Pagination\", \"Submissions | Table Pagination\"]);\nlet mainTable = union customEvents\n    | where timestamp > ago(30d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events)\n    | where true;\nlet byTable = mainTable;\nlet queryTable = () {\n    byTable\n    | extend dimension = customDimensions[\"tablePagination\"]\n    | extend dimension = iif(isempty(dimension), \"<undefined>\", dimension)\n};\nlet byCohortTable = queryTable\n    | project name, dimension;\nlet topSegments = byCohortTable\n    | summarize Events = count() by name, dimension\n    | summarize make_list(dimension,1000);    //Returns a dynamic JSON array of all the values of Expr in the group. Needed here.\nlet topEventMetrics = byCohortTable\n    | where dimension in (topSegments);\nlet otherEventUsers = byCohortTable\n    | where dimension !in (topSegments)\n    | extend dimension = \"Other\";\notherEventUsers\n| union topEventMetrics\n| summarize Events = count() by name, dimension\n| order by dimension asc\n| render columnchart\nwith (\n    xtitle=\"Table\",\n    ytitle=\"Counts\",\n    title=\"Pagination Usage by Table\")\n\n",
                  "SpecificChart": "UnstackedColumn"
                }
              }
            }
          },
          "6": {
            "position": {
              "x": 0,
              "y": 6,
              "colSpan": 9,
              "rowSpan": 5
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "7f10e6c8-aa3b-48fd-9534-fe194127357b",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "let usg_events = dynamic([\"*\"]);\nlet mainTable = union customEvents\n    | where timestamp > ago(90d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events)\n    | where isnotempty(customDimensions[\"tableFilter\"])\n    | extend ActiveMembership = todynamic(tostring(customDimensions[\"activeMembership\"]))\n    | evaluate bag_unpack(ActiveMembership)\n    | extend TableFilter = todynamic(tostring(customDimensions[\"tableFilter\"]))\n    | evaluate bag_unpack(TableFilter);\nlet queryTable = mainTable;\nqueryTable\n| extend TimeDiff = datetime_diff('day', todatetime(endRange), todatetime(startRange))\n| order by timestamp desc \n| project\n    [\"No. Days in Range\"] = TimeDiff,\n    [\"Start Date\"] = todatetime(startRange),\n    [\"Table Filter Name\"] = name,\n    [\"Organization Name\"] = parsedName,\n    [\"Date filter was Used\"] = todatetime(timestamp);\nqueryTable\n\n",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "AnalyticsGrid",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Analytics",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${appinsights_name}",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": true,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "Query": "let usg_events = dynamic([\"*\"]);\nlet mainTable = union customEvents\n    | where timestamp > ago(30d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events)\n    | where isnotempty(customDimensions[\"tableFilter\"])\n    | extend ActiveMembership = todynamic(tostring(customDimensions[\"activeMembership\"]))\n    | evaluate bag_unpack(ActiveMembership)\n    | extend TableFilter = todynamic(tostring(customDimensions[\"tableFilter\"]))\n    | evaluate bag_unpack(TableFilter);\nlet queryTable = mainTable;\nqueryTable\n| extend TimeDiff = datetime_diff('day', todatetime(endRange), todatetime(startRange))\n| order by timestamp desc \n| project\n    [\"No. Days in Range\"] = TimeDiff,\n    [\"Start Date\"] = todatetime(startRange),\n    [\"Table Filter Name\"] = name,\n    [\"Organization Name\"] = parsedName,\n    [\"Date filter was Used\"] = todatetime(timestamp),\n    memberType;\nqueryTable\n\n",
                  "PartTitle": "Table Filter Event Usage"
                }
              }
            }
          },
          "7": {
            "position": {
              "x": 9,
              "y": 6,
              "colSpan": 9,
              "rowSpan": 5
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "1a5f54c6-441c-4164-b54b-2ea880afdcee",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "value": "P1D",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "// select count from page where member type ='prime-admin'\nlet usg_events = dynamic([\"*\"]);\nlet mainTable = union pageViews\n    | where timestamp > ago(90d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events);\nlet queryTable = mainTable;\n    queryTable\n    | extend dimension = todynamic(tostring(customDimensions[\"activeMembership\"]))\n    | evaluate bag_unpack(dimension)\n    | extend member_type = iif(isempty(memberType), \"<undefined>\", memberType)\n    | summarize PageCount = count() by member_type, operation_Name\n| project [\"Page Viewed\"] = operation_Name, p = pack(strcat(member_type, \" visit count\"), PageCount)\n| summarize b = make_bag(p) by [\"Page Viewed\"]\n| evaluate bag_unpack(b)\n\n",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "AnalyticsGrid",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Analytics",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${appinsights_name}",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": false,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "Query": "let usg_events = dynamic([\"*\"]);\nlet mainTable = union pageViews\n    | where timestamp > ago(30d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events);\nlet queryTable = mainTable;\n    queryTable\n    | extend dimension = todynamic(tostring(customDimensions[\"activeMembership\"]))\n    | evaluate bag_unpack(dimension)\n    | extend member_type = iif(isempty(memberType), \"unauthenticated\", memberType)\n    | summarize PageCount = count() by member_type, operation_Name\n| project [\"Page Viewed\"] = operation_Name, p = pack(strcat(member_type, \" visit count\"), PageCount)\n| summarize b = make_bag(p) by [\"Page Viewed\"]\n| order by ['Page Viewed'] asc \n| evaluate bag_unpack(b)\n\n",
                  "PartTitle": "Page Views by Member Type",
                  "IsQueryContainTimeRange": true
                }
              }
            }
          },
          "8": {
            "position": {
              "x": 0,
              "y": 11,
              "colSpan": 9,
              "rowSpan": 5
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "526d8e53-6d5e-4c42-81af-9ef1c9ccfba1",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "let usg_events = dynamic([\"Daily Data | Table Pagination\", \"Submissions | Table Pagination\"]);\nlet mainTable = union customEvents\n    | where timestamp > ago(2d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events)\n    | where true;\nlet byTable = mainTable;\nlet queryTable = () {\n    byTable\n    | extend dimActiveMembership = todynamic(tostring(customDimensions[\"activeMembership\"]))\n    | evaluate bag_unpack(dimActiveMembership)\n    | extend member_type = iif(isempty(memberType), \"unauthenticated\", memberType)\n    | extend dimTablePagination = todynamic(tostring(customDimensions[\"tablePagination\"]))\n    | evaluate bag_unpack(dimTablePagination)\n};\nlet byCohortTable = queryTable\n    | project name, member_type, pageSize, pageNumber;\nlet topSegments = byCohortTable\n    | summarize PageCount = count() by name, member_type, pageSize, pageNumber\n    | project\n    [\"Event Name\"] = name,\n    [\"Page Size\"] = pageSize,\n    [\"Page Number\"] = pageNumber,\n    p = pack(strcat(member_type, \" Table Page Count\"), PageCount)\n| summarize b = make_bag(p) by [\"Event Name\"], [\"Page Size\"], [\"Page Number\"]\n| evaluate bag_unpack(b)\n| order by [\"Event Name\"] asc, [\"Page Size\"] asc, [\"Page Number\"] asc;\ntopSegments",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "AnalyticsGrid",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Analytics",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${appinsights_name}",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": true,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "Query": "let usg_events = dynamic([\"Daily Data | Table Pagination\", \"Submissions | Table Pagination\"]);\nlet mainTable = union customEvents\n    | where timestamp > ago(30d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events)\n    | where true;\nlet byTable = mainTable;\nlet queryTable = () {\n    byTable\n    | extend dimActiveMembership = todynamic(tostring(customDimensions[\"activeMembership\"]))\n    | evaluate bag_unpack(dimActiveMembership)\n    | extend member_type = iif(isempty(memberType), \"unauthenticated\", memberType)\n    | extend dimTablePagination = todynamic(tostring(customDimensions[\"tablePagination\"]))\n    | evaluate bag_unpack(dimTablePagination)\n};\nlet byCohortTable = queryTable\n    | project name, member_type, pageSize, pageNumber;\nlet topSegments = byCohortTable\n    | summarize PageCount = count() by name, member_type, pageSize, pageNumber\n    | project\n    [\"Event Name\"] = name,\n    [\"Page Size\"] = pageSize,\n    [\"Page Number\"] = pageNumber,\n    p = pack(strcat(member_type, \" Table Page Count\"), PageCount)\n| summarize b = make_bag(p) by [\"Event Name\"], [\"Page Size\"], [\"Page Number\"]\n| evaluate bag_unpack(b)\n| order by [\"Event Name\"] asc, [\"Page Size\"] asc, [\"Page Number\"] asc;\ntopSegments\n",
                  "PartTitle": "Table Pagination Counts by Member Type"
                }
              }
            }
          },
          "9": {
            "position": {
              "x": 9,
              "y": 11,
              "colSpan": 9,
              "rowSpan": 5
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "1071a5f0-114e-4c2f-bb11-5362584215c3",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "let usg_events = dynamic([\"File Validator\"]);\nlet mainTable = union customEvents\n    | where timestamp > ago(30d)\n    | where isempty(operation_SyntheticSource)\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events)\n    | extend fileValidatorProps = tostring(customDimensions[\"fileValidator\"]);\nlet queryTable = mainTable\n    | where 'user_Id' != 'user_AuthenticatedId' or ('user_Id' == 'user_AuthenticatedId' and isnotempty(user_Id))\n    // | summarize ['Count'] = count() by bin(timestamp, 1d), session_Id, fileValidatorProps\n    | extend dimension = todynamic(fileValidatorProps)\n    | evaluate bag_unpack(dimension)\n    | extend ['Pass/Fail'] = iif(errorCount == 0, \"Pass\", \"Fail\")\n    | order by timestamp desc \n    | project\n        [\"Date\"] = format_datetime(timestamp, 'MM/dd/yyyy'),\n        [\"Session Id\"] = session_Id,\n        // [\"Count\"],\n        ['Pass/Fail'],\n        [\"Error Count\"] = errorCount,\n        [\"Warning Count\"] = warningCount,\n         [\"Schema\"] = schema,\n        [\"File Type\"] = fileType; \nqueryTable\n\n",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "AnalyticsGrid",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Analytics",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${appinsights_name}",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": true,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "PartTitle": "File Validator Statistics"
                }
              }
            }
          },
          "10": {
            "position": {
              "x": 0,
              "y": 16,
              "colSpan": 6,
              "rowSpan": 4
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "5fc74968-4b70-41a1-862f-9726e124425e",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "let usg_events = dynamic([\"*\"]);\nlet mainTable = union customEvents\n    | where timestamp > ago(200h)\n    | where isempty(operation_SyntheticSource) //filtering out HTTP requests made by bots\n    | extend name =replace(\"\\n\", \"\", name)\n    | extend name =replace(\"\\r\", \"\", name)\n    | where '*' in (usg_events) or name in (usg_events);\nlet queryTable = mainTable;\nlet splitTable =  () {\n    queryTable\n    | extend timeInSeconds = todynamic(tostring(customDimensions[\"sessionLength\"]))\n    | evaluate bag_unpack(timeInSeconds)\n    | order by timestamp desc\n};\nsplitTable\n",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "AnalyticsGrid",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Analytics",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${appinsights_name}",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": true,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "PartTitle": "Session Duration with Membership Metadata"
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
                "format": "utc",
                "granularity": "auto",
                "relative": "30d"
              },
              "displayCache": {
                "name": "UTC Time",
                "value": "Past 30 days"
              },
              "filteredPartIds": [
                "StartboardPart-LogsDashboardPart-474730cc-d9f7-4265-b616-7c5e5575ec43",
                "StartboardPart-LogsDashboardPart-5b8c5fda-eccc-4eaa-8781-fb435376f4c9",
                "StartboardPart-LogsDashboardPart-0a2227e3-fa86-46b0-960a-b2c6c531d90c",
                "StartboardPart-LogsDashboardPart-c52af3ae-3577-450d-a4b4-bdb19c269e85",
                "StartboardPart-LogsDashboardPart-c52af3ae-3577-450d-a4b4-bdb19c269fe7",
                "StartboardPart-LogsDashboardPart-4f1ca6dd-eee2-4ea3-8c92-97fa8b6702a5",
                "StartboardPart-LogsDashboardPart-0e6386ee-24a5-46d1-96b6-80a0ae9fd14f",
                "StartboardPart-LogsDashboardPart-839425fe-018b-4550-a18e-a9b83e5713c5",
                "StartboardPart-LogsDashboardPart-839425fe-018b-4550-a18e-a9b83e5712d7"
              ]
            }
          }
        }
      }
    }
  }