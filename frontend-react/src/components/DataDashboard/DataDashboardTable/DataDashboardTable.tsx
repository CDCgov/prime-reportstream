import React, { Dispatch, SetStateAction } from "react";

import { FeatureName } from "../../../AppRouter";
import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import { RSReceiver } from "../../../config/endpoints/settings";
import { useOrganizationReceiversFeed } from "../../../hooks/UseOrganizationReceiversFeed";
import Spinner from "../../Spinner";
import { NoServicesBanner } from "../../alerts/NoServicesAlert";
import Pagination from "../../Table/Pagination";
import TableFilters from "../../Table/TableFilters";
import ReceiverServices from "../ReceiverServices/ReceiverServices";
import useReceiverDeliveries from "../../../hooks/network/DataDashboard/UseReceiverDeliveries";
import AdminFetchAlert from "../../alerts/AdminFetchAlert";
import { Table } from "../../../shared/Table/Table";
import { getSlots } from "../../../hooks/UsePagination";
import { PageSettingsActionType } from "../../../hooks/filters/UsePages";
import { SortSettingsActionType } from "../../../hooks/filters/UseSortOrder";
import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";
import { transformColumnIDtoTitle } from "../../../utils/misc";

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
    const {
        data: results,
        filterManager,
        isLoading: isDeliveriesLoading,
    } = useReceiverDeliveries(activeService.name);

    if (isDeliveriesLoading || !results) return <Spinner />;

    const data = results?.data.map((dataRow) =>
        Object.entries(dataRow).map((cell) => ({
            columnKey: cell[0],
            columnHeader: transformColumnIDtoTitle(cell[0]),
            content:
                isNaN(cell[1]) && !isNaN(Date.parse(cell[1]))
                    ? formatDateWithoutSeconds(cell[1])
                    : cell[1],
            columnCustomSort: () => {
                filterManager?.updateSort({
                    type: SortSettingsActionType.CHANGE_COL,
                    payload: {
                        column: cell[0],
                    },
                });
                filterManager?.updateSort({
                    type: SortSettingsActionType.SWAP_ORDER,
                });
            },
            columnCustomSortSettings: filterManager.sortSettings,
        }))
    );

    const currentPageNum = filterManager.pageSettings.currentPage;

    return (
        <>
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
                    }) =>
                        trackAppInsightEvent(featureEvent, {
                            tableFilter: { startRange: from, endRange: to },
                        })
                    }
                />
            </div>
            <Table apiSortable borderless rowData={data} />
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
