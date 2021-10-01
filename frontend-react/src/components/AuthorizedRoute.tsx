// @ts-nocheck // TODO: fix types in this file
import React from 'react';
import {Redirect} from 'react-router-dom';

import {SecureRoute, useOktaAuth} from '@okta/okta-react';

import {permissionCheck} from '../webreceiver-utils';

export const AuthorizedRoute = ({component: Component, authorize: permission, ...rest}) => {
    const { authState } = useOktaAuth();
    return (<SecureRoute {...rest} render={props => {

        if (authState && !permissionCheck(permission, authState)) {
            // permission not authorized so redirect to home page
            return <Redirect to={{pathname: '/'}}/>
        }

        // authorized so return component
        return <Component {...props} />
    }}/>);
}

