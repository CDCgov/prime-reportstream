import { getMembershipsFromToken } from "../hooks/UseOktaMemberships";
import { OKTA_AUTH } from "../okta";

export function getMembershipsFromLocalToken() {
    return getMembershipsFromToken(
        OKTA_AUTH.tokenManager.getTokensSync().accessToken
    );
}
