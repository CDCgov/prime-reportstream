import React, { useMemo, useState } from "react";
import { Table, Icon } from "@trussworks/react-uswds";

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
    Main: string;
    Sub: string;
    FileName: string;
    TransportResults: string;
}

interface MessageReceiversRowProps {
    receiver: NormalizedReceiverData;
    activeColumn: string;
    activeColumnSortOrder: string;
}

interface MessageReceiversColProps {
    columnHeaderTitle: string;
    activeColumn: string;
    setActiveColumn: (colTitle: string) => void;
    activeColumnSortOrder: string;
    setActiveColumnSortOrder: (sortOrder: string) => void;
    filterIcon: JSX.Element;
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
    None: "",
    ASC: "asc",
    DESC: "desc",
};

const MessageReceiversCol = ({
    columnHeaderTitle,
    activeColumn,
    setActiveColumn,
    activeColumnSortOrder,
    setActiveColumnSortOrder,
    filterIcon,
    rowSpan,
}: MessageReceiversColProps) => {
    const isCurrentlyActiveColumn = columnHeaderTitle === activeColumn;
    const handleColHeaderClick = () => {
        if (!isCurrentlyActiveColumn) {
            // Reset active column and sort order on new column click
            setActiveColumn(columnHeaderTitle);
            setActiveColumnSortOrder(FilterOptionsEnum.None);
        }

        // Explicitly set the proceeding sort order
        switch (true) {
            case activeColumnSortOrder === FilterOptionsEnum.None:
                setActiveColumnSortOrder(FilterOptionsEnum.ASC);
                break;
            case activeColumnSortOrder === FilterOptionsEnum.ASC:
                setActiveColumnSortOrder(FilterOptionsEnum.DESC);
                break;
            case activeColumnSortOrder === FilterOptionsEnum.DESC:
                setActiveColumnSortOrder(FilterOptionsEnum.None);
                break;
        }
    };
    return (
        <th
            className={
                isCurrentlyActiveColumn &&
                activeColumnSortOrder !== FilterOptionsEnum.None
                    ? "active-col-header"
                    : ""
            }
            onClick={handleColHeaderClick}
            rowSpan={rowSpan}
        >
            <div className="column-header-title-container">
                <p>{columnHeaderTitle}</p>
                {isCurrentlyActiveColumn ? filterIcon : <Icon.UnfoldMore />}
            </div>
        </th>
    );
};

const MessageReceiversRow = ({
    receiver,
    activeColumn,
    activeColumnSortOrder,
}: MessageReceiversRowProps) => {
    const checkForActiveColumn = (colName: string) =>
        colName === activeColumn &&
        activeColumnSortOrder !== FilterOptionsEnum.None
            ? "active-col-td"
            : "";
    return (
        <tr>
            <td className={checkForActiveColumn(ColumnDataEnum.Name)}>
                {receiver.Name}
            </td>
            <td className={checkForActiveColumn(ColumnDataEnum.Service)}>
                {receiver.Service}
            </td>
            <td className={checkForActiveColumn(ColumnDataEnum.Date)}>
                {receiver.Date}
            </td>
            <td
                className={`message-receiver-break-word ${checkForActiveColumn(
                    ColumnDataEnum.ReportId
                )}`}
            >
                {receiver.ReportId}
            </td>
            <td className={checkForActiveColumn(ColumnDataEnum.ReportId)}>
                <p className="font-mono-sm border-1px bg-primary-lighter radius-md padding-top-4px padding-bottom-4px padding-left-1 padding-right-1">
                    {receiver.Main.toUpperCase()}
                </p>
            </td>
            <td className={checkForActiveColumn(ColumnDataEnum.Sub)}>
                {receiver.Sub}
            </td>
            <td
                className={`message-receiver-break-word ${checkForActiveColumn(
                    ColumnDataEnum.FileName
                )}`}
            >
                {receiver.FileName}
            </td>
            <td
                className={checkForActiveColumn(
                    ColumnDataEnum.TransportResults
                )}
            >
                {receiver.TransportResults}
            </td>
        </tr>
    );
};

