const env = {
  OKTA_redirect: 'http://localhost:7071/api/download',
  OKTA_clientId:  '0oa6fm8j4G1xfrthd4h6',
  OKTA_baseUrl: 'hhs-prime.okta.com',
}


export function login() {
    
  var jwt = window.sessionStorage.getItem("jwt");

  console.log( `jwt = ${jwt}`)

  if( jwt == null ){
    let config = {
    logo: 'https://logo.clearbit.com/cdc.gov',
    language: 'en',
    features: {
      registration: false, // Enable self-service registration flow
      rememberMe: false, // Setting to false will remove the checkbox to save username
      router: true, // Leave this set to true for the API demo
    },
    el: "#okta-login-container",
    baseUrl: `https://${env.OKTA_baseUrl}`,
    clientId: env.OKTA_clientId,
    redirectUri: env.OKTA_redirect,
    authParams: {
      issuer: `https://${env.OKTA_baseUrl}/oauth2/default`
    }
  };
  
  let signInWidget = new OktaSignIn(config);
  signInWidget
    .showSignInToGetTokens( { scopes: ['openid', 'email', 'profile', 'simple_report'] } )
    .then( tokens => {
      let jwt = tokens.accessToken.value;
      window.sessionStorage.setItem('jwt', jwt) ; 
      console.log( 'reload' );
      window.location.replace( './download.html' )
  });
}
}