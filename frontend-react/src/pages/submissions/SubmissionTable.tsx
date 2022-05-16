import { useController } from "rest-hooks";
import { useEffect, useMemo } from "react";

import useFilterManager from "../../hooks/filters/UseFilterManager";
import useCursorManager from "../../hooks/filters/UseCursorManager";
import SubmissionsResource from "../../resources/SubmissionsResource";
import { getStoredOrg } from "../../contexts/SessionStorageTools";
import Table, { ColumnConfig, TableConfig } from "../../components/Table/Table";
import TableFilters from "../../components/Table/TableFilters";
import usePagination, { PaginationActionType } from "../../hooks/UsePagination";

const cursorExtractor = (s: SubmissionsResource) => s.timestamp;

function SubmissionTable() {
    const filterManager = useFilterManager();
    const {
        cursors,
        hasPrev,
        hasNext,
        update: updateCursors,
    } = useCursorManager(filterManager.rangeSettings.to);

    const organization = getStoredOrg();
    const pageSize = filterManager.pageSettings.size;
    const startCursor = filterManager.selectedRange.start;
    const endCursor = filterManager.selectedRange.end;
    const sortOrder = filterManager.sortSettings.order;

    const paginationArgs = useMemo(
        () => ({
            startCursor,
            pageSize,
            cursorExtractor,
        }),
        [startCursor, pageSize]
    );
    // TODO(mreifman): When the args change, what happens when the hook re-renders?
    const { state: paginationState, dispatch: paginationDispatch } =
        usePagination(paginationArgs);
    const {
        fetchStartCursor,
        fetchCount,
        resultsPage: submissions,
    } = paginationState;
    const { fetch } = useController();
    useEffect(() => {
        fetch(SubmissionsResource.list(), {
            organization,
            cursor: fetchStartCursor,
            endCursor,
            pageSize: fetchCount,
            sort: sortOrder,
            showFailed: false,
        })
            .then((d) => {
                // TODO(mreifman): Figure out how to properly type the resolved value of the fetch.
                paginationDispatch({
                    type: PaginationActionType.SET_RESULTS,
                    payload: d as SubmissionsResource[],
                });
            })
            .catch((e) => console.log(e));
    }, [
        // TODO(mreifman): We don't want this effect to depend on the end cursor or sort order
        fetch,
        fetchStartCursor,
        fetchCount,
        paginationDispatch,
    ]);

    useEffect(() => {
        paginationDispatch({
            type: PaginationActionType.RESET,
            payload: paginationArgs,
        });
    }, [paginationArgs, paginationDispatch]);

    const transformDate = (s: string) => {
        return new Date(s).toLocaleString();
    };

    const columns: Array<ColumnConfig> = [
        {
            dataAttr: "id",
            columnHeader: "Report ID",
            link: true,
            linkBasePath: "/submissions",
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
                cursorManager={{
                    cursors,
                    hasPrev,
                    hasNext,
                    update: updateCursors,
                }}
            />
            <Table
                config={submissionsConfig}
                filterManager={filterManager}
                cursorManager={{
                    cursors,
                    hasPrev,
                    hasNext,
                    update: updateCursors,
                }}
            />
        </>
    );
}

export default SubmissionTable;
