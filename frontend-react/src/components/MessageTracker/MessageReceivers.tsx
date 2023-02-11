import React, { useMemo, useState } from "react";
import { Table, Icon } from "@trussworks/react-uswds";

import { DetailItem } from "../DetailItem/DetailItem";
import { ReceiverData } from "../../config/endpoints/messageTracker";
import { formattedDateFromTimestamp } from "../../utils/DateTimeUtils";

import { QualityFilters } from "./QualityFilters";
import { parseFileLocation } from "../../utils/misc";

type MessageReceiverProps = {
    receiverDetails: ReceiverData[];
};

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

enum FilterOptions {
    None = "",
    ASC = "asc",
    DESC = "desc",
}

const MessageReceiversColHeader = ({
    columnHeaderTitle,
    activeColumn,
    setActiveColumn,
    activeColumnSortOrder,
    setActiveColumnSortOrder,
    filterIcon,
}) => {
    const isCurrentlyActiveColumn = columnHeaderTitle === activeColumn;
    const handleColHeaderClick = () => {
        if (!isCurrentlyActiveColumn) {
            // Reset active column and sort order on new column click
            setActiveColumn(columnHeaderTitle);
            setActiveColumnSortOrder(FilterOptions.None);
        }

        // Explicitly set the proceeding sort order
        switch (true) {
            case activeColumnSortOrder === FilterOptions.None:
                setActiveColumnSortOrder(FilterOptions.ASC);
                break;
            case activeColumnSortOrder === FilterOptions.ASC:
                setActiveColumnSortOrder(FilterOptions.DESC);
                break;
            case activeColumnSortOrder === FilterOptions.DESC:
                setActiveColumnSortOrder(FilterOptions.None);
                break;
        }
    };
    return (
        <th
            className={
                isCurrentlyActiveColumn &&
                activeColumnSortOrder !== FilterOptions.None
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
}) => {
    const checkForActiveColumn = (colName) =>
        colName === activeColumn && activeColumnSortOrder !== FilterOptions.None
            ? "active-col-td"
            : "";
    const { folderLocation, sendingOrg, fileName } = parseFileLocation(
        receiver.fileUrl
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
            activeColumnSortOrder !== FilterOptions.None
                ? receiverDetails.sort((a, b) => {
                      const { folderLocationA, sendingOrgA, fileNameA } =
                          parseFileLocation(a.fileUrl);
                      const { folderLocationB, sendingOrgB, fileNameB } =
                          parseFileLocation(b.fileUrl);
                      switch (true) {
                          case activeColumn === ColumnDataEnum.Name:
                          case activeColumn === ColumnDataEnum.Service:
                          case activeColumn === ColumnDataEnum.ReportId:
                          case activeColumn === ColumnDataEnum.TransportResults:
                              if (activeColumnSortOrder === FilterOptions.ASC) {
                                  return a[activeColumn] > b[activeColumn]
                                      ? 1
                                      : -1;
                              } else {
                                  return a[activeColumn] < b[activeColumn]
                                      ? 1
                                      : -1;
                              }
                          case activeColumn === ColumnDataEnum.Date:
                              if (activeColumnSortOrder === FilterOptions.ASC) {
                                  return new Date(a[activeColumn]) >
                                      new Date(b[activeColumn])
                                      ? 1
                                      : -1;
                              } else {
                                  return new Date(a[activeColumn]) <
                                      new Date(b[activeColumn])
                                      ? 1
                                      : -1;
                              }
                          case activeColumn === ColumnDataEnum.Main:
                              if (activeColumnSortOrder === FilterOptions.ASC) {
                                  return folderLocationA > folderLocationB
                                      ? 1
                                      : -1;
                              } else {
                                  return folderLocationA < folderLocationB
                                      ? 1
                                      : -1;
                              }
                          case activeColumn === ColumnDataEnum.Sub:
                              if (activeColumnSortOrder === FilterOptions.ASC) {
                                  return sendingOrgA > sendingOrgB ? 1 : -1;
                              } else {
                                  return sendingOrgA < sendingOrgB ? 1 : -1;
                              }
                          case activeColumn === ColumnDataEnum.FileName:
                              if (activeColumnSortOrder === FilterOptions.ASC) {
                                  return fileNameA > fileNameB ? 1 : -1;
                              } else {
                                  return fileNameA < fileNameB ? 1 : -1;
                              }
                      }
                  })
                : receiverDetails,
        [activeColumn, activeColumnSortOrder]
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
                                        columnHeaderTitle={ColumnDataEnum[key]}
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
