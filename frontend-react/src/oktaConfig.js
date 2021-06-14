const oktaAuthConfig = {
    // Note: If your app is configured to use the Implicit flow
    // instead of the Authorization Code with Proof of Code Key Exchange (PKCE)
    // you will need to add `pkce: false`
    issuer: `https://hhs-prime.okta.com/oauth2/default`,
    clientId: '0oa6fm8j4G1xfrthd4h6',
    redirectUri: window.location.origin + '/login/callback',
    responseMode: 'fragment',
    tokenManager: {
        storage: 'sessionStorage'
    }

 
  };
  
  const oktaSignInConfig = {
    // Additional documentation on config options can be found at https://github.com/okta/okta-signin-widget#basic-config-options
    logo: '//logo.clearbit.com/cdc.gov',
    language: 'en',
    features: {
        registration: false, // Enable self-service registration flow
        rememberMe: false, // Setting to false will remove the checkbox to save username
        router: true, // Leave this set to true for the API demo
    },
    baseUrl: `https://hhs-prime.okta.com`,
    clientId: '0oa6fm8j4G1xfrthd4h6',
    redirectUri: window.location.origin + '/login/callback',    
    authParams: {
        issuer: `https://hhs-prime.okta.com/oauth2/default`
    }        
  };
  
  export { oktaAuthConfig, oktaSignInConfig };