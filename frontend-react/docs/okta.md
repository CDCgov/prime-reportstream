# Okta-side configuration

Our frontend is configured to identify as the "Web" application.

## Dev-side configuration

Our use of okta in frontend is configured by the following environment variables whose values can be found in the application listing within Okta:
-VITE_OKTA_CLIENTID
-VITE_OKTA_URL

These variables can be assigned locally for local development (.env.\*.local) or by github actions (using values in secrets storage either in github itself or azure).

We use Okta's [Embedded Sign-In Widget for React](https://developer.okta.com/docs/guides/sign-in-to-spa-embedded-widget/react/main/), which includes other Okta-related libraries for react, to handle okta workflows.
