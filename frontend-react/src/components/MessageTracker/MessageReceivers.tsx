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
    const sortedData = useMemo(
        () =>
            activeColumnSortOrder !== FilterOptionsEnum.None
                ? receiverDetails.sort((a, b) => {
                      const fileLocationObj = {
                          a: { ...parseFileLocation(a?.fileUrl || "") },
                          b: { ...parseFileLocation(b?.fileUrl || "") },
                      };
                      const activeColumnA =
                          a[activeColumn as keyof ReceiverData] !== null
                              ? a[activeColumn as keyof ReceiverData]
                              : "";
                      const activeColumnB =
                          b[activeColumn as keyof ReceiverData];
                      if (!activeColumnA || !activeColumnB) return 0;

                      switch (true) {
                          case activeColumn === ColumnDataEnum.Name:
                          case activeColumn === ColumnDataEnum.Service:
                          case activeColumn === ColumnDataEnum.ReportId:
                          case activeColumn === ColumnDataEnum.TransportResults:
                              if (
                                  activeColumnSortOrder ===
                                  FilterOptionsEnum.ASC
                              ) {
                                  return activeColumnA > activeColumnB ? 1 : -1;
                              } else {
                                  return activeColumnA < activeColumnB ? 1 : -1;
                              }
                          case activeColumn === ColumnDataEnum.Date:
                              const dateA = Date.parse(activeColumnA as string);
                              const dateB = Date.parse(activeColumnB as string);
                              if (
                                  activeColumnSortOrder ===
                                  FilterOptionsEnum.ASC
                              ) {
                                  return dateA > dateB ? 1 : -1;
                              } else {
                                  return dateA < dateB ? 1 : -1;
                              }
                          case activeColumn === ColumnDataEnum.Main:
                              if (
                                  activeColumnSortOrder ===
                                  FilterOptionsEnum.ASC
                              ) {
                                  return fileLocationObj.a.folderLocation >
                                      fileLocationObj.b.folderLocation
                                      ? 1
                                      : -1;
                              } else {
                                  return fileLocationObj.a.folderLocation <
                                      fileLocationObj.b.folderLocation
                                      ? 1
                                      : -1;
                              }
                          case activeColumn === ColumnDataEnum.Sub:
                              if (
                                  activeColumnSortOrder ===
                                  FilterOptionsEnum.ASC
                              ) {
                                  return fileLocationObj.a.sendingOrg >
                                      fileLocationObj.b.sendingOrg
                                      ? 1
                                      : -1;
                              } else {
                                  return fileLocationObj.a.sendingOrg <
                                      fileLocationObj.b.sendingOrg
                                      ? 1
                                      : -1;
                              }
                          case activeColumn === ColumnDataEnum.FileName:
                              if (
                                  activeColumnSortOrder ===
                                  FilterOptionsEnum.ASC
                              ) {
                                  return fileLocationObj.a.fileName >
                                      fileLocationObj.b.fileName
                                      ? 1
                                      : -1;
                              } else {
                                  return fileLocationObj.a.fileName <
                                      fileLocationObj.b.fileName
                                      ? 1
                                      : -1;
                              }
                          default:
                              return 0;
                      }
                  })
                : receiverDetails,
        [activeColumn, activeColumnSortOrder, receiverDetails]
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
