/* Makes row objects string-indexed */
import {
    Button,
    IconArrowDownward,
    IconArrowUpward,
    IconNavigateBefore,
    IconNavigateNext,
} from "@trussworks/react-uswds";
import React, { ReactNode, useMemo, useCallback, useState } from "react";

import Pagination, { PaginationProps } from "../../components/Table/Pagination";
import {
    CursorActionType,
    CursorManager,
} from "../../hooks/filters/UseCursorManager";
import { FilterManager } from "../../hooks/filters/UseFilterManager";
import {
    SortOrder,
    SortSettingsActionType,
} from "../../hooks/filters/UseSortOrder";

import { TableRows } from "./TableRows";

export interface ActionableColumn {
    action: Function;
    param?: string;
}

export interface LinkableColumn {
    link: boolean;
    linkBasePath?: string;
    linkAttr?: string; // if no linkAttr is given, defaults to dataAttr
}

// each table row will be a map keyed off the dataAttr value of
// a column from the column config from the same tableConfig.
// values will largely be assumed to be primitive values, or values
// to be passed into a transform function defined in the column config
export interface TableRow {
    [key: string]: any;
}

/** @alias for any type of feature column */
type ColumnFeature = ActionableColumn | LinkableColumn;
/** ColumnConfig tells the Table element how to render each column
 *
 * @property dataAttr - Name of the object attribute to be rendered in the column
 * @property columnHeader - The column name
 * @property sortable - boolean
 * @property link - boolean indicating column values are links
 * @property linkAttr - the attribute to plug into the link url
 * @property valueMap - provides key/value pairs to map API values to UI values
 * @property transform - a function used to transform values within the column */
export interface ColumnConfig {
    dataAttr: string;
    columnHeader: string;
    feature?: ColumnFeature;
    sortable?: boolean;
    valueMap?: Map<string | number, any>;
    transform?: Function;
    editable?: boolean;
    localSort?: boolean;
}

export interface TableConfig {
    columns: Array<ColumnConfig>;
    rows: Array<TableRow>;
}

export interface DatasetAction {
    label: string;
    method?: Function;
}

export interface DatasetActionProps extends DatasetAction {
    disabled: boolean;
}

export type RowSideEffect = (row: TableRow | null) => Promise<void>;

/** Configuration pattern for Table component
 * @remarks Working to deprecate cursorManager for paginationProps */
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
    paginationProps?: PaginationProps;
    enableEditableRows?: boolean;
    editableCallback?: RowSideEffect;
    classes?: string;
}

export interface LegendItem {
    label: string;
    value: string;
}

/** A configurable USWDS table for easy reuse across ReportStream
 * @remarks In the process of deprecating cursorManager and replacing with paginationProps
 * which gives us number pagination
 * */
const Table = ({
    config,
    title,
    legend,
    datasetAction,
    filterManager,
    cursorManager,
    paginationProps,
    enableEditableRows,
    editableCallback = () => Promise.resolve(),
    classes,
}: TableProps) => {
    const [rowToEdit, setRowToEdit] = useState<number | undefined>();

    /* memoizedRows is the source of truth for rendered table rows. If a local
     * sort is applied, this function reactively sorts the rows passed in. If
     * the sort column, order, or localSort value change in SortSettings,
     * this reactively updates to account for that, too. */
    const memoizedRows = useMemo(() => {
        if (!config?.rows.length) {
            return [];
        }
        const column = filterManager?.sortSettings?.column || "";
        const locally = filterManager?.sortSettings?.locally || false;
        const localOrder = filterManager?.sortSettings?.localOrder || "DESC";
        const valueType = typeof config?.rows[0]?.[column];
        if (locally) {
            switch (valueType) {
                case "string": {
                    return config.rows.sort((a, b) =>
                        localOrder === "ASC"
                            ? a[column].localeCompare(b[column])
                            : b[column].localeCompare(a[column])
                    );
                }
                case "bigint":
                case "number": {
                    return config.rows.sort((a, b) =>
                        localOrder === "ASC"
                            ? a[column] - b[column]
                            : b[column] - a[column]
                    );
                }
            }
        }
        return config.rows;
    }, [config.rows, filterManager?.sortSettings]);

    const wrapperClasses = useMemo(
        () => `grid-container margin-bottom-10 ${classes}`,
        [classes]
    );

    const addRow = useCallback(() => {
        setRowToEdit(memoizedRows.length);
    }, [memoizedRows, setRowToEdit]);

    const renderArrow = () => {
        const { order, localOrder, locally } = filterManager?.sortSettings || {
            order: "DESC",
            locally: false,
            localOrder: "DESC",
        };

        const isOrder = (sortOrder: SortOrder) =>
            order === sortOrder || (locally && localOrder === sortOrder);

        if (filterManager && isOrder("ASC")) {
            return <IconArrowUpward />;
        } else if (filterManager && isOrder("DESC")) {
            return <IconArrowDownward />;
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
        const isSortedColumn = (colConfig: ColumnConfig) =>
            colConfig.sortable &&
            filterManager?.sortSettings.column === colConfig.dataAttr;
        return (
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
                                    // Only updates cursor when NOT locally sorting
                                    if (!colConfig.localSort) {
                                        updateCursorForNetworkSort();
                                    }
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
        );
    };

    // this button will be placed above the rendered table and on `click` will run an arbitrary function
    // passed in from the Table's parent, or an addRow function defined by the Table.
    // in order to avoid problems around timing, takes a `disabled` prop.
    // TODO: split this out of Table component
    const DatasetActionButton = ({
        label,
        method = () => {},
        disabled,
    }: DatasetActionProps) => (
        <Button type={"button"} onClick={() => method()} disabled={disabled}>
            {label}
        </Button>
    );

    const TableInfo = () => {
        return (
            <div className="grid-col-12 display-flex flex-align-end flex-justify-between">
                <div className="grid-col-8 display-flex flex-column">
                    {title ? <h2>{title}</h2> : null}
                    {legend ? legend : null}
                </div>
                <div className="grid-col-2 display-flex flex-column">
                    {datasetAction ? (
                        <DatasetActionButton
                            label={datasetAction.label}
                            method={datasetAction.method || addRow}
                            disabled={!!rowToEdit}
                        />
                    ) : null}
                </div>
            </div>
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
        <div className={wrapperClasses}>
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
                        <TableRows
                            rows={memoizedRows}
                            onSave={editableCallback}
                            enableEditableRows={enableEditableRows}
                            filterManager={filterManager}
                            columns={config.columns}
                            rowToEdit={rowToEdit}
                            setRowToEdit={setRowToEdit}
                        />
                    </tbody>
                </table>
                {/** @todo Deprecate cursorManager for paginationProps */}
                {cursorManager && <PaginationButtons {...cursorManager} />}
                {paginationProps && <Pagination {...paginationProps} />}
            </div>
        </div>
    );
};

export default Table;
