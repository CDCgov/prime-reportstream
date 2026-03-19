import {
    Button,
    ButtonGroup,
    Grid,
    GridContainer,
    Icon,
    Label,
    Modal,
    ModalFooter,
    ModalRef,
    TextInput,
} from "@trussworks/react-uswds";
import DOMPurify from "dompurify";
import { PropsWithChildren, Suspense, useCallback, useEffect, useRef, useState } from "react";

import { showToast } from "../../contexts/Toast";
import useCreateResend from "../../hooks/api/UseCreateResend/UseCreateResend";
import useResends, { RSResend } from "../../hooks/api/UseResends/UseResends";
import useSendFailures, { RSSendFailure } from "../../hooks/api/UseSendFailures/UseSendFailures";
import Table from "../../shared/Table/Table";
import { filterMatch as resendFilterMatch } from "../../utils/filters/resendFilters";
import { filterMatch as sendFailureFilterMatch } from "../../utils/filters/sendFailuresFilters";
import { formatDate } from "../../utils/misc";
import Spinner from "../Spinner";
import { USLink } from "../USLink";

interface DataForDialog {
    info: RSSendFailure;
    resends: RSResend[];
}

// Improves readability
const DRow = (props: PropsWithChildren<{ label: string }>) => {
    return (
        <Grid row className={"modal-info-row"}>
            <Grid className={"modal-info-label text-no-wrap"}>{props.label}:</Grid>
            <Grid className={"modal-info-value rs-wordbreak-force"}>{props.children}</Grid>
        </Grid>
    );
};

const RenderInfoModal = (props: { infoDataJson: string }) => {
    if (!props?.infoDataJson?.length || props?.infoDataJson === "{}") {
        return <></>; // happens before any item is clicked
    }
    const data = JSON.parse(props.infoDataJson) as DataForDialog;
    const infoData = data.info;
    const retryDataArray = data.resends;

    return (
        <GridContainer className={"rs-admindash-modal-container"}>
            <Grid className={"modal-info-title"}>Info Details: {infoData.actionId}</Grid>
            <DRow label={"Receiving Org"}>{infoData.receiver}</DRow>
            <DRow label={"Failed at"}>{formatDate(infoData.failedAt)}</DRow>
            <DRow label={"Action ID"}>{infoData.actionId}</DRow>
            <DRow label={"Report ID"}>{infoData.reportId}</DRow>
            <DRow label={"Report File Receiver"}>{infoData.reportFileReceiver}</DRow>
            <DRow label={"File URI"}>
                {infoData.bodyUrl}
                <br />
                {infoData.reportFileReceiver}
            </DRow>
            <DRow label={"Results"}>{infoData.actionResult}</DRow>
            {/*There may be zero or multiple retries. We can't tell which attempt goes with which retry*/}
            {/*from existin data so show them all*/}
            {retryDataArray.map((retryData) => (
                <>
                    <Grid className={"modal-info-title modal-info-title-resend"}>
                        Resend Details: {retryData.actionId}
                    </Grid>
                    <DRow label={"Resent at"}>{formatDate(retryData.createdAt)}</DRow>
                    <DRow label={"Resent by"}>{retryData.username}</DRow>
                    <DRow label={"Result"}>{retryData.actionResult}</DRow>
                </>
            ))}
        </GridContainer>
    );
};

const RenderResendModal = (props: {
    htmlContentForGithubIssue: string;
    htmlContentResultText: string;
    // the following props should be better integrated into a local modal component
    loading: boolean;
    closeResendModal: () => void;
    startResend: () => void;
}) => {
    return (
        <>
            <p className={""}>
                <b>You are about to trigger a retransmission.</b>
                <br />
                Copy the information below into a github issue to coordinate fixing. (This is only until tracking is in
                place in the server.)
            </p>
            <div
                className="rs-editable-compare-base rs-editable-compare-static rs-resend-textarea"
                contentEditable={false}
                dangerouslySetInnerHTML={{
                    __html: DOMPurify.sanitize(props.htmlContentForGithubIssue),
                }}
            />
            <p>Result (Copy to save):</p>
            <div
                className="rs-editable-compare-base rs-editable-compare-static rs-resend-textarea"
                contentEditable={false}
                dangerouslySetInnerHTML={{
                    __html: DOMPurify.sanitize(props.htmlContentResultText),
                }}
            />
            <ModalFooter>
                <ButtonGroup>
                    <Button type="button" outline onClick={props.closeResendModal}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={props.loading} onClick={() => props.startResend()}>
                        Trigger Resend
                    </Button>
                </ButtonGroup>
            </ModalFooter>
        </>
    );
};

