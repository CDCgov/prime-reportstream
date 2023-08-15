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
import TableFilters, {
    TableFilterDateLabel,
} from "../../../components/Table/TableFilters";
import { PaginationProps } from "../../../components/Table/Pagination";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import usePagination from "../../../hooks/UsePagination";
import { NoServicesBanner } from "../../../components/alerts/NoServicesAlert";
import { RSReceiver } from "../../../config/endpoints/settings";
import { useOrganizationReceiversFeed } from "../../../hooks/UseOrganizationReceiversFeed";
import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import { FeatureName } from "../../../AppRouter";
import AdminFetchAlert from "../../../components/alerts/AdminFetchAlert";
import { isDateExpired } from "../../../utils/DateTimeUtils";

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
        <div className="grid-col-12">
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
    const featureEvent = `${FeatureName.DAILY_DATA} | ${EventName.TABLE_FILTER}`;
    const handleFetchAndDownload = (id: string) => {
        getReportAndDownload(
            id,
            oktaToken?.accessToken || "",
            activeMembership?.parsedName || "",
        );
    };
    const transformDate = (s: string) => {
        return new Date(s).toLocaleString();
    };
    const handleExpirationDate = (expiresDate: string) => {
        return !isDateExpired(expiresDate);
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
            dataAttr: DeliveriesDataAttr.FILE_NAME,
            columnHeader: "File",
            feature: {
                action: handleFetchAndDownload,
                param: DeliveriesDataAttr.REPORT_ID,
                actionButtonHandler: handleExpirationDate,
                actionButtonParam: DeliveriesDataAttr.EXPIRES,
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
            <TableFilters
                startDateLabel={TableFilterDateLabel.START_DATE}
                endDateLabel={TableFilterDateLabel.END_DATE}
                showDateHints={true}
                filterManager={filterManager}
                onFilterClick={({ from, to }: { from: string; to: string }) =>
                    trackAppInsightEvent(featureEvent, {
                        tableFilter: { startRange: from, endRange: to },
                    })
                }
            />
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
        activeService?.name,
    );
    const pageSize = filterManager.pageSettings.size;
    const sortOrder = filterManager.sortSettings.order;
    const rangeTo = filterManager.rangeSettings.to;
    const rangeFrom = filterManager.rangeSettings.from;

    // The start cursor is the high value when results are in descending order
    // and the low value when the results are in ascending order.
    const startCursor = sortOrder === "DESC" ? rangeTo : rangeFrom;
    const isCursorInclusive = sortOrder === "ASC";
    const analyticsEventName = `${FeatureName.DAILY_DATA} | ${EventName.TABLE_PAGINATION}`;

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
        analyticsEventName,
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
    const {
        loadingServices,
        services,
        activeService,
        setActiveService,
        isDisabled,
    } = useOrganizationReceiversFeed();

    if (loadingServices) return <Spinner />;

    if (isDisabled) {
        return <AdminFetchAlert />;
    }

    if (!loadingServices && !activeService)
        return (
            <div className="usa-section margin-bottom-10">
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
