import { format, parseISO } from "date-fns";

import ReportLink from "./ReportLink";
import { USLink } from "../../../components/USLink";
import ReportResource from "../../../resources/ReportResource";

interface Props {
    /* REQUIRED
    To populate the <DailyData> component with data, you must pass in an array of
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
                        <USLink href={"/report-details?reportId=" + report.reportId} key="daily">
                            {report.reportId}
                        </USLink>
                    </th>
                    <th scope="row">{format(parseISO(report.sent.toString()), "yyyy-MM-dd HH:mm")}</th>
                    <th scope="row">
                        {}
                        {format(parseISO(report.expires.toString()), "yyyy-MM-dd HH:mm")}
                    </th>
                    <th scope="row">{report.total}</th>
                    <th scope="row">
                        <ReportLink
                            reportId={report.reportId}
                            reportExpires={report.expires}
                            fileType={report.fileType}
                        />
                    </th>
                </tr>
            ))}
        </tbody>
    );
}

export default TableReportsData;
