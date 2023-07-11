import React, { Dispatch, SetStateAction } from "react";

import { FeatureName } from "../../../AppRouter";
import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import { RSReceiver } from "../../../config/endpoints/settings";
import { useOrganizationReceiversFeed } from "../../../hooks/UseOrganizationReceiversFeed";
import Spinner from "../../Spinner";
import { NoServicesBanner } from "../../alerts/NoServicesAlert";
import { PaginationProps } from "../../Table/Pagination";
import Table, { ColumnConfig, TableConfig } from "../../Table/Table";
import TableFilters from "../../Table/TableFilters";
import { Delivery } from "../../../config/endpoints/deliveries";
import ReceiverServices from "../ReceiverServices/ReceiverServices";
import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";
import useReceiverDeliveries, {
    DeliveriesAttr,
} from "../../../hooks/network/DataDashboard/UseReceiverDeliveries";
import { FilterManager } from "../../../hooks/filters/UseFilterManager";

// const extractCursor = (d: Delivery) => d.createdAt;

interface DashboardTableContentProps {
    filterManager: FilterManager;
    paginationProps?: PaginationProps;
    isLoading: boolean;
    deliveriesList: Delivery[] | undefined;
}

const DashboardTableContent: React.FC<DashboardTableContentProps> = ({
    filterManager,
    // paginationProps,
    isLoading,
    deliveriesList,
}) => {
    if (isLoading) return <Spinner />;

    const columns: Array<ColumnConfig> = [
        {
            dataAttr: DeliveriesAttr.CREATED_AT,
            columnHeader: "Date sent to you",
            sortable: true,
            transform: formatDateWithoutSeconds,
        },
        {
            dataAttr: DeliveriesAttr.ORDERING_PROVIDER,
            columnHeader: "Ordering Provider",
        },
        {
            dataAttr: DeliveriesAttr.ORDERING_FACILITY,
            columnHeader: "Performing facility",
        },
        {
            dataAttr: DeliveriesAttr.SUBMITTER,
            columnHeader: "Submitter",
        },
        {
            dataAttr: DeliveriesAttr.REPORT_ID,
            columnHeader: "Report ID",
            feature: {
                link: true,
                linkBasePath: "/data-dashboard/report-details/",
            },
        },
    ];

    const resultsTableConfig: TableConfig = {
        columns: columns,
        rows: deliveriesList || [],
    };

    return (
        <>
            <Table
                classes="margin-top-1"
                config={resultsTableConfig}
                filterManager={filterManager}
                // paginationProps={paginationProps}
            />
        </>
    );
};

function DashboardFilterAndTable({
    receiverServices,
    activeService,
    setActiveService,
}: {
    receiverServices: RSReceiver[];
    activeService: RSReceiver;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
}) {
    const featureEvent = `${FeatureName.DATA_DASHBOARD} | ${EventName.TABLE_FILTER}`;

    const handleSetActive = (name: string) => {
        setActiveService(receiverServices.find((item) => item.name === name));
    };

    // Pagination and filter props
    const { fetchResults, filterManager, isDeliveriesLoading } =
        useReceiverDeliveries(activeService.name);

    // const pageSize = filterManager.pageSettings.size;
    // const sortOrder = filterManager.sortSettings.order;
    // const rangeTo = filterManager.rangeSettings.to;
    // const rangeFrom = filterManager.rangeSettings.from;
    //
    // const startCursor = sortOrder === "DESC" ? rangeTo : rangeFrom;
    // const isCursorInclusive = sortOrder === "ASC";
    // const analyticsEventName = `${FeatureName.DATA_DASHBOARD} | ${EventName.TABLE_PAGINATION}`;
    //
    // const { paginationProps } = usePagination<Delivery>({
    //     startCursor,
    //     isCursorInclusive,
    //     pageSize,
    //     fetchResults,
    //     extractCursor,
    //     analyticsEventName,
    // });
    //
    // if (paginationProps) {
    //     paginationProps.label = "Data Dashboard pagination";
    // }

    return (
        <>
            <div className="text-bold font-sans-md">
                Showing all results ({fetchResults?.meta.totalFilteredCount})
            </div>
            <div className="display-flex flex-row">
                <ReceiverServices
                    receiverServices={receiverServices}
                    activeService={activeService}
                    handleSetActive={handleSetActive}
                />
                <TableFilters
                    startDateLabel="From: (mm/dd/yyy)"
                    endDateLabel="To: (mm/dd/yyyy)"
                    filterManager={filterManager}
                    onFilterClick={({
                        from,
                        to,
                    }: {
                        from: string;
                        to: string;
                    }) =>
                        trackAppInsightEvent(featureEvent, {
                            tableFilter: { startRange: from, endRange: to },
                        })
                    }
                />
            </div>
            <DashboardTableContent
                filterManager={filterManager}
                // paginationProps={paginationProps}
                isLoading={isDeliveriesLoading}
                deliveriesList={fetchResults?.data}
            />
        </>
    );
}

export default function DataDashboardTable() {
    const { loadingServices, services, activeService, setActiveService } =
        useOrganizationReceiversFeed();

    if (loadingServices) return <Spinner />;

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
                <DashboardFilterAndTable
                    receiverServices={services}
                    activeService={activeService}
                    setActiveService={setActiveService}
                />
            )}
        </>
    );
}
