import { Header, Title, PrimaryNav, Button, Link, Dropdown } from '@trussworks/react-uswds'
import { useOktaAuth } from '@okta/okta-react';
import { useEffect, useState } from 'react';
import { useResource } from "rest-hooks";
import OrganizationResource from "../resources/OrganizationResource";

/**
 * 
 * @returns OrganizationDropDown
 */
const OrganizationDropDown = () => {
    let orgs = useResource( OrganizationResource.list(), {sortBy:undefined}).sort( (a,b) => a.description.localeCompare(b.description) )
    
    return (
        <Dropdown id="input-dropdown" name="input-dropdown">
        {orgs.map( org => <option key={org.name} value={org.name}>{org.description}</option>)}
      </Dropdown>
    );
}

/**
 * 
 * @returns SignInOrUser
 */
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
       {(authState.accessToken?.claims.organization.some( (o:String) => o === "DHPrimeAdmins"))? 
        <OrganizationDropDown></OrganizationDropDown> : "" }
      <br />
      <a href="/" id="logout" onClick={()=>oktaAuth.signOut()} className="usa-link">Logout</a>
    </div> :
    <Button type="button" outline><Link href="/daily">Sign In</Link></Button>
  );
}

export const ReportStreamHeader = () => {

  const { authState } = useOktaAuth();

  let itemsMenu = [
    <Link href="/daily" key="daily" id="daily" data-attribute="hidden" hidden={true} className="usa-nav__link"><span>Daily data</span></Link>,
    <Link href="/documentation/about" id="docs" key="docs" className="usa-nav__link"><span>Documentation</span></Link>
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