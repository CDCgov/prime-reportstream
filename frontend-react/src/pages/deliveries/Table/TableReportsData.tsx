import moment from "moment";

import ReportResource from "../../../resources/ReportResource";
import { USLink } from "../../../components/USLink";

import ReportLink from "./ReportLink";

interface Props {
    /* REQUIRED
    To populate the <DeliveriesTable> component with data, you must pass in an array of
    ReportResource items to be mapped with the TableReportsData (this) component. */
    reports: ReportResource[];
}

/* 
    TableData maps the reports (passed in via props) to a list item and returns all rows
    as a <tbody>
*/
function TableReportsData(props: Props) {
    return (
        <tbody id="tBody" className="font-mono-2xs">
            {props.reports.map((report, idx) => (
                <tr key={idx}>
                    <th scope="row">
                        <USLink
                            href={"/report-details?reportId=" + report.reportId}
                            key="daily"
                        >
                            {report.reportId}
                        </USLink>
                    </th>
                    <th scope="row">
                        {moment
                            .utc(report.sent)
                            .local()
                            .format("YYYY-MM-DD HH:mm")}
                    </th>
                    <th scope="row">
                        {moment
                            .utc(report.expires)
                            .local()
                            .format("YYYY-MM-DD HH:mm")}
                    </th>
                    <th scope="row">{report.total}</th>
                    <th scope="row">
                        <ReportLink report={report} />
                    </th>
                </tr>
            ))}
        </tbody>
    );
}

export default TableReportsData;
