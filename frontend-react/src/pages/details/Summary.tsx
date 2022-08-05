import { NavLink } from "react-router-dom";

import ReportLink from "../daily/Table/ReportLink";
import ReportResource from "../../resources/ReportResource";
import { useOrgName } from "../../hooks/UseOrgName";
import { RSReportInterface } from "../../network/api/History/Reports";

interface Props {
    /* REQUIRED
    Passing in a report allows this component to extract key properties (id) 
    and display them on the Details page. */
    report: ReportResource | RSReportInterface | undefined;
}

function Summary(props: Props) {
    const { report }: Props = props;
    const orgName: string = useOrgName();

    return (
        <section className="grid-container tablet:margin-top-6">
            <nav
                className="usa-breadcrumb usa-breadcrumb--wrap"
                aria-label="Breadcrumbs"
            >
                <ol className="usa-breadcrumb__list">
                    <li className="usa-breadcrumb__list-item">
                        <NavLink
                            to="/daily-data"
                            key="daily"
                            className="usa-breadcrumb__link"
                            id="orgName"
                        >
                            {orgName}
                        </NavLink>
                    </li>
                    <li
                        className="usa-breadcrumb__list-item usa-current"
                        aria-current="page"
                    >
                        <span>Report details</span>
                    </li>
                </ol>
            </nav>
            <ReportLink report={report} button />
            <h3 className="margin-top-0 margin-bottom-4">
                <p id="download" className="margin-top-0 margin-bottom-0">
                    Report:{" "}
                    <span id="report.id">
                        {report ? report.reportId : "this is the report id"}
                    </span>
                </p>
            </h3>
        </section>
    );
}

export default Summary;
