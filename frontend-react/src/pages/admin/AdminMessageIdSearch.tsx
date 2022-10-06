import React from "react";
import { NetworkErrorBoundary } from "rest-hooks";

import HipaaNotice from "../../components/HipaaNotice";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { BasicHelmet } from "../../components/header/BasicHelmet";
import { MessageIdSearch } from "../../components/MessageIdSearch/MessageIdSearch";
import { ErrorPage } from "../error/ErrorPage";

export function AdminMessageIdSearch() {
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <BasicHelmet pageTitle="Message Id Search" />
            <MessageIdSearch />
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}

export function AdminMessageIdSearchWithAuth() {
    return (
        <AuthElement
            element={<AdminMessageIdSearch />}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
