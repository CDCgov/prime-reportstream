import { useOktaAuth } from "@okta/okta-react";
import { senderClient } from "../webreceiver-utils";
import AuthResource from "./AuthResource";

export default class SenderAuthResource extends AuthResource {
    static useFetchInit = (init: RequestInit): RequestInit => {
        // TODO: is this ok because useFetchInit is static? (may be a bug, investigate)
        // eslint-disable-next-line react-hooks/rules-of-hooks
        const { authState } = useOktaAuth();
        const senderOrganization = senderClient(authState);

        return {
            ...init,
            headers: {
                ...init.headers,
                Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                Organization: senderOrganization,
                'Access-Control-Allow-Origin': '*',
                'authentication-type': 'okta',
            },
        };
    };
}
