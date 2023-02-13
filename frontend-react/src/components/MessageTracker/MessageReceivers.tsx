import React, { useMemo, useState } from "react";
import { Table, Icon } from "@trussworks/react-uswds";

import { ReceiverData } from "../../config/endpoints/messageTracker";
import { formattedDateFromTimestamp } from "../../utils/DateTimeUtils";
import { parseFileLocation } from "../../utils/misc";

type MessageReceiverProps = {
    receiverDetails: ReceiverData[];
};

interface MessageReceiversColRowProps {
    receiver: ReceiverData;
    activeColumn: string;
    activeColumnSortOrder: string;
}

interface MessageReceiversColHeaderProps {
    columnHeaderTitle: string;
    activeColumn: string;
    setActiveColumn: (colTitle: string) => void;
    activeColumnSortOrder: string;
    setActiveColumnSortOrder: (sortOrder: string) => void;
    filterIcon: JSX.Element;
}

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

const MessageReceiversColHeader = ({
    columnHeaderTitle,
    activeColumn,
    setActiveColumn,
    activeColumnSortOrder,
    setActiveColumnSortOrder,
    filterIcon,
}: MessageReceiversColHeaderProps) => {
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
            scope="col"
            onClick={handleColHeaderClick}
        >
            {columnHeaderTitle}{" "}
            {isCurrentlyActiveColumn ? filterIcon : <Icon.UnfoldMore />}
        </th>
    );
};

const MessageReceiversColRow = ({
    receiver,
    activeColumn,
    activeColumnSortOrder,
}: MessageReceiversColRowProps) => {
    const checkForActiveColumn = (colName: string) =>
        colName === activeColumn &&
        activeColumnSortOrder !== FilterOptionsEnum.None
            ? "active-col-td"
            : "";
    const { folderLocation, sendingOrg, fileName } = parseFileLocation(
        receiver?.fileUrl || ""
    );
    return (
        <tr>
            <td className={checkForActiveColumn(ColumnDataEnum.Name)}>
                {receiver.receivingOrg ? receiver.receivingOrg : "N/A"}
            </td>
            <td className={checkForActiveColumn(ColumnDataEnum.Service)}>
                {receiver.receivingOrgSvc ? receiver.receivingOrgSvc : "N/A"}
            </td>
            <td className={checkForActiveColumn(ColumnDataEnum.Date)}>
                {formattedDateFromTimestamp(receiver.createdAt, "MMMM DD YYYY")}
            </td>
            <td className={checkForActiveColumn(ColumnDataEnum.ReportId)}>
                {receiver.reportId ? receiver.reportId : "N/A"}
            </td>
            <td className={checkForActiveColumn(ColumnDataEnum.Main)}>
                {folderLocation ? folderLocation : "N/A"}
            </td>
            <td className={checkForActiveColumn(ColumnDataEnum.Sub)}>
                {sendingOrg ? sendingOrg : "N/A"}
            </td>
            <td className={checkForActiveColumn(ColumnDataEnum.FileName)}>
                {fileName ? fileName : "N/A"}
            </td>
            <td
                className={checkForActiveColumn(
                    ColumnDataEnum.TransportResults
                )}
            >
                {receiver.transportResult ? receiver.transportResult : "N/A"}
            </td>
        </tr>
    );
};

