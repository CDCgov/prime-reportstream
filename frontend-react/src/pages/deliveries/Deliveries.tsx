import React from "react";
import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import { FeatureName } from "../../utils/FeatureName";

import DeliveriesTable from "./Table/DeliveriesTable";

function DeliveriesPage() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails || {};
    return (
        <GridContainer>
            <Helmet>
                <Helmet>
                    <title>Daily Data - ReportStream</title>
                    <meta
                        name="description"
                        content="Daily Data shows what data a public health entity has received with the option to download."
                    />
                </Helmet>
            </Helmet>
            <article className="padding-bottom-5 tablet:padding-top-6">
                <Title preTitle={description} title={FeatureName.DAILY_DATA} />
                {withCatchAndSuspense(<DeliveriesTable />)}
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export default DeliveriesPage;
