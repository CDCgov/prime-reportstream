import React from "react";
import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { FeatureName } from "../../utils/FeatureName";
import TemplateEmailChallenge from "../../components/email/Template/TemplateChallenge";

export function AdminEmailPreviewBase() {
    return (
        <GridContainer>
            <Helmet>
                <title>{FeatureName.ADMIN}</title>
            </Helmet>
            <article>
                <TemplateEmailChallenge />
            </article>
            <HipaaNotice />
        </GridContainer>
    );
}

export function AdminEmailPreview() {
    return (
        <AuthElement
            element={<AdminEmailPreviewBase />}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
