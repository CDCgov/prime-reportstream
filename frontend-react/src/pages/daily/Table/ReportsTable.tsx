import { useMemo, useState } from "react";
import { useOktaAuth } from "@okta/okta-react";

import { getUniqueReceiverSvc } from "../../../utils/ReportUtils";
import Table, { TableConfig } from "../../../components/Table/Table";
import { getStoredOrg } from "../../../contexts/SessionStorageTools";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import Spinner from "../../../components/Spinner";
import useReportsList from "../../../hooks/network/History/ReportsHooks";
import { RSReport } from "../../../network/api/History/Reports";

import TableButtonGroup from "./TableButtonGroup";
import { getReportAndDownload } from "./ReportsUtils";

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "sent",
        locally: true,
    },
};

/* TODO (#4859): Extract SVC logic from ReportsTable */
const useReceiverFeeds = (reports: RSReport[]) => {};

/* 
    This is the main exported component from this file. It provides container styling,
    table headers, and applies the <TableData> component to the table that is created in this
    component.
*/
function ReportsTable() {
    const auth = useOktaAuth();
    const organization = getStoredOrg();
    const filterManager = useFilterManager(filterManagerDefaults);
    const { reports, loading, error, trigger } = useReportsList();

    const receiverSVCs: string[] = Array.from(getUniqueReceiverSvc(reports));
    const [chosen, setChosen] = useState(receiverSVCs?.[0] || undefined);

    /* TODO (#4859): Clean this up by refactoring how we swap feeds */
    const filteredReports = useMemo(
        () =>
            reports?.filter((report) => report.receivingOrgSvc === chosen) ||
            [],
        [chosen, reports]
    );
    /* This syncs the chosen state from <TableButtonGroup> with the chosen state here */
    const handleCallback = (chosen: string) => {
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

    if (loading) return <Spinner />;
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
                <Table
                    config={resultsTableConfig}
                    filterManager={filterManager}
                />
            </div>
            <div className="grid-col-12">
                {reports?.filter((report) => report.receivingOrgSvc === chosen)
                    .length === 0 ? (
                    <p>No results</p>
                ) : null}
            </div>
        </>
    );
}

export default ReportsTable;
