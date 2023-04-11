import classnames from "classnames";
import sanitizeHtml from "sanitize-html";

import styles from "./Table.module.scss";

interface RowData {
    columnKey: string;
    content: string | HTMLElement;
    modalContent?: string | HTMLElement;
}

export interface TableProps {
    borderless?: boolean;
    children: React.ReactNode;
    compact?: boolean;
    emptyState?: boolean;
    fullWidth?: boolean;
    scrollable?: boolean;
    sortable?: boolean;
    stackedStyle?: "default" | "headers";
    striped?: boolean;
    rowData: RowData[][];
}

export const Table = ({
    borderless,
    children,
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
            </table>
        </div>
    );
};
