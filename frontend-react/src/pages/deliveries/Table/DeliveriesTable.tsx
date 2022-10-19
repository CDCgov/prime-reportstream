import { Dispatch, SetStateAction, useEffect, useMemo, useState } from "react";

import Table, { TableConfig } from "../../../components/Table/Table";
import useFilterManager, {
    extractFiltersFromManager,
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import { useSessionContext } from "../../../contexts/SessionContext";
import { useReceiversList } from "../../../hooks/network/Organizations/ReceiversHooks";
import { RSReceiver } from "../../../network/api/Organizations/Receivers";
import { useOrgDeliveries } from "../../../hooks/network/History/DeliveryHooks";
import Spinner from "../../../components/Spinner";
import TableFilters from "../../../components/Table/TableFilters";

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

const ServiceDisplay = ({
    services,
    activeService,
    handleSetActive,
}: {
    services: RSReceiver[];
    activeService: RSReceiver | undefined;
    handleSetActive: (v: string) => void;
}) => {
    return (
        <div className="grid-container grid-col-12">
            {services && services?.length > 1 ? (
                <ServicesDropdown
                    services={services}
                    active={activeService?.name || ""}
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
    const { oktaToken, activeMembership } = useSessionContext();
    const filterManager = useFilterManager(filterManagerDefaults);
    const { loadingServices, services, activeService, setActiveService } =
        useReceiverFeeds();
    const filters = useMemo(
        () => extractFiltersFromManager(filterManager),
        [filterManager]
    );
    // TODO: Doesn't update parameters because of the config memo dependency array
    const { serviceReportsList } = useOrgDeliveries(
        activeMembership?.parsedName,
        activeService?.name,
        filters
    );

    const handleSetActive = (name: string) => {
        setActiveService(services.find((item) => item.name === name));
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
    if (loadingServices) return <Spinner />;
    return (
        <>
            <ServiceDisplay
                services={services}
                activeService={activeService}
                handleSetActive={handleSetActive}
            />
            <TableFilters filterManager={filterManager} />
            <Table config={resultsTableConfig} filterManager={filterManager} />
        </>
    );
}

export default DeliveriesTable;
