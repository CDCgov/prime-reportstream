import { Dispatch, SetStateAction, useEffect, useMemo, useState } from "react";
import { useResource } from "rest-hooks";

import Table, { TableConfig } from "../../../components/Table/Table";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import { useSessionContext } from "../../../contexts/SessionContext";
import { useReceiversList } from "../../../hooks/network/Organizations/ReceiversHooks";
import { RSReceiver } from "../../../network/api/Organizations/Receivers";
import ReportResource from "../../../resources/ReportResource";
import { getUniqueReceiverSvc } from "../../../utils/ReportUtils";

import { getReportAndDownload } from "./ReportsUtils";
import ServicesDropdown from "./ServicesDropdown";

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
    loadingServices: boolean;
    services: RSReceiver[];
    activeService: RSReceiver | undefined;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
}
/** Fetches a list of receivers for your active organization, and provides a controller to switch
 * between them */
export const useReceiverFeeds = (): ReceiverFeeds => {
    const { activeMembership } = useSessionContext();
    const {
        data: receivers,
        loading,
        trigger: getReceiversList,
    } = useReceiversList(activeMembership?.parsedName);
    const [active, setActive] = useState<RSReceiver | undefined>();
    useEffect(() => {
        // IF activeMembership?.parsedName is not undefined
        if (
            activeMembership?.parsedName !== undefined &&
            receivers === undefined
        ) {
            // Trigger useReceiversList()
            getReceiversList();
        }
        // Ignoring getReceiverList() as dep
    }, [activeMembership?.parsedName, receivers]); //eslint-disable-line

    useEffect(() => {
        if (receivers?.length) {
            setActive(receivers[0]);
        }
    }, [receivers]);

    return {
        loadingServices: loading,
        services: receivers,
        activeService: active,
        setActiveService: setActive,
    };
};

/*
    This is the main exported component from this file. It provides container styling,
    table headers, and applies the <TableData> component to the table that is created in this
    component.
*/
function ReportsTable() {
    const { oktaToken, activeMembership } = useSessionContext();
    const reports: ReportResource[] = useResource(ReportResource.list(), {});
    const fm = useFilterManager(filterManagerDefaults);

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
            oktaToken?.accessToken || "",
            activeMembership?.parsedName || ""
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

    return (
        <>
            <div className="grid-container grid-col-12">
                {receiverSVCs.length > 1 ? (
                    <ServicesDropdown
                        active={chosen}
                        services={receiverSVCs}
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
