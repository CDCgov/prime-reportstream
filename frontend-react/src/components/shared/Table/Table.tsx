import classnames from "classnames";
import sanitizeHtml from "sanitize-html";

import styles from "./Table.module.scss";
import { useMemo, useState } from "react";
import { Icon } from "@trussworks/react-uswds";

const FilterOptionsEnum = {
    NONE: "none",
    ASC: "asc",
    DESC: "desc",
} as const;

interface SortableTableHeaderProps {
    columnTitle: string;
    activeColumn: string;
    sortOrder: string;
    setSortOrder: (order: string) => void;
    setActiveColumn: (column: string) => void;
}

interface RowData {
    columnKey: string;
    content: string | HTMLElement;
    modalContent?: string | HTMLElement;
}

export interface TableProps {
    borderless?: boolean;
    compact?: boolean;
    emptyState?: boolean;
    fullWidth?: boolean;
    scrollable?: boolean;
    sortable?: boolean;
    stackedStyle?: "default" | "headers";
    striped?: boolean;
    rowData: RowData[][];
}

const SortableTableHeader = ({
    columnTitle,
    activeColumn,
    sortOrder,
    setSortOrder,
    setActiveColumn,
}: SortableTableHeaderProps) => {
    let SortIcon = Icon.SortArrow;
    const isActive = columnTitle === activeColumn;
    if (isActive && sortOrder === FilterOptionsEnum.ASC) {
        SortIcon = Icon.ArrowUpward;
    } else if (isActive && sortOrder === FilterOptionsEnum.DESC) {
        SortIcon = Icon.ArrowDownward;
    }
    const isCurrentlyActiveColumn = columnTitle === activeColumn;

    const handleHeaderClick = () => {
        if (!isCurrentlyActiveColumn) {
            // Reset active column and sort order on new column click
            setActiveColumn(columnTitle);
            setSortOrder(FilterOptionsEnum.ASC);
        } else if (sortOrder === FilterOptionsEnum.NONE) {
            // Explicitly set the proceeding sort order
            setSortOrder(FilterOptionsEnum.ASC);
        } else if (sortOrder === FilterOptionsEnum.ASC) {
            setSortOrder(FilterOptionsEnum.DESC);
        } else if (sortOrder === FilterOptionsEnum.DESC) {
            setSortOrder(FilterOptionsEnum.NONE);
        }
    };
    return (
        <th
            role="button"
            tabIndex={0}
            className={classnames({
                "active-col-header": isActive,
            })}
            onClick={handleHeaderClick}
        >
            <div className="column-header-title-container">
                <p>{columnTitle}</p>
                {<SortIcon />}
            </div>
        </th>
    );
};

const SortableTable = ({
    rowData,
    columnHeaders,
}: {
    rowData: RowData[][];
    columnHeaders: string[];
}) => {
    // const [sortedData, useSortedData] = useState(rowData);
    const [activeColumn, setActiveColumn] = useState("");
    const [sortOrder, setSortOrder] = useState(FilterOptionsEnum.NONE);
    const sortedData = useMemo(() => {
        return sortOrder !== FilterOptionsEnum.NONE && activeColumn
            ? rowData.sort((a, b): number => {
                  if (sortOrder === FilterOptionsEnum.ASC) {
                      return a.find((item) => item.columnKey === activeColumn)
                          .content <
                          b.find((item) => item.columnKey === activeColumn)
                              .content
                          ? 1
                          : -1;
                  } else {
                      return a.find((item) => item.columnKey === activeColumn)
                          .content >
                          b.find((item) => item.columnKey === activeColumn)
                              .content
                          ? 1
                          : -1;
                  }
              })
            : rowData;
    }, [activeColumn, rowData, sortOrder]);
    return (
        <>
            <thead>
                <tr>
                    {columnHeaders.map((header) => {
                        return (
                            <SortableTableHeader
                                columnTitle={header}
                                activeColumn={activeColumn}
                                sortOrder={sortOrder}
                                setActiveColumn={setActiveColumn}
                                setSortOrder={setSortOrder}
                            />
                        );
                    })}
                </tr>
            </thead>
            <tbody>
                {sortedData.map((row) => {
                    return (
                        <tr>
                            {row.map((data) => {
                                return (
                                    <td
                                        dangerouslySetInnerHTML={{
                                            __html: sanitizeHtml(data.content),
                                        }}
                                    />
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
    emptyState,
    fullWidth,
    scrollable,
    sortable,
    stackedStyle,
    striped,
    rowData,
}: TableProps) => {
    const classes = classnames("usa-table", {
        "usa-table--borderless": borderless,
        "usa-table--compact": compact,
        "width-full": fullWidth,
        "usa-table--stacked": stackedStyle === "default",
        "usa-table--stacked-header": stackedStyle === "headers",
        "usa-table--striped": striped,
    });

    const columnHeaders = [
        ...new Set(rowData.flat().map((data: RowData) => data.columnKey)),
    ];

    return (
        <div
            {...(scrollable && {
                className: "usa-table-container--scrollable",
                tabIndex: 0,
            })}
        >
            <table className={classes} data-testid="table">
                {sortable ? (
                    <SortableTable
                        rowData={rowData}
                        columnHeaders={columnHeaders}
                    />
                ) : (
                    <>
                        <thead>
                            <tr>
                                {columnHeaders.map((header) => {
                                    return <td>{header}</td>;
                                })}
                            </tr>
                        </thead>
                        <tbody>
                            {rowData.map((row) => {
                                return (
                                    <tr>
                                        {row.map((data) => {
                                            return (
                                                <td
                                                    dangerouslySetInnerHTML={{
                                                        __html: sanitizeHtml(
                                                            data.content
                                                        ),
                                                    }}
                                                />
                                            );
                                        })}
                                    </tr>
                                );
                            })}
                        </tbody>
                    </>
                )}
            </table>
        </div>
    );
};
