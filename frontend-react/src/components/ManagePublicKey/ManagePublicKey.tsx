import React from "react";
import { GridContainer } from "@trussworks/react-uswds";

import { AuthElement } from "../AuthElement";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { SiteAlert, Icon } from "@trussworks/react-uswds";
import { USLink } from "../USLink";

const LightbulbIcon = Icon.Lightbulb;

export function ManagePublicKey() {
    return (
        <GridContainer className="padding-bottom-5 tablet:padding-top-6">
            <h1 className="margin-top-0 margin-bottom-5">Manage Public Key</h1>
            <p className="font-sans-md">
                Send your public key to begin the REST API authentication
                process.
            </p>
            <SiteAlert variant="info" showIcon={false}>
                <LightbulbIcon />
                <span className="padding-left-1">
                    If you need more information on generating your public key,
                    reference page 7 in the{" "}
                    <USLink href="/resources/programmers-guide">
                        API Programmer’s Guide.
                    </USLink>
                </span>
            </SiteAlert>
        </GridContainer>
    );
}

export const ManagePublicKeyWithAuth = () => (
    <AuthElement
        element={withCatchAndSuspense(<ManagePublicKey />)}
        requiredUserType={MemberType.SENDER}
    />
);
