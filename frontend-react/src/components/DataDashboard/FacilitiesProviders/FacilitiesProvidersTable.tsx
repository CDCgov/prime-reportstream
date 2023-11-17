import { Dispatch, SetStateAction } from "react";

import TableFilters from "../../Table/TableFilters";
import ReceiverServices from "../ReceiverServices/ReceiverServices";
import { RSReceiver } from "../../../config/endpoints/settings";
import { Table } from "../../../shared/Table/Table";
import { DeliveriesAttr } from "../../../hooks/network/DataDashboard/UseReceiverSubmitters";
import { SortSettingsActionType } from "../../../hooks/filters/UseSortOrder";
import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";
import Pagination from "../../Table/Pagination";
import { PageSettingsActionType } from "../../../hooks/filters/UsePages";
import { getSlots } from "../../../hooks/UsePagination";
import {
    transformFacilityTypeClass,
    transformFacilityTypeLabel,
} from "../../../utils/DataDashboardUtils";
import { RSSubmitter } from "../../../config/endpoints/dataDashboard";
import { FilterManager } from "../../../hooks/filters/UseFilterManager";

export interface FacilitiesProvidersTableProps {
    receiverServices: RSReceiver[];
    activeService: RSReceiver;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
    onFilterClick: (from: string, to: string) => void;
    filterManager: FilterManager;
    submitters: RSSubmitter[];
    submittersTotal: number;
    pagesTotal: number;
}

export default function FacilitiesProvidersTable({
    receiverServices,
    activeService,
    setActiveService,
    filterManager,
    onFilterClick,
    submitters,
    submittersTotal,
    pagesTotal,
}: FacilitiesProvidersTableProps) {
    const handleSetActive = (name: string) => {
        setActiveService(receiverServices.find((item) => item.name === name));
    };

    const onColumnCustomSort = (columnID: string) => {
        filterManager.updateSort({
            type: SortSettingsActionType.CHANGE_COL,
            payload: {
                column: columnID,
            },
        });
        filterManager.updateSort({
            type: SortSettingsActionType.SWAP_ORDER,
        });
    };
    const data = submitters.map((dataRow) => [
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
                    Showing all results ({submittersTotal})
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
                            onFilterClick(from, to);
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
                        slots={getSlots(currentPageNum, pagesTotal)}
                    />
                )}
            </section>
        </div>
    );
}
