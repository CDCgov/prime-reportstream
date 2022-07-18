import { Dispatch, SetStateAction, useEffect, useMemo, useState } from "react";

import { getUniqueReceiverSvc } from "../../../utils/ReportUtils";
import Table, { TableConfig } from "../../../components/Table/Table";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import Spinner from "../../../components/Spinner";
import { useReportsList } from "../../../hooks/network/History/ReportsHooks";
import { RSReportInterface } from "../../../network/api/History/Reports";
import { useSessionContext } from "../../../contexts/SessionContext";
import { showError } from "../../../components/AlertNotifications";

import TableButtonGroup from "./TableButtonGroup";
import { getReportAndDownload } from "./ReportsUtils";

/** @todo: page size default set to 10 once paginated */
const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "sent",
        locally: true,
    },
    pageDefaults: {
        size: 100,
    },
};

interface ReceiverFeeds {
    activeFeed: RSReportInterface[] | undefined;
    setActiveFeed: Dispatch<SetStateAction<string | undefined>>;
    feeds: string[] | undefined;
}
const useReceiverFeeds = (reports: RSReportInterface[]): ReceiverFeeds => {
    /* Keeps a list of all feeds for a receiver */
    const receiverSVCs: string[] = useMemo(
        () => Array.from(getUniqueReceiverSvc(reports)),
        [reports]
    );
    const [chosen, setChosen] = useState<string | undefined>(undefined);
    /* Once reports are fetched, this effect updates the chosen feed to the first feed */
    useEffect(() => {
        if (reports?.length >= 1 && receiverSVCs.length >= 1) {
            setChosen(receiverSVCs[0]);
        }
    }, [receiverSVCs, reports]);
    /* Provides only the feed's objects */
    const filteredReports = useMemo(
        () =>
            reports?.filter((report) => report.receivingOrgSvc === chosen) ||
            [],
        [chosen, reports]
    );
    return {
        activeFeed: filteredReports,
        setActiveFeed: setChosen,
        feeds: receiverSVCs,
    };
};

/*
    This is the main exported component from this file. It provides container styling,
    table headers, and applies the <TableData> component to the table that is created in this
    component.
*/
function ReportsTable() {
    const { memberships, oktaToken } = useSessionContext();
    const { data: reports, loading, error } = useReportsList();
    const { activeFeed, setActiveFeed, feeds } = useReceiverFeeds(reports);
    const filterManager = useFilterManager(filterManagerDefaults);

    useEffect(() => {
        if (error !== "") {
            showError(error);
        }
    }, [error]);

    const handleFetchAndDownload = (id: string) => {
        getReportAndDownload(
            id,
            oktaToken?.accessToken || "",
            memberships.state.active?.parsedName || ""
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
        rows: activeFeed || [],
    };

    if (loading) return <Spinner />;

    return (
        <>
            <div className="grid-col-12">
                {feeds && feeds.length > 1 ? (
                    <TableButtonGroup
                        senders={feeds}
                        chosenCallback={setActiveFeed}
                    />
                ) : null}
            </div>
            <div className="grid-col-12">
                <Table
                    config={resultsTableConfig}
                    filterManager={filterManager}
                />
            </div>
            <div className="grid-container margin-bottom-10">
                <div className="grid-col-12">
                    {activeFeed?.length === 0 ? <p>No results</p> : null}
                </div>
            </div>
        </>
    );
}

export default ReportsTable;
