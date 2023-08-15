import React, { Dispatch, SetStateAction } from "react";

import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import TableFilters from "../../Table/TableFilters";
import ReceiverServices from "../ReceiverServices/ReceiverServices";
import { RSReceiver } from "../../../config/endpoints/settings";
import { useOrganizationReceiversFeed } from "../../../hooks/UseOrganizationReceiversFeed";
import Spinner from "../../Spinner";
import { NoServicesBanner } from "../../alerts/NoServicesAlert";
import { FeatureName } from "../../../AppRouter";
import { Table } from "../../../shared/Table/Table";
import useReceiverSubmitters, {
    DeliveriesAttr,
} from "../../../hooks/network/DataDashboard/UseReceiverSubmitters";
import { SortSettingsActionType } from "../../../hooks/filters/UseSortOrder";
import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";
import Pagination from "../../Table/Pagination";
import { PageSettingsActionType } from "../../../hooks/filters/UsePages";
import { getSlots } from "../../../hooks/UsePagination";
import {
    transformFacilityTypeClass,
    transformFacilityTypeLabel,
} from "../../../utils/DataDashboardUtils";

function FacilitiesProvidersFilterAndTable({
    receiverServices,
    activeService,
    setActiveService,
}: {
    receiverServices: RSReceiver[];
    activeService: RSReceiver;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
}) {
    const featureEvent = `${FeatureName.FACILITIES_PROVIDERS} | ${EventName.TABLE_FILTER}`;

    const handleSetActive = (name: string) => {
        setActiveService(receiverServices.find((item) => item.name === name));
    };

    const {
        data: results,
        filterManager,
        isLoading,
    } = useReceiverSubmitters(activeService.name);

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
                        }) => {
                            filterManager?.updatePage({
                                type: PageSettingsActionType.RESET,
                            });

                            trackAppInsightEvent(featureEvent, {
                                tableFilter: { startRange: from, endRange: to },
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
                <FacilitiesProvidersFilterAndTable
                    receiverServices={services}
                    activeService={activeService}
                    setActiveService={setActiveService}
                />
            )}
        </>
    );
}
