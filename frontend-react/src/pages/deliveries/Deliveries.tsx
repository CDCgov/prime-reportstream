import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import { FeatureName } from "../../utils/FeatureName";

import DeliveriesTable from "./Table/DeliveriesTable";

function DeliveriesPage() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails || {};
    return (
        <GridContainer>
            <Helmet>
                <title>{FeatureName.DAILY_DATA}</title>
            </Helmet>
            <article className="padding-bottom-5 tablet:padding-top-6">
                <Title preTitle={description} title={FeatureName.DAILY_DATA} />
                <DeliveriesTable />
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export default DeliveriesPage;
