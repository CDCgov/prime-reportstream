// AutoUpdateFileChromatic
import classnames from "classnames";
import { ReactNode, useState } from "react";
import { Icon } from "@trussworks/react-uswds";

import styles from "./Table.module.scss";

enum FilterOptions {
    ASC = "asc",
    DESC = "desc",
    NONE = "none",
}

interface SortableTableHeaderProps {
    activeColumn: string;
    columnHeaderData: RowData;
    onActiveColumnChange: (column: string) => void;
    onSortOrderChange: (sortOrder: FilterOptions) => void;
    sortOrder: string;
    sticky?: boolean;
}

interface RowData {
    columnHeader: string;
    columnKey: string;
    content: string | ReactNode;
}

export interface TableProps {
    borderless?: boolean;
    compact?: boolean;
    fullWidth?: boolean;
    rowData: RowData[][];
    scrollable?: boolean;
    sortable?: boolean;
    stackedStyle?: "default" | "headers";
    sticky?: boolean;
    striped?: boolean;
}

const SortableTableHeader = ({
    activeColumn,
    columnHeaderData,
    onActiveColumnChange,
    onSortOrderChange,
    sortOrder,
    sticky,
}: SortableTableHeaderProps) => {
    let SortIcon = Icon.SortArrow;
    const isActive = columnHeaderData.columnKey === activeColumn;
    if (isActive && sortOrder === FilterOptions.ASC) {
        SortIcon = Icon.ArrowUpward;
    } else if (isActive && sortOrder === FilterOptions.DESC) {
        SortIcon = Icon.ArrowDownward;
    }

    const handleHeaderClick = () => {
        if (!isActive) {
            // Reset active column and sort order on new column click
            onActiveColumnChange(columnHeaderData.columnKey);
            onSortOrderChange(FilterOptions.ASC);
        } else if (sortOrder === FilterOptions.NONE) {
            // Explicitly set the proceeding sort order
            onSortOrderChange(FilterOptions.ASC);
        } else if (sortOrder === FilterOptions.ASC) {
            onSortOrderChange(FilterOptions.DESC);
        } else if (sortOrder === FilterOptions.DESC) {
            onSortOrderChange(FilterOptions.NONE);
        }
    };
    return (
        <th
            className={classnames("column-header column-header--clickable", {
                "column-header--sticky": sticky,
            })}
        >
            <button
                className={classnames("column-header-button", {
                    "column-header-button--active": isActive,
                })}
                onClick={handleHeaderClick}
            >
                <div className="column-header column-header--sortable">
                    <p className="column-header-text">
                        {columnHeaderData.columnHeader}
                    </p>
                    {<SortIcon size={3} />}
                </div>
            </button>
        </th>
    );
};

function sortTableData(
    activeColumn: string,
    rowData: RowData[][],
    sortOrder: FilterOptions
) {
    return sortOrder !== FilterOptions.NONE && activeColumn
        ? rowData.sort((a, b): number => {
              const contentColA =
                  a.find((item) => item.columnKey === activeColumn) || "";
              const contentColB =
                  b.find((item) => item.columnKey === activeColumn) || "";
              if (sortOrder === FilterOptions.ASC) {
                  return contentColA < contentColB ? 1 : -1;
              } else {
                  return contentColA > contentColB ? 1 : -1;
              }
          })
        : rowData;
}

const SortableTable = ({
    columnHeaders,
    rowData,
    sticky,
}: {
    columnHeaders: RowData[];
    rowData: RowData[][];
    sticky?: boolean;
}) => {
    const [activeColumn, setActiveColumn] = useState("");
    const [sortOrder, setSortOrder] = useState(FilterOptions.NONE);
    const sortedData = sortTableData(activeColumn, rowData, sortOrder);
    return (
        <>
            <thead>
                <tr>
                    {columnHeaders.map((columnHeaderData, index) => {
                        return (
                            <SortableTableHeader
                                key={index}
                                columnHeaderData={columnHeaderData}
                                activeColumn={activeColumn}
                                sortOrder={sortOrder}
                                onActiveColumnChange={setActiveColumn}
                                onSortOrderChange={setSortOrder}
                                sticky={sticky}
                            />
                        );
                    })}
                </tr>
            </thead>
            <tbody>
                {sortedData.map((row, index) => {
                    return (
                        <tr key={index}>
                            {row.map((data, dataIndex) => {
                                const isActive =
                                    data.columnKey === activeColumn;
                                return (
                                    <td
                                        key={dataIndex}
                                        className={classnames("column-data", {
                                            "column-data--active": isActive,
                                        })}
                                    >
                                        {data.content}
                                    </td>
                                );
                            })}
                        </tr>
                    );
                })}
            </tbody>
        </>
    );
};

export const Table = ({
    borderless,
    compact,
    fullWidth,
    rowData,
    scrollable,
    sortable,
    stackedStyle,
    sticky,
    striped,
}: TableProps) => {
    const classes = classnames("usa-table", {
        "usa-table--borderless": borderless,
        "usa-table--compact": compact,
        "usa-table--stacked": stackedStyle === "default",
        "usa-table--stacked-header": stackedStyle === "headers",
        "usa-table--striped": striped,
        "width-full": fullWidth,
    });

    const columnHeaders = rowData.flat().filter((rowItemFilter, pos, arr) => {
        return (
            arr
                .map((rowItemMap) => rowItemMap.columnKey)
                .indexOf(rowItemFilter.columnKey) === pos
        );
    });

    return (
        <div
            className={styles.Table}
            {...(scrollable && {
                className: `usa-table-container--scrollable ${styles.Table} ${
                    sticky && styles.Table__StickyHeader
                }`,
                tabIndex: 0,
            })}
        >
            {rowData.length ? (
                <table className={classes}>
                    {sortable ? (
                        <SortableTable
                            sticky={sticky}
                            rowData={rowData}
                            columnHeaders={columnHeaders}
                        />
                    ) : (
                        <>
                            <thead>
                                <tr>
                                    {columnHeaders.map((header, index) => {
                                        return (
                                            <th
                                                key={index}
                                                className={classnames(
                                                    "column-header",
                                                    {
                                                        "column-header--sticky":
                                                            sticky,
                                                    }
                                                )}
                                            >
                                                <p className="column-header-text">
                                                    {header.columnHeader}
                                                </p>
                                            </th>
                                        );
                                    })}
                                </tr>
                            </thead>
                            <tbody>
                                {rowData.map((row, index) => {
                                    return (
                                        <tr key={index}>
                                            {row.map((data, dataIndex) => {
                                                return (
                                                    <td
                                                        key={dataIndex}
                                                        className="column-data"
                                                    >
                                                        {data.content}
                                                    </td>
                                                );
                                            })}
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </>
                    )}
                </table>
            ) : (
                <h2>No data to show</h2>
            )}
        </div>
    );
};
