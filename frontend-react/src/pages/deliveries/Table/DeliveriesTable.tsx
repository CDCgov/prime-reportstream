import React, { Dispatch, SetStateAction } from "react";

import Table, {
    ColumnConfig,
    TableConfig,
} from "../../../components/Table/Table";
import { FilterManager } from "../../../hooks/filters/UseFilterManager";
import { useSessionContext } from "../../../contexts/SessionContext";
import {
    useOrgDeliveries,
    DeliveriesDataAttr,
} from "../../../hooks/network/History/DeliveryHooks";
import Spinner from "../../../components/Spinner";
import TableFilters from "../../../components/Table/TableFilters";
import { PaginationProps } from "../../../components/Table/Pagination";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import usePagination from "../../../hooks/UsePagination";
import { NoServicesBanner } from "../../../components/alerts/NoServicesAlert";
import { RSReceiver } from "../../../config/endpoints/settings";
import { useOrganizationReceiversFeed } from "../../../hooks/UseOrganizationReceiversFeed";

import { getReportAndDownload } from "./ReportsUtils";
import ServicesDropdown from "./ServicesDropdown";

const extractCursor = (d: RSDelivery) => d.batchReadyAt;

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

interface DeliveriesTableContentProps {
    filterManager: FilterManager;
    paginationProps?: PaginationProps;
    isLoading: boolean;
    serviceReportsList: RSDelivery[] | undefined;
}

const DeliveriesTableContent: React.FC<DeliveriesTableContentProps> = ({
    filterManager,
    paginationProps,
    isLoading,
    serviceReportsList,
}) => {
    const { oktaToken, activeMembership } = useSessionContext();
    const handleFetchAndDownload = (id: string) => {
        getReportAndDownload(
            id,
            oktaToken?.accessToken || "",
            activeMembership?.parsedName || ""
        );
    };
    const transformDate = (s: string) => {
        return new Date(s).toLocaleString();
    };
    const columns: Array<ColumnConfig> = [
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
            transform: transformDate,
        },
        {
            dataAttr: DeliveriesDataAttr.EXPIRES,
            columnHeader: "Expires",
            sortable: true,
            transform: transformDate,
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
                param: DeliveriesDataAttr.REPORT_ID,
            },
        },
    ];

    const resultsTableConfig: TableConfig = {
        columns: columns,
        rows: serviceReportsList || [],
    };

    if (isLoading) return <Spinner />;

    return (
        <>
            <TableFilters filterManager={filterManager} />
            <Table
                config={resultsTableConfig}
                filterManager={filterManager}
                paginationProps={paginationProps}
            />
        </>
    );
};

const DeliveriesTableWithNumberedPagination = ({
    services,
    activeService,
    setActiveService,
}: {
    services: RSReceiver[];
    activeService: RSReceiver | undefined;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
}) => {
    const handleSetActive = (name: string) => {
        setActiveService(services.find((item) => item.name === name));
    };

    const { fetchResults, filterManager } = useOrgDeliveries(
        activeService?.name
    );
    const pageSize = filterManager.pageSettings.size;
    const sortOrder = filterManager.sortSettings.order;
    const rangeTo = filterManager.rangeSettings.to;
    const rangeFrom = filterManager.rangeSettings.from;

    // The start cursor is the high value when results are in descending order
    // and the low value when the results are in ascending order.
    const startCursor = sortOrder === "DESC" ? rangeTo : rangeFrom;
    const isCursorInclusive = sortOrder === "ASC";

    const {
        currentPageResults: serviceReportsList,
        paginationProps,
        isLoading,
    } = usePagination<RSDelivery>({
        startCursor,
        isCursorInclusive,
        pageSize,
        fetchResults,
        extractCursor,
    });

    if (paginationProps) {
        paginationProps.label = "Deliveries pagination";
    }

    return (
        <>
            <ServiceDisplay
                services={services}
                activeService={activeService}
                handleSetActive={handleSetActive}
            />
            <DeliveriesTableContent
                filterManager={filterManager}
                paginationProps={paginationProps}
                isLoading={isLoading}
                serviceReportsList={serviceReportsList}
            />
        </>
    );
};

export const DeliveriesTable = () => {
    const { loadingServices, services, activeService, setActiveService } =
        useOrganizationReceiversFeed();

    if (loadingServices) return <Spinner />;

    if (!loadingServices && !activeService)
        return (
            <div className="grid-container usa-section margin-bottom-10">
                <NoServicesBanner
                    featureName="Active Services"
                    organization=""
                    serviceType={"receiver"}
                />
            </div>
        );

    return (
        <>
            {activeService && (
                <DeliveriesTableWithNumberedPagination
                    services={services}
                    activeService={activeService}
                    setActiveService={setActiveService}
                />
            )}
        </>
    );
};

export default DeliveriesTable;
