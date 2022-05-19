import { useController } from "rest-hooks";
import { useCallback } from "react";

import useFilterManager from "../../hooks/filters/UseFilterManager";
import useCursorManager from "../../hooks/filters/UseCursorManager";
import SubmissionsResource from "../../resources/SubmissionsResource";
import { getStoredOrg } from "../../contexts/SessionStorageTools";
import Table, { ColumnConfig, TableConfig } from "../../components/Table/Table";
import TableFilters from "../../components/Table/TableFilters";
import usePagination from "../../hooks/UsePagination";

const extractCursor = (s: SubmissionsResource) => s.timestamp;

function SubmissionTable() {
    const filterManager = useFilterManager();
    const {
        cursors,
        hasPrev,
        hasNext,
        update: updateCursors,
    } = useCursorManager(filterManager.rangeSettings.to);

    const organization = getStoredOrg();
    const pageSize = 2; // filterManager.pageSettings.size;
    const startCursor = filterManager.selectedRange.start;
    const endCursor = filterManager.selectedRange.end;
    const sortOrder = filterManager.sortSettings.order;

    const { fetch: controllerFetch } = useController();
    const fetchResults = useCallback(
        (fetchStartCursor: string, fetchCount: number) => {
            return controllerFetch(SubmissionsResource.list(), {
                organization,
                cursor: fetchStartCursor,
                endCursor,
                pageSize: fetchCount,
                sort: sortOrder,
                showFailed: false,
            }) as unknown as Promise<SubmissionsResource[]>;
        },
        [organization, endCursor, sortOrder, controllerFetch]
    );

    const { resultsPage: submissions, paginationProps } =
        usePagination<SubmissionsResource>({
            startCursor,
            pageSize,
            fetchResults,
            extractCursor,
        });

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
        pagination: paginationProps,
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