/**
 * This is factored out so refreshing only rerenders the table itself.
 */
const DataLoadRenderTable = ({
    lastMileData,
    lastMileResends,
    filterText,
    handleRetrySendClick,
    handleShowDetailsClick,
}: {
    lastMileResends: RSResend[];
    lastMileData: RSSendFailure[];
    filterText: string;
    handleRetrySendClick: (jsonRowData: string) => void;
    handleShowDetailsClick: (jsonRowData: string) => void;
}) => {
    const fiterResends = (reportId: string) => {
        return lastMileResends.filter((each) => resendFilterMatch(each, reportId));
    };

    const rowData = lastMileData
        .filter((eachRow) => sendFailureFilterMatch(eachRow, filterText))
        .map((eachRow) => {
            // would be nice if org and receiver name were separate
            const parts = eachRow.receiver.split(".") || eachRow.receiver;
            const org = parts[0] || "";
            const recvrName = parts.slice(1).join(".");
            const linkRecvSettings = `/admin/orgreceiversettings/org/${org}/receiver/${recvrName}/action/edit`;
            const resends = fiterResends(eachRow.reportId);
            const dataForDialog: DataForDialog = {
                info: eachRow,
                resends: resends,
            };
            return [
                {
                    columnKey: "FailedAt",
                    columnHeader: "Failed At",
                    content: formatDate(eachRow.failedAt),
                },
                {
                    columnKey: "ReportId",
                    columnHeader: "ReportId",
                    content: (
                        <>
                            <Button
                                type="button"
                                unstyled
                                className={"font-mono-xs"}
                                title={"Show Info"}
                                key={`details_${eachRow.actionId}`}
                                onClick={() => handleShowDetailsClick(JSON.stringify(dataForDialog, null, 4))}
                            >
                                {eachRow.reportId}
                                {<Icon.Launch className="text-bottom margin-left-2px" />}
                            </Button>
                            <span className={"rs-resendmarker"} title={"Resends attempted."}>
                                {resends.length > 0 && (
                                    <Icon.Warning className="text-middle margin-left-2px text-gold" />
                                )}
                            </span>
                        </>
                    ),
                },
                {
                    columnKey: "Receiver",
                    columnHeader: "Receiver",
                    content: (
                        <>
                            <USLink
                                title={"Jump to Settings"}
                                href={linkRecvSettings}
                                key={`recv_link_${eachRow.actionId}`}
                                className={"font-mono-xs padding-right-4"}
                            >
                                {eachRow.receiver}
                            </USLink>
                            <Button
                                key={`retry_${eachRow.actionId}`}
                                onClick={() => handleRetrySendClick(JSON.stringify(eachRow, null, 2))}
                                type="button"
                                className="padding-1 usa-button--outline"
                                title="Requeue items for resend"
                            >
                                Resend...
                            </Button>
                        </>
                    ),
                },
            ];
        });

    return <Table borderless striped rowData={rowData} />;
};

