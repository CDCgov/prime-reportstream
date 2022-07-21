import { useReducer } from "react";

import { ResponseError, WatersResponse } from "../network/api/WatersApi";
import { Destination } from "../resources/ActionDetailsResource";

const PAYLOAD_MAX_BYTES = 50 * 1000 * 1000; // no idea why this isn't in "k" (* 1024).
// const REPORT_MAX_ITEMS = 10000;
// const REPORT_MAX_ITEM_COLUMNS = 2000;

export enum ErrorType {
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
    contentType?: ContentType;
    fileType?: FileType;
    fileName: string;
    errors: ResponseError[];
    destinations: string;
    reportId: string;
    successTimestamp?: string;
    cancellable: boolean;
    errorType?: ErrorType;
    warnings: ResponseError[];
    localError: string;
}

export enum FileHandlerActionType {
    RESET = "RESET",
    PREPARE_FOR_REQUEST = "PREPARE_FOR_REQUEST",
    SET_FILE_TYPE = "SET_FILE_TYPE",
    SET_CANCELLABLE = "SET_CANCELLABLE",
    FILE_SELECTED = "FILE_SELECTED",
    REQUEST_COMPLETE = "REQUEST_COMPLETE",
}

interface RequestCompletePayload {
    // destinations?: string;
    // reportId?: string;
    // successTimestamp?: string;
    // errorType?: ErrorType;
    // warnings?: ResponseError[];
    // errors?: ResponseError[];
    // cancellable: boolean;
    // fileInputResetValue: number;
    response: WatersResponse;
}

interface FileSelectedPayload {
    file: File;
}

interface SetFileTypePayload {
    fileType: FileType;
}

interface SetCancellablePayload {
    cancellable: boolean;
}

type FileHandlerActionPayload =
    | RequestCompletePayload
    | SetFileTypePayload
    | FileSelectedPayload
    | SetCancellablePayload;

interface FileHandlerAction {
    type: FileHandlerActionType;
    payload?: FileHandlerActionPayload; // reset actions will have no payload
}

type FileHandlerReducer = (
    state: FileHandlerState,
    action: FileHandlerAction
) => FileHandlerState;

// const parseCsvForError = (
//     fileName: string,
//     filecontent: string
// ): string | undefined => {
//     // count the number of lines
//     const linecount = (filecontent.match(/\n/g) || []).length + 1;
//     if (linecount > REPORT_MAX_ITEMS) {
//         return `The file '${fileName}' has too many rows. The maximum number of rows allowed is ${REPORT_MAX_ITEMS}.`;
//     }
//     if (linecount <= 1) {
//         return `The file '${fileName}' doesn't contain any valid data. File should have a header line and at least one line of data.`;
//     }

//     // get the first line and examine it
//     const firstline = (filecontent.match(/^(.*)\n/) || [""])[0];
//     // ideally, the columns would be comma seperated, but they may be tabs, because the first
//     // line is a header, we don't have to worry about escaped delimiters in strings (e.g. ,"Smith, John",)
//     const columncount =
//         (firstline.match(/,/g) || []).length ||
//         (firstline.match(/\t/g) || []).length;

//     if (columncount > REPORT_MAX_ITEM_COLUMNS) {
//         return `The file '${fileName}' has too many columns. The maximum number of allowed columns is ${REPORT_MAX_ITEM_COLUMNS}.`;
//     }
//     // todo: this is a good place to do basic validation of the upload file. e.g. does it have
//     // all the required columns? Are any rows obviously not correct (empty or obviously wrong type)?
//     return;
// };

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
        localError: "",
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
        localError: "",
    };
}

function calculateFileSelectedState(
    state: FileHandlerState,
    payload: FileSelectedPayload
): Partial<FileHandlerState> {
    const { file } = payload;
    try {
        let uploadType;
        if (file.type) {
            uploadType = file.type;
        } else {
            // look at the filename extension.
            // it's all we have to go off of for now
            const fileNameArray = file.name.split(".");
            uploadType = fileNameArray[fileNameArray.length - 1];
        }

        if (
            uploadType !== "text/csv" &&
            uploadType !== "csv" &&
            uploadType !== "hl7"
        ) {
            return {
                ...state,
                localError: "The file type must be .csv or .hl7",
            };
        }

        if (file.size > PAYLOAD_MAX_BYTES) {
            const maxkbytes = (PAYLOAD_MAX_BYTES / 1024).toLocaleString(
                "en-US",
                { maximumFractionDigits: 2, minimumFractionDigits: 2 }
            );

            return {
                ...state,
                localError: `The file '${file.name}' is too large. The maximum file size is ${maxkbytes}k`,
            };
        }
        // load the "contents" of the file. Hope it fits in memory!
        // const filecontent = await file.text();

        const contentType =
            uploadType === "csv" || uploadType === "text/csv"
                ? ContentType.CSV
                : ContentType.HL7;

        const fileType = uploadType.match("hl7") ? FileType.HL7 : FileType.CSV;
        return {
            ...state,
            fileType,
            fileName: file.name,
            // fileContent: filecontent,
            contentType,
            cancellable: true,
        };
    } catch (err: any) {
        // todo: have central error reporting mechanism.
        console.error(err);
        // showError(`An unexpected error happened: '${err.toString()}'`);
        return {
            ...state,
            localError: `An unexpected error happened: '${err.toString()}'`,
            cancellable: false,
        };
    }
}

function calculateRequestCompleteState(
    state: FileHandlerState,
    payload: RequestCompletePayload
): Partial<FileHandlerState> {
    const {
        response: { destinations, id, timestamp, errors, status, warnings },
    } = payload;

    const destinationList = destinations?.length
        ? destinations.map((d: Destination) => d.organization).join(", ")
        : "";

    return {
        destinations: destinationList,
        isSubmitting: false,
        fileInputResetValue: state.fileInputResetValue + 1,
        errors,
        cancellable: errors?.length ? true : false,
        warnings,
        errorType: errors?.length && status ? ErrorType.SERVER : ErrorType.FILE,
        reportId: id,
        successTimestamp: timestamp,
    };
}

function reducer(
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
        case FileHandlerActionType.SET_FILE_TYPE:
            const { fileType } = payload as SetFileTypePayload;
            return { ...state, fileType };
        case FileHandlerActionType.SET_CANCELLABLE:
            const { cancellable } = payload as SetCancellablePayload;
            return { ...state, cancellable };
        case FileHandlerActionType.FILE_SELECTED:
            const fileSelectedState = calculateFileSelectedState(
                state,
                payload as FileSelectedPayload
            );
            console.log("!!! file selected", fileSelectedState);
            return { ...state, ...fileSelectedState };
        case FileHandlerActionType.REQUEST_COMPLETE:
            const requestCompleteState = calculateRequestCompleteState(
                state,
                payload as RequestCompletePayload
            );
            return { ...state, ...requestCompleteState };
        default:
            return state;
    }
}

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
