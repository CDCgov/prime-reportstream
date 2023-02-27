import React from "react";
import { Helmet } from "react-helmet-async";

import HipaaNotice from "../../components/HipaaNotice";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { MessageTracker } from "../../components/MessageTracker/MessageTracker";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";

export function AdminMessageTracker() {
    return (
        <>
            <Helmet>
                <title>Message Id Search</title>
            </Helmet>
            <MessageTracker />
            <HipaaNotice />
        </>
    );
}

export function AdminMessageTrackerWithAuth() {
    return (
        <AuthElement
            element={withCatchAndSuspense(<AdminMessageTracker />)}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
