import React from "react";
import { NetworkErrorBoundary } from "rest-hooks";

import HipaaNotice from "../../components/HipaaNotice";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { BasicHelmet } from "../../components/header/BasicHelmet";
import { MessageTracker } from "../../components/MessageTracker/MessageTracker";
import { ErrorPage } from "../error/ErrorPage";

export function AdminMessageTracker() {
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <BasicHelmet pageTitle="Message Id Search" />
            <MessageTracker />
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}

export function AdminMessageTrackerWithAuth() {
    return (
        <AuthElement
            element={<AdminMessageTracker />}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
