import React, { useRef, useState } from "react";
import { useResource } from "rest-hooks";
import DOMPurify from "dompurify";
import {
    Button,
    ButtonGroup,
    Grid,
    GridContainer,
    Label,
    Modal,
    ModalFooter,
    ModalHeading,
    ModalRef,
    Table,
    TextInput,
} from "@trussworks/react-uswds";

import { AdmSendFailuresResource } from "../../resources/AdmSendFailuresResource";
import { formatDate } from "../../utils/misc";
import { showAlertNotification, showError } from "../AlertNotifications";
import { getStoredOktaToken } from "../../utils/SessionStorageTools";
import AdmAction from "../../resources/AdmActionResource";

// Improves readability
const DRow = (props: React.PropsWithChildren<{ label: string }>) => {
    return (
        <Grid row className={"modal-info-row"}>
            <Grid className={"modal-info-label"}>{props.label}:</Grid>
            <Grid className={"modal-info-value"}>{props.children}</Grid>
        </Grid>
    );
};

const renderInfoModal = (props: { infoDataJson: string }) => {
    if (!props?.infoDataJson?.length || props?.infoDataJson === "{}") {
        return <></>; // happens before any item is clicked
    }
    const infoData = JSON.parse(props.infoDataJson) as AdmSendFailuresResource;

    return (
        <GridContainer className={"rs-admindash-modal-container"}>
            <Grid className={"modal-info-title"}>
                Info Details {infoData.actionId}
            </Grid>
            <DRow label={"Receiving Org"}>{infoData.receiver}</DRow>
            <DRow label={"Failed at"}>{formatDate(infoData.failedAt)}</DRow>
            <DRow label={"Action ID"}>{infoData.actionId}</DRow>
            <DRow label={"Report ID"}>{infoData.reportId}</DRow>
            <DRow label={"File URI"}>
                {infoData.bodyUrl}
                <br />
                {infoData.reportFileReceiver}
            </DRow>
            <DRow label={"Result message"}>{infoData.actionResult}</DRow>
        </GridContainer>
    );
};

// const _renderActionsModal = (props: { infoDataJson: string }) => {
//     if (!props?.infoDataJson?.length || props?.infoDataJson === "{}") {
//         return <></>; // happens before any item is clicked
//     }
//     const infoDataArray = JSON.parse(props.infoDataJson) as AdmAction[];
//
//     return (
//         <GridContainer className={"rs-admindash-modal-container"}>
//             {infoDataArray.map((infoData) => (
//                 <>
//                     <Grid className={"modal-info-title"}>
//                         Resend Details {infoData.actionId}
//                     </Grid>
//                     <DRow label={"Resent at"}>
//                         {formatDate(infoData.createdAt)}
//                     </DRow>
//                     <DRow label={"Resent by"}>{infoData.username}</DRow>
//                     <DRow label={"Result"}>{infoData.actionResult}</DRow>
//                 </>
//             ))}
//         </GridContainer>
//     );
// };

