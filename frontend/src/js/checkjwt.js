let token = window.sessionStorage.getItem("jwt");

let claims = token? JSON.parse(atob(token.split('.')[1])): null;

if( !token || !claims || moment().isAfter( moment.unix( claims.exp ) ) )
  window.location.replace( `login.html?return=data-index.html`);
else{
  window.org = claims.organization[0];
  window.user = claims.sub;
  window.jwt = token;
  window.orgName = 'Pima County, AZ'
}