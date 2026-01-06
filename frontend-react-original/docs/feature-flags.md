# Feature Flags in the ReportStream Frontend

Feature flags are used by the ReportStream frontend to expose certain feature, behaviors or interfaces only to specific users or environments. Feature flags can be set either on a per user basis using the feature flags UI within the application, or on a per environment basis using environment variables.

## Feature Flags UI

Admins can access a UI that will allow them to manage feature flags for their user alone.

## Feature Flags Environment Variable

When deploying or running ReportStream, any feature flags that should be enabled can be set as a comma delimited list using the `VITE_FEATURE_FLAGS` environment variable. Feature flags set via environment variables cannot be turned off on the user level.
