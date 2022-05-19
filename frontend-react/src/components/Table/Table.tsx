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

export interface ActionableColumn {
    action: Function;
    param?: string;
}

export interface LinkableColumn {
    link: boolean;
    linkBasePath?: string;
    linkAttr?: string; // if no linkAttr is given, defaults to dataAttr
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
type ColumnFeature = ActionableColumn | LinkableColumn;
export interface ColumnConfig {
    dataAttr: string;
    columnHeader: string;
    feature?: ColumnFeature;
    sortable?: boolean;
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

export interface ColumnProps {
    rowIndex: number;
    colIndex: number;
    rowData: TableRow;
    columnConfig: ColumnConfig;
    editing?: boolean;
}

export interface TableProps {
    config: TableConfig;
    title?: string;
    /* The Legend component is the responsibility
     * of the parent to pass in, allowing it to be as
     * versatile as possible */
    legend?: ReactNode;
    datasetAction?: DatasetAction;
    filterManager?: FilterManager;
    cursorManager?: CursorManager;
    enableEditableRows?: boolean;
    editableCallback?: Function;
}

export interface LegendItem {
    label: string;
    value: string;
}

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

    const showMappedValue = (columnConfig: ColumnConfig, object: TableRow) => {
        if (columnConfig.valueMap) {
            return (
                <span>
                    {columnConfig.valueMap?.get(
                        object[columnConfig.dataAttr]
                    ) || object[columnConfig.dataAttr]}
                </span>
            );
        } else {
            return <span>{object[columnConfig.dataAttr]}</span>;
        }
    };

    const ColumnData = ({
        rowIndex,
        colIndex,
        rowData,
        columnConfig,
        editing,
    }: ColumnProps) => {
        // Easy-to-read way to transform value
        const transform = (
            transformFunc: Function,
            transformVal: string | number
        ) => {
            return transformFunc(transformVal);
        };
        // Runtime type checking for ColumnFeature
        const hasFeature = (attr: string): boolean => {
            if (!columnConfig.feature) return false;
            return Object.keys(columnConfig.feature).includes(attr);
        };
        // Editing state indicator
        const isEditing = (): boolean =>
            (editing && columnConfig.editable) || false;
        // <td> wrapper w/ key
        const tableData = (child: ReactNode) => (
            <td key={`${rowIndex}:${colIndex}`}>{child}</td>
        );

        let displayValue = rowData[columnConfig.dataAttr];

        if (columnConfig.transform) {
            displayValue = transform(
                columnConfig.transform,
                rowData[columnConfig.dataAttr]
            );
        }

        if (hasFeature("link")) {
            // Render column value as NavLink
            const feature = columnConfig?.feature as LinkableColumn;
            return tableData(
                <NavLink
                    className="usa-link"
                    to={`${feature.linkBasePath || ""}${
                        rowData[feature.linkAttr || columnConfig.dataAttr]
                    }`}
                >
                    {columnConfig.valueMap
                        ? showMappedValue(columnConfig, rowData)
                        : displayValue}
                </NavLink>
            );
        }

        if (hasFeature("action")) {
            // Make column value actionable
            const { action, param } = columnConfig.feature as ActionableColumn;

            if (!rowData[param!!]) {
                console.warn(`The row attribute '${param}' could not be found`);
            }

            const doAction = () => {
                if (param) return action(rowData[param]);
                return action();
            };
            return tableData(
                <button
                    className="usa-link bg-transparent border-transparent"
                    onClick={() => doAction()}
                >
                    {displayValue}
                </button>
            );
        }

        if (isEditing()) {
            // Make column value editable
            return tableData(
                <input
                    className="usa-input"
                    /* This directly updates the rowData object, NOT the
                     * displayValue, which prevents multi-layer callbacks
                     * being required. */
                    onChange={(event) =>
                        (rowData[columnConfig.dataAttr] = event.target.value)
                    }
                    /* This ensures the value seen in the edit field is
                     * the same as the server-provided data, NOT the
                     * displayed data (in case of transformation/map) */
                    defaultValue={rowData[columnConfig.dataAttr]}
                />
            );
        }

        return columnConfig.valueMap
            ? tableData(showMappedValue(columnConfig, rowData))
            : tableData(displayValue);
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
        const editableRowButtonValue = (isEditing: boolean) =>
            isEditing ? "Save" : "Edit";
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
                                <ColumnData
                                    key={`${rowIndex}:${colIndex}:TOP`}
                                    rowIndex={rowIndex}
                                    colIndex={colIndex}
                                    columnConfig={colConfig}
                                    rowData={object}
                                    editing={editing === rowIndex}
                                />
                            ))}
                            {enableEditableRows ? (
                                <td key={`${rowIndex}:EDIT`}>
                                    <Button
                                        type="submit"
                                        onClick={() => {
                                            if (editing !== undefined) {
                                                setEditing(undefined);
                                                return;
                                            }
                                            setEditing(rowIndex);
                                        }}
                                    >
                                        {editableRowButtonValue(
                                            editing === rowIndex
                                        )}
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
