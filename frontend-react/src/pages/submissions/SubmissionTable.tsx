import { NetworkErrorBoundary, useController, useResource } from "rest-hooks";
import { Suspense, useCallback, useEffect } from "react";

import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import usePagination from "../../hooks/UsePagination";
import { RangeField } from "../../hooks/filters/UseDateRange";
import useFilterManager, {
    cursorOrRange,
    FilterManager,
    FilterManagerDefaults,
} from "../../hooks/filters/UseFilterManager";
import useCursorManager, {
    CursorActionType,
    CursorManager,
} from "../../hooks/filters/UseCursorManager";
import Table, { ColumnConfig, TableConfig } from "../../components/Table/Table";
import TableFilters from "../../components/Table/TableFilters";
import { PaginationProps } from "../../components/Table/Pagination";
import { getStoredOrg } from "../../contexts/SessionStorageTools";
import {
    CheckFeatureFlag,
    FeatureFlagName,
} from "../../pages/misc/FeatureFlags";
import SubmissionsResource from "../../resources/SubmissionsResource";

const extractCursor = (s: SubmissionsResource) => s.timestamp;

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "timestamp",
        order: "DESC",
    },
};

interface SubmissionTableContentProps {
    cursorManager?: CursorManager;
    filterManager: FilterManager;
    paginationProps?: PaginationProps;
    submissions: SubmissionsResource[];
}

const SubmissionTableContent: React.FC<SubmissionTableContentProps> = ({
    cursorManager,
    filterManager,
    paginationProps,
    submissions,
}) => {
    const transformDate = (s: string) => {
        return new Date(s).toLocaleString();
    };

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
                cursorManager={cursorManager}
            />
            <Table
                config={submissionsConfig}
                filterManager={filterManager}
                cursorManager={cursorManager}
                paginationProps={paginationProps}
            />
        </>
    );
};

function SubmissionTableWithCursorManager() {
    const filterManager = useFilterManager(filterManagerDefaults);
    const cursorManager = useCursorManager(filterManager.rangeSettings.to);

    /* Our API call! Updates when any of the given state variables update.
     * The logical swap of cursors and range value is to account for which end of the
     * range needs to update when paginating with a specific sort order.
     *
     * DESC -> Start [ -> ] End (Start uses cursor to increment towards end)
     * ASC -> Start [ <- ] End (End uses cursor to increment towards start)
     */
    const submissions: SubmissionsResource[] = useResource(
        SubmissionsResource.list(),
        {
            organization: getStoredOrg(),
            cursor: cursorOrRange(
                filterManager.sortSettings.order,
                RangeField.TO,
                cursorManager.cursors.current,
                filterManager.rangeSettings.to
            ),
            endCursor: cursorOrRange(
                filterManager.sortSettings.order,
                RangeField.FROM,
                cursorManager.cursors.current,
                filterManager.rangeSettings.from
            ),
            pageSize: filterManager.pageSettings.size + 1, // Pulls +1 to check for next page
            sort: filterManager.sortSettings.order,
            showFailed: false, // No plans for this to be set to true
        }
    );

    /* Effect to add next cursor whenever submissions returns a new array */
    const updateCursor = cursorManager.update;
    useEffect(() => {
        const nextCursor =
            submissions[filterManager.pageSettings.size]?.timestamp ||
            undefined;
        if (nextCursor) {
            updateCursor({
                type: CursorActionType.ADD_NEXT,
                payload: nextCursor,
            });
        }
    }, [submissions, filterManager.pageSettings.size, updateCursor]);

    return (
        <SubmissionTableContent
            cursorManager={cursorManager}
            filterManager={filterManager}
            submissions={submissions}
        />
    );
}

function SubmissionTableWithNumberedPagination() {
    const organization = getStoredOrg();

    const filterManager = useFilterManager(filterManagerDefaults);
    const pageSize = filterManager.pageSettings.size;
    const sortOrder = filterManager.sortSettings.order;
    const rangeTo = filterManager.rangeSettings.to;
    const rangeFrom = filterManager.rangeSettings.from;

    const { fetch: controllerFetch } = useController();
    const fetchResults = useCallback(
        (currentCursor: string, numResults: number) => {
            // The `cursor` and `endCursor` parameters are always the high and
            // low values of the range, respectively. When the results are in
            // descending order we move the high value and keep the low value
            // constant; in ascending order we keep the high value constant and
            // move the low value.
            const cursor = sortOrder === "DESC" ? currentCursor : rangeTo;
            const endCursor = sortOrder === "DESC" ? rangeFrom : currentCursor;
            return controllerFetch(SubmissionsResource.list(), {
                organization,
                cursor,
                endCursor,
                pageSize: numResults,
                sort: sortOrder,
                showFailed: false,
            }) as unknown as Promise<SubmissionsResource[]>;
        },
        [organization, sortOrder, controllerFetch, rangeFrom, rangeTo]
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
    // exclusive: the requst will return results whose cursor values are < the
    // cursor.
    const isCursorInclusive = sortOrder === "ASC";

    const {
        currentPageResults: submissions,
        paginationProps,
        isLoading,
    } = usePagination<SubmissionsResource>({
        startCursor,
        isCursorInclusive,
        pageSize,
        fetchResults,
        extractCursor,
    });

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

function SubmissionTable() {
    const isNumberedPaginationOn = CheckFeatureFlag(
        FeatureFlagName.NUMBERED_PAGINATION
    );
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="message" />}
        >
            <Suspense fallback={<Spinner />}>
                {isNumberedPaginationOn && (
                    <SubmissionTableWithNumberedPagination />
                )}
                {!isNumberedPaginationOn && (
                    <SubmissionTableWithCursorManager />
                )}
            </Suspense>
        </NetworkErrorBoundary>
    );
}

export default SubmissionTable;
