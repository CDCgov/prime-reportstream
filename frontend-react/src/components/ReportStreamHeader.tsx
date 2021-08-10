import { Header, Title, PrimaryNav, Button, Link } from '@trussworks/react-uswds'
import { useOktaAuth } from '@okta/okta-react';
//import { UserClaims } from '@okta/okta-auth-js';
//import OktaAuthBrowser from '@okta/okta-auth-js/lib/browser/browser';
import { useEffect, useState } from 'react';


const SignInOrUser = () => {

  const { oktaAuth, authState } = useOktaAuth();

  const [ user, setUser ] = useState('');

  useEffect( () => {
    if( authState && authState.isAuthenticated )
      oktaAuth.getUser().then( cl => setUser( cl.email? cl.email : 'unknown user' ) )
  });

  return (    
    authState && authState.isAuthenticated? 
    <div className="prime-user-account"><span id="emailUser">{ user? user : '' }</span>
      <br />
      <a href="/" id="logout" onClick={()=>oktaAuth.signOut()} className="usa-link">Logout</a>
    </div> :
    <Button type="button" outline><Link href="/daily">Sign In</Link></Button>
  );
}

export const ReportStreamHeader = () => {

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { oktaAuth, authState } = useOktaAuth();

  var itemsMenu = [
    <Link href="/daily" key="daily" data-attribute="hidden" hidden={true} className="usa-nav__link"><span>Daily data</span></Link>,
    <Link href="/documentation/about" key="docs" className="usa-nav__link"><span>Documentation</span></Link>
  ];

  if( !authState || !authState.isAuthenticated )
    itemsMenu = itemsMenu.slice(1);

    return (
        <Header basic={true}>
        <div className="usa-nav-container">
          <div className="usa-navbar">
            <Title><a href="/">ReportStream</a></Title>
          </div>
          <PrimaryNav items={itemsMenu}>
            <SignInOrUser />
          </PrimaryNav>
        </div>
      </Header>        
    );
}