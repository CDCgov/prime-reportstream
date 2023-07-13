import React, { Dispatch, SetStateAction } from "react";

import { FeatureName } from "../../../AppRouter";
import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import { RSReceiver } from "../../../config/endpoints/settings";
import { useOrganizationReceiversFeed } from "../../../hooks/UseOrganizationReceiversFeed";
import Spinner from "../../Spinner";
import { NoServicesBanner } from "../../alerts/NoServicesAlert";
import { PaginationProps } from "../../Table/Pagination";
import TableFilters from "../../Table/TableFilters";
import { RSReceiverDelivery } from "../../../config/endpoints/dataDashboard";
import ReceiverServices from "../ReceiverServices/ReceiverServices";
import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";
import useReceiverDeliveries, {
    DeliveriesAttr,
} from "../../../hooks/network/DataDashboard/UseReceiverDeliveries";
import { FilterManager } from "../../../hooks/filters/UseFilterManager";
import AdminFetchAlert from "../../alerts/AdminFetchAlert";
import { Table } from "../../../shared/Table/Table";

// const extractCursor = (d: RSReceiverDelivery) => d.createdAt;

interface DashboardTableContentProps {
    filterManager: FilterManager;
    paginationProps?: PaginationProps;
    isLoading: boolean;
    deliveriesList: RSReceiverDelivery[] | undefined;
}

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

    console.log("fetchResults = ", fetchResults);

    const data = fetchResults?.data.map((dataRow) => [
        {
            columnKey: "DateSentToYou",
            columnHeader: "Date sent to you",
            content: dataRow.createdAt,
        },
        {
            columnKey: "OrderingProvider",
            columnHeader: "Ordering Provider",
            content: dataRow.orderingProvider,
        },
        {
            columnKey: "PerformingFacility",
            columnHeader: "Performing facility",
            content: dataRow.orderingFacility,
        },
        {
            columnKey: "Submitter",
            columnHeader: "Submitter",
            content: dataRow.submitter,
        },
        {
            columnKey: "ReportID",
            columnHeader: "Report ID",
            content: dataRow.reportId,
        },
    ]);

    // const pageSize = filterManager.pageSettings.size;
    // const sortOrder = filterManager.sortSettings.order;
    // const rangeTo = filterManager.rangeSettings.to;
    // const rangeFrom = filterManager.rangeSettings.from;
    //
    // const startCursor = sortOrder === "DESC" ? rangeTo : rangeFrom;
    // const isCursorInclusive = sortOrder === "ASC";
    // const analyticsEventName = `${FeatureName.DATA_DASHBOARD} | ${EventName.TABLE_PAGINATION}`;
    //
    // const { paginationProps } = usePagination<RSReceiverDelivery>({
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
            <Table rowData={data} />
        </>
    );
}

export default function DataDashboardTable() {
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
                <DashboardFilterAndTable
                    receiverServices={services}
                    activeService={activeService}
                    setActiveService={setActiveService}
                />
            )}
        </>
    );
}
