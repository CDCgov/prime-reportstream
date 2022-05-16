import { useResource } from "rest-hooks";
import { useEffect } from "react";

import useFilterManager, {
    cursorOrRange,
} from "../../hooks/filters/UseFilterManager";
import useCursorManager, {
    CursorActionType,
} from "../../hooks/filters/UseCursorManager";
import SubmissionsResource from "../../resources/SubmissionsResource";
import { getStoredOrg } from "../../contexts/SessionStorageTools";
import Table, { ColumnConfig, TableConfig } from "../../components/Table/Table";
import TableFilters from "../../components/Table/TableFilters";
import { RangeField } from "../../hooks/filters/UseDateRange";

function SubmissionTable() {
    const filterManager = useFilterManager();
    const {
        cursors,
        hasPrev,
        hasNext,
        update: updateCursors,
    } = useCursorManager(filterManager.rangeSettings.to);

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
                cursors.current,
                filterManager.rangeSettings.to
            ),
            endCursor: cursorOrRange(
                filterManager.sortSettings.order,
                RangeField.FROM,
                cursors.current,
                filterManager.rangeSettings.from
            ),
            pageSize: filterManager.pageSettings.size + 1, // Pulls +1 to check for next page
            sort: filterManager.sortSettings.order,
            showFailed: false, // No plans for this to be set to true
        }
    );

    /* Effect to add next cursor whenever submissions returns a new array */
    useEffect(() => {
        const nextCursor =
            submissions[filterManager.pageSettings.size]?.timestamp ||
            undefined;
        if (nextCursor) {
            updateCursors({
                type: CursorActionType.ADD_NEXT,
                payload: nextCursor,
            });
        }
    }, [submissions, filterManager.pageSettings.size, updateCursors]);

    const transformDate = (s: string) => {
        return new Date(s).toLocaleString();
    };

    const columns: Array<ColumnConfig> = [
        {
            dataAttr: "id",
            columnHeader: "Report ID",
            link: true,
            linkBasePath: "/submissions/",
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
