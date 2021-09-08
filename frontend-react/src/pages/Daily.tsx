import moment from "moment";
import { useResource } from "rest-hooks";
import ReportResource from "../resources/ReportResource";
import OrganizationResource from "../resources/OrganizationResource";
import { useOktaAuth } from "@okta/okta-react";
import { groupToOrg } from "../webreceiver-utils";
import download from "downloadjs";
import { getListOfSenders } from "../controllers/ReportController";
import { ButtonGroup, Button } from "@trussworks/react-uswds"
import { useState } from "react";


const TableData = (props) => {
    return (
        <tbody id="tBody" className="font-mono-2xs">
            {props.reports.map((report, idx) => (
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
                        <ReportLink report={report} />
                    </th>
                </tr>
            ))}
        </tbody>
    );
};

const ReportLink = (props) => {
    console.log("From ReportLink: " + props.report)

    const handleClick = (e: any) => {
        e.preventDefault();
        if (props.report !== undefined) {
            console.log(props.report.content);
            download(props.report.content, props.report.fileName, props.report.mimeType);
        }
    };

    return (
        <a href="/" onClick={handleClick} className="usa-link">
            {props.report !== undefined
                ? props.report.fileType === "HL7_BATCH"
                    ? "HL7(BATCH)"
                    : props.report.fileType
                : ""}
        </a>
    );
};

const TableReports = ({ sortBy }: { sortBy?: string }) => {
    const reports: ReportResource[] = useResource(ReportResource.list(), { sortBy });
    const senders: string[] = Array.from(getListOfSenders(reports));
    const [chosen, setChosen] = useState(senders[0])

    // Returns the chosen state from <TableButtonGroup>
    const handleCallback = (chosen) => {
        setChosen(chosen)
    }

    return (
        <section className="grid-container margin-top-5">
            <div className="grid-col-12">
                <h2>Test results</h2>
                {
                    // Button group shows to switch between senders ONLY if there is more than one sender
                    senders.length > 1 ?
                        <TableButtonGroup senders={senders} chosenCallback={handleCallback} />
                        :
                        null
                }
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
                    <TableData reports={reports.filter(report => report.sendingOrg === chosen)} />
                </table>
                {
                    reports.filter(report => report.sendingOrg === chosen).length === 0 ?
                        <p>No results</p>
                        :
                        null
                }
            </div>
        </section>
    );
};

const TableButtonGroup = (props) => {

    const senders: string[] = props.senders
    const [chosen, setChosen] = useState(senders[0])

    const handleClick = (id) => {
        setChosen(id)
        props.chosenCallback(id)
    }

    return (
        <ButtonGroup type="segmented">
            {
                senders.map((val) => {
                    return <Button
                        key={val}
                        id={val}
                        onClick={() => handleClick(val)}
                        type="button"
                        outline={val != chosen}>
                        {val}
                    </Button>
                })
            }
        </ButtonGroup>
    )
}

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
