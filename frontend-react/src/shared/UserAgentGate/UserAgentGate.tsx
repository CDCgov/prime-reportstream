export interface UserAgentGateProps extends React.PropsWithChildren {
    regex: RegExp;
    userAgent: string;
    failElement: React.ReactElement;
}

/**
 * Serves as a gate to prevent user agents that do not pass regex's test
 * from rendering children.
 */
function UserAgentGate({
    userAgent,
    regex,
    failElement,
    children,
}: UserAgentGateProps) {
    if (!regex.test(userAgent)) {
        return failElement;
    }

    return <>{children}</>;
}

export default UserAgentGate;
