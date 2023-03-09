import { OktaAuthOptions, OktaAuth } from "@okta/okta-auth-js";
import { WidgetOptions } from "@okta/okta-signin-widget";

import config from "./config";

const { OKTA_URL, OKTA_CLIENT_ID, APP_ENV } = config;

const oktaAuthConfig: OktaAuthOptions = {
    issuer: `${OKTA_URL}/oauth2/default`,
    clientId: OKTA_CLIENT_ID as string,
    redirectUri: window.location.origin + "/login/callback",
    postLogoutRedirectUri: window.location.origin,
    responseMode: "fragment",
    tokenManager: {
        storage: APP_ENV === "test" ? "memory" : "localStorage",
    },
    scopes: ["openid", "email"],
    services: {
        autoRenew: false,
    },
};

const oktaSignInConfig: WidgetOptions = {
    logo: "/assets/cdc-logo.svg",
    language: "en",
    features: {
        registration: false, // Disable self-service registration flow
        rememberMe: false, // Setting to false will remove the checkbox to save username
        router: true, // Leave this set to true for the API demo
        // TODO: bring up enabling these once we have OktaPreview
        // securityImage: true, // helps prevent spoofing, may require CORS to allow it through.
        // mfaOnlyFlow: true, // government requires mfa
    },
    baseUrl: OKTA_URL as string,
    clientId: OKTA_CLIENT_ID as string,
    redirectUri: `${window.location.origin}/login/callback`,
    authParams: {
        issuer: `${OKTA_URL}/oauth2/default`,
    },
    scopes: ["openid", "email"],
    useClassicEngine: true,
};

const OKTA_AUTH = new OktaAuth(oktaAuthConfig);
OKTA_AUTH.start();

export { oktaAuthConfig, oktaSignInConfig, OKTA_AUTH };
