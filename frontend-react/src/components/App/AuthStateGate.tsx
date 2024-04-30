import { useOktaAuth } from "@okta/okta-react";
import { PropsWithChildren } from "react";

/**
 *
 * Prevents children from rendering until authState is initialized
 */
const AuthStateGate = ({ children }: PropsWithChildren) => {
    const { authState } = useOktaAuth();

    if (!authState) return null;
    return children;
};

export default AuthStateGate;
