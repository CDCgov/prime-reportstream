/* Makes row objects string-indexed */
import {
    Button,
    IconNavigateBefore,
    IconNavigateNext,
    IconArrowUpward,
    IconArrowDownward,
} from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";
import React, { ReactNode, useState } from "react";

import {
    CursorActionType,
    CursorManager,
} from "../../hooks/filters/UseCursorManager";
import { FilterManager } from "../../hooks/filters/UseFilterManager";
import { SortSettingsActionType } from "../../hooks/filters/UseSortOrder";

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
    editable?: boolean;
}

export interface TableConfig {
    columns: Array<ColumnConfig>;
    rows: Array<TableRow>;
}

export interface DatasetAction {
    label: string;
    method: Function;
}

export interface TableProps {
    config: TableConfig;
    title?: string;
    legend?: ReactNode;
    datasetAction?: DatasetAction;
    filterManager?: FilterManager;
    cursorManager?: CursorManager;
    enableEditableRows?: boolean;
}

export interface LegendItem {
    label: string;
    value: string;
}

export const Legend = ({ items }: { items: LegendItem[] }) => {
    const makeItem = (label: string, value: string) => (
        <div className="display-flex">
            <b>{`${label}:`}</b>
            <span className="padding-left-05">{value}</span>
        </div>
    );
    return (
        <section className="display-flex flex-column">
            {items.map((item) => makeItem(item.label, item.value))}
        </section>
    );
};

const Table = ({
    config,
    title,
    legend,
    datasetAction,
    filterManager,
    cursorManager,
    enableEditableRows,
}: TableProps) => {
    const renderArrow = () => {
        if (filterManager && filterManager.sortSettings.order === "ASC") {
            return <IconArrowUpward />;
        } else if (
            filterManager &&
            filterManager.sortSettings.order === "DESC"
        ) {
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
                                    filterManager?.updateSort({
                                        type: SortSettingsActionType.CHANGE_COL,
                                        payload: {
                                            column: colConfig.dataAttr,
                                        },
                                    });
                                    filterManager?.updateSort({
                                        type: SortSettingsActionType.SWAP_ORDER,
                                    });
                                    /* IMPORTANT:
                                     * The conditional presented in this call is measuring
                                     * sortSettings.order BEFORE it's swapped (which we do
                                     * above this). This is why the logic is backwards */
                                    cursorManager?.update({
                                        type: CursorActionType.RESET,
                                        payload:
                                            filterManager?.sortSettings
                                                .order === "ASC"
                                                ? filterManager?.rangeSettings
                                                      .to
                                                : filterManager?.rangeSettings
                                                      .from,
                                    });
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
                {enableEditableRows ? (
                    // This extends the header bottom border to cover this column
                    <th key={"edit"}>{""}</th>
                ) : null}
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

    const renderColumn = (
        object: TableRow,
        columnConfig: ColumnConfig,
        editing?: boolean
    ) => {
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
            if (editing && columnConfig.editable)
                return <input value={displayValue} />;
            return columnConfig.valueMap
                ? showMappedValue(columnConfig, object)
                : displayValue;
        }
    };

    const DatasetActionButton = ({ label, method }: DatasetAction) => {
        return (
            <Button type={"button"} onClick={() => method()}>
                {label}
            </Button>
        );
    };

    const TableInfo = () => {
        return (
            <div className="grid-col-12 display-flex flex-align-end flex-justify-between">
                <div className="grid-col-8 display-flex flex-column">
                    {title ? <h2>{title}</h2> : null}
                    {legend ? legend : null}
                </div>
                <div className="grid-col-2 display-flex flex-column">
                    {datasetAction ? (
                        <DatasetActionButton {...datasetAction} />
                    ) : null}
                </div>
            </div>
        );
    };

    /* Iterates each row, and then uses the key value from columns.keys()
     * to render each cell in the appropriate column. */
    const TableRows = () => {
        const [editing, setEditing] = useState<number | undefined>();
        return (
            <>
                {config.rows.map((object, rowIndex) => {
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
                                    {editing === rowIndex
                                        ? renderColumn(object, colConfig, true)
                                        : renderColumn(object, colConfig)}
                                </td>
                            ))}
                            {enableEditableRows ? (
                                <td key={`${rowIndex}:EDIT`}>
                                    <Button
                                        type="button"
                                        onClick={() => {
                                            if (editing !== undefined) {
                                                setEditing(undefined);
                                                return;
                                            }
                                            setEditing(rowIndex);
                                        }}
                                    >
                                        {editing !== undefined &&
                                        editing === rowIndex
                                            ? "Save"
                                            : "Edit"}
                                    </Button>
                                </td>
                            ) : null}
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
            <TableInfo />
            <div className="grid-col-12">
                <table
                    className="usa-table usa-table--borderless usa-table--striped prime-table"
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
