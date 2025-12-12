import { Navigate } from "react-router";

/**
 * Callback page for logout for misc cleanup.
 */
export default function LogoutCallback() {
    return <Navigate to="/" />;
}
