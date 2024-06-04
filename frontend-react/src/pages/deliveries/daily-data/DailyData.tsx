import { Dispatch, FC, SetStateAction } from "react";

import AdminFetchAlert from "../../../components/alerts/AdminFetchAlert";
import { NoServicesBanner } from "../../../components/alerts/NoServicesAlert";
import Spinner from "../../../components/Spinner";
import Pagination, {
    PaginationProps,
} from "../../../components/Table/Pagination";
import TableFilters, {
    TableFilterDateLabel,
} from "../../../components/Table/TableFilters";
import { USLink } from "../../../components/USLink";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import { RSReceiver } from "../../../config/endpoints/settings";
import useOrgDeliveries, {
    DeliveriesDataAttr,
} from "../../../hooks/api/deliveries/UseOrgDeliveries/UseOrgDeliveries";
import useOrganizationReceivers from "../../../hooks/api/organizations/UseOrganizationReceivers/UseOrganizationReceivers";
import { FilterManager } from "../../../hooks/filters/UseFilterManager/UseFilterManager";
import { SortSettingsActionType } from "../../../hooks/filters/UseSortOrder/UseSortOrder";
import useAppInsightsContext from "../../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import usePagination, {
    getSlots,
    ResultsFetcher,
} from "../../../hooks/UsePagination/UsePagination";
import { Table } from "../../../shared";
import ReportFileDownloadButton from "../../../shared/ReportFileDownloadButton/ReportFileDownloadButton";
import { EventName } from "../../../utils/AppInsights";
import { isDateExpired } from "../../../utils/DateTimeUtils";
import { FeatureName } from "../../../utils/FeatureName";

const extractCursor = (d: RSDelivery) => d.batchReadyAt;

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
    const transformDate = (s: string) => {
        return new Date(s).toLocaleString();
    };
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
    if (isLoading || !serviceReportsList || !paginationProps)
        return <Spinner />;

    const data = serviceReportsList.map(
        ({
            reportId,
            expires,
            batchReadyAt,
            reportItemCount,
            fileName,
            receiver,
        }) => [
            {
                columnKey: DeliveriesDataAttr.REPORT_ID,
                columnHeader: "Report ID",
                content: (
                    <USLink href={`/report-details/${reportId}`}>
                        {reportId}
                    </USLink>
                ),
            },
            {
                columnKey: DeliveriesDataAttr.BATCH_READY,
                columnHeader: "Time received",
                content: (
                    <span className="text-no-wrap">
                        {transformDate(batchReadyAt)}
                    </span>
                ),
                columnCustomSort: () =>
                    onColumnCustomSort(DeliveriesDataAttr.BATCH_READY),
                columnCustomSortSettings: filterManager.sortSettings,
            },
            {
                columnKey: DeliveriesDataAttr.EXPIRES,
                columnHeader: "File available until",
                content: (
                    <span className="text-no-wrap">
                        {transformDate(expires)}
                    </span>
                ),
                columnCustomSort: () =>
                    onColumnCustomSort(DeliveriesDataAttr.EXPIRES),
                columnCustomSortSettings: filterManager.sortSettings,
            },
            {
                columnKey: DeliveriesDataAttr.ITEM_COUNT,
                columnHeader: "Items",
                content: reportItemCount,
            },
            {
                columnKey: DeliveriesDataAttr.FILE_NAME,
                columnHeader: "Filename",
                content: (
                    <>
                        {!isDateExpired(expires) ? (
                            <ReportFileDownloadButton
                                unstyled
                                reportId={reportId}
                                className="font-mono-2xs line-height-alt-4"
                            >
                                {fileName}
                            </ReportFileDownloadButton>
                        ) : (
                            <div>{fileName}</div>
                        )}
                    </>
                ),
            },
            {
                columnKey: DeliveriesDataAttr.RECEIVER,
                columnHeader: "Receiver",
                content: (
                    <span className="text-no-wrap">
                        {receiver.includes(".")
                            ? receiver.split(/\.(.*)/)[1]
                            : receiver}
                    </span>
                ),
            },
        ],
    );

    return (
        <>
            <Table apiSortable borderless striped rowData={data} />
            {data.length && (
                <Pagination
                    currentPageNum={paginationProps.currentPageNum}
                    setSelectedPage={paginationProps.setSelectedPage}
                    slots={getSlots(
                        paginationProps.currentPageNum,
                        paginationProps.slots.length,
                    )}
                />
            )}
        </>
    );
};

const DeliveriesFilterAndTable = ({
    fetchResults,
    filterManager,
    services,
    setService,
}: {
    fetchResults: ResultsFetcher<any>;
    filterManager: FilterManager;
    services: RSReceiver[];
    setService?: Dispatch<SetStateAction<string>>;
}) => {
    const appInsights = useAppInsightsContext();
    const featureEvent = `${FeatureName.DAILY_DATA} | ${EventName.TABLE_FILTER}`;
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
        setSearchTerm,
        searchTerm,
    } = usePagination<RSDelivery>({
        startCursor,
        isCursorInclusive,
        pageSize,
        fetchResults,
        extractCursor,
        analyticsEventName,
    });

    const receiverDropdown = [
        ...new Set(
            services.map((data) => {
                return data.name;
            }),
        ),
    ].map((receiver) => {
        return { value: receiver, label: receiver };
    });

    return (
        <>
            <TableFilters
                receivers={receiverDropdown}
                startDateLabel={TableFilterDateLabel.START_DATE}
                endDateLabel={TableFilterDateLabel.END_DATE}
                showDateHints={true}
                filterManager={filterManager}
                setSearchTerm={setSearchTerm}
                searchTerm={searchTerm}
                setService={setService}
                onFilterClick={({ from, to }: { from: string; to: string }) =>
                    appInsights?.trackEvent({
                        name: featureEvent,
                        properties: {
                            tableFilter: {
                                startRange: from,
                                endRange: to,
                            },
                        },
                    })
                }
                resultLength={paginationProps?.resultLength}
                isPaginationLoading={paginationProps?.isPaginationLoading}
            />
            {services.length === 0 ? (
                <div className="usa-section margin-bottom-5">
                    <NoServicesBanner />
                </div>
            ) : (
                <DeliveriesTable
                    filterManager={filterManager}
                    paginationProps={paginationProps}
                    isLoading={isLoading}
                    serviceReportsList={serviceReportsList}
                />
            )}
        </>
    );
};

export function DailyData() {
    const { isLoading, isDisabled, activeReceivers } =
        useOrganizationReceivers();
    const { fetchResults, filterManager, setService } = useOrgDeliveries();

    if (isLoading) return <Spinner />;

    if (isDisabled) {
        return <AdminFetchAlert />;
    }
    return (
        <DeliveriesFilterAndTable
            fetchResults={fetchResults}
            filterManager={filterManager}
            setService={setService}
            services={activeReceivers}
        />
    );
}

export default DailyData;
