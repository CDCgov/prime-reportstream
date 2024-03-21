import { useState } from "react";

import { RSReceiver } from "../../../config/endpoints/settings";
import {
    EventName,
    useAppInsightsContext,
} from "../../../contexts/AppInsights";
import { PageSettingsActionType } from "../../../hooks/filters/UsePages";
import { SortSettingsActionType } from "../../../hooks/filters/UseSortOrder";
import useReceiverSubmitters, {
    DeliveriesAttr,
} from "../../../hooks/network/DataDashboard/UseReceiverSubmitters";
import { useOrganizationReceivers } from "../../../hooks/UseOrganizationReceivers";
import { getSlots } from "../../../hooks/UsePagination";
import { Table } from "../../../shared";
import {
    transformFacilityTypeClass,
    transformFacilityTypeLabel,
} from "../../../utils/DataDashboardUtils";
import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";
import { FeatureName } from "../../../utils/FeatureName";
import AdminFetchAlert from "../../alerts/AdminFetchAlert";
import { NoServicesBanner } from "../../alerts/NoServicesAlert";
import Spinner from "../../Spinner";
import Pagination from "../../Table/Pagination";
import DataDashboardTableFilters from "../DataDashboardTable/DataDashboardTableFilters/DataDashboardTableFilters";
import ReceiverServices from "../ReceiverServices/ReceiverServices";

function FacilitiesProvidersFilterAndTable({
    receiverServices,
    activeReceiver,
    setActiveReceiver,
}: {
    receiverServices: RSReceiver[];
    activeReceiver: RSReceiver;
    setActiveReceiver: (receiver: RSReceiver) => void;
}) {
    const { appInsights } = useAppInsightsContext();
    const featureEvent = `${FeatureName.FACILITIES_PROVIDERS} | ${EventName.TABLE_FILTER}`;

    const handleSetActive = (name: string) => {
        const result = receiverServices.find((item) => item.name === name);
        if (result) setActiveReceiver(result);
    };

    const {
        data: results,
        filterManager,
        isLoading,
    } = useReceiverSubmitters(activeReceiver.name);

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
            columnKey: DeliveriesAttr.NAME,
            columnHeader: "Name",
            content: dataRow.name,
            columnCustomSort: () => onColumnCustomSort(DeliveriesAttr.NAME),
            columnCustomSortSettings: filterManager.sortSettings,
        },
        {
            columnKey: DeliveriesAttr.LOCATION,
            columnHeader: "Location",
            content: dataRow.location,
            columnCustomSort: () => onColumnCustomSort(DeliveriesAttr.LOCATION),
            columnCustomSortSettings: filterManager.sortSettings,
        },
        {
            columnKey: DeliveriesAttr.FACILITY_TYPE,
            columnHeader: "Facility type",
            content: dataRow.type ? (
                <span className={transformFacilityTypeClass(dataRow.type)}>
                    {transformFacilityTypeLabel(dataRow.type)}
                </span>
            ) : (
                ""
            ),
            columnCustomSort: () =>
                onColumnCustomSort(DeliveriesAttr.FACILITY_TYPE),
            columnCustomSortSettings: filterManager.sortSettings,
        },
        {
            columnKey: DeliveriesAttr.TEST_RESULT_COUNT,
            columnHeader: "Report count",
            content: dataRow.testResultCount,
            columnCustomSort: () =>
                onColumnCustomSort(DeliveriesAttr.TEST_RESULT_COUNT),
            columnCustomSortSettings: filterManager.sortSettings,
        },
        {
            columnKey: DeliveriesAttr.REPORT_DATE,
            columnHeader: "Most recent report date",
            content: formatDateWithoutSeconds(dataRow.firstReportDate),
            columnCustomSort: () =>
                onColumnCustomSort(DeliveriesAttr.REPORT_DATE),
            columnCustomSortSettings: filterManager.sortSettings,
        },
    ]);

    const currentPageNum = filterManager.pageSettings.currentPage;

    return (
        <div>
            <section id="facilities-providers">
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
                                    tableFilter: {
                                        startRange: from,
                                        endRange: to,
                                    },
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
                        slots={getSlots(
                            currentPageNum,
                            results?.meta.totalPages,
                        )}
                    />
                )}
            </section>
        </div>
    );
}

export default function FacilitiesProvidersTable() {
    const { isLoading, isDisabled, activeReceivers } =
        useOrganizationReceivers();
    const [activeReceiver, setActiveReceiver] = useState(activeReceivers?.[0]);
    if (isLoading) return <Spinner />;

    if (isDisabled) return <AdminFetchAlert />;

    if (!isLoading && !activeReceiver)
        return (
            <div className="usa-section margin-bottom-10">
                <NoServicesBanner />
            </div>
        );

    return (
        <>
            {activeReceiver && (
                <FacilitiesProvidersFilterAndTable
                    receiverServices={activeReceivers}
                    activeReceiver={activeReceiver}
                    setActiveReceiver={setActiveReceiver}
                />
            )}
        </>
    );
}
