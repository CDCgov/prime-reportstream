import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";

import DailyData from "./daily-data/DailyData";
import HipaaNotice from "../../components/HipaaNotice";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";
import Title from "../../components/Title";
import { USSmartLink } from "../../components/USLink";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import { FeatureName } from "../../utils/FeatureName";

function DeliveriesPage() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails ?? {};
    return (
        <GridContainer>
            <Helmet>
                <title>Daily Data - ReportStream</title>
                <meta
                    name="description"
                    content="Daily Data shows what data a public health entity has received with the option to download."
                />
                <meta
                    property="og:image"
                    content="/assets/img/opengraph/howwehelpyou-3.png"
                />
                <meta
                    property="og:image:alt"
                    content="An abstract illustration of screens and a document."
                />
            </Helmet>
            <article className="padding-bottom-5 tablet:padding-top-6">
                <Title
                    preTitle={description}
                    title={FeatureName.DAILY_DATA}
                    postTitle="View information about all data sent to your organization"
                    removeBottomMargin
                />

                <p className="margin-top-0">
                    You can find additional detail for COVID data on the{" "}
                    <USSmartLink href="/data-dashboard">
                        Data Dashboard.
                    </USSmartLink>
                </p>
                {withCatchAndSuspense(<DailyData />)}
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export default DeliveriesPage;
