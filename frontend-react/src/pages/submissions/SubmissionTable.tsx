import { useResource } from "rest-hooks";
import { useEffect } from "react";

import useFilterManager from "../../hooks/filters/UseFilterManager";
import useCursorManager from "../../hooks/filters/UseCursorManager";
import SubmissionsResource from "../../resources/SubmissionsResource";
import { getStoredOrg } from "../../contexts/SessionStorageTools";
import Table, { ColumnConfig, TableConfig } from "../../components/Table/Table";

import SubmissionFilters from "./SubmissionFilters";

/* TODO: This component needs to be able to sort ASC and DESC
 *   while being properly paginated. */

function SubmissionTable() {
    const filterManager = useFilterManager();
    const cursorManager = useCursorManager(
        filterManager.startRange.toISOString()
    ); // First cursor set to StartRange on load

    /* Our API call! Updates when any of the given state variables update */
    const submissions: SubmissionsResource[] = useResource(
        SubmissionsResource.list(),
        {
            organization: getStoredOrg(),
            cursor: cursorManager.values.cursor,
            endCursor: filterManager.endRange.toISOString(),
            pageSize: filterManager.count + 1, // Pulls +1 to check for next page
            sort: filterManager.order,
            showFailed: false, // No plans for this to be set to true
        }
    );

    /* Effect to add next cursor whenever submissions returns a new array */
    useEffect(() => {
        const nextCursor =
            submissions[filterManager.count]?.timestamp || undefined;
        if (nextCursor) {
            cursorManager.controller.addNextCursor(nextCursor);
        }
    }, [submissions, filterManager.count, cursorManager.controller]);

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
            // sortable: true,
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
