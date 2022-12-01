import { act, renderHook } from "@testing-library/react-hooks";

import { PAYLOAD_MAX_BYTES, PAYLOAD_MAX_KBYTES } from "../utils/FileUtils";
import { Destination } from "../resources/ActionDetailsResource";
import { ResponseError } from "../config/endpoints/waters";

import useFileHandler, {
    INITIAL_STATE,
    FileHandlerActionType,
    RequestCompletePayload,
} from "./UseFileHandler";

const fakeDestination: Destination = {
    organization_id: "an org id",
    organization: "an org",
    service: "some service",
    filteredReportRows: [],
    sending_at: Date.now().toString(),
    itemCount: 1,
    sentReports: [],
    filteredReportItems: [],
    itemCountBeforeQualityFiltering: 0,
};

const fakeError: ResponseError = {
    field: "error field",
    indices: [1],
    message: "error message",
    trackingIds: ["track me"],
    scope: "some scope",
    errorCode: "INVALID_HL7_MESSAGE_VALIDATION",
    details: "this happened",
};

const fakeWarning: ResponseError = {
    field: "warning field",
    indices: [1],
    message: "warning message",
    trackingIds: ["track me"],
    scope: "some warning scope",
    errorCode: "INVALID_HL7_MESSAGE_VALIDATION",
    details: "this happened - a warning",
};

const fileSelectedTypedPayload = {
    file: {
        type: "csv",
        size: 1,
        name: "aCsv.csv",
    } as File,
};

const fileSelectedUntypedPayload = {
    file: {
        size: 1,
        name: "aCsv.csv",
    } as File,
};

const fileSelectedBadTypePayload = {
    file: {
        size: 1,
        name: "aCsv.docx",
    } as File,
};

const fileSelectedTooBigPayload = {
    file: {
        size: PAYLOAD_MAX_BYTES + 1,
        name: "aCsv.csv",
    } as File,
};

const requestCompleteSuccessPayload: RequestCompletePayload = {
    response: {
        destinations: [fakeDestination],
        id: "1",
        timestamp: new Date(0).toString(),
        errors: [],
        warnings: [],
    },
};

const requestCompleteErrorPayload: RequestCompletePayload = {
    response: {
        destinations: [fakeDestination],
        errors: [fakeError],
        warnings: [fakeWarning],
    },
};

