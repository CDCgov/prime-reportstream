import { Dispatch, FC, SetStateAction } from "react";

import { getReportAndDownload } from "./ReportsUtils";
import ServicesDropdown from "./ServicesDropdown";
import AdminFetchAlert from "../../../components/alerts/AdminFetchAlert";
import { NoServicesBanner } from "../../../components/alerts/NoServicesAlert";
import Spinner from "../../../components/Spinner";
import { PaginationProps } from "../../../components/Table/Pagination";
import Table, {
    ColumnConfig,
    TableConfig,
} from "../../../components/Table/Table";
import TableFilters, {
    TableFilterDateLabel,
} from "../../../components/Table/TableFilters";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import { RSReceiver } from "../../../config/endpoints/settings";
import useSessionContext from "../../../contexts/Session/useSessionContext";
import { FilterManager } from "../../../hooks/filters/UseFilterManager";
import {
    DeliveriesDataAttr,
    useOrgDeliveries,
} from "../../../hooks/network/History/DeliveryHooks";
import useAppInsightsContext from "../../../hooks/useAppInsightsContext";
import { useOrganizationReceiversFeed } from "../../../hooks/UseOrganizationReceiversFeed";
import usePagination from "../../../hooks/UsePagination";
import { EventName } from "../../../utils/AppInsights";
import { CustomerStatusType } from "../../../utils/DataDashboardUtils";
import { isDateExpired } from "../../../utils/DateTimeUtils";
import { FeatureName } from "../../../utils/FeatureName";

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
                    active={activeService?.name ?? ""}
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

const DeliveriesTable: FC<DeliveriesTableContentProps> = ({
    filterManager,
    paginationProps,
    isLoading,
    serviceReportsList,
}) => {
    const { authState, activeMembership } = useSessionContext();
    const handleFetchAndDownload = (id: string) => {
        getReportAndDownload(
            id,
            authState.accessToken?.accessToken ?? "",
            activeMembership?.parsedName ?? "",
        );
    };
    const transformDate = (s: string) => {
        return new Date(s).toLocaleString();
    };
    const handleExpirationDate = (expiresDate: string) => {
        return !isDateExpired(expiresDate);
    };
    const columns: ColumnConfig[] = [
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
        rows: serviceReportsList ?? [],
    };

    if (isLoading) return <Spinner />;

    return (
        <>
            <Table
                config={resultsTableConfig}
                filterManager={filterManager}
                paginationProps={paginationProps}
            />
        </>
    );
};

const DeliveriesFilterAndTable = ({
    services,
    activeService,
    setActiveService,
}: {
    services: RSReceiver[];
    activeService: RSReceiver | undefined;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
}) => {
    const appInsights = useAppInsightsContext();
    const featureEvent = `${FeatureName.DAILY_DATA} | ${EventName.TABLE_FILTER}`;
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
            <TableFilters
                startDateLabel={TableFilterDateLabel.START_DATE}
                endDateLabel={TableFilterDateLabel.END_DATE}
                showDateHints={true}
                filterManager={filterManager}
                onFilterClick={({ from, to }: { from: string; to: string }) =>
                    appInsights?.trackEvent({
                        name: featureEvent,
                        properties: {
                            tableFilter: { startRange: from, endRange: to },
                        },
                    })
                }
            />
            <DeliveriesTable
                filterManager={filterManager}
                paginationProps={paginationProps}
                isLoading={isLoading}
                serviceReportsList={serviceReportsList}
            />
        </>
    );
};

const DailyData = () => {
    const {
        isLoading,
        data: services,
        activeService,
        setActiveService,
        isDisabled,
    } = useOrganizationReceiversFeed();

    if (isLoading) return <Spinner />;

    if (isDisabled) {
        return <AdminFetchAlert />;
    }

    if (
        !isLoading &&
        (!activeService ||
            activeService?.customerStatus === CustomerStatusType.INACTIVE)
    )
        return (
            <div className="usa-section margin-bottom-5">
                <NoServicesBanner />
            </div>
        );
    return (
        <>
            {activeService && (
                <DeliveriesFilterAndTable
                    services={services!}
                    activeService={activeService}
                    setActiveService={setActiveService}
                />
            )}
        </>
    );
};

export default DailyData;
