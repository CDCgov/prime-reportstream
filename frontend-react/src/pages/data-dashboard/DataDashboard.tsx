import { Helmet } from "react-helmet-async";

import { FeatureName } from "../../utils/FeatureName";
import { USNavLink } from "../../components/USLink";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import DataDashboardTable from "../../components/DataDashboard/DataDashboardTable/DataDashboardTable";
import HipaaNotice from "../../components/HipaaNotice";
import { HeroWrapper } from "../../shared";

import styles from "./DataDashboard.module.scss";

function DataDashboardPage() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails ?? {};
    return (
        <div className={styles.DataDashboard}>
            <div className="bg-primary-darker text-white">
                <div className="grid-container">
                    <header className="usa-section usa-prose">
                        <div className="font-sans-lg text-blue-30">
                            {description}
                        </div>
                        <div className="font-sans-2xl text-bold">
                            Data Dashboard
                        </div>
                        <hr className="margin-bottom-3" />
                        <div className="font-sans-lg">
                            All the labs, facilities, aggregators, etc. that
                            have ever contributed data to your reports.
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
                            <DataDashboardTable />
                            <HipaaNotice />
                        </article>
                    </section>
                </div>
            </HeroWrapper>
        </div>
    );
}

export default DataDashboardPage;
