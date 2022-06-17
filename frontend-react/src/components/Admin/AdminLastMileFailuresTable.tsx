import React, { useRef, useState } from "react";
import { useResource } from "rest-hooks";
import {
    Button,
    ButtonGroup,
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
import { getStoredOktaToken } from "../../contexts/SessionStorageTools";

export function AdminLastMileFailuresTable() {
    const defaultDaysToShow = "15"; // numeric input but treat as string for easier passing around
    const [daysToShow, setDaysToShow] = useState(defaultDaysToShow);
    const lastMileData: AdmSendFailuresResource[] = useResource(
        AdmSendFailuresResource.list(),
        { days_to_show: daysToShow }
    );

    // this is the input box filter
    const [filter, setFilter] = useState("");

    // used to show hide the modal
    const modalShowInfoRef = useRef<ModalRef>(null);
    // content inside the modal that changes based on what button in the list is clicked
    const [htmlModalShowInfoContent, setHtmlModalShowInfoContent] =
        useState("");

    const handleShowDetailsClick = (jsonRowData: string) => {
        setHtmlModalShowInfoContent(jsonRowData);
        modalShowInfoRef?.current?.toggleModal(undefined, true);
    };

    const modalResendRef = useRef<ModalRef>(null); // used to show/hide modal
    // this sets the content of the modal
    const [htmlContentForGithubIssue, settHtmlContentForGithubIssue] =
        useState("");

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
        const formatted = `
Report ID:
${data.reportId}

File Name:
${data.fileName}

Destination:
${data.receiver}`;
        settHtmlContentForGithubIssue(formatted);
        setCurrentReportId(data.reportId);
        setCurrentReceiver(data.receiver);

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
            const url =
                `${process.env.REACT_APP_BACKEND_URL}/api/requeue/send?` +
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
                showError(`Triggering resend command failed. ${body}`);
            } else {
                // oddly, this api just returns a bunch of messages on success.
                showAlertNotification("success", `Success: ${body}`);
            }

            // if we reach here then it succeeded.
            modalResendRef?.current?.toggleModal(undefined, false);
        } catch (e: any) {
            console.trace(e);
            showError(`Triggering resend command failed. ${e.toString()}`);
        }
        setLoading(false);
    };

    const modalShowInfoId = "sendFailuresModalDetails";
    const modalResendId = "sendFailuresModalDetails";

    return (
        <>
            <section className="grid-container margin-bottom-5">
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
                            Days to show:
                        </Label>
                        <TextInput
                            id="days_to_show"
                            name="days_to_show"
                            type="number"
                            defaultValue={defaultDaysToShow}
                            autoComplete="off"
                            aria-autocomplete="none"
                            autoFocus
                            onChange={(evt) => setDaysToShow(evt.target.value)}
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
                            </th>
                        </tr>
                    </thead>

                    <tbody id="tBodyLastMile" className="font-mono-2xs">
                        {lastMileData
                            .filter((eachRow) => eachRow.filterMatch(filter))
                            .map((eachRow) => (
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
                            ))}
                    </tbody>
                </Table>

                <Modal
                    isLarge={true}
                    className="rs-compare-modal"
                    ref={modalShowInfoRef}
                    id={modalShowInfoId}
                    aria-labelledby={`${modalShowInfoId}-heading`}
                    aria-describedby={`${modalShowInfoId}-description`}
                >
                    <div
                        className="rs-editable-compare-base rs-editable-compare-static"
                        contentEditable={false}
                        dangerouslySetInnerHTML={{
                            __html: `${htmlModalShowInfoContent}`,
                        }}
                    />
                </Modal>

                {/* Confirm before sending modal */}
                <Modal isLarge={true} ref={modalResendRef} id={modalResendId}>
                    <ModalHeading id={`${modalResendId}-heading`}>
                        Are you sure you want to continue?
                    </ModalHeading>
                    <p className="usa-prose">
                        You are about to trigger a retransmission.
                    </p>
                    <p className="usa-prose">
                        Copy the information below into a github issue to
                        coordinate fixing. (This is only until tracking is in
                        place in the server.)
                    </p>
                    <div
                        className="rs-editable-compare-base rs-editable-compare-static"
                        contentEditable={false}
                        dangerouslySetInnerHTML={{
                            __html: `${htmlContentForGithubIssue}`,
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
        </>
    );
}
