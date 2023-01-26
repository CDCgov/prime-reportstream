import React, { useCallback, useEffect, useState } from "react";

import Spinner from "../../components/Spinner";
import usePagination from "../../hooks/UsePagination";
import useFilterManager, {
    FilterManager,
    FilterManagerDefaults,
} from "../../hooks/filters/UseFilterManager";
import Table, { ColumnConfig, TableConfig } from "../../components/Table/Table";
import TableFilters from "../../components/Table/TableFilters";
import { PaginationProps } from "../../components/Table/Pagination";
import { useSessionContext } from "../../contexts/SessionContext";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";
import { EventName, trackAppInsightEvent } from "../../utils/Analytics";
import { FeatureName } from "../../AppRouter";
import { useOrganizationSubmissions } from "../../hooks/api/Deliveries/UseOrganizationSubmissions";

const extractCursor = (s: OrganizationSubmission) => s.timestamp;

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "timestamp",
        order: "DESC",
    },
};

interface SubmissionTableContentProps {
    filterManager: FilterManager;
    paginationProps?: PaginationProps;
    submissions: OrganizationSubmission[];
}

function transformDate(s: string) {
    return new Date(s).toLocaleString();
}

const SubmissionTableContent = ({
    filterManager,
    paginationProps,
    submissions,
}: SubmissionTableContentProps) => {
    const analyticsEventName = `${FeatureName.SUBMISSIONS} | ${EventName.TABLE_FILTER}`;
    const columns: Array<ColumnConfig> = [
        {
            dataAttr: "id",
            columnHeader: "Report ID",
            feature: {
                link: true,
                linkBasePath: "/submissions/",
            },
        },
        {
            dataAttr: "timestamp",
            columnHeader: "Date/time submitted",
            sortable: true,
            transform: transformDate,
        },
        { dataAttr: "externalName", columnHeader: "File" },
        { dataAttr: "reportItemCount", columnHeader: "Records" },
        {
            dataAttr: "httpStatus",
            columnHeader: "Status",
            valueMap: new Map([[201, "Success"]]),
        },
    ];

    const submissionsConfig: TableConfig = {
        columns: columns,
        rows: submissions,
    };

    return (
        <>
            <TableFilters
                filterManager={filterManager}
                onFilterClick={({ from, to }: { from: string; to: string }) =>
                    trackAppInsightEvent(analyticsEventName, {
                        tableFilter: { startRange: from, endRange: to },
                    })
                }
            />
            <Table
                config={submissionsConfig}
                filterManager={filterManager}
                paginationProps={paginationProps}
            />
        </>
    );
};

function SubmissionTableWithNumberedPagination() {
    const { activeMembership } = useSessionContext();

    const filterManager = useFilterManager(filterManagerDefaults);
    const pageSize = filterManager.pageSettings.size;
    const sortOrder = filterManager.sortSettings.order;
    const rangeTo = filterManager.rangeSettings.to;
    const rangeFrom = filterManager.rangeSettings.from;
    const [pageParams, setPageParams] = useState({
        organization: activeMembership?.parsedName,
        cursor: "",
        since: rangeFrom,
        until: rangeTo,
        pageSize: 0,
        sortdir: sortOrder,
        showFailed: false,
    });

    const { data } = useOrganizationSubmissions(activeMembership?.parsedName, {
        params: pageParams,
        enabled: !!pageParams.cursor && !!pageParams.pageSize,
    });

    // TODO: Integrate pagination into hook
    const [__TEMPORARY_PAGINATION_FIX, set__TEMPORARY_PAGINATION_FIX] =
        useState<(...args: any) => void>();

    const fetchResults = useCallback(
        async (currentCursor: string, numResults: number) => {
            return new Promise<OrganizationSubmission[]>((resolve, reject) => {
                const onResults = (
                    results: OrganizationSubmission[] | Error
                ) => {
                    if ("length" in results) {
                        resolve(results);
                    }
                    reject(results);
                };
                set__TEMPORARY_PAGINATION_FIX(() => onResults);
                setPageParams((v) => ({
                    ...v,
                    cursor: currentCursor,
                    pageSize: numResults,
                }));
            });
        },
        []
    );

    // The start cursor is the high value when results are in descending order
    // and the low value when the results are in ascending order.
    const startCursor =
        sortOrder === "DESC"
            ? filterManager.rangeSettings.to
            : filterManager.rangeSettings.from;
    // The API treats the request range as the interval [from, to).
    // When we move the `endCursor` value in ascending requests, the cursor is
    // inclusive: the request will return results whose cursor values are >= the
    // cursor.
    // When we move the `cursor` value in descending requests, the cursor is
    // exclusive: the request will return results whose cursor values are < the
    // cursor.
    const isCursorInclusive = sortOrder === "ASC";
    const analyticsEventName = `${FeatureName.SUBMISSIONS} | ${EventName.TABLE_PAGINATION}`;

    const {
        currentPageResults: submissions,
        paginationProps,
        isLoading,
    } = usePagination<OrganizationSubmission>({
        startCursor,
        isCursorInclusive,
        pageSize,
        fetchResults,
        extractCursor,
        analyticsEventName,
    });

    // On new data, if we're waiting for a callback then callback
    // and clear callback state
    useEffect(() => {
        if (__TEMPORARY_PAGINATION_FIX && data) {
            __TEMPORARY_PAGINATION_FIX(data);
            set__TEMPORARY_PAGINATION_FIX(undefined);
        }
    }, [__TEMPORARY_PAGINATION_FIX, data]);

    if (isLoading) {
        return <Spinner />;
    }

    if (paginationProps) {
        paginationProps.label = "Submissions pagination";
    }

    return (
        <SubmissionTableContent
            filterManager={filterManager}
            paginationProps={paginationProps}
            submissions={submissions}
        />
    );
}

const SubmissionTable = () =>
    withCatchAndSuspense(<SubmissionTableWithNumberedPagination />);

export default SubmissionTable;