export function AdminLastMileFailuresTable() {
    const defaultDaysToShow = "15"; // numeric input but treat as string for easier passing around
    const [daysToShow, setDaysToShow] = useState(defaultDaysToShow);
    const lastMileData: AdmSendFailuresResource[] = useResource(
        AdmSendFailuresResource.list(),
        { days_to_show: daysToShow }
    );
    const lastMileResends: AdmAction[] = useResource(AdmAction.list(), {
        days_to_show: daysToShow,
    });

    // this is the input box filter
    const [filter, setFilter] = useState("");

    // used to show hide the modal
    const modalShowInfoRef = useRef<ModalRef>(null);
    const [currentJsonDataForModal, setCurrentJsonDataForModal] =
        useState<string>("{}");

    const handleShowDetailsClick = (jsonRowData: string) => {
        setCurrentJsonDataForModal(jsonRowData);
        modalShowInfoRef?.current?.toggleModal(undefined, true);
    };

    const handleShowResendsClick = (jsonRowData: string) => {
        setCurrentJsonDataForModal(jsonRowData);
        modalShowInfoRef?.current?.toggleModal(undefined, true);
    };

    const modalResendRef = useRef<ModalRef>(null); // used to show/hide modal
    // this sets the content of the modal
    const [htmlContentForGithubIssue, setHtmlContentForGithubIssue] =
        useState("");

    const [htmlContentResultText, setHtmlContentResultText] = useState("");

    // these track what the modal is showing so when the confirm button is pressed
    // the handler can easily get to them for the async network fetch
    const [currentReportId, setCurrentReportId] = useState("");
    const [currentReceiver, setCurrentReceiver] = useState("");
    const [loading, setLoading] = useState(false);

    // called from the list when rety button is clicked.
    // all the data is serialized to a json string as a cheap clone.
    const handleRetrySendClick = (jsonRowData: string) => {
        const data = JSON.parse(jsonRowData) as AdmSendFailuresResource;

        // the content has line feeds, etc. so the formatted content isn't tabbed here in the code
        const formatted = `Report ID:
${data.reportId}

File Name:
${data.fileName}

Destination:
${data.receiver}`;

        setHtmlContentForGithubIssue(formatted);
        setCurrentReportId(data.reportId);
        setCurrentReceiver(data.receiver);
        // we clear this value and it's set by the server response
        setHtmlContentResultText("");

        // we need to show confirmation dialog, then do action to trigger resent
        modalResendRef?.current?.toggleModal(undefined, true);
    };

    const closeResendModal = () => {
        modalResendRef?.current?.toggleModal(undefined, false);
    };

    // Trigger a resend by issuing an api call
    const startResend = async () => {
        try {
            setLoading(true);
            setHtmlContentResultText(`Starting...`);
            const url =
                `${process.env.REACT_APP_BACKEND_URL}/api/adm/resend?` +
                `reportId=${currentReportId}&receiver=${currentReceiver}`;
            const response = await fetch(url, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${getStoredOktaToken()}`,
                },
                mode: "cors",
            });

            const body = await response.text();

            if (!response.ok) {
                const msg = `Triggering resend command failed.\n${body}`;
                showError(msg);
                setHtmlContentResultText(msg);
            } else {
                // oddly, this api just returns a bunch of messages on success.
                const msg = `Success. \n ${body}`;
                showAlertNotification("success", msg);
                setHtmlContentResultText(msg);
            }
        } catch (e: any) {
            console.trace(e);
            const msg = `Triggering resend command failed. ${e.toString()}`;
            showError(msg);
            setHtmlContentResultText(msg);
        }
        setLoading(false);
    };

    const modalShowInfoId = "sendFailuresModalDetails";
    const modalResendId = "sendFailuresModalDetails";

    const fiterResends = (reportId: string) => {
        return lastMileResends.filter((each) => each.filterMatch(reportId));
    };

    return (
        <section className="grid-container rs-container-unbounded">
            <h2>Last Mile failures</h2>
            <form autoComplete="off" className="grid-row">
                <div className="flex-fill">
                    <Label
                        className="font-sans-xs usa-label"
                        htmlFor="input_filter"
                    >
                        Filter:
                    </Label>
                    <TextInput
                        id="input_filter"
                        name="input_filter"
                        type="text"
                        autoComplete="off"
                        aria-autocomplete="none"
                        autoFocus
                        onChange={(evt) => setFilter(evt.target.value)}
                    />
                    Searches FULL information incl error text
                </div>
                <div className="flex-auto">
                    <Label
                        className="font-sans-xs usa-label"
                        htmlFor="days_to_show"
                    >
                        Days to show: (refresh after focus
                    </Label>
                    <TextInput
                        id="days_to_show"
                        name="days_to_show"
                        type="number"
                        defaultValue={defaultDaysToShow}
                        autoComplete="off"
                        aria-autocomplete="none"
                        autoFocus
                        onBlur={(evt) => setDaysToShow(evt.target.value)}
                    />
                </div>
            </form>
            <Table
                key="lastmiletable"
                aria-label="List of failed sends"
                striped
                fullWidth
            >
                <thead>
                    <tr>
                        <th scope="col">Failed At</th>
                        <th scope="col">ReportId</th>
                        <th scope="col">Receiver</th>
                        <th scope="col">
                            ⓘnfo
                            <br />
                            ↺Resend
                            <br />
                            ✅Resent
                        </th>
                    </tr>
                </thead>

                <tbody id="tBodyLastMile" className="font-mono-2xs">
                    {lastMileData
                        .filter((eachRow) => eachRow.filterMatch(filter))
                        .map((eachRow) => {
                            const resends = fiterResends(eachRow.reportId);
                            return (
                                <tr
                                    className={"hide-buttons-on-hover"}
                                    key={`lastmile_row_${eachRow.pk()}`}
                                >
                                    <td>{formatDate(eachRow.failedAt)}</td>
                                    <td>{eachRow.reportId}</td>
                                    <td>{eachRow.receiver}</td>
                                    <td>
                                        <ButtonGroup type="segmented">
                                            <Button
                                                key={`details_${eachRow.pk()}`}
                                                onClick={() =>
                                                    handleShowDetailsClick(
                                                        JSON.stringify(
                                                            eachRow,
                                                            null,
                                                            4
                                                        )
                                                    )
                                                }
                                                type="button"
                                                size="small"
                                                className="padding-1 usa-button--outline"
                                                title="Show Info"
                                            >
                                                {"ⓘ"}
                                            </Button>
                                            {resends.length ? (
                                                <Button
                                                    key={`resentinfo_${eachRow.pk()}`}
                                                    onClick={() =>
                                                        handleShowResendsClick(
                                                            JSON.stringify(
                                                                [...resends],
                                                                null,
                                                                4
                                                            )
                                                        )
                                                    }
                                                    type="button"
                                                    size="small"
                                                    className="padding-1 usa-button--outline"
                                                    title="Show Info"
                                                >
                                                    {"✅"}
                                                </Button>
                                            ) : null}
                                            <Button
                                                key={`retry_${eachRow.pk()}`}
                                                onClick={() =>
                                                    handleRetrySendClick(
                                                        JSON.stringify(
                                                            eachRow,
                                                            null,
                                                            2
                                                        )
                                                    )
                                                }
                                                type="button"
                                                size="small"
                                                className="padding-1 usa-button--outline"
                                                title="Resend"
                                            >
                                                {"↺"}
                                            </Button>
                                        </ButtonGroup>
                                    </td>
                                </tr>
                            );
                        })}
                </tbody>
            </Table>

            <Modal
                isLarge={true}
                className="rs-admindash-modal rs-compare-modal rs-resend-modal"
                ref={modalShowInfoRef}
                id={modalShowInfoId}
                aria-labelledby={`${modalShowInfoId}-heading`}
                aria-describedby={`${modalShowInfoId}-description`}
            >
                {renderInfoModal({ infoDataJson: currentJsonDataForModal })}
            </Modal>

            {/* Confirm before sending modal */}
            <Modal
                isLarge={true}
                ref={modalResendRef}
                id={modalResendId}
                className={"rs-resend-modal"}
            >
                <ModalHeading id={`${modalResendId}-heading`}>
                    Are you sure you want to continue?
                </ModalHeading>
                <p className="usa-prose">
                    You are about to trigger a retransmission.
                </p>
                <p className="usa-prose">
                    Copy the information below into a github issue to coordinate
                    fixing. (This is only until tracking is in place in the
                    server.)
                </p>
                <div
                    className="rs-editable-compare-base rs-editable-compare-static rs-resend-textarea"
                    contentEditable={false}
                    dangerouslySetInnerHTML={{
                        __html: DOMPurify.sanitize(htmlContentForGithubIssue),
                    }}
                />
                <p>Result (Copy to save):</p>
                <div
                    className="rs-editable-compare-base rs-editable-compare-static rs-resend-textarea"
                    contentEditable={false}
                    dangerouslySetInnerHTML={{
                        __html: DOMPurify.sanitize(htmlContentResultText),
                    }}
                />
                <ModalFooter>
                    <ButtonGroup>
                        <Button
                            type="button"
                            size="small"
                            disabled={loading}
                            onClick={() => startResend()}
                        >
                            Trigger Resend
                        </Button>
                        <Button
                            type="button"
                            size="small"
                            onClick={closeResendModal}
                        >
                            Cancel
                        </Button>
                    </ButtonGroup>
                </ModalFooter>
            </Modal>
        </section>
    );
}