export const MessageReceivers = ({ receiverDetails }: MessageReceiverProps) => {
    const [activeColumn, setActiveColumn] = useState("");
    const [activeColumnSortOrder, setActiveColumnSortOrder] = useState("");
    const filterIcon = useMemo(
        () =>
            activeColumnSortOrder === "asc" ? (
                <Icon.ExpandLess />
            ) : activeColumnSortOrder === "desc" ? (
                <Icon.ExpandMore />
            ) : (
                <Icon.UnfoldMore />
            ),
        [activeColumnSortOrder]
    );
    const normalizedData = useMemo(
        () =>
            receiverDetails.map((receiverItem) => {
                const formattedData = {};
                for (const key of Object.keys(ColumnDataEnum)) {
                    const columnTitle = ColumnDataEnum[key];
                    let propertyData = "N/A";
                    switch (true) {
                        case columnTitle === ColumnDataEnum.Name:
                            if (receiverItem.receivingOrg)
                                propertyData = receiverItem.receivingOrg;
                            break;
                        case columnTitle === ColumnDataEnum.Service:
                            if (receiverItem.receivingOrgSvc)
                                propertyData = receiverItem.receivingOrgSvc;
                            break;
                        case columnTitle === ColumnDataEnum.Date:
                            if (receiverItem.createdAt)
                                propertyData = formattedDateFromTimestamp(
                                    receiverItem.createdAt,
                                    "MMMM DD YYYY"
                                );
                            break;
                        case columnTitle === ColumnDataEnum.ReportId:
                            if (receiverItem.reportId)
                                propertyData = receiverItem.reportId;
                            break;
                        case columnTitle === ColumnDataEnum.Main:
                            const { folderLocation } = parseFileLocation(
                                receiverItem?.fileUrl || ""
                            );
                            propertyData = folderLocation;
                            break;
                        case columnTitle === ColumnDataEnum.Sub:
                            const { sendingOrg } = parseFileLocation(
                                receiverItem?.fileUrl || ""
                            );
                            propertyData = sendingOrg;
                            break;
                        case columnTitle === ColumnDataEnum.FileName:
                            const { fileName } = parseFileLocation(
                                receiverItem?.fileUrl || ""
                            );
                            propertyData = fileName;
                            break;
                        case columnTitle === ColumnDataEnum.TransportResults:
                            if (receiverItem.transportResult)
                                propertyData = receiverItem.transportResult;
                            break;
                    }
                    formattedData[key] = propertyData;
                }
                return formattedData;
            }),
        [receiverDetails]
    );
    console.log("normalizedData = ", normalizedData);
    const sortedData = useMemo(
        () =>
            activeColumnSortOrder !== FilterOptionsEnum.None
                ? normalizedData.sort((a, b) => {
                      const activeColumnAData =
                          activeColumn === ColumnDataEnum.Date
                              ? Date.parse(a[activeColumn])
                              : a[activeColumn];
                      const activeColumnBData =
                          activeColumn === ColumnDataEnum.Date
                              ? Date.parse(b[activeColumn])
                              : b[activeColumn];

                      if (activeColumnSortOrder === FilterOptionsEnum.ASC) {
                          return activeColumnAData > activeColumnBData ? 1 : -1;
                      } else {
                          return activeColumnAData < activeColumnBData ? 1 : -1;
                      }
                  })
                : receiverDetails,
        [activeColumn, activeColumnSortOrder, normalizedData, receiverDetails]
    );
    return (
        <>
            <h2>Receivers:</h2>

            <Table key="messagedetails" aria-label="Message Details" bordered>
                <thead>
                    <tr>
                        {Object.keys(ColumnDataEnum).map(
                            (key: string, index) => {
                                return (
                                    <MessageReceiversColHeader
                                        key={index}
                                        columnHeaderTitle={
                                            ColumnDataEnum[
                                                key as keyof typeof ColumnDataEnum
                                            ]
                                        }
                                        activeColumn={activeColumn}
                                        setActiveColumn={setActiveColumn}
                                        activeColumnSortOrder={
                                            activeColumnSortOrder
                                        }
                                        setActiveColumnSortOrder={
                                            setActiveColumnSortOrder
                                        }
                                        filterIcon={filterIcon}
                                    />
                                );
                            }
                        )}
                    </tr>
                </thead>
                <tbody style={{ wordBreak: "break-all" }}>
                    {sortedData.map((receiver, index) => (
                        <MessageReceiversColRow
                            key={index}
                            receiver={receiver}
                            activeColumn={activeColumn}
                            activeColumnSortOrder={activeColumnSortOrder}
                        />
                    ))}
                </tbody>
            </Table>
        </>
    );
};
