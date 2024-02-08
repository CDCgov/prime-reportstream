import React, { FC, useState } from "react";

import Table, {
    ColumnConfig,
    TableConfig,
} from "../../../components/Table/Table";
import { FilterManager } from "../../../hooks/filters/UseFilterManager";
import { useSessionContext } from "../../../contexts/Session";
import {
    useOrgDeliveries,
    DeliveriesDataAttr,
} from "../../../hooks/network/History/DeliveryHooks";
import Spinner from "../../../components/Spinner";
import TableFilters, {
    TableFilterDateLabel,
} from "../../../components/Table/TableFilters";
import { PaginationProps } from "../../../components/Table/Pagination";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import usePagination from "../../../hooks/UsePagination";
import { NoServicesBanner } from "../../../components/alerts/NoServicesAlert";
import { RSReceiver } from "../../../config/endpoints/settings";
import { FeatureName } from "../../../utils/FeatureName";
import AdminFetchAlert from "../../../components/alerts/AdminFetchAlert";
import { isDateExpired } from "../../../utils/DateTimeUtils";
import {
    EventName,
    useAppInsightsContext,
} from "../../../contexts/AppInsights";
import { useOrganizationReceivers } from "../../../hooks/UseOrganizationReceivers";

import { getReportAndDownload } from "./ReportsUtils";

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
    const columns: Array<ColumnConfig> = [
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
        rows: serviceReportsList || [],
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
    services,
    activeReceiver,
    setActiveReceiver,
}: {
    services: RSReceiver[];
    activeReceiver: RSReceiver | undefined;
    setActiveReceiver: (receiver: RSReceiver) => void;
}) => {
    const { appInsights } = useAppInsightsContext();
    const featureEvent = `${FeatureName.DAILY_DATA} | ${EventName.TABLE_FILTER}`;
    const handleSetActiveService = (name: string) => {
        const result = services.find((item) => item.name === name);
        if (result) setActiveReceiver(result);
    };

    const { fetchResults, filterManager } = useOrgDeliveries(
        activeReceiver?.name,
    );

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
    const results = async () => await fetchResults(startCursor, 100);
    results()
        .then((data) => {
            console.log("Results:", data);
        })
        .catch((error) => {
            console.error("Error fetching results:", error);
        });

    if (paginationProps) {
        paginationProps.label = "Deliveries pagination";
    }

    const receiverDropdown = [
        ...new Set(
            services?.map((data) => {
                return data.name;
            }),
        ),
    ].map((receiver) => {
        return { value: receiver, label: receiver };
    });
    return (
        <>
            <section className="bg-blue-5 padding-2">
                <p className="text-bold">
                    View data from a specific receiver or date and time range
                </p>

                <TableFilters
                    receivers={receiverDropdown}
                    startDateLabel={TableFilterDateLabel.START_DATE}
                    endDateLabel={TableFilterDateLabel.END_DATE}
                    showDateHints={true}
                    filterManager={filterManager}
                    handleSetActiveService={handleSetActiveService}
                    onFilterClick={({
                        from,
                        to,
                    }: {
                        from: string;
                        to: string;
                    }) =>
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
                />
            </section>
            <DeliveriesTable
                filterManager={filterManager}
                paginationProps={paginationProps}
                isLoading={isLoading}
                serviceReportsList={serviceReportsList}
            />
        </>
    );
};

export const DailyData = () => {
    const { isLoading, isDisabled, activeReceivers } =
        useOrganizationReceivers();
    const [activeReceiver, setActiveReceiver] = useState(activeReceivers?.[0]);

    if (isLoading) return <Spinner />;

    if (isDisabled) {
        return <AdminFetchAlert />;
    }

    if (!isLoading && !activeReceiver)
        return (
            <div className="usa-section margin-bottom-5">
                <NoServicesBanner />
            </div>
        );
    return (
        <DeliveriesFilterAndTable
            services={activeReceivers!!}
            activeReceiver={activeReceiver}
            setActiveReceiver={setActiveReceiver}
        />
    );
};

export default DailyData;
