/* Makes row objects string-indexed */
import {
    Button,
    ButtonGroup,
    IconNavigateBefore,
    IconNavigateNext,
    IconArrowUpward,
    IconArrowDownward,
} from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";
import React from "react";

import { CursorManager } from "../../hooks/filters/UseCursorManager";
import { FilterManager } from "../../hooks/filters/UseFilterManager";

export interface TableRow {
    [key: string]: any;
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
    sortable?: boolean;
    link?: boolean;
    linkBasePath?: string;
    linkAttr?: string; // if no linkAttr is given, defaults to dataAttr
    valueMap?: Map<string | number, any>;
    transform?: Function;
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
    const renderArrow = () => {
        if (filterManager && filterManager.sort.order === "ASC") {
            return <IconArrowUpward />;
        } else if (filterManager && filterManager.sort.order === "DESC") {
            return <IconArrowDownward />;
        }
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
                                    filterManager.sort.set(
                                        colConfig.dataAttr,
                                        filterManager?.sort.order
                                    );
                                    cursorManager?.controller.reset();
                                    debugger;
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
                    to={`${columnConfig?.linkBasePath || ""}/${
                        object[columnConfig?.linkAttr || columnConfig.dataAttr]
                    }`}
                >
                    {columnConfig.valueMap
                        ? showMappedValue(columnConfig, object)
                        : displayValue}
                </NavLink>
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
                {config.rows.map((object, rowIndex) => {
                    // Caps page size when filterManager exists
                    if (
                        filterManager &&
                        rowIndex >= filterManager?.pageSize.count
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
    function PaginationButtons({ values, controller }: CursorManager) {
        return (
            <ButtonGroup type="segmented" className="float-right margin-top-5">
                {values.hasPrev && (
                    <Button
                        type="button"
                        onClick={() => controller.goTo(values.currentIndex - 1)}
                    >
                        <span>
                            <IconNavigateBefore className="text-middle" />
                            Previous
                        </span>
                    </Button>
                )}
                {values.hasNext && (
                    <Button
                        type="button"
                        onClick={() => controller.goTo(values.currentIndex + 1)}
                    >
                        <span>
                            Next
                            <IconNavigateNext className="text-middle" />
                        </span>
                    </Button>
                )}
            </ButtonGroup>
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
                    <PaginationButtons
                        values={cursorManager.values}
                        controller={cursorManager.controller}
                    />
                ) : null}
            </div>
        </div>
    );
};

export default Table;
