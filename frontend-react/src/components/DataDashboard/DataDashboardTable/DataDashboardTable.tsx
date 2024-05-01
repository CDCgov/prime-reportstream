import { useState } from "react";

import DataDashboardTableFilters from "./DataDashboardTableFilters/DataDashboardTableFilters";
import { RSReceiver } from "../../../config/endpoints/settings";
import { PageSettingsActionType } from "../../../hooks/filters/UsePages";
import { SortSettingsActionType } from "../../../hooks/filters/UseSortOrder";
import useReceiverDeliveries, {
    DeliveriesAttr,
} from "../../../hooks/network/DataDashboard/UseReceiverDeliveries";
import useAppInsightsContext from "../../../hooks/UseAppInsightsContext";
import { useOrganizationReceivers } from "../../../hooks/UseOrganizationReceivers";
import { getSlots } from "../../../hooks/UsePagination";
import Table from "../../../shared/Table/Table";
import { EventName } from "../../../utils/AppInsights";
import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";
import { FeatureName } from "../../../utils/FeatureName";
import AdminFetchAlert from "../../alerts/AdminFetchAlert";
import { NoServicesBanner } from "../../alerts/NoServicesAlert";
import Spinner from "../../Spinner";
import Pagination from "../../Table/Pagination";
import { USLink } from "../../USLink";
import ReceiverServices from "../ReceiverServices/ReceiverServices";

function DashboardFilterAndTable({
    receiverServices,
    activeReceiver,
    setActiveReceiver,
}: {
    receiverServices: RSReceiver[];
    activeReceiver: RSReceiver;
    setActiveReceiver: (receiver: RSReceiver) => void;
}) {
    const appInsights = useAppInsightsContext();
    const featureEvent = `${FeatureName.DATA_DASHBOARD} | ${EventName.TABLE_FILTER}`;

    const handleSetActive = (name: string) => {
        const result = receiverServices.find((item) => item.name === name);
        if (result) setActiveReceiver(result);
    };

    const {
        data: results,
        filterManager,
        isLoading,
    } = useReceiverDeliveries(activeReceiver.name);

    if (isLoading || !results) return <Spinner />;

    const onColumnCustomSort = (columnID: string) => {
        filterManager?.updateSort({
            type: SortSettingsActionType.CHANGE_COL,
            payload: {
                column: columnID,
            },
        });
        filterManager?.updateSort({
            type: SortSettingsActionType.SWAP_ORDER,
        });
    };
    const data = results?.data.map((dataRow) => [
        {
            columnKey: DeliveriesAttr.CREATED_AT,
            columnHeader: "Date sent to you",
            content: formatDateWithoutSeconds(dataRow.createdAt),
            columnCustomSort: () =>
                onColumnCustomSort(DeliveriesAttr.CREATED_AT),
            columnCustomSortSettings: filterManager.sortSettings,
        },
        {
            columnKey: DeliveriesAttr.ORDERING_PROVIDER,
            columnHeader: "Ordering provider",
            content: dataRow.orderingProvider,
            columnCustomSort: () =>
                onColumnCustomSort(DeliveriesAttr.ORDERING_PROVIDER),
            columnCustomSortSettings: filterManager.sortSettings,
        },
        {
            columnKey: DeliveriesAttr.ORDERING_FACILITY,
            columnHeader: "Performing facility",
            content: dataRow.orderingFacility,
            columnCustomSort: () =>
                onColumnCustomSort(DeliveriesAttr.ORDERING_FACILITY),
            columnCustomSortSettings: filterManager.sortSettings,
        },
        {
            columnKey: DeliveriesAttr.SUBMITTER,
            columnHeader: "Submitter",
            content: dataRow.submitter,
            columnCustomSort: () =>
                onColumnCustomSort(DeliveriesAttr.SUBMITTER),
            columnCustomSortSettings: filterManager.sortSettings,
        },
        {
            columnKey: DeliveriesAttr.REPORT_ID,
            columnHeader: "Report ID",
            content: (
                <USLink
                    href={`/data-dashboard/report-details/${dataRow.reportId}`}
                >
                    {dataRow.reportId}
                </USLink>
            ),
            columnCustomSort: () =>
                onColumnCustomSort(DeliveriesAttr.REPORT_ID),
            columnCustomSortSettings: filterManager.sortSettings,
        },
    ]);

    const currentPageNum = filterManager.pageSettings.currentPage;

    return (
        <>
            <div className="text-bold font-sans-md">
                Showing all results ({results?.meta.totalFilteredCount})
            </div>
            <div className="display-flex flex-row">
                <ReceiverServices
                    receiverServices={receiverServices}
                    activeService={activeReceiver}
                    handleSetActive={handleSetActive}
                />
                <DataDashboardTableFilters
                    startDateLabel="From: (mm/dd/yyyy)"
                    endDateLabel="To: (mm/dd/yyyy)"
                    filterManager={filterManager}
                    onFilterClick={({
                        from,
                        to,
                    }: {
                        from: string;
                        to: string;
                    }) => {
                        filterManager?.updatePage({
                            type: PageSettingsActionType.RESET,
                        });

                        appInsights?.trackEvent({
                            name: featureEvent,
                            properties: {
                                tableFilter: { startRange: from, endRange: to },
                            },
                        });
                    }}
                />
            </div>
            <Table apiSortable borderless rowData={data} />
            {data.length > 0 && (
                <Pagination
                    currentPageNum={currentPageNum}
                    setSelectedPage={(pageNum) => {
                        filterManager.updatePage({
                            type: PageSettingsActionType.SET_PAGE,
                            payload: { page: pageNum },
                        });
                    }}
                    slots={getSlots(currentPageNum, results?.meta.totalPages)}
                />
            )}
        </>
    );
}

export default function DataDashboardTable() {
    const { isLoading, isDisabled, activeReceivers } =
        useOrganizationReceivers();
    const [activeReceiver, setActiveReceiver] = useState(activeReceivers?.[0]);
    if (isLoading) return <Spinner />;

    if (isDisabled) {
        return <AdminFetchAlert />;
    }

    if (!isLoading && !activeReceiver)
        return (
            <div className="usa-section margin-bottom-10">
                <NoServicesBanner />
            </div>
        );

    return (
        <>
            {activeReceiver && (
                <DashboardFilterAndTable
                    receiverServices={activeReceivers}
                    activeReceiver={activeReceiver}
                    setActiveReceiver={setActiveReceiver}
                />
            )}
        </>
    );
}
