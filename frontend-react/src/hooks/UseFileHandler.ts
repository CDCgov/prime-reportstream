import { useReducer } from "react";

import { ResponseError } from "../network/api/WatersApi";

enum ErrorType {
    SERVER = "server",
    FILE = "file",
}

enum FileType {
    "CSV" = "CSV",
    "HL7" = "HL7",
}

enum ContentType {
    "CSV" = "text/csv",
    "HL7" = "application/hl7-v2",
}

// Internal state for the hook.
export interface FileHandlerState {
    isSubmitting: boolean;
    fileInputResetValue: number;
    fileContent: string;
    contentType?: string;
    fileType?: FileType;
    fileName: string;
    errors: ResponseError[];
    destinations: string;
    reportId: string;
    successTimestamp?: string;
    cancellable: boolean;
    errorType?: ErrorType;
    warnings: ResponseError[];
}

enum FileHandlerActionType {
    RESET = "RESET",
    PREPARE_FOR_REQUEST = "PREPARE_FOR_REQUEST",
    SET_FILE_TYPE = "SET_FILE_TYPE",
    REQUEST_COMPLETE = "REQUEST_COMPLETE",
    FILE_SELECTED = "FILE_SELECTED",
    SET_CANCELLABLE = "SET_CANCELLABLE",
}

interface RequestCompletePayload {
    destinations?: string;
    reportId?: string;
    successTimestamp?: string;
    errorType?: ErrorType;
    warnings?: ResponseError[];
    errors?: ResponseError[];
    cancellable: boolean;
    fileInputResetValue: number;
}

interface FileSelectedPayload {
    contentType: ContentType;
    fileName: string;
    fileContent: string;
    cancellable: boolean;
}

interface SetFileTypePayload {
    fileType: FileType;
}

interface SetCancellablePayload {
    cancellable: boolean;
}

interface FileHandlerAction {
    type: FileHandlerActionType;
    payload?:
        | RequestCompletePayload
        | SetFileTypePayload
        | FileSelectedPayload
        | SetCancellablePayload; // reset actions will have no payload
}

type FileHandlerReducer = (
    state: FileHandlerState,
    action: FileHandlerAction
) => FileHandlerState;

// Currently returning a static object, but leaving this as a function
// in case we need to make it dynamic for some reason later
function getInitialState(): FileHandlerState {
    return {
        isSubmitting: false,
        fileInputResetValue: 0,
        fileContent: "",
        fileName: "",
        errors: [],
        destinations: "",
        reportId: "",
        successTimestamp: "",
        cancellable: false,
        warnings: [],
    };
}

// state for resetting / setting state at beginning of submission
function getPreSubmitState(): Partial<FileHandlerState> {
    return {
        isSubmitting: true,
        errors: [],
        destinations: "",
        reportId: "",
        successTimestamp: "",
        warnings: [],
    };
}

function reducer<T>(
    state: FileHandlerState,
    action: FileHandlerAction
): FileHandlerState {
    const { type, payload } = action;
    switch (type) {
        case FileHandlerActionType.RESET:
            return getInitialState();
        case FileHandlerActionType.PREPARE_FOR_REQUEST:
            const preSubmitState = getPreSubmitState();
            return { ...state, ...preSubmitState };
        case FileHandlerActionType.PROCESS_RESULTS:
            return processResultsReducer(
                state,
                payload as ProcessResultsPayload<T>
            );
        case FileHandlerActionType.SET_SELECTED_PAGE:
            return setSelectedPageReducer(
                state,
                payload as SetSelectedPagePayload
            );
        default:
            return state;
    }
}

// Input parameters to the hook.
export interface UseFileHandlerProps<T> {
    // Whether the cursor value in requests is inclusive. If true, a page's
    // cursor is taken from the first result on the page, otherwise it's taken
    // from the last result on the previous page.
    isCursorInclusive: boolean;
    // Function for extracting the cursor value from a result in the paginated
    // set.
    extractCursor: CursorExtractor<T>;
    // Callback function for fetching results.
    fetchResults: ResultsFetcher<T>;
    // Number of items on a page of results in the UI.
    pageSize: number;
    // Initial cursor for the paginated set.
    startCursor: string;
}

// Hook for paginating through a set of results by means of a cursor values
// extracted from the items in the set.
function useFileHandler(): {
    state: FileHandlerState;
    dispatch: React.Dispatch<FileHandlerAction>;
} {
    const [state, dispatch] = useReducer<FileHandlerReducer>(
        reducer,
        getInitialState()
    );

    // do we need to return the whole state? there may be things we can keep internal.
    // this may be a second refactor to move functionality out of the components in here.
    return {
        state,
        dispatch,
    };
}

export default useFileHandler;
