import React, { Dispatch, SetStateAction } from "react";

import { FeatureName } from "../../../AppRouter";
import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import usePagination from "../../../hooks/UsePagination";
import { RSReceiver } from "../../../config/endpoints/settings";
import { useOrganizationReceiversFeed } from "../../../hooks/UseOrganizationReceiversFeed";
import Spinner from "../../Spinner";
import { NoServicesBanner } from "../../alerts/NoServicesAlert";
import { FilterManager } from "../../../hooks/filters/UseFilterManager";
import { PaginationProps } from "../../Table/Pagination";
import Table, { ColumnConfig, TableConfig } from "../../Table/Table";
import TableFilters from "../../Table/TableFilters";
import {
    useOrgDeliveries,
    DataDashboardAttr,
} from "../../../hooks/network/DataDashboard/DataDashboardHooks";
import { RSDelivery } from "../../../config/endpoints/dataDashboard";

import ReceiverServices from "./ReceiverServices";

const extractCursor = (d: RSDelivery) => d.batchReadyAt;

interface DashboardTableContentProps {
    receivers: RSReceiver[];
    activeService: RSReceiver | undefined;
    handleSetActive: (v: string) => void;
    filterManager: FilterManager;
    paginationProps?: PaginationProps;
    isLoading: boolean;
    receiverList: RSDelivery[] | undefined;
}

const DashboardTableContent: React.FC<DashboardTableContentProps> = ({
    receivers,
    activeService,
    handleSetActive,
    filterManager,
    paginationProps,
    isLoading,
    receiverList,
}) => {
    if (isLoading) return <Spinner />;

    const featureEvent = `${FeatureName.DAILY_DATA} | ${EventName.TABLE_FILTER}`;

    const transformDate = (s: string) => {
        return new Date(s).toLocaleString();
    };
    const columns: Array<ColumnConfig> = [
        {
            dataAttr: DataDashboardAttr.DATE_SENT,
            columnHeader: "Date sent to you",
            sortable: true,
            transform: transformDate,
        },
        {
            dataAttr: DataDashboardAttr.PROVIDER,
            columnHeader: "Ordering Provider",
            feature: {
                link: true,
                linkBasePath: "/data-dashboard/facilities-providers/:provider",
            },
        },
        {
            dataAttr: DataDashboardAttr.FACILITY,
            columnHeader: "Performing facility",
            feature: {
                link: true,
                linkBasePath: "/data-dashboard/facilities-providers/:facility",
            },
        },
        {
            dataAttr: DataDashboardAttr.SUBMITTER,
            columnHeader: "Submitter",
            feature: {
                link: true,
                linkBasePath: "/data-dashboard/facilities-providers/:submitter",
            },
        },
        {
            dataAttr: DataDashboardAttr.REPORT_ID,
            columnHeader: "Report ID",
            feature: {
                link: true,
                linkBasePath: "/data-dashboard/report-details/:reportId",
            },
        },
    ];

    const resultsTableConfig: TableConfig = {
        columns: columns,
        rows: receiverList || [],
    };

    return (
        <>
            <div className="text-bold font-sans-md">
                Showing all results ({receiverList?.length})
            </div>
            <div className="display-flex flex-row">
                <ReceiverServices
                    receivers={receivers}
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
            <Table
                classes="margin-top-1"
                config={resultsTableConfig}
                filterManager={filterManager}
                paginationProps={paginationProps}
            />
        </>
    );
};

function DashboardTableWithPagination({
    receivers,
    activeService,
    setActiveService,
}: {
    receivers: RSReceiver[];
    activeService: RSReceiver | undefined;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
}) {
    const handleSetActive = (name: string) => {
        setActiveService(receivers.find((item) => item.name === name));
    };

    const { fetchResults, filterManager } = useOrgDeliveries(
        activeService?.name
    );
    const pageSize = filterManager.pageSettings.size;
    const sortOrder = filterManager.sortSettings.order;
    const rangeTo = filterManager.rangeSettings.to;
    const rangeFrom = filterManager.rangeSettings.from;

    const startCursor = sortOrder === "DESC" ? rangeTo : rangeFrom;
    const isCursorInclusive = sortOrder === "ASC";
    const analyticsEventName = `${FeatureName.DATA_DASHBOARD} | ${EventName.TABLE_PAGINATION}`;

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
        paginationProps.label = "Dashboard pagination";
    }

    return (
        <>
            <DashboardTableContent
                receivers={receivers}
                activeService={activeService}
                handleSetActive={handleSetActive}
                filterManager={filterManager}
                paginationProps={paginationProps}
                isLoading={isLoading}
                receiverList={serviceReportsList}
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
                <DashboardTableWithPagination
                    receivers={services}
                    activeService={activeService}
                    setActiveService={setActiveService}
                />
            )}
        </>
    );
}
