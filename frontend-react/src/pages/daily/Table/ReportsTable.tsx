import { useResource } from "rest-hooks";
import { SetStateAction, useMemo, useState } from "react";
import { useOktaAuth } from "@okta/okta-react";

import { getUniqueReceiverSvc } from "../../../utils/ReportUtils";
import ReportResource from "../../../resources/ReportResource";
import Table, { TableConfig } from "../../../components/Table/Table";
import { getStoredOrg } from "../../../contexts/SessionStorageTools";

import TableButtonGroup from "./TableButtonGroup";
import { getReportAndDownload } from "./ReportsUtils";

/* 
    This is the main exported component from this file. It provides container styling,
    table headers, and applies the <TableData> component to the table that is created in this
    component.
*/
function ReportsTable({ sortBy }: { sortBy?: string }) {
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

    const handleFetchAndDownload = (id: string) => {
        getReportAndDownload(
            id,
            auth?.authState?.accessToken?.accessToken || "",
            organization || ""
        );
    };

    const resultsTableConfig: TableConfig = {
        columns: [
            {
                dataAttr: "reportId",
                columnHeader: "Report ID",
                feature: {
                    link: true,
                    linkBasePath: "/report-details?reportId=",
                },
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
                feature: {
                    action: handleFetchAndDownload,
                    param: "reportId",
                },
            },
        ],
        rows: filteredReports,
    };

    return (
        <>
            <div className="grid-col-12">
                {receiverSVCs.length > 1 ? (
                    <TableButtonGroup
                        senders={receiverSVCs}
                        chosenCallback={handleCallback}
                    />
                ) : null}
            </div>
            <div className="grid-col-12">
                <Table config={resultsTableConfig} />
            </div>
            <div className="grid-col-12">
                {reports.filter((report) => report.receivingOrgSvc === chosen)
                    .length === 0 ? (
                    <p>No results</p>
                ) : null}
            </div>
        </>
    );
}

export default ReportsTable;
