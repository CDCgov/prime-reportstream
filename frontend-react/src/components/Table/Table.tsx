/* Makes row objects string-indexed */
import {
    Button,
    ButtonGroup,
    IconNavigateBefore,
    IconNavigateNext,
} from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";
import React from "react";

import { ICursorManager } from "../../hooks/UseCursorManager";
import { IFilterManager } from "../../hooks/UseFilterManager";

export interface TableRow {
    [key: string]: any;
}

/* ColumnConfig tells the Table element how to render each column, including the
 * data attribute of the object the column maps to, the header, and ... TODO: finish
 *
 * @property dataAttr: Name of the object attribute to be rendered in the column
 * @property columnHeader: The column name
 * @property sort: React.SetStateAction function */
export interface ColumnConfig {
    dataAttr: string;
    columnHeader: string;
    sortable?: boolean;
    link?: boolean;
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
    filterManager?: IFilterManager;
    cursorManager?: ICursorManager;
}

const Table = ({ config, filterManager, cursorManager }: TableProps) => {
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
                                    filterManager.update.setSortSettings(
                                        colConfig.dataAttr
                                    );
                                }}
                            >
                                {colConfig.columnHeader}
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

    const renderRow = (object: TableRow, columnConfig: ColumnConfig) => {
        let textValue = object[columnConfig.dataAttr];
        if (columnConfig.transform) {
            textValue = columnConfig.transform(textValue);
        }
        if (columnConfig.link) {
            return (
                <NavLink
                    to={`/submissions/${
                        object[columnConfig?.linkAttr || columnConfig.dataAttr]
                    }`}
                >
                    {columnConfig.valueMap
                        ? columnConfig.valueMap?.get(
                              object[columnConfig.dataAttr]
                          ) || object[columnConfig.dataAttr]
                        : textValue}
                </NavLink>
            );
        } else {
            return columnConfig.valueMap
                ? columnConfig.valueMap?.get(object[columnConfig.dataAttr])
                : textValue;
        }
    };

    /* Iterates each row, and then uses the key value from columns.keys()
     * to render each cell in the appropriate column. */
    const TableRows = () => {
        return (
            <>
                {config.rows.map((object, rowIndex) => (
                    <tr key={rowIndex}>
                        {config.columns.map((colConfig, colIndex) => (
                            <td key={`${rowIndex}:${colIndex}`}>
                                {renderRow(object, colConfig)}
                            </td>
                        ))}
                    </tr>
                ))}
            </>
        );
    };

    /* Handles pagination button logic and display */
    function PaginationButtons({ values, controller }: ICursorManager) {
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
