import moment from "moment";
import { useResource } from "rest-hooks";
import ReportResource from "../resources/ReportResource";
import OrganizationResource from "../resources/OrganizationResource";
import { useOktaAuth } from "@okta/okta-react";
import { groupToOrg } from "../webreceiver-utils";
import download from "downloadjs";

const TableData = ({ sortBy }: { sortBy?: string }) => {
    const reports = useResource(ReportResource.list(), { sortBy });

    return (
        <tbody id="tBody" className="font-mono-2xs">
            {reports.map((report, idx) => (
                <tr key={idx}>
                    <th scope="row">
                        <a
                            href={"/report-details?reportId=" + report.reportId}
                            className="usa-link"
                        >
                            {report.reportId}
                        </a>
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
                        <ReportLink reportId={report.reportId} />
                    </th>
                </tr>
            ))}
        </tbody>
    );
};

const ReportLink = ({ reportId }) => {
    let report = useResource(ReportResource.detail(), { reportId });

    console.log(report);

    const handleClick = (e: any) => {
        e.preventDefault();
        if (report !== undefined) {
            console.log(report.content);
            download(report.content, report.fileName, report.mimeType);
        }
    };

    return (
        <a href="/" onClick={handleClick} className="usa-link">
            {report !== undefined
                ? report.fileType === "HL7_BATCH"
                    ? "HL7(BATCH)"
                    : report.fileType
                : ""}
        </a>
    );
};

const TableReports = () => {
    return (
        <section className="grid-container margin-top-5">
            <div className="grid-col-12">
                <h2>Test results</h2>

                <table
                    className="usa-table usa-table--borderless prime-table"
                    summary="Previous results"
                >
                    <thead>
                        <tr>
                            <th scope="col">Report Id</th>
                            <th scope="col">Date Sent</th>
                            <th scope="col">Expires</th>
                            <th scope="col">Total tests</th>
                            <th scope="col">File</th>
                        </tr>
                    </thead>
                    <TableData />
                </table>
            </div>
        </section>
    );
};

const OrgName = () => {
    const { authState } = useOktaAuth();

    // finds the first organization that does not have the word "sender" in it
    const organization = groupToOrg(
        authState!.accessToken?.claims.organization.find(o => !o.toLowerCase().includes('sender'))
    );
    const org = useResource(OrganizationResource.detail(), {
        name: organization,
    });

    return (
        <span id="orgName" className="text-normal text-base">
            {org?.description}
        </span>
    );
};

const HipaaNotice = () => {
    return (
        <section className="grid-container usa-section usa-prose font-sans-2xs text-base-darker">
            <p>
                Data aggregated with ReportStream may be subject to the Privacy Act of 1974, the Health Insurance Portability and Accountability Act of 1996 (HIPAA), and other laws, and requires special safeguarding.
            </p>
        </section>

    );
}

export const Daily = () => {
    return (
        <>
            <section className="grid-container margin-bottom-5">
                <h3 className="margin-bottom-0">
                    <OrgName />
                </h3>
                <h1 className="margin-top-0 margin-bottom-0">COVID-19</h1>
            </section>
            <section className="grid-container margin-top-0"></section>
            <TableReports />
            <HipaaNotice />
        </>
    );
};
