import { Alert } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";

import styles from "./DataDashboard.module.scss";
import DataDashboardTable from "../../components/DataDashboard/DataDashboardTable/DataDashboardTable";
import HipaaNotice from "../../components/HipaaNotice";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary/RSErrorBoundary";
import { USLink, USNavLink } from "../../components/USLink";
import useOrganizationSettings from "../../hooks/api/organizations/UseOrganizationSettings/UseOrganizationSettings";
import { HeroWrapper } from "../../shared";
import { FeatureName } from "../../utils/FeatureName";

function DataDashboardPage() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails ?? {};
    return (
        <>
            <Helmet>
                <title>Data Dashboard - ReportStream</title>
                <meta
                    name="description"
                    content="ReportStream's data dashboard shows what data you have received and allows you to dive into detail for each facility or report."
                />
                <meta property="og:image" content="/assets/img/opengraph/howwehelpyou-3.png" />
                <meta property="og:image:alt" content="An abstract illustration of screens and a document." />
            </Helmet>
            <Alert type="error" headingLevel="h2">
                ReportStream is not maintaining this dashboard. Visit <USLink href="/daily-data">Daily Data</USLink> to
                view the status of data sent to you.
            </Alert>
            <div className={styles.DataDashboard}>
                <div className="bg-primary-darker text-white">
                    <div className="grid-container">
                        <header className="usa-section usa-prose">
                            <div className="font-sans-lg text-blue-30">{description}</div>
                            <div className="font-sans-2xl text-bold">Data Dashboard</div>
                            <hr className="margin-bottom-3" />
                            <div className="font-sans-lg">
                                All the labs, facilities, aggregators, etc. that have ever contributed data to your
                                reports.
                            </div>
                            <hr className="margin-bottom-3" />
                            <div className="font-sans-2xs">
                                Jump to:{" "}
                                <USNavLink href="/data-dashboard/facilities-providers">
                                    All facilities & providers
                                </USNavLink>
                            </div>
                            <hr />
                        </header>
                    </div>
                </div>
                <HeroWrapper>
                    <div className="grid-container">
                        <section className="usa-section">
                            <Helmet>
                                <title>{FeatureName.DATA_DASHBOARD}</title>
                            </Helmet>
                            <article>
                                {withCatchAndSuspense(<DataDashboardTable />)}
                                <HipaaNotice />
                            </article>
                        </section>
                    </div>
                </HeroWrapper>
            </div>
        </>
    );
}

export default DataDashboardPage;
