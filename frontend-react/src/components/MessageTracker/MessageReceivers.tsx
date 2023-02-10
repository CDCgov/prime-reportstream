import React, { useMemo, useState } from "react";
import { Table, Icon } from "@trussworks/react-uswds";

import { DetailItem } from "../DetailItem/DetailItem";
import { ReceiverData } from "../../config/endpoints/messageTracker";
import { formattedDateFromTimestamp } from "../../utils/DateTimeUtils";

import { QualityFilters } from "./QualityFilters";

type MessageReceiverProps = {
    receiverDetails: ReceiverData[];
};

const ColumnDataEnum = {
    Name: { title: "Name", respKey: "receivingOrg" },
    Service: { title: "Service", respKey: "receivingOrgSvc" },
    Date: { title: "Date", respKey: "createdAt" },
    ReportId: { title: "Report Id", respKey: "reportId" },
    Main: { title: "Main" },
    Sub: { title: "Sub" },
    FileName: { title: "File Name", respKey: "fileName" },
    TransportResults: {
        title: "Transport Results",
        respKey: "transportResult",
    },
};

enum FilterOptions {
    None = "",
    ASC = "asc",
    DESC = "desc",
}

const MessageReceiversColHeader = ({
    columnDataKey,
    activeColumn,
    setActiveColumn,
    activeColumnSortOrder,
    setActiveColumnSortOrder,
    filterIcon,
}) => {
    const isCurrentlyActiveColumn = columnDataKey === activeColumn;
    const handleColHeaderClick = () => {
        if (!isCurrentlyActiveColumn) {
            // Reset active column and sort order on new column click
            setActiveColumn(columnDataKey);
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
            {columnDataKey}{" "}
            {isCurrentlyActiveColumn ? filterIcon : <Icon.UnfoldMore />}
        </th>
    );
};

const MessageReceiversColRow = () => {};

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
    console.log(receiverDetails);
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
                                        columnDataKey={
                                            ColumnDataEnum[key].title
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
                    {receiverDetails.map((receiver, i) => (
                        <tr key={i}>
                            <td>
                                {receiver.receivingOrg
                                    ? receiver.receivingOrg
                                    : "N/A"}
                            </td>
                            <td>
                                {receiver.receivingOrgSvc
                                    ? receiver.receivingOrgSvc
                                    : "N/A"}
                            </td>
                            <td>
                                {receiver.reportId ? receiver.reportId : "N/A"}
                            </td>
                            <td>
                                {receiver.fileName ? receiver.fileName : "N/A"}
                            </td>
                            <td>
                                {receiver.fileUrl ? receiver.fileUrl : "N/A"}
                            </td>
                            <td>
                                {formattedDateFromTimestamp(
                                    receiver.createdAt,
                                    "MMMM DD YYYY"
                                )}
                            </td>
                            <td>
                                {receiver.transportResult
                                    ? receiver.transportResult
                                    : "N/A"}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </Table>
        </>
    );
};
