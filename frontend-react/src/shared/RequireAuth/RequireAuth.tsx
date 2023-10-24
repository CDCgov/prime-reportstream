import { Navigate, useLocation } from "react-router";

import { ErrorNoPage } from "../../pages/error/legacy-content/ErrorNoPage";
import { PERMISSIONS } from "../../utils/UsefulTypes";
import { useSessionContext } from "../../contexts/SessionContext";

export interface RequireAuthProps extends React.PropsWithChildren {
    type?: PERMISSIONS | PERMISSIONS[];
}

/**
 * Component wrapper to enforce having a user session and certain configurable
 * organization(s). Will redirect to login page if anonymous, render
 * a 404 if user is denied, or render the protected children.
 */
export function RequireAuth({
    type = [PERMISSIONS.PRIME_ADMIN],
    children,
}: RequireAuthProps) {
    const { authState } = useSessionContext();
    const location = useLocation();
    const typeArr = Array.isArray(type) ? type : [type];

    if (!authState!!.isAuthenticated) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }

    const match = (
        authState!!.idToken?.claims.organization as string[] | undefined
    )?.find((g: string) =>
        typeArr.find((t) => {
            if (t === PERMISSIONS.PRIME_ADMIN) {
                return g === t;
            }

            return g.startsWith(t);
        }),
    );

    if (!match) {
        return <ErrorNoPage />;
    }

    return <>{children}</>;
}
