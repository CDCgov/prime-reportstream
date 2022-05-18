/* Makes row objects string-indexed */
import {
    Button,
    IconArrowDownward,
    IconArrowUpward,
    IconNavigateBefore,
    IconNavigateNext,
} from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";
import React, { useMemo } from "react";

import {
    CursorActionType,
    CursorManager,
} from "../../hooks/filters/UseCursorManager";
import { FilterManager } from "../../hooks/filters/UseFilterManager";
import {
    SortSettings,
    SortSettingsActionType,
} from "../../hooks/filters/UseSortOrder";

export interface TableRow {
    [key: string]: any;
}

export interface ActionableColumn {
    action: Function;
    param?: string;
}

/* ColumnConfig tells the Table element how to render each column
 *
 * @property dataAttr: Name of the object attribute to be rendered in the column
 * @property columnHeader: The column name
 * @property sortable: React.SetStateAction function
 * @property link: boolean indicating column values are links
 * @property linkAttr: the attribute to plug into the link url
 * @property valueMap: provides key/value pairs to map API values to UI values
 * @property transform: a function used to transform values within the column */
export interface ColumnConfig {
    dataAttr: string;
    columnHeader: string;
    actionable?: ActionableColumn;
    sortable?: boolean;
    link?: boolean;
    linkBasePath?: string;
    linkAttr?: string; // if no linkAttr is given, defaults to dataAttr
    valueMap?: Map<string | number, any>;
    transform?: Function;
    localSort?: boolean;
}

export interface TableConfig {
    columns: Array<ColumnConfig>;
    rows: Array<TableRow>;
}

export interface TableProps {
    config: TableConfig;
    filterManager?: FilterManager;
    cursorManager?: CursorManager;
}

