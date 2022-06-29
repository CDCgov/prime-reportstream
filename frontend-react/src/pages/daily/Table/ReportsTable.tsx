import { useResource } from "rest-hooks";
import { SetStateAction, useEffect, useMemo, useState } from "react";
import { useOktaAuth } from "@okta/okta-react";

import { getUniqueReceiverSvc } from "../../../utils/ReportUtils";
import ReportResource from "../../../resources/ReportResource";
import Table, { TableConfig } from "../../../components/Table/Table";
import { getStoredOrg } from "../../../contexts/SessionStorageTools";
import useFilterManager from "../../../hooks/filters/UseFilterManager";
import { PageSettingsActionType } from "../../../hooks/filters/UsePages";

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
    const fm = useFilterManager({
        sortDefaults: {
            column: "sent",
            locally: true,
        },
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

    /* TODO: Extend FilterManagerDefaults to include pageSize defaults */
    useEffect(() => {
        fm.updatePage({
            type: PageSettingsActionType.SET_SIZE,
            payload: {
                size: 100,
            },
        });
    }, []); // eslint-disable-line

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
                sortable: true,
                localSort: true,
                transform: (s: string) => {
                    return new Date(s).toLocaleString();
                },
            },
            {
                dataAttr: "expires",
                columnHeader: "Expires",
                sortable: true,
                localSort: true,
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
                <Table config={resultsTableConfig} filterManager={fm} />
            </div>
            <div className="grid-container margin-bottom-10">
                <div className="grid-col-12">
                    {reports.filter(
                        (report) => report.receivingOrgSvc === chosen
                    ).length === 0 ? (
                        <p>No results</p>
                    ) : null}
                </div>
            </div>
        </>
    );
}

export default ReportsTable;
