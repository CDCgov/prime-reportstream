import { Dispatch, FC, SetStateAction } from "react";

import { getReportAndDownload } from "./ReportsUtils";
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
import {
    EventName,
    useAppInsightsContext,
} from "../../../contexts/AppInsights";
import { useSessionContext } from "../../../contexts/Session";
import { FilterManager } from "../../../hooks/filters/UseFilterManager";
import {
    DeliveriesDataAttr,
    useOrgDeliveries,
} from "../../../hooks/network/History/DeliveryHooks";
import { useOrganizationReceivers } from "../../../hooks/UseOrganizationReceivers";
import usePagination, { ResultsFetcher } from "../../../hooks/UsePagination";
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
            columnHeader: "Time received",
            sortable: true,
            transform: transformDate,
        },
        {
            dataAttr: DeliveriesDataAttr.EXPIRES,
            columnHeader: "File available until",
            sortable: true,
            transform: transformDate,
        },
        {
            dataAttr: DeliveriesDataAttr.ITEM_COUNT,
            columnHeader: "Items",
        },
        {
            dataAttr: DeliveriesDataAttr.FILE_NAME,
            columnHeader: "Filename",
            feature: {
                action: handleFetchAndDownload,
                param: DeliveriesDataAttr.REPORT_ID,
                actionButtonHandler: handleExpirationDate,
                actionButtonParam: DeliveriesDataAttr.EXPIRES,
            },
        },
        {
            dataAttr: DeliveriesDataAttr.RECEIVER,
            columnHeader: "Receiver",
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
    fetchResults,
    filterManager,
    services,
    setService,
    initialService,
}: {
    fetchResults: ResultsFetcher<any>;
    filterManager: FilterManager;
    services: RSReceiver[];
    setService?: Dispatch<SetStateAction<string | undefined>>;
    initialService: RSReceiver;
}) => {
    const { appInsights } = useAppInsightsContext();
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
                initialService={initialService}
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

export const DailyData = () => {
    const { isLoading, isDisabled, activeReceivers } =
        useOrganizationReceivers();
    const initialService = activeReceivers?.[0];
    const { fetchResults, filterManager, setService } = useOrgDeliveries(
        initialService?.name,
    );

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
            initialService={initialService}
        />
    );
};

export default DailyData;