// testing hook results rather than underlying functions in case we want to refactor things
describe("useFileHandler", () => {
    test("returns expected initial state", () => {
        const {
            result: {
                current: { state },
            },
        } = renderHook(() => useFileHandler());
        expect(state).toEqual(INITIAL_STATE);
    });

    test("resets state to initial on reset", async () => {
        const { result } = renderHook(() => {
            return useFileHandler();
        });

        const initialState = { ...result.current.state };
        expect(result.current.state.reportId).toEqual("");

        act(() =>
            result.current.dispatch({
                type: FileHandlerActionType.REQUEST_COMPLETE,
                payload: requestCompleteSuccessPayload,
            })
        );
        expect(result.current.state).not.toEqual(initialState);

        act(() => {
            result.current.dispatch({
                type: FileHandlerActionType.RESET,
            });
        });

        expect(result.current.state).toEqual(initialState);
    });

    test("partially resets state to initial on Prepare For Request", async () => {
        const { result } = renderHook(() => {
            return useFileHandler();
        });

        const initialState = { ...result.current.state };

        act(() =>
            result.current.dispatch({
                type: FileHandlerActionType.REQUEST_COMPLETE,
                payload: requestCompleteSuccessPayload,
            })
        );
        expect(result.current.state).not.toEqual(initialState);

        act(() => {
            result.current.dispatch({
                type: FileHandlerActionType.PREPARE_FOR_REQUEST,
            });
        });

        expect(result.current.state.errors).toEqual(INITIAL_STATE.errors);
        expect(result.current.state.destinations).toEqual(
            INITIAL_STATE.destinations
        );
        expect(result.current.state.reportId).toEqual(INITIAL_STATE.reportId);
        expect(result.current.state.successTimestamp).toEqual(
            INITIAL_STATE.successTimestamp
        );
        expect(result.current.state.warnings).toEqual(INITIAL_STATE.warnings);
        expect(result.current.state.localError).toEqual(
            INITIAL_STATE.localError
        );
    });

    test("returns local error on file selected if file is too big", () => {
        const { result } = renderHook(() => {
            return useFileHandler();
        });

        act(() =>
            result.current.dispatch({
                type: FileHandlerActionType.FILE_SELECTED,
                payload: fileSelectedTooBigPayload,
            })
        );

        expect(result.current.state.localError).toEqual(
            `The file 'aCsv.csv' is too large. The maximum file size is ${PAYLOAD_MAX_KBYTES}k`
        );
    });
    test("returns local error on file selected if file is not csv or hl7", () => {
        const { result } = renderHook(() => {
            return useFileHandler();
        });

        act(() =>
            result.current.dispatch({
                type: FileHandlerActionType.FILE_SELECTED,
                payload: fileSelectedBadTypePayload,
            })
        );

        expect(result.current.state.localError).toEqual(
            "The file type must be .csv or .hl7"
        );
    });
    test("returns expected state on file selected with file type", () => {
        const { result } = renderHook(() => {
            return useFileHandler();
        });

        act(() =>
            result.current.dispatch({
                type: FileHandlerActionType.FILE_SELECTED,
                payload: fileSelectedTypedPayload,
            })
        );

        expect(result.current.state).toEqual({
            ...INITIAL_STATE,
            fileType: "CSV",
            fileName: "aCsv.csv",
            contentType: "text/csv",
            cancellable: true,
        });
    });
    test("returns expected state on file selected without file type", () => {
        const { result } = renderHook(() => {
            return useFileHandler();
        });

        act(() =>
            result.current.dispatch({
                type: FileHandlerActionType.FILE_SELECTED,
                payload: fileSelectedUntypedPayload,
            })
        );

        expect(result.current.state).toEqual({
            ...INITIAL_STATE,
            fileType: "CSV",
            fileName: "aCsv.csv",
            contentType: "text/csv",
            cancellable: true,
        });
    });

    test("returns expected state on request complete on success", () => {
        const { result } = renderHook(() => {
            return useFileHandler();
        });

        act(() =>
            result.current.dispatch({
                type: FileHandlerActionType.REQUEST_COMPLETE,
                payload: requestCompleteSuccessPayload,
            })
        );

        expect(result.current.state).toEqual({
            ...INITIAL_STATE,
            destinations: "an org",
            errorType: "file",
            fileInputResetValue: 1,
            reportId: "1",
            successTimestamp: new Date(0).toString(),
            overallStatus: undefined,
            reportItems: [
                {
                    filteredReportItems: [],
                    filteredReportRows: [],
                    itemCount: 1,
                    itemCountBeforeQualityFiltering: 0,
                    organization: "an org",
                    organization_id: "an org id",
                    sending_at: expect.any(String),
                    sentReports: [],
                    service: "some service",
                },
            ],
        });
    });

    test("returns expected state on request complete on error", () => {
        const { result } = renderHook(() => {
            return useFileHandler();
        });

        act(() =>
            result.current.dispatch({
                type: FileHandlerActionType.REQUEST_COMPLETE,
                payload: requestCompleteErrorPayload,
            })
        );

        expect(result.current.state).toEqual({
            ...INITIAL_STATE,
            destinations: "an org",
            fileInputResetValue: 1,
            errors: [fakeError],
            cancellable: true,
            warnings: [fakeWarning],
            errorType: "file",
            overallStatus: undefined,
            reportItems: [
                {
                    filteredReportItems: [],
                    filteredReportRows: [],
                    itemCount: 1,
                    itemCountBeforeQualityFiltering: 0,
                    organization: "an org",
                    organization_id: "an org id",
                    sending_at: expect.any(String),
                    sentReports: [],
                    service: "some service",
                },
            ],
        });
    });
});
