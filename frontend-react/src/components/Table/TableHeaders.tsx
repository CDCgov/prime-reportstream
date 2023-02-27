/* Renders the header row of the table from columns.values() */
import React from "react";
import { Icon } from "@trussworks/react-uswds";

import { FilterManager } from "../../hooks/filters/UseFilterManager";
import {
    SortOrder,
    SortSettingsActionType,
} from "../../hooks/filters/UseSortOrder";

import { ColumnConfig, TableConfig } from "./Table";

interface TableHeaderProps {
    config: TableConfig;
    filterManager: FilterManager | undefined;
    enableEditableRows: boolean | undefined;
}
export const TableHeaders = ({
    filterManager,
    config,
    enableEditableRows,
}: TableHeaderProps) => {
    const isSortedColumn = (colConfig: ColumnConfig) =>
        colConfig.sortable &&
        filterManager?.sortSettings.column === colConfig.dataAttr;

    const renderArrow = () => {
        const { order, localOrder, locally } = filterManager?.sortSettings || {
            order: "DESC",
            locally: false,
            localOrder: "DESC",
        };

        const isOrder = (sortOrder: SortOrder) =>
            order === sortOrder || (locally && localOrder === sortOrder);

        if (filterManager && isOrder("ASC")) {
            return <Icon.ArrowUpward />;
        } else if (filterManager && isOrder("DESC")) {
            return <Icon.ArrowDownward />;
        }
    };

    const swapSort = (currentColumn: ColumnConfig) => {
        if (currentColumn.localSort) {
            // Sets local sort to true and swaps local sort order
            filterManager?.updateSort({
                type: SortSettingsActionType.APPLY_LOCAL_SORT,
                payload: {
                    locally: true,
                },
            });
            filterManager?.updateSort({
                type: SortSettingsActionType.SWAP_LOCAL_ORDER,
            });
        } else {
            // Sets local sort to false and swaps the order
            filterManager?.updateSort({
                type: SortSettingsActionType.APPLY_LOCAL_SORT,
                payload: {
                    locally: false,
                },
            });
            filterManager?.updateSort({
                type: SortSettingsActionType.SWAP_ORDER,
            });
        }
        filterManager?.updateSort({
            type: SortSettingsActionType.CHANGE_COL,
            payload: {
                column: currentColumn.dataAttr,
            },
        });
    };

    return (
        <thead>
            <tr>
                {config.columns?.map((colConfig) => {
                    if (colConfig.sortable && filterManager) {
                        return (
                            <th
                                className="rs-sortable-header"
                                key={colConfig.columnHeader}
                                onClick={() => {
                                    // Swaps the order and set column
                                    swapSort(colConfig);
                                }}
                            >
                                {colConfig.columnHeader}
                                {isSortedColumn(colConfig)
                                    ? renderArrow()
                                    : null}
                            </th>
                        );
                    } else {
                        return (
                            <th key={colConfig.columnHeader}>
                                {colConfig.columnHeader}
                            </th>
                        );
                    }
                })}
                {enableEditableRows ? (
                    // This extends the header bottom border to cover this column
                    <th key={"edit"}>{""}</th>
                ) : null}
            </tr>
        </thead>
    );
};
