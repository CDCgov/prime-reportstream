import { OktaAuthOptions } from "@okta/okta-auth-js";

const oktaAuthConfig: OktaAuthOptions = {
    issuer: `${process.env.REACT_APP_OKTA_URL}/oauth2/default`,
    clientId: process.env.REACT_APP_OKTA_CLIENTID as string,
    redirectUri: window.location.origin + "/login/callback",
    postLogoutRedirectUri: window.location.origin,
    responseMode: "fragment",
    tokenManager: {
        storage: "sessionStorage",
    },
    scopes: ["openid", "email"],
};

const oktaSignInConfig = {
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
    baseUrl: process.env.REACT_APP_OKTA_URL as string,
    clientId: process.env.REACT_APP_OKTA_CLIENTID as string,
    redirectUri: `${window.location.origin}/login/callback`,
    authParams: {
        issuer: `${process.env.REACT_APP_OKTA_URL}/oauth2/default`,
    },
    scopes: ["openid", "email"],
};

export { oktaAuthConfig, oktaSignInConfig };
