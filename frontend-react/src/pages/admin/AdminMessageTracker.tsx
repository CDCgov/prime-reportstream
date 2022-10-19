import React from "react";

import HipaaNotice from "../../components/HipaaNotice";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { BasicHelmet } from "../../components/header/BasicHelmet";
import { MessageTracker } from "../../components/MessageTracker/MessageTracker";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";

export function AdminMessageTracker() {
    return (
        <>
            <BasicHelmet pageTitle="Message Id Search" />
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
