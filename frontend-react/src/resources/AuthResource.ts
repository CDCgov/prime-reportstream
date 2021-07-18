/* eslint-disable react-hooks/rules-of-hooks */
import { Resource } from '@rest-hooks/rest';
import { useOktaAuth } from '@okta/okta-react';


export default class AuthResource extends Resource {

    pk(parent?: any, key?: string): string | undefined {
        throw new Error('Method not implemented.');
    }

    static useFetchInit = (init: RequestInit): RequestInit => {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { oktaAuth, authState } = useOktaAuth();
      return {
          ...init,
          headers: {
              ...init.headers,
              'Authorization': `Bearer ${authState.accessToken?.accessToken}`
          },
      };
    };

    static getBaseUrl = () => {
        if (window.location.origin.includes("localhost"))
            return "http://localhost:7071";
        else if (window.location.origin.includes("staging"))
            return "https://staging.prime.cdc.gov";
        else
            return "https://prime.cdc.gov";
    }
  }