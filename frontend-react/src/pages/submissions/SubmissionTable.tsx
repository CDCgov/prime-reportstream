import { useResource } from "rest-hooks";
import { useEffect } from "react";

import useFilterManager from "../../hooks/UseFilterManager";
import useCursorManager from "../../hooks/UseCursorManager";
import SubmissionsResource from "../../resources/SubmissionsResource";
import { getStoredOrg } from "../../contexts/SessionStorageTools";
import Table, { ColumnConfig, TableConfig } from "../../components/Table/Table";

import SubmissionFilters from "./SubmissionFilters";

function SubmissionTable() {
    const filterManager = useFilterManager({
        sort: {
            column: "timestamp",
            order: "DESC",
        },
    });
    const cursorManager = useCursorManager();

    /* Our API call! Updates when any of the given state variables update */
    const submissions: SubmissionsResource[] = useResource(
        SubmissionsResource.list(),
        {
            organization: getStoredOrg(),
            cursor: cursorManager.values.cursor,
            endCursor: filterManager.filters.endRange,
            pageSize: filterManager.filters.pageSize + 1, // Pulls +1 to check for next page
            sort: filterManager.filters.sort.order,
            showFailed: false, // No plans for this to be set to true
        }
    );

    /* Effect to add next cursor whenever submissions returns a new array */
    useEffect(() => {
        const nextCursor =
            submissions[filterManager.filters.pageSize]?.timestamp || undefined;

        // Ensures first page cursor is always the start of your range
        if (cursorManager.values.currentIndex === 0) {
            cursorManager.controller.reset(filterManager.filters.startRange);
        }
        if (nextCursor) cursorManager.controller.addNextCursor(nextCursor);
    }, [submissions, cursorManager, filterManager]);

    const transformDate = (s: string) => {
        return new Date(s).toLocaleString();
    };

    const columns: Array<ColumnConfig> = [
        { dataAttr: "id", columnHeader: "Report ID", link: true },
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
            <SubmissionFilters
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
}

export default SubmissionTable;
