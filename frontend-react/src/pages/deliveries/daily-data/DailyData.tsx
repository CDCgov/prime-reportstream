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
import useSessionContext from "../../../contexts/Session/useSessionContext";
import useOrgDeliveries, {
    DeliveriesDataAttr,
} from "../../../hooks/api/deliveries/UseOrgDeliveries/UseOrgDeliveries";
import useOrganizationReceivers from "../../../hooks/api/organizations/UseOrganizationReceivers/UseOrganizationReceivers";
import { FilterManager } from "../../../hooks/filters/UseFilterManager/UseFilterManager";
import useAppInsightsContext from "../../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import usePagination, {
    ResultsFetcher,
} from "../../../hooks/UsePagination/UsePagination";
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

const DeliveriesFilterAndTable = ({ services }: { services: RSReceiver[] }) => {
    const { fetchResults, filterManager, setService } = useOrgDeliveries();
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

    if (paginationProps) {
        paginationProps.label = "Pagination";
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

    if (isLoading) return <Spinner />;

    if (isDisabled) {
        return <AdminFetchAlert />;
    }
    return <DeliveriesFilterAndTable services={activeReceivers} />;
}

export default DailyData;
