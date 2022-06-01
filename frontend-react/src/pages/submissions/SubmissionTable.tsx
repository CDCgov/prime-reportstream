import { NetworkErrorBoundary, useResource } from "rest-hooks";
import { Suspense, useEffect } from "react";

import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
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
import Pagination, {
    OVERFLOW_INDICATOR,
    SlotItem,
} from "../../components/Table/Pagination";
import { getStoredOrg } from "../../contexts/SessionStorageTools";
import {
    CheckFeatureFlag,
    FeatureFlagName,
} from "../../pages/misc/FeatureFlags";
import SubmissionsResource from "../../resources/SubmissionsResource";

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "timestamp",
        order: "DESC",
    },
};

interface SubmissionTableContentProps {
    cursorManager: CursorManager;
    filterManager: FilterManager;
    submissions: SubmissionsResource[];
}

const SubmissionTableContent: React.FC<SubmissionTableContentProps> = ({
    cursorManager,
    filterManager,
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
    const slots: SlotItem[] = [1, 2, 3, 4, 5, 6, OVERFLOW_INDICATOR];
    const currentPageNum = 2;
    return (
        <>
            <div>TK</div>
            <Pagination
                currentPageNum={currentPageNum}
                setCurrentPage={(p: number) =>
                    console.log("Set current page:", p)
                }
                slots={slots}
            />
        </>
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
