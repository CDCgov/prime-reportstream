import { OktaAuthOptions } from "@okta/okta-auth-js";

const oktaAuthConfig: OktaAuthOptions = {
    issuer: `https://hhs-prime.okta.com/oauth2/default`,
    clientId: process.env.REACT_APP_OKTA_CLIENTID as string,
    redirectUri: window.location.origin + "/login/callback",
    postLogoutRedirectUri: window.location.origin, // tomn - this was window.localStorage.origin... which seemed wrong
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
    },
    baseUrl: `https://hhs-prime.okta.com`,
    clientId: process.env.REACT_APP_OKTA_CLIENTID as string,
    redirectUri: `${window.location.origin}/login/callback`,
    authParams: {
        issuer: "https://hhs-prime.okta.com/oauth2/default",
    },
    scopes: ["openid", "email"],
};

export { oktaAuthConfig, oktaSignInConfig };
