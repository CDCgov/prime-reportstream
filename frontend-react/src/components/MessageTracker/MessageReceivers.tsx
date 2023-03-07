import React, { useMemo, useRef, useState } from "react";
import {
    Table,
    Icon,
    Modal,
    ModalRef,
    ModalHeading,
} from "@trussworks/react-uswds";
import classnames from "classnames";

import { ReceiverData } from "../../config/endpoints/messageTracker";
import { parseFileLocation } from "../../utils/misc";

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

export const MessageReceiversCol = ({
    columnKey,
    columnHeaderTitle,
    activeColumn,
    setActiveColumn,
    activeColumnSortOrder,
    setActiveColumnSortOrder,
    rowSpan,
}: MessageReceiversColProps) => {
    const isCurrentlyActiveColumn = columnKey === activeColumn;
    const handleColHeaderClick = () => {
        if (!isCurrentlyActiveColumn) {
            // Reset active column and sort order on new column click
            setActiveColumn(columnKey);
            setActiveColumnSortOrder(FilterOptionsEnum.ASC);
        } else if (activeColumnSortOrder === FilterOptionsEnum.NONE) {
            // Explicitly set the proceeding sort order
            setActiveColumnSortOrder(FilterOptionsEnum.ASC);
        } else if (activeColumnSortOrder === FilterOptionsEnum.ASC) {
            setActiveColumnSortOrder(FilterOptionsEnum.DESC);
        } else if (activeColumnSortOrder === FilterOptionsEnum.DESC) {
            setActiveColumnSortOrder(FilterOptionsEnum.NONE);
        }
    };
    let SortIcon = Icon.SortArrow;
    if (
        isCurrentlyActiveColumn &&
        activeColumnSortOrder === FilterOptionsEnum.ASC
    ) {
        SortIcon = Icon.ArrowUpward;
    } else if (
        isCurrentlyActiveColumn &&
        activeColumnSortOrder === FilterOptionsEnum.DESC
    ) {
        SortIcon = Icon.ArrowDownward;
    }
    return (
        <th
            role="button"
            tabIndex={0}
            className={classnames({
                "active-col-header":
                    isCurrentlyActiveColumn &&
                    activeColumnSortOrder !== FilterOptionsEnum.NONE,
            })}
            onClick={handleColHeaderClick}
            rowSpan={rowSpan}
        >
            <div className="column-header-title-container">
                <p
                    className={classnames({
                        "text-pre":
                            columnHeaderTitle === ColumnDataTitles.fileName,
                    })}
                >
                    {columnHeaderTitle}
                </p>
                {<SortIcon />}
            </div>
        </th>
    );
};

export const MessageReceiversRow = ({
    receiver,
    activeColumn,
    activeColumnSortOrder,
    handleCellClick,
}: MessageReceiversRowProps) => {
    // We highlight the entire active column so within the row,
    // we need to know which column is active
    const getColumnClasses = (colName: string) =>
        classnames({
            "active-col-td":
                colName === activeColumn &&
                activeColumnSortOrder !== FilterOptionsEnum.NONE,
        });

    return (
        <tr>
            <td className={getColumnClasses(ColumnDataTitles.name)}>
                {receiver.name}
            </td>
            <td className={getColumnClasses(ColumnDataTitles.service)}>
                {receiver.service}
            </td>
            <td className={getColumnClasses(ColumnDataTitles.date)}>
                {receiver.date}
            </td>
            <td
                role="button"
                tabIndex={0}
                className={`message-receiver-break-word ${getColumnClasses(
                    ColumnDataTitles.reportId
                )}`}
                onClick={() => {
                    handleCellClick(
                        ColumnDataTitles.reportId,
                        receiver.reportId
                    );
                }}
            >
                {receiver.reportId}
            </td>
            <td className={getColumnClasses(ColumnDataTitles.main)}>
                <p
                    className={classnames(
                        "font-mono-sm padding-left-1 padding-right-1 margin-top-0",
                        {
                            "bg-blue-5 border-1px bg-primary-lighter radius-md":
                                receiver.main === StatusEnum.BATCH,
                            "bg-blue-10 border-1px bg-primary-lighter radius-md":
                                receiver.main === StatusEnum.PROCESS,
                            "bg-blue-20 border-1px bg-primary-lighter radius-md":
                                receiver.main === StatusEnum.READY,
                        }
                    )}
                >
                    {receiver.main.toLocaleUpperCase()}
                </p>
            </td>
            <td className={getColumnClasses(ColumnDataTitles.sub)}>
                {receiver.sub}
            </td>
            <td
                role="button"
                tabIndex={0}
                className={`message-receiver-break-word ${getColumnClasses(
                    ColumnDataTitles.fileName
                )}`}
                onClick={() => {
                    handleCellClick(
                        ColumnDataTitles.fileName,
                        receiver.fileName
                    );
                }}
            >
                {receiver.fileName}
            </td>
            <td
                role="button"
                tabIndex={0}
                className={`message-receiver-break-word ${getColumnClasses(
                    ColumnDataTitles.transportResults
                )}`}
                onClick={() => {
                    handleCellClick(
                        ColumnDataTitles.transportResults,
                        receiver.transportResults
                    );
                }}
            >
                {receiver.transportResults}
            </td>
        </tr>
    );
};

// Since we're splicing together both data from receiverDetails && fileUrl
// To simplify any proceeding logic within this component, we need to
// merge and normalize the data. The data currently appears like:
//   {
//     "reportId": "4b3c73df-83b1-48f9-a5a2-ce0c38662f7c",
//     "receivingOrg": "ignore",
//     "receivingOrgSvc": "HL7_NULL",
//     "transportResult": null,
//     "fileName": null,
//     "fileUrl": "http://azurite:10000/devstoreaccount1/reports/batch%2Fignore.HL7_NULL%2Ftx-covid-19-4b3c73df-83b1-48f9-a5a2-ce0c38662f7c-20230203182255.internal.csv",
//     "createdAt": "2023-02-03T18:22:55.580322",
//     "qualityFilters": []
// }
// And we parse fileUrl using parseFileLocation to find: folderLocation, sendingOrg and fileName. We use normalizedData to fix this. If there's no data, we fill in the string with N/A.

// Our expected output should look like the following:
//   NormalizedReceiverData {
//     name: string;
//     service: string;
//     date: string;
//     reportId: string;
//     main: string;
//     sub: string;
//     fileName: string;
//     transportResults: string;
// }
export function formatMessages(data: ReceiverData[]): NormalizedReceiverData[] {
    return data.map((receiverItem: ReceiverData): NormalizedReceiverData => {
        const formattedData: NormalizedReceiverData = Object.keys(
            ColumnDataTitles
        ).reduce((accumulator: any, currentValue: string) => {
            accumulator[currentValue] = NO_DATA_STRING;
            return accumulator;
        }, {});
        for (const key of Object.keys(
            ColumnDataTitles
        ) as NormalizedReceiverKey[]) {
            const columnTitle = ColumnDataTitles[key];
            switch (columnTitle) {
                case ColumnDataTitles.name:
                    if (receiverItem.receivingOrg)
                        formattedData.name = receiverItem.receivingOrg;
                    break;
                case ColumnDataTitles.service:
                    if (receiverItem.receivingOrgSvc)
                        formattedData.service = receiverItem.receivingOrgSvc;
                    break;
                case ColumnDataTitles.date:
                    if (receiverItem.createdAt)
                        formattedData.date = dateTimeFormatter.format(
                            new Date(receiverItem.createdAt)
                        );
                    break;
                case ColumnDataTitles.reportId:
                    if (receiverItem.reportId)
                        formattedData.reportId = receiverItem.reportId;
                    break;
                case ColumnDataTitles.main:
                    const { folderLocation } = parseFileLocation(
                        receiverItem?.fileUrl || NO_DATA_STRING
                    );
                    formattedData.main = folderLocation as Status;
                    break;
                case ColumnDataTitles.sub:
                    const { sendingOrg } = parseFileLocation(
                        receiverItem?.fileUrl || NO_DATA_STRING
                    );
                    formattedData.sub = sendingOrg;
                    break;
                case ColumnDataTitles.fileName:
                    const { fileName } = parseFileLocation(
                        receiverItem?.fileUrl || NO_DATA_STRING
                    );
                    formattedData.fileName = fileName;
                    break;
                case ColumnDataTitles.transportResults:
                    if (receiverItem.transportResult)
                        formattedData.transportResults =
                            receiverItem.transportResult;
                    break;
            }
        }
        return formattedData;
    });
}

// All of the normalized data can be sorted by the same sort algo
// which requires a return of -1 0 1. For dates, we need to
// convert them to a numerical value.
export function sortMessages(
    data: NormalizedReceiverData[],
    column?: NormalizedReceiverKey,
    sortOrder: FilterOption = "none"
) {
    return sortOrder !== FilterOptionsEnum.NONE && column
        ? data.sort(
              (
                  a: NormalizedReceiverData,
                  b: NormalizedReceiverData
              ): number => {
                  const activeColumnAData =
                      column === "date" ? Date.parse(a[column]) : a[column];
                  const activeColumnBData =
                      column === "date" ? Date.parse(b[column]) : b[column];

                  if (sortOrder === FilterOptionsEnum.ASC) {
                      return activeColumnAData > activeColumnBData ? 1 : -1;
                  } else {
                      return activeColumnAData < activeColumnBData ? 1 : -1;
                  }
              }
          )
        : data;
}

export const MessageReceivers = ({ receiverDetails }: MessageReceiverProps) => {
    const modalRef = useRef<ModalRef>(null);
    const [modalText, setModalText] = useState({ title: "", body: "" });
    const [activeColumn, setActiveColumn] = useState<
        NormalizedReceiverKey | undefined
    >();
    const [activeColumnSortOrder, setActiveColumnSortOrder] =
        useState<FilterOption>(FilterOptionsEnum.NONE);
    const normalizedData = useMemo(
        () => formatMessages(receiverDetails),
        [receiverDetails]
    );
    const sortedData = useMemo(
        () => sortMessages(normalizedData, activeColumn, activeColumnSortOrder),
        [activeColumn, activeColumnSortOrder, normalizedData]
    );
    function handleCellClick(title: string, body: string) {
        setModalText({ title: `${title}:`, body });
        modalRef?.current?.toggleModal();
    }
    const commonProps = {
        activeColumn,
        setActiveColumn,
        activeColumnSortOrder,
        setActiveColumnSortOrder,
    };
    return (
        <>
            <h2>Receivers:</h2>
            <div className="message-receivers-table">
                <Table
                    key="messagedetails"
                    aria-label="Message Details"
                    bordered
                >
                    <thead>
                        <tr>
                            <MessageReceiversCol
                                {...commonProps}
                                columnKey={"name"}
                                columnHeaderTitle={ColumnDataTitles.name}
                                rowSpan={2}
                            />
                            <MessageReceiversCol
                                {...commonProps}
                                columnKey={"service"}
                                columnHeaderTitle={ColumnDataTitles.service}
                                rowSpan={2}
                            />
                            <MessageReceiversCol
                                {...commonProps}
                                columnKey={"date"}
                                columnHeaderTitle={ColumnDataTitles.date}
                                rowSpan={2}
                            />
                            <MessageReceiversCol
                                {...commonProps}
                                columnKey={"reportId"}
                                columnHeaderTitle={ColumnDataTitles.reportId}
                                rowSpan={2}
                            />
                            <th colSpan={3}>
                                <p>File Location</p>
                            </th>
                            <MessageReceiversCol
                                {...commonProps}
                                columnKey={"transportResults"}
                                columnHeaderTitle={
                                    ColumnDataTitles.transportResults
                                }
                                rowSpan={2}
                            />
                        </tr>
                        <tr>
                            <MessageReceiversCol
                                {...commonProps}
                                columnKey={"main"}
                                columnHeaderTitle={ColumnDataTitles.main}
                                rowSpan={1}
                            />
                            <MessageReceiversCol
                                {...commonProps}
                                columnKey={"sub"}
                                columnHeaderTitle={ColumnDataTitles.sub}
                                rowSpan={1}
                            />
                            <MessageReceiversCol
                                {...commonProps}
                                columnKey={"fileName"}
                                columnHeaderTitle={ColumnDataTitles.fileName}
                                rowSpan={1}
                            />
                        </tr>
                    </thead>
                    <tbody>
                        {sortedData.map((receiver: NormalizedReceiverData) => (
                            <MessageReceiversRow
                                key={`${receiver.reportId}${receiver.name}${receiver.service}`}
                                receiver={receiver}
                                activeColumn={activeColumn}
                                activeColumnSortOrder={activeColumnSortOrder}
                                handleCellClick={handleCellClick}
                            />
                        ))}
                    </tbody>
                </Table>
            </div>
            <Modal id="message-receivers-modal" ref={modalRef}>
                <ModalHeading>{modalText.title}</ModalHeading>
                <div className="usa-prose">
                    <p>{modalText.body}</p>
                </div>
            </Modal>
        </>
    );
};