// Main component. Tracks state but does not load/contain data.
export function AdminLastMileFailuresTable() {
    const modalShowInfoId = "sendFailuresModalDetails";
    const modalResendId = "sendFailuresModalDetails";
    const defaultDaysToShow = 15;
    const [daysToShow, setDaysToShow] = useState(defaultDaysToShow);
    const { data: lastMileData, refetch: refetchLastMileData } = useSendFailures({ daysToShow: daysToShow });
    const { data: lastMileResends, refetch: refetchLastMileResends } = useResends({ daysToShow: daysToShow });
    const { mutate: createResend, isPending, error, data, isSuccess } = useCreateResend();
    const refresh = useCallback(async () => {
        await refetchLastMileData();
        await refetchLastMileResends();
    }, [refetchLastMileData, refetchLastMileResends]);

    // this is the input box filter
    const [filter, setFilter] = useState("");

    // used to show hide the modal
    const modalShowInfoRef = useRef<ModalRef>(null);
    const [currentJsonDataForModal, setCurrentJsonDataForModal] = useState<string>("{}");

    const handleShowDetailsClick = useCallback(
        (jsonRowData: string) => {
            setCurrentJsonDataForModal(jsonRowData);
            modalShowInfoRef?.current?.toggleModal(undefined, true);
        },
        [modalShowInfoRef],
    );

    const modalResendRef = useRef<ModalRef>(null); // used to show/hide modal
    // this sets the content of the modal
    const [htmlContentForGithubIssue, setHtmlContentForGithubIssue] = useState("");

    const [htmlContentResultText, setHtmlContentResultText] = useState("");

    // these track what the modal is showing so when the confirm button is pressed
    // the handler can easily get to them for the async network fetch
    const [currentReportId, setCurrentReportId] = useState("");
    const [currentReceiver, setCurrentReceiver] = useState("");
    const [loading, setLoading] = useState(false);

    // called from the list when retry button is clicked.
    // all the data is serialized to a json string as a cheap clone.
    const handleRetrySendClick = useCallback(
        (jsonRowData: string) => {
            const data = JSON.parse(jsonRowData) as RSSendFailure;

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
            // we clear this value and its set by the server response
            setHtmlContentResultText("");

            // we need to show confirmation dialog, then do action to trigger resent
            modalResendRef?.current?.toggleModal(undefined, true);
        },
        [modalResendRef],
    );

    const closeResendModal = useCallback(() => {
        modalResendRef?.current?.toggleModal(undefined, false);
    }, [modalResendRef]);

    // Trigger a resend by issuing an api call
    const startResend = useCallback(() => {
        setLoading(true);
        createResend({
            reportId: currentReportId,
            receiver: currentReceiver,
        });
    }, [createResend, currentReceiver, currentReportId]);

    useEffect(() => {
        if (error) {
            const msg = `Triggering resend command failed. ${error.toString()}`;
            showToast(msg, "error");
            setHtmlContentResultText(msg);
        }
    }, [error]);

    useEffect(() => {
        if (isPending) {
            setHtmlContentResultText(`Starting...`);
            setLoading(true);
        } else {
            setLoading(false);
        }
    }, [isPending]);

    useEffect(() => {
        if (isSuccess) {
            setHtmlContentResultText(`Success. \n ${data}`);
        }
    }, [data, isSuccess]);

    return (
        <section>
            <h2>Last Mile Failures</h2>

            <form autoComplete="off" className="grid-row margin-0">
                <div className="flex-fill margin-1">
                    <Label className="font-sans-xs usa-label text-bold" htmlFor="input_filter">
                        Filter:
                    </Label>
                    <TextInput
                        id="input_filter"
                        name="input_filter"
                        type="text"
                        autoComplete="off"
                        aria-autocomplete="none"
                        inputSize={"medium"}
                        onChange={(evt) => setFilter(evt.target.value)}
                    />
                    Searches FULL information incl error text
                </div>
                <div className="flex-auto margin-1">
                    <Label className="font-sans-xs usa-label text-bold" htmlFor="days_to_show">
                        Days to show:
                    </Label>
                    <TextInput
                        id="days_to_show"
                        name="days_to_show"
                        type="number"
                        defaultValue={defaultDaysToShow}
                        autoComplete="off"
                        aria-autocomplete="none"
                        onBlur={(evt) => setDaysToShow(parseInt(evt.target.value))}
                    />
                </div>
                <div className="flex-auto margin-1 padding-3">
                    <Label className="font-sans-xs usa-label text-bold" htmlFor="days_to_show">
                        {" "}
                    </Label>
                    <Button
                        className={"margin-05"}
                        id="refresh"
                        name="refresh"
                        type={"button"}
                        onClick={() => void refresh()}
                    >
                        Refresh
                    </Button>
                </div>
            </form>

            <div className={"grid-row margin-0 rs-container-unbounded"}>
                <Suspense fallback={<Spinner />}>
                    <DataLoadRenderTable
                        lastMileData={lastMileData}
                        lastMileResends={lastMileResends}
                        filterText={filter}
                        handleRetrySendClick={handleRetrySendClick}
                        handleShowDetailsClick={handleShowDetailsClick}
                    />
                </Suspense>
            </div>

            <Modal
                isLarge={true}
                className="rs-admindash-modal rs-compare-modal rs-resend-modal"
                ref={modalShowInfoRef}
                id={modalShowInfoId}
                aria-labelledby={`${modalShowInfoId}-heading`}
                aria-describedby={`${modalShowInfoId}-description`}
            >
                {/*Put into render component for testability*/}
                <RenderInfoModal infoDataJson={currentJsonDataForModal} />
            </Modal>

            {/* Confirm before sending modal */}
            <Modal isLarge={true} ref={modalResendRef} id={modalResendId} className={"rs-resend-modal"}>
                {/*Put into render component for testability*/}
                <RenderResendModal
                    htmlContentForGithubIssue={htmlContentForGithubIssue}
                    htmlContentResultText={htmlContentResultText}
                    loading={loading}
                    closeResendModal={closeResendModal}
                    startResend={() => void startResend()}
                />
            </Modal>
        </section>
    );
}