const Table = ({ config, filterManager, cursorManager }: TableProps) => {
    /* memoizedRows is the source of truth for rendered table rows. If a local
     * sort is applied, this function reactively sorts the rows passed in. If
     * the sort column, order, or localSort value change in SortSettings,
     * this reactively updates to account for that, too. */
    const memoizedRows = useMemo(() => {
        const { column, order, locally } =
            filterManager?.sortSettings ||
            ({
                // Default values
                column: "",
                order: "DESC",
                locally: false,
            } as SortSettings);
        const valueType = typeof config.rows[0][column];
        if (locally) {
            switch (valueType) {
                case "string": {
                    return config.rows.sort((a, b) =>
                        order === "ASC"
                            ? a[column].localeCompare(b[column])
                            : b[column].localeCompare(a[column])
                    );
                }
                case "bigint":
                case "number": {
                    return config.rows.sort((a, b) =>
                        order === "ASC"
                            ? a[column] - b[column]
                            : b[column] - a[column]
                    );
                }
            }
        }
        return config.rows;
    }, [config.rows, filterManager?.sortSettings]);

    const renderArrow = () => {
        const { order } = filterManager?.sortSettings || {
            order: "DESC",
        };
        if (filterManager && order === "ASC") {
            return <IconArrowUpward />;
        } else if (filterManager && order === "DESC") {
            return <IconArrowDownward />;
        }
    };

    const swapSort = (config: ColumnConfig) => {
        // Update localSortFunc based on config.localSort
        if (config.localSort) {
            filterManager?.updateSort({
                type: SortSettingsActionType.APPLY_LOCAL_SORT,
                payload: {
                    locally: true,
                },
            });
        } else {
            filterManager?.updateSort({
                type: SortSettingsActionType.APPLY_LOCAL_SORT,
                payload: {
                    locally: false,
                },
            });
        }
        filterManager?.updateSort({
            type: SortSettingsActionType.CHANGE_COL,
            payload: {
                column: config.dataAttr,
            },
        });
        filterManager?.updateSort({
            type: SortSettingsActionType.SWAP_ORDER,
        });
    };

    const updateCursorForNetworkSort = () => {
        /* IMPORTANT:
         * The conditional presented in this call is measuring
         * sortSettings.order BEFORE it's swapped (which we do
         * above this). This is why the logic is backwards */
        cursorManager?.update({
            type: CursorActionType.RESET,
            payload:
                filterManager?.sortSettings.order === "ASC"
                    ? filterManager?.rangeSettings.to
                    : filterManager?.rangeSettings.from,
        });
    };

    /* Renders the header row of the table from columns.values() */
    const TableHeaders = () => {
        return (
            <tr>
                {config.columns?.map((colConfig) => {
                    if (colConfig.sortable && filterManager) {
                        return (
                            <th
                                key={colConfig.columnHeader}
                                onClick={() => {
                                    // Swaps the order and set column
                                    swapSort(colConfig);
                                    // Only updates cursor when not locally
                                    // sorting
                                    if (!colConfig.localSort) {
                                        updateCursorForNetworkSort();
                                    }
                                }}
                            >
                                {colConfig.columnHeader}
                                {colConfig.sortable ? renderArrow() : null}
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
            </tr>
        );
    };

    const showMappedValue = (
        columnConfig: ColumnConfig,
        object: TableRow
    ): string => {
        if (columnConfig.valueMap) {
            return (
                columnConfig.valueMap?.get(object[columnConfig.dataAttr]) ||
                object[columnConfig.dataAttr]
            );
        } else {
            return object[columnConfig.dataAttr];
        }
    };

    const renderRow = (object: TableRow, columnConfig: ColumnConfig) => {
        let displayValue = object[columnConfig.dataAttr];
        // Transforms value if transform function is given
        if (columnConfig.transform) {
            displayValue = columnConfig.transform(displayValue);
        }

        if (columnConfig.link) {
            // Render column value as NavLink
            return (
                <NavLink
                    className="usa-link"
                    to={`${columnConfig?.linkBasePath || ""}${
                        object[columnConfig?.linkAttr || columnConfig.dataAttr]
                    }`}
                >
                    {columnConfig.valueMap
                        ? showMappedValue(columnConfig, object)
                        : displayValue}
                </NavLink>
            );
        } else if (columnConfig.actionable) {
            const { action, param } = columnConfig.actionable;
            const doAction = () => {
                if (param) return action(object[param]);
                return action();
            };
            return (
                <button
                    className="usa-link bg-transparent border-transparent"
                    onClick={() => doAction()}
                >
                    {displayValue}
                </button>
            );
        } else {
            return columnConfig.valueMap
                ? showMappedValue(columnConfig, object)
                : displayValue;
        }
    };

    /* Iterates each row, and then uses the key value from columns.keys()
     * to render each cell in the appropriate column. */
    const TableRows = () => {
        return (
            <>
                {memoizedRows.map((object, rowIndex) => {
                    // Caps page size when filterManager exists
                    if (
                        filterManager &&
                        rowIndex >= filterManager?.pageSettings.size
                    )
                        return null;
                    return (
                        <tr key={rowIndex}>
                            {config.columns.map((colConfig, colIndex) => (
                                <td key={`${rowIndex}:${colIndex}`}>
                                    {renderRow(object, colConfig)}
                                </td>
                            ))}
                        </tr>
                    );
                })}
            </>
        );
    };

    /* Handles pagination button logic and display */
    function PaginationButtons(cm: CursorManager) {
        return (
            <div className="float-right margin-top-5">
                {cm.hasPrev && (
                    <Button
                        unstyled
                        type="button"
                        className="margin-right-2"
                        onClick={() =>
                            cm.update({ type: CursorActionType.PAGE_DOWN })
                        }
                    >
                        <span>
                            <IconNavigateBefore className="text-middle" />
                            Previous
                        </span>
                    </Button>
                )}
                {cm.hasNext && (
                    <Button
                        unstyled
                        type="button"
                        onClick={() =>
                            cm.update({ type: CursorActionType.PAGE_UP })
                        }
                    >
                        <span>
                            Next
                            <IconNavigateNext className="text-middle" />
                        </span>
                    </Button>
                )}
            </div>
        );
    }

    return (
        <div className="grid-container margin-bottom-10">
            <div className="grid-col-12">
                <table
                    className="usa-table usa-table--borderless prime-table"
                    aria-label="Submission history from the last 30 days"
                >
                    <thead>
                        <TableHeaders />
                    </thead>
                    <tbody className="font-mono-2xs">
                        <TableRows />
                    </tbody>
                </table>
                {cursorManager ? (
                    <PaginationButtons {...cursorManager} />
                ) : null}
            </div>
        </div>
    );
};

export default Table;
