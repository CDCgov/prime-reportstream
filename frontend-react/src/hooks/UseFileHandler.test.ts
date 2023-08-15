import { act, renderHook, RenderHookResult } from "@testing-library/react";

import { PAYLOAD_MAX_BYTES, PAYLOAD_MAX_KBYTES } from "../utils/FileUtils";
import { Destination } from "../resources/ActionDetailsResource";
import { ErrorCode, ResponseError } from "../config/endpoints/waters";
import { SchemaOption } from "../senders/hooks/UseSenderSchemaOptions";
import { FileType } from "../utils/TemporarySettingsAPITypes";

import useFileHandler, {
    INITIAL_STATE,
    FileHandlerActionType,
    RequestCompletePayload,
    UseFileHandlerHookResult,
} from "./UseFileHandler";

export const fakeDestination: Destination = {
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

export const fakeError: ResponseError = {
    field: "error field",
    indices: [1],
    message: "error message",
    trackingIds: ["track me"],
    scope: "some scope",
    errorCode: ErrorCode.INVALID_HL7_MSG_VALIDATION,
    details: "this happened",
};

export const fakeWarning: ResponseError = {
    field: "warning field",
    indices: [1],
    message: "warning message",
    trackingIds: ["track me"],
    scope: "some warning scope",
    errorCode: ErrorCode.INVALID_HL7_MSG_VALIDATION,
    details: "this happened - a warning",
};

const fileSelectedTypedPayload = {
    file: {
        type: "csv",
        size: 1,
        name: "aCsv.csv",
    } as File,
    fileContent: "content",
};

const fileSelectedUntypedPayload = {
    file: {
        size: 1,
        name: "aCsv.csv",
    } as File,
    fileContent: "content",
};

const fileSelectedBadTypePayload = {
    file: {
        size: 1,
        name: "aCsv.docx",
    } as File,
    fileContent: "content",
};

const fileSelectedTooBigPayload = {
    file: {
        size: PAYLOAD_MAX_BYTES + 1,
        name: "aCsv.csv",
    } as File,
    fileContent: "content",
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
            }),
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
            }),
        );
        expect(result.current.state).not.toEqual(initialState);

        act(() => {
            result.current.dispatch({
                type: FileHandlerActionType.PREPARE_FOR_REQUEST,
            });
        });

        expect(result.current.state.errors).toEqual(INITIAL_STATE.errors);
        expect(result.current.state.destinations).toEqual(
            INITIAL_STATE.destinations,
        );
        expect(result.current.state.reportId).toEqual(INITIAL_STATE.reportId);
        expect(result.current.state.successTimestamp).toEqual(
            INITIAL_STATE.successTimestamp,
        );
        expect(result.current.state.warnings).toEqual(INITIAL_STATE.warnings);
        expect(result.current.state.localError).toEqual(
            INITIAL_STATE.localError,
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
            }),
        );

        expect(result.current.state.localError).toEqual(
            `The file 'aCsv.csv' is too large. The maximum file size is ${PAYLOAD_MAX_KBYTES}k`,
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
            }),
        );

        expect(result.current.state.localError).toEqual(
            "The file type must be .csv or .hl7",
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
            }),
        );

        expect(result.current.state).toEqual({
            ...INITIAL_STATE,
            file: {
                name: "aCsv.csv",
                size: 1,
                type: "csv",
            },
            fileContent: "content",
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
            }),
        );

        expect(result.current.state).toEqual({
            ...INITIAL_STATE,
            file: {
                name: "aCsv.csv",
                size: 1,
            },
            fileContent: "content",
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
            }),
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
            }),
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

    describe("when selecting a schema option", () => {
        let renderer: RenderHookResult<
            UseFileHandlerHookResult,
            UseFileHandlerHookResult
        >;

        function doDispatch(payload: SchemaOption | null) {
            renderer = renderHook(() => useFileHandler());

            act(() =>
                renderer.result.current.dispatch({
                    type: FileHandlerActionType.SCHEMA_SELECTED,
                    payload,
                }),
            );
        }

        describe("when a schema option is unselected", () => {
            beforeEach(() => {
                doDispatch(null);
            });

            test("clears out the selected schema from state", () => {
                expect(
                    renderer.result.current.state.selectedSchemaOption,
                ).toBeNull();
            });
        });

        describe("when a schema option is selected", () => {
            const schemaOption: SchemaOption = {
                value: "test",
                title: "test",
                format: FileType.CSV,
            };

            beforeEach(() => {
                doDispatch(schemaOption);
            });

            test("sets the selected schema in state", () => {
                expect(
                    renderer.result.current.state.selectedSchemaOption,
                ).toEqual(schemaOption);
            });
        });

        describe("when there's already file data in the useFileHandler state", () => {
            let renderer: RenderHookResult<
                UseFileHandlerHookResult,
                UseFileHandlerHookResult
            >;

            beforeEach(() => {
                renderer = renderHook(() => useFileHandler());

                act(() => {
                    renderer.result.current.dispatch({
                        type: FileHandlerActionType.FILE_SELECTED,
                        payload: {
                            file: new File(
                                [new Blob(["whatever"])],
                                "blep.csv",
                                {
                                    type: "csv",
                                },
                            ),
                        },
                    });
                });
            });

            describe("when selecting a schema with the same format as the file", () => {
                test("does not reset the other useFileHandler state values", () => {
                    expect(renderer.result.current.state).toEqual(
                        expect.objectContaining({
                            fileName: "blep.csv",
                            fileType: "CSV",
                            contentType: "text/csv",
                        }),
                    );

                    act(() => {
                        renderer.result.current.dispatch({
                            type: FileHandlerActionType.SCHEMA_SELECTED,
                            payload: {
                                value: "test-csv",
                                title: "test-csv",
                                format: FileType.CSV,
                            },
                        });
                    });

                    expect(renderer.result.current.state).toEqual(
                        expect.objectContaining({
                            fileName: "blep.csv",
                            fileType: "CSV",
                            contentType: "text/csv",
                        }),
                    );
                });
            });

            describe("when selecting a schema with a different format from the file", () => {
                test("resets the other useFileHandler state values", () => {
                    expect(renderer.result.current.state).toEqual(
                        expect.objectContaining({
                            fileName: "blep.csv",
                            fileType: "CSV",
                            contentType: "text/csv",
                        }),
                    );

                    act(() => {
                        renderer.result.current.dispatch({
                            type: FileHandlerActionType.SCHEMA_SELECTED,
                            payload: {
                                value: "test-hl7",
                                title: "test-hl7",
                                format: FileType.HL7,
                            },
                        });
                    });

                    expect(renderer.result.current.state).not.toEqual(
                        expect.objectContaining({
                            fileName: "blep.csv",
                            fileType: "CSV",
                            contentType: "text/csv",
                        }),
                    );
                });
            });
        });
    });
});
