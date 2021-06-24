import { Resource } from '@rest-hooks/rest';
import { useOktaAuth } from '@okta/okta-react';


export default class AuthResource extends Resource {

    pk(parent?: any, key?: string): string | undefined {
        throw new Error('Method not implemented.');
    }
    static useFetchInit = (init: RequestInit) => {
      // eslint-disable-next-line react-hooks/rules-of-hooks
      const { oktaAuth, authState } = useOktaAuth();
      
      return {
      ...init,
        headers: {
          ...init.headers,
          'Authorization': `Bearer ${authState.accessToken?.accessToken}`
        },
      }
    };
  }