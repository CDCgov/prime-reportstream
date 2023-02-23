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

type MessageReceiverProps = {
    receiverDetails: ReceiverData[];
};

interface NormalizedReceiverData {
    Name: string;
    Service: string;
    Date: string;
    ReportId: string;
    Main: "Batch" | "Process" | "Ready";
    Sub: string;
    FileName: string;
    TransportResults: string;
}

interface MessageReceiversRowProps {
    receiver: NormalizedReceiverData;
    activeColumn: string;
    activeColumnSortOrder: string;
    handleCellClick: (title: string, body: string) => void;
}

interface MessageReceiversColProps {
    columnHeaderTitle: string;
    activeColumn: string;
    setActiveColumn: (colTitle: string) => void;
    activeColumnSortOrder: string;
    setActiveColumnSortOrder: (sortOrder: string) => void;
    rowSpan: number;
}

const dateTimeFormatter = new Intl.DateTimeFormat("en-US", {
    month: "2-digit",
    day: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
});

const ColumnDataEnum = {
    Name: "Name",
    Service: "Service",
    Date: "Date",
    ReportId: "Report Id",
    Main: "Main",
    Sub: "Sub",
    FileName: "File Name",
    TransportResults: "Transport Results",
};

const FilterOptionsEnum = {
    NONE: "none",
    ASC: "asc",
    DESC: "desc",
};

const StatusEnum = {
    BATCH: "BATCH",
    PROCESS: "PROCESS",
    READY: "READY",
};

const MessageReceiversCol = ({
    columnHeaderTitle,
    activeColumn,
    setActiveColumn,
    activeColumnSortOrder,
    setActiveColumnSortOrder,
    rowSpan,
}: MessageReceiversColProps) => {
    const isCurrentlyActiveColumn = columnHeaderTitle === activeColumn;
    const handleColHeaderClick = () => {
        if (!isCurrentlyActiveColumn) {
            // Reset active column and sort order on new column click
            setActiveColumn(columnHeaderTitle);
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
                            columnHeaderTitle === ColumnDataEnum.FileName,
                    })}
                >
                    {columnHeaderTitle}
                </p>
                {<SortIcon />}
            </div>
        </th>
    );
};