export const MessageReceivers = ({ receiverDetails }: MessageReceiverProps) => {
    const [activeColumn, setActiveColumn] = useState("");
    const [activeColumnSortOrder, setActiveColumnSortOrder] = useState("");
    const filterIcon = useMemo(
        () =>
            activeColumnSortOrder === FilterOptionsEnum.ASC ? (
                <Icon.ExpandLess />
            ) : activeColumnSortOrder === FilterOptionsEnum.DESC ? (
                <Icon.ExpandMore />
            ) : (
                <Icon.UnfoldMore />
            ),
        [activeColumnSortOrder]
    );
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
                        switch (true) {
                            case columnTitle === ColumnDataEnum.Name:
                                if (receiverItem.receivingOrg)
                                    formattedData.Name =
                                        receiverItem.receivingOrg;
                                break;
                            case columnTitle === ColumnDataEnum.Service:
                                if (receiverItem.receivingOrgSvc)
                                    formattedData.Service =
                                        receiverItem.receivingOrgSvc;
                                break;
                            case columnTitle === ColumnDataEnum.Date:
                                if (receiverItem.createdAt)
                                    formattedData.Date =
                                        dateTimeFormatter.format(
                                            new Date(receiverItem.createdAt)
                                        );

                                break;
                            case columnTitle === ColumnDataEnum.ReportId:
                                if (receiverItem.reportId)
                                    formattedData.ReportId =
                                        receiverItem.reportId;
                                break;
                            case columnTitle === ColumnDataEnum.Main:
                                const { folderLocation } = parseFileLocation(
                                    receiverItem?.fileUrl || ""
                                );
                                formattedData.Main = folderLocation;
                                break;
                            case columnTitle === ColumnDataEnum.Sub:
                                const { sendingOrg } = parseFileLocation(
                                    receiverItem?.fileUrl || ""
                                );
                                formattedData.Sub = sendingOrg;
                                break;
                            case columnTitle === ColumnDataEnum.FileName:
                                const { fileName } = parseFileLocation(
                                    receiverItem?.fileUrl || ""
                                );
                                formattedData.FileName = fileName;
                                break;
                            case columnTitle ===
                                ColumnDataEnum.TransportResults:
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
        () =>
            activeColumnSortOrder !== FilterOptionsEnum.None
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
    return (
        <>
            <h2>Receivers:</h2>

            <Table key="messagedetails" aria-label="Message Details" bordered>
                <thead>
                    <tr>
                        <MessageReceiversCol
                            columnHeaderTitle={ColumnDataEnum.Name}
                            activeColumn={activeColumn}
                            setActiveColumn={setActiveColumn}
                            activeColumnSortOrder={activeColumnSortOrder}
                            setActiveColumnSortOrder={setActiveColumnSortOrder}
                            filterIcon={filterIcon}
                            rowSpan={2}
                        />
                        <MessageReceiversCol
                            columnHeaderTitle={ColumnDataEnum.Service}
                            activeColumn={activeColumn}
                            setActiveColumn={setActiveColumn}
                            activeColumnSortOrder={activeColumnSortOrder}
                            setActiveColumnSortOrder={setActiveColumnSortOrder}
                            filterIcon={filterIcon}
                            rowSpan={2}
                        />
                        <MessageReceiversCol
                            columnHeaderTitle={ColumnDataEnum.Date}
                            activeColumn={activeColumn}
                            setActiveColumn={setActiveColumn}
                            activeColumnSortOrder={activeColumnSortOrder}
                            setActiveColumnSortOrder={setActiveColumnSortOrder}
                            filterIcon={filterIcon}
                            rowSpan={2}
                        />
                        <MessageReceiversCol
                            columnHeaderTitle={ColumnDataEnum.ReportId}
                            activeColumn={activeColumn}
                            setActiveColumn={setActiveColumn}
                            activeColumnSortOrder={activeColumnSortOrder}
                            setActiveColumnSortOrder={setActiveColumnSortOrder}
                            filterIcon={filterIcon}
                            rowSpan={2}
                        />
                        <th colSpan={3}>File Location</th>
                        <MessageReceiversCol
                            columnHeaderTitle={ColumnDataEnum.TransportResults}
                            activeColumn={activeColumn}
                            setActiveColumn={setActiveColumn}
                            activeColumnSortOrder={activeColumnSortOrder}
                            setActiveColumnSortOrder={setActiveColumnSortOrder}
                            filterIcon={filterIcon}
                            rowSpan={2}
                        />
                    </tr>
                    <tr>
                        <MessageReceiversCol
                            columnHeaderTitle={ColumnDataEnum.Main}
                            activeColumn={activeColumn}
                            setActiveColumn={setActiveColumn}
                            activeColumnSortOrder={activeColumnSortOrder}
                            setActiveColumnSortOrder={setActiveColumnSortOrder}
                            filterIcon={filterIcon}
                            rowSpan={1}
                        />
                        <MessageReceiversCol
                            columnHeaderTitle={ColumnDataEnum.Sub}
                            activeColumn={activeColumn}
                            setActiveColumn={setActiveColumn}
                            activeColumnSortOrder={activeColumnSortOrder}
                            setActiveColumnSortOrder={setActiveColumnSortOrder}
                            filterIcon={filterIcon}
                            rowSpan={1}
                        />
                        <MessageReceiversCol
                            columnHeaderTitle={ColumnDataEnum.FileName}
                            activeColumn={activeColumn}
                            setActiveColumn={setActiveColumn}
                            activeColumnSortOrder={activeColumnSortOrder}
                            setActiveColumnSortOrder={setActiveColumnSortOrder}
                            filterIcon={filterIcon}
                            rowSpan={1}
                        />
                    </tr>
                </thead>
                <tbody className="message-receivers-table">
                    {sortedData.map(
                        (receiver: NormalizedReceiverData, index) => (
                            <MessageReceiversRow
                                key={index}
                                receiver={receiver}
                                activeColumn={activeColumn}
                                activeColumnSortOrder={activeColumnSortOrder}
                            />
                        )
                    )}
                </tbody>
            </Table>
        </>
    );
};
