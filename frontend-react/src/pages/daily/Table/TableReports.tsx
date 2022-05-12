import { useResource } from "rest-hooks";
import { SetStateAction, useMemo, useState } from "react";
import download from "downloadjs";
import { useOktaAuth } from "@okta/okta-react";

import { getUniqueReceiverSvc } from "../../../utils/ReportUtils";
import ReportResource from "../../../resources/ReportResource";
import Table, { TableConfig } from "../../../components/Table/Table";
import { getStoredOrg } from "../../../contexts/SessionStorageTools";

import TableButtonGroup from "./TableButtonGroup";

/* 
    This is the main exported component from this file. It provides container styling,
    table headers, and applies the <TableData> component to the table that is created in this
    component.
*/
function TableReports({ sortBy }: { sortBy?: string }) {
    const auth = useOktaAuth();
    const organization = getStoredOrg();
    const reports: ReportResource[] = useResource(ReportResource.list(), {
        sortBy,
    });
    const receiverSVCs: string[] = Array.from(getUniqueReceiverSvc(reports));
    const [chosen, setChosen] = useState(receiverSVCs[0]);
    const filteredReports = useMemo(
        () => reports.filter((report) => report.receivingOrgSvc === chosen),
        [chosen, reports]
    );

    /* This syncs the chosen state from <TableButtonGroup> with the chosen state here */
    const handleCallback = (chosen: SetStateAction<string>) => {
        setChosen(chosen);
    };

    const downloadReport = (id: string) => {
        fetch(`${process.env.REACT_APP_BACKEND_URL}/api/history/report/${id}`, {
            headers: {
                Authorization: `Bearer ${
                    auth.authState?.accessToken?.accessToken || ""
                }`,
                Organization: organization!!,
            },
        })
            .then((res) => res.json())
            .then((report) => {
                // The filename to use for the download should not contain blob folders if present
                let filename = decodeURIComponent(report.fileName);
                let filenameStartIndex = filename.lastIndexOf("/");
                if (
                    filenameStartIndex >= 0 &&
                    filename.length > filenameStartIndex + 1
                )
                    filename = filename.substring(filenameStartIndex + 1);
                download(report.content, filename, report.mimetype);
            })
            .catch((error) => console.log(error));
    };

    const resultsTableConfig: TableConfig = {
        columns: [
            {
                dataAttr: "reportId",
                columnHeader: "Report ID",
                link: true,
                linkBasePath: "/report-details?reportId=",
            },
            {
                dataAttr: "sent",
                columnHeader: "Date Sent",
                transform: (s: string) => {
                    return new Date(s).toLocaleString();
                },
            },
            {
                dataAttr: "expires",
                columnHeader: "Expires",
                transform: (s: string) => {
                    return new Date(s).toLocaleString();
                },
            },
            {
                dataAttr: "total",
                columnHeader: "Total Tests",
            },
            {
                dataAttr: "fileType",
                columnHeader: "File",
                actionable: {
                    action: downloadReport,
                    param: "reportId",
                },
            },
        ],
        rows: filteredReports,
    };

    return (
        <section className="grid-container margin-top-5">
            <div className="grid-col-12">
                {
                    /* Button group only shows when there is more than a single sender. */
                    receiverSVCs.length > 1 ? (
                        <TableButtonGroup
                            senders={receiverSVCs}
                            chosenCallback={handleCallback}
                        />
                    ) : null
                }
                <Table config={resultsTableConfig} />
                {reports.filter((report) => report.receivingOrgSvc === chosen)
                    .length === 0 ? (
                    <p>No results</p>
                ) : null}
            </div>
        </section>
    );
}

export default TableReports;