const MessageReceiversRow = ({
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

    const uppercaseMain = receiver.Main.toUpperCase();
    return (
        <tr>
            <td className={getColumnClasses(ColumnDataEnum.Name)}>
                {receiver.Name}
            </td>
            <td className={getColumnClasses(ColumnDataEnum.Service)}>
                {receiver.Service}
            </td>
            <td className={getColumnClasses(ColumnDataEnum.Date)}>
                {receiver.Date}
            </td>
            <td
                role="button"
                tabIndex={0}
                className={`message-receiver-break-word ${getColumnClasses(
                    ColumnDataEnum.ReportId
                )}`}
                onClick={() => {
                    handleCellClick(ColumnDataEnum.ReportId, receiver.ReportId);
                }}
            >
                {receiver.ReportId}
            </td>
            <td className={getColumnClasses(ColumnDataEnum.Main)}>
                <p
                    className={classnames(
                        "font-mono-sm border-1px bg-primary-lighter radius-md padding-left-1 padding-right-1 margin-top-0",
                        {
                            "bg-blue-5": uppercaseMain === StatusEnum.BATCH,
                            "bg-blue-10": uppercaseMain === StatusEnum.PROCESS,
                            "bg-blue-20": uppercaseMain === StatusEnum.READY,
                        }
                    )}
                >
                    {uppercaseMain}
                </p>
            </td>
            <td className={getColumnClasses(ColumnDataEnum.Sub)}>
                {receiver.Sub}
            </td>
            <td
                role="button"
                tabIndex={0}
                className={`message-receiver-break-word ${getColumnClasses(
                    ColumnDataEnum.FileName
                )}`}
                onClick={() => {
                    handleCellClick(ColumnDataEnum.FileName, receiver.FileName);
                }}
            >
                {receiver.FileName}
            </td>
            <td
                role="button"
                tabIndex={0}
                className={`message-receiver-break-word ${getColumnClasses(
                    ColumnDataEnum.TransportResults
                )}`}
                onClick={() => {
                    handleCellClick(
                        ColumnDataEnum.TransportResults,
                        receiver.TransportResults
                    );
                }}
            >
                {receiver.TransportResults}
            </td>
        </tr>
    );
};

export const MessageReceivers = ({ receiverDetails }: MessageReceiverProps) => {
    const modalRef = useRef<ModalRef>(null);
    const [modalText, setModalText] = useState({ title: "", body: "" });
    const [activeColumn, setActiveColumn] = useState("");
    const [activeColumnSortOrder, setActiveColumnSortOrder] = useState(
        FilterOptionsEnum.NONE
    );
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
    //     Name: string;
    //     Service: string;
    //     Date: string;
    //     ReportId: string;
    //     Main: string;
    //     Sub: string;
    //     FileName: string;
    //     TransportResults: string;
    // }
    const normalizedData = useMemo(
        () =>
            receiverDetails.map(
                (receiverItem: ReceiverData): NormalizedReceiverData => {
                    const formattedData = Object.keys(ColumnDataEnum).reduce(
                        (accumulator: any, currentValue: string) => {
                            accumulator[currentValue] = "N/A";
                            return accumulator;
                        },
                        {}
                    );
                    for (const key of Object.keys(ColumnDataEnum)) {
                        const columnTitle =
                            ColumnDataEnum[key as keyof typeof ColumnDataEnum];
                        switch (columnTitle) {
                            case ColumnDataEnum.Name:
                                if (receiverItem.receivingOrg)
                                    formattedData.Name =
                                        receiverItem.receivingOrg;
                                break;
                            case ColumnDataEnum.Service:
                                if (receiverItem.receivingOrgSvc)
                                    formattedData.Service =
                                        receiverItem.receivingOrgSvc;
                                break;
                            case ColumnDataEnum.Date:
                                if (receiverItem.createdAt)
                                    formattedData.Date =
                                        dateTimeFormatter.format(
                                            new Date(receiverItem.createdAt)
                                        );

                                break;
                            case ColumnDataEnum.ReportId:
                                if (receiverItem.reportId)
                                    formattedData.ReportId =
                                        receiverItem.reportId;
                                break;
                            case ColumnDataEnum.Main:
                                const { folderLocation } = parseFileLocation(
                                    receiverItem?.fileUrl || ""
                                );
                                formattedData.Main = folderLocation;
                                break;
                            case ColumnDataEnum.Sub:
                                const { sendingOrg } = parseFileLocation(
                                    receiverItem?.fileUrl || ""
                                );
                                formattedData.Sub = sendingOrg;
                                break;
                            case ColumnDataEnum.FileName:
                                const { fileName } = parseFileLocation(
                                    receiverItem?.fileUrl || ""
                                );
                                formattedData.FileName = fileName;
                                break;
                            case ColumnDataEnum.TransportResults:
                                if (receiverItem.transportResult)
                                    formattedData.TransportResults =
                                        receiverItem.transportResult;
                                break;
                        }
                    }
                    return formattedData;
                }
            ),
        [receiverDetails]
    );
    const sortedData = useMemo(
        // All of the normalized data can be sorted by the same sort algo
        // which requires a return of -1 0 1. For dates, we need to
        // convert them to a numerical value.
        () =>
            activeColumnSortOrder !== FilterOptionsEnum.NONE
                ? normalizedData.sort(
                      (
                          a: NormalizedReceiverData,
                          b: NormalizedReceiverData
                      ): number => {
                          const activeColumnAData =
                              activeColumn === ColumnDataEnum.Date
                                  ? Date.parse(
                                        a[
                                            activeColumn as keyof typeof ColumnDataEnum
                                        ]
                                    )
                                  : a[
                                        activeColumn as keyof typeof ColumnDataEnum
                                    ];
                          const activeColumnBData =
                              activeColumn === ColumnDataEnum.Date
                                  ? Date.parse(
                                        b[
                                            activeColumn as keyof typeof ColumnDataEnum
                                        ]
                                    )
                                  : b[
                                        activeColumn as keyof typeof ColumnDataEnum
                                    ];

                          if (activeColumnSortOrder === FilterOptionsEnum.ASC) {
                              return activeColumnAData > activeColumnBData
                                  ? 1
                                  : -1;
                          } else {
                              return activeColumnAData < activeColumnBData
                                  ? 1
                                  : -1;
                          }
                      }
                  )
                : normalizedData,
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
                                columnHeaderTitle={ColumnDataEnum.Name}
                                rowSpan={2}
                            />
                            <MessageReceiversCol
                                {...commonProps}
                                columnHeaderTitle={ColumnDataEnum.Service}
                                rowSpan={2}
                            />
                            <MessageReceiversCol
                                {...commonProps}
                                columnHeaderTitle={ColumnDataEnum.Date}
                                rowSpan={2}
                            />
                            <MessageReceiversCol
                                {...commonProps}
                                columnHeaderTitle={ColumnDataEnum.ReportId}
                                rowSpan={2}
                            />
                            <th colSpan={3}>
                                <p>File Location</p>
                            </th>
                            <MessageReceiversCol
                                {...commonProps}
                                columnHeaderTitle={
                                    ColumnDataEnum.TransportResults
                                }
                                rowSpan={2}
                            />
                        </tr>
                        <tr>
                            <MessageReceiversCol
                                {...commonProps}
                                columnHeaderTitle={ColumnDataEnum.Main}
                                rowSpan={1}
                            />
                            <MessageReceiversCol
                                {...commonProps}
                                columnHeaderTitle={ColumnDataEnum.Sub}
                                rowSpan={1}
                            />
                            <MessageReceiversCol
                                {...commonProps}
                                columnHeaderTitle={ColumnDataEnum.FileName}
                                rowSpan={1}
                            />
                        </tr>
                    </thead>
                    <tbody>
                        {sortedData.map(
                            (receiver: NormalizedReceiverData, index) => (
                                <MessageReceiversRow
                                    key={index}
                                    receiver={receiver}
                                    activeColumn={activeColumn}
                                    activeColumnSortOrder={
                                        activeColumnSortOrder
                                    }
                                    handleCellClick={handleCellClick}
                                />
                            )
                        )}
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
