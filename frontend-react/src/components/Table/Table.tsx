/* Makes row objects string-indexed */
import React, { ReactNode, useMemo, useCallback, useState } from "react";

import Pagination, { PaginationProps } from "../../components/Table/Pagination";
import { FilterManager } from "../../hooks/filters/UseFilterManager";

import { TableRowData, TableRows } from "./TableRows";
import { TableHeaders } from "./TableHeaders";
import { DatasetAction, TableInfo } from "./TableInfo";

export interface ActionableColumn {
    action: Function;
    param?: string;
    actionButtonHandler?: Function;
    actionButtonParam?: string;
}

export interface LinkableColumn {
    link: boolean;
    linkBasePath?: string;
    linkAttr?: string; // if no linkAttr is given, defaults to dataAttr
    linkState?: unknown;
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
    rows: Array<TableRowData>;
}

export type RowSideEffect = (row: TableRowData | null) => Promise<void>;

/** Configuration pattern for Table component
 * @remarks Working to deprecate cursorManager for paginationProps */
export interface TableProps {
    config: TableConfig;
    title?: string;
    tableRowsClassName?: string;
    /* The Legend component is the responsibility
     * of the parent to pass in, allowing it to be as
     * versatile as possible */
    legend?: ReactNode;
    datasetAction?: DatasetAction;
    filterManager?: FilterManager;
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
    tableRowsClassName,
    config,
    title,
    legend,
    datasetAction,
    filterManager,
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
            return null;
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
                            : b[column].localeCompare(a[column]),
                    );
                }
                case "bigint":
                case "number": {
                    return config.rows.sort((a, b) =>
                        localOrder === "ASC"
                            ? a[column] - b[column]
                            : b[column] - a[column],
                    );
                }
            }
        }
        return config.rows;
    }, [config.rows, filterManager?.sortSettings]);

    const wrapperClasses = `margin-bottom-10 ${classes}`;

    const addRow = useCallback(() => {
        setRowToEdit(memoizedRows?.length || 0);
    }, [memoizedRows, setRowToEdit]);

    /** If a user provides a label with no method, we supply the basic "Add Row" method with whatever
     * label they gave. Otherwise, we provide their entire DatasetAction */
    const memoizedDatasetAction = useMemo(() => {
        if (!!datasetAction && !datasetAction.method) {
            return {
                label: datasetAction?.label,
                method: addRow,
            };
        } else {
            return datasetAction;
        }
    }, [addRow, datasetAction]);

    return (
        <div className={wrapperClasses}>
            <TableInfo
                title={title}
                legend={legend}
                datasetAction={memoizedDatasetAction}
                rowToEdit={rowToEdit}
            />
            <div>
                <table
                    className="usa-table usa-table--borderless usa-table--striped prime-table"
                    aria-label="Submission history from the last 30 days"
                >
                    <TableHeaders
                        config={config}
                        filterManager={filterManager}
                        enableEditableRows={enableEditableRows}
                    />
                    {!!memoizedRows ? (
                        <TableRows
                            className={tableRowsClassName}
                            rows={memoizedRows}
                            onSave={editableCallback}
                            enableEditableRows={enableEditableRows}
                            filterManager={filterManager}
                            columns={config.columns}
                            rowToEdit={rowToEdit}
                            setRowToEdit={setRowToEdit}
                        />
                    ) : undefined}
                </table>
                {!memoizedRows ? <span>No data to show</span> : undefined}
                {paginationProps && <Pagination {...paginationProps} />}
            </div>
        </div>
    );
};

export default Table;
