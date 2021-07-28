import { OktaAuthOptions } from "@okta/okta-auth-js";

const oktaAuthConfig: OktaAuthOptions = {
    issuer: `https://hhs-prime.okta.com/oauth2/default`,
    clientId: '0oa6fm8j4G1xfrthd4h6',
    redirectUri: window.location.origin + '/login/callback',
    responseMode: 'fragment',
    tokenManager: {
        storage: 'sessionStorage'
    },
    scopes: ['openid', 'email']
  };
  
  const oktaSignInConfig = {
    logo: '//logo.clearbit.com/cdc.gov',
    language: 'en',
    features: {
        registration: false, // Disable self-service registration flow
        rememberMe: false, // Setting to false will remove the checkbox to save username
        router: true, // Leave this set to true for the API demo
    },
    baseUrl: `https://hhs-prime.okta.com`,
    clientId: '0oa6fm8j4G1xfrthd4h6',
    redirectUri: `${window.location.origin}/login/callback`,
    authParams: {
        issuer: 'https://hhs-prime.okta.com/oauth2/default'
    },
    scopes: ['openid', 'email'] 
  };
  
  export { oktaAuthConfig, oktaSignInConfig };