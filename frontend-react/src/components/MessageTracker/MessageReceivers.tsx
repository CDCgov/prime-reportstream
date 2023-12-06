import React from "react";
import classnames from "classnames";

import { ReceiverData } from "../../config/endpoints/messageTracker";
import { parseFileLocation } from "../../utils/misc";
import { Table } from "../../shared/Table/Table";

const NO_DATA_STRING = "N/A";

export type MessageReceiverProps = {
    receiverDetails: ReceiverData[];
};

export interface NormalizedReceiverData {
    name: string;
    service: string;
    date: string;
    reportId: string;
    main: "batch" | "process" | "ready";
    sub: string;
    fileName: string;
    transportResults: string;
}

export interface MessageReceiversRowProps {
    receiver: NormalizedReceiverData;
    activeColumn?: NormalizedReceiverKey;
    activeColumnSortOrder: string;
    handleCellClick: (title: string, body: string) => void;
}

export interface MessageReceiversColProps {
    columnKey: NormalizedReceiverKey;
    columnHeaderTitle: ColumnDataTitle;
    activeColumn?: NormalizedReceiverKey;
    setActiveColumn: (col: NormalizedReceiverKey) => void;
    activeColumnSortOrder: FilterOption;
    setActiveColumnSortOrder: (sortOrder: FilterOption) => void;
    rowSpan: number;
}

export const dateTimeFormatter = new Intl.DateTimeFormat("en-US", {
    month: "2-digit",
    day: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
});

export const ColumnDataTitles = {
    name: "Name",
    service: "Service",
    date: "Date",
    reportId: "Report Id",
    main: "Main",
    sub: "Sub",
    fileName: "File Name",
    transportResults: "Transport Results",
} as const satisfies {
    [k in keyof NormalizedReceiverData]: string;
};
export type ColumnDataTitle =
    (typeof ColumnDataTitles)[keyof typeof ColumnDataTitles];
export type NormalizedReceiverKey = keyof typeof ColumnDataTitles;

const FilterOptionsEnum = {
    NONE: "none",
    ASC: "asc",
    DESC: "desc",
} as const;
export type FilterOption =
    (typeof FilterOptionsEnum)[keyof typeof FilterOptionsEnum];

export const StatusEnum = {
    BATCH: "batch",
    PROCESS: "process",
    READY: "ready",
} as const satisfies {
    [k in NormalizedReceiverData["main"] as Uppercase<k>]: k;
};
export type Status = (typeof StatusEnum)[keyof typeof StatusEnum];

export const MessageReceivers = ({ receiverDetails }: MessageReceiverProps) => {
    const formattedTableData = receiverDetails.map((row) => {
        return [
            {
                columnKey: "name",
                columnHeader: "Name",
                content: row.receivingOrg,
            },
            {
                columnKey: "service",
                columnHeader: "Service",
                content: row.receivingOrgSvc,
            },
            {
                columnKey: "date",
                columnHeader: "Date",
                content: dateTimeFormatter.format(new Date(row.createdAt)),
            },
            {
                columnKey: "reportId",
                columnHeader: "Report Id",
                content: row.reportId,
            },
            {
                columnKey: "fileLocationMain",
                columnHeader: "Main",
                content: (() => {
                    const status = parseFileLocation(
                        row?.fileUrl || NO_DATA_STRING,
                    ).folderLocation;
                    return (
                        <p
                            className={classnames(
                                "font-mono-sm padding-left-1 padding-right-1 margin-top-0",
                                {
                                    "bg-blue-5 border-1px bg-primary-lighter radius-md":
                                        status === StatusEnum.BATCH,
                                    "bg-blue-10 border-1px bg-primary-lighter radius-md":
                                        status === StatusEnum.PROCESS,
                                    "bg-blue-20 border-1px bg-primary-lighter radius-md":
                                        status === StatusEnum.READY,
                                },
                            )}
                        >
                            {status.toLocaleUpperCase()}
                        </p>
                    );
                })(),
            },
            {
                columnKey: "fileLocationSub",
                columnHeader: "Sub",
                content: parseFileLocation(row?.fileUrl || NO_DATA_STRING)
                    .sendingOrg,
            },
            {
                columnKey: "fileLocationFileName",
                columnHeader: "File Name",
                content: parseFileLocation(row?.fileUrl || NO_DATA_STRING)
                    .fileName,
            },
            {
                columnKey: "transportResults",
                columnHeader: "Transport Results",
                content: row.transportResult,
            },
        ];
    });
    return (
        <>
            <h2>Receivers:</h2>
            <Table scrollable sticky sortable rowData={formattedTableData} />
        </>
    );
};
