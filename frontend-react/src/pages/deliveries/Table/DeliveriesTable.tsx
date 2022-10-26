import { useMemo } from "react";

import Table, { TableConfig } from "../../../components/Table/Table";
import useFilterManager, {
    extractFiltersFromManager,
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import { useSessionContext } from "../../../contexts/SessionContext";
import { RSReceiver } from "../../../network/api/Organizations/Receivers";
import { useOrgDeliveries } from "../../../hooks/network/History/DeliveryHooks";
import TableFilters from "../../../components/Table/TableFilters";
import { MembershipActionType } from "../../../hooks/UseOktaMemberships";

import { getReportAndDownload } from "./ReportsUtils";
import ServicesDropdown from "./ServicesDropdown";

enum DeliveriesDataAttr {
    REPORT_ID = "reportId",
    BATCH_READY = "batchReadyAt",
    EXPIRES = "expires",
    ITEM_COUNT = "reportItemCount",
    FILE_TYPE = "fileType",
}
/** @todo: page size default set to 10 once paginated */
const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: DeliveriesDataAttr.BATCH_READY,
        locally: true,
    },
    pageDefaults: {
        size: 100,
    },
};

const ServiceDisplay = ({
    services,
    activeService,
    handleSetActive,
}: {
    services: RSReceiver[] | undefined;
    activeService: string | undefined;
    handleSetActive: (v: string) => void;
}) => {
    return (
        <div className="grid-container grid-col-12">
            {services && services?.length > 1 ? (
                <ServicesDropdown
                    services={services}
                    active={activeService || ""}
                    chosenCallback={handleSetActive}
                />
            ) : (
                <p>
                    Default service:{" "}
                    <strong>
                        {(services?.length && services[0].name.toUpperCase()) ||
                            ""}
                    </strong>
                </p>
            )}
        </div>
    );
};

/*
    This is the main exported component from this file. It provides container styling,
    table headers, and applies the <TableData> component to the table that is created in this
    component.
*/
function DeliveriesTable() {
    const {
        oktaToken,
        activeMembership,
        dispatch: updateSession,
    } = useSessionContext();
    const filterManager = useFilterManager(filterManagerDefaults);
    const filters = useMemo(
        () => extractFiltersFromManager(filterManager),
        [filterManager]
    );
    // TODO: Doesn't update parameters because of the config memo dependency array
    const { serviceReportsList } = useOrgDeliveries(
        activeMembership?.parsedName,
        activeMembership?.service,
        filters
    );

    const handleSetActive = (name: string) => {
        const val = activeMembership?.allServices?.find(
            (item) => item.name === name
        );
        if (!!val?.name) {
            updateSession({
                type: MembershipActionType.SET_ACTIVE_SERVICE,
                payload: val.name,
            });
        }
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
                dataAttr: DeliveriesDataAttr.REPORT_ID,
                columnHeader: "Report ID",
                feature: {
                    link: true,
                    linkBasePath: "/report-details/",
                },
            },
            {
                dataAttr: DeliveriesDataAttr.BATCH_READY,
                columnHeader: "Available",
                sortable: true,
                localSort: true,
                transform: (s: string) => {
                    return new Date(s).toLocaleString();
                },
            },
            {
                dataAttr: DeliveriesDataAttr.EXPIRES,
                columnHeader: "Expires",
                sortable: true,
                localSort: true,
                transform: (s: string) => {
                    return new Date(s).toLocaleString();
                },
            },
            {
                dataAttr: DeliveriesDataAttr.ITEM_COUNT,
                columnHeader: "Items",
            },
            {
                dataAttr: DeliveriesDataAttr.FILE_TYPE,
                columnHeader: "File",
                feature: {
                    action: handleFetchAndDownload,
                    param: DeliveriesDataAttr.FILE_TYPE,
                },
            },
        ],
        rows: serviceReportsList || [],
    };
    return (
        <>
            <ServiceDisplay
                services={activeMembership?.allServices}
                activeService={activeMembership?.service}
                handleSetActive={handleSetActive}
            />
            <TableFilters filterManager={filterManager} />
            <Table config={resultsTableConfig} filterManager={filterManager} />
        </>
    );
}

export default DeliveriesTable;
