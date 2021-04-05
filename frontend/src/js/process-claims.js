let token_end = window.sessionStorage.getItem("jwt");
let claims_end = token_end? JSON.parse(atob(token_end.split('.')[1])): null;

if( token_end && claims_end && moment().isBefore( moment.unix( claims_end.exp ) ) ){
  document.getElementById( "emailUser").innerHTML = claims_end.sub
  document.getElementById( "logout" ).innerHTML = 'Logout'
}
