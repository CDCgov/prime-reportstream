import React from 'react';
import { Redirect } from 'react-router-dom';
import OktaSignInWidget from '../components/OktaSignInWidget';
import { useOktaAuth } from '@okta/okta-react';

export const Login = ({ config }) => {
  const { oktaAuth, authState } = useOktaAuth();

  const onSuccess = (tokens) => {
    oktaAuth.handleLoginRedirect(tokens);
  };

  const onError = (err) => {
    console.log('error logging in', err);
  };


  return authState && authState.isAuthenticated ?
    <Redirect to={{ pathname: '/' }}/> :
    <OktaSignInWidget
      config={config}
      onSuccess={onSuccess}
      onError={onError}/>;
};
