import { Dispatch, useReducer } from "react";
import pick from "lodash.pick";

import { ResponseError, WatersResponse } from "../config/endpoints/waters";
import { Destination } from "../resources/ActionDetailsResource";
import { PAYLOAD_MAX_BYTES, PAYLOAD_MAX_KBYTES } from "../utils/FileUtils";
import { SchemaOption } from "../senders/hooks/UseSenderSchemaOptions";
import { ContentType, FileType } from "../utils/TemporarySettingsAPITypes";

export enum ErrorType {
    SERVER = "server",
    FILE = "file",
}

// Internal state for the hook.
export interface FileHandlerState {
    fileInputResetValue: number; // TODO: remove, this is a force re-render hack
    fileContent: string;
    contentType?: ContentType;
    fileType?: FileType;
    fileName: string;
    file?: File;
    errors: ResponseError[];
    destinations: string;
    reportItems: Destination[] | undefined; //FilteredReportItem[][];
    reportId: string;
    successTimestamp?: string; // TODO: remove
    cancellable: boolean; // TODO: remove
    errorType?: ErrorType;
    warnings: ResponseError[];
    localError: string; // TODO: remove, should be part of standard error escalation
    overallStatus: string;
    selectedSchemaOption: SchemaOption;
}

export enum FileHandlerActionType {
    RESET = "RESET",
    PREPARE_FOR_REQUEST = "PREPARE_FOR_REQUEST",
    FILE_SELECTED = "FILE_SELECTED",
    REQUEST_COMPLETE = "REQUEST_COMPLETE",
    SCHEMA_SELECTED = "SCHEMA_SELECTED",
}

export interface RequestCompletePayload {
    response: WatersResponse;
}

interface FileSelectedPayload {
    file: File;
    fileContent: string;
}

type ResetPayload = Partial<FileHandlerState>;

type SchemaSelectedPayload = SchemaOption;

type FileHandlerActionPayload =
    | RequestCompletePayload
    | FileSelectedPayload
    | SchemaSelectedPayload
    | ResetPayload
    | null;

export interface FileHandlerAction {
    type: FileHandlerActionType;
    payload?: FileHandlerActionPayload; // reset actions will have no payload
}

type FileHandlerReducer = (
    state: FileHandlerState,
    action: FileHandlerAction,
) => FileHandlerState;

export const INITIAL_STATE = {
    fileInputResetValue: 0,
    fileContent: "",
    fileName: "",
    errors: [],
    destinations: "",
    reportItems: [],
    reportId: "",
    successTimestamp: "",
    cancellable: false,
    warnings: [],
    localError: "",
    overallStatus: "",
    selectedSchemaOption: { value: "", format: "" as FileType, title: "" },
};

// Currently returning a static object, but leaving this as a function
// in case we need to make it dynamic for some reason later
function getInitialState(): FileHandlerState {
    return INITIAL_STATE;
}

// state for resetting / setting state at beginning of submission
function getPreSubmitState(): Partial<FileHandlerState> {
    return {
        ...pick(INITIAL_STATE, [
            "errors",
            "destinations",
            "reportId",
            "successTimestamp",
            "warnings",
            "localError",
            "overallStatus",
        ]),
    };
}

// update state when file is selected in form
function calculateFileSelectedState(
    state: FileHandlerState,
    payload: FileSelectedPayload,
): Partial<FileHandlerState> {
    const { file, fileContent } = payload;
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
            return {
                ...state,
                localError: `The file '${file.name}' is too large. The maximum file size is ${PAYLOAD_MAX_KBYTES}k`,
            };
        }

        // previously loading file contents here
        // since this is an async action we'll do this in the calling component
        // prior to dispatching into the reducer, and handle the file content in local state
        const contentType =
            uploadType === "csv" || uploadType === "text/csv"
                ? ContentType.CSV
                : ContentType.HL7;

        const fileType = uploadType.match("hl7") ? FileType.HL7 : FileType.CSV;
        return {
            ...state,
            file,
            fileContent,
            fileType,
            fileName: file.name,
            contentType,
            cancellable: true,
        };
    } catch (err: any) {
        // todo: have central error reporting mechanism.
        console.warn(err);
        return {
            ...state,
            localError: `An unexpected error happened: '${err.toString()}'`,
            cancellable: false,
        };
    }
}

// update state when API request is complete
function calculateRequestCompleteState(
    state: FileHandlerState,
    payload: RequestCompletePayload,
): Partial<FileHandlerState> {
    const {
        response: {
            destinations,
            id,
            timestamp,
            errors,
            status,
            warnings,
            overallStatus,
        },
    } = payload;

    const destinationList = destinations?.length
        ? destinations.map((d: Destination) => d.organization).join(", ")
        : "";

    return {
        destinations: destinationList,
        reportItems: destinations, //destinationReportItemList,
        fileInputResetValue: state.fileInputResetValue + 1,
        errors,
        cancellable: !!errors?.length,
        warnings,
        errorType: errors?.length && status ? ErrorType.SERVER : ErrorType.FILE,
        // pulled from old Upload implementation. Not sure why id is being renamed here, when reportId also exists on the response
        reportId: id || "",
        successTimestamp: errors?.length ? "" : timestamp,
        overallStatus: overallStatus,
    };
}

function reducer(
    state: FileHandlerState,
    action: FileHandlerAction,
): FileHandlerState {
    const { type, payload } = action;
    switch (type) {
        case FileHandlerActionType.RESET:
            return {
                ...getInitialState(),
                ...(payload || {}),
            };
        case FileHandlerActionType.PREPARE_FOR_REQUEST:
            const preSubmitState = getPreSubmitState();
            return { ...state, ...preSubmitState };
        case FileHandlerActionType.FILE_SELECTED:
            const fileSelectedState = calculateFileSelectedState(
                state,
                payload as FileSelectedPayload,
            );
            return { ...state, ...fileSelectedState };
        case FileHandlerActionType.REQUEST_COMPLETE:
            const requestCompleteState = calculateRequestCompleteState(
                state,
                payload as RequestCompletePayload,
            );
            return { ...state, ...requestCompleteState };
        case FileHandlerActionType.SCHEMA_SELECTED:
            const selectedSchemaOption = payload as SchemaOption;

            // reset anything related to the file if the selected schema format
            // and the file format don't match
            if (selectedSchemaOption?.format !== state.fileType) {
                return {
                    ...state,
                    fileContent: "",
                    fileInputResetValue: state.fileInputResetValue + 1,
                    fileName: "",
                    contentType: undefined,
                    selectedSchemaOption,
                };
            }

            return {
                ...state,
                selectedSchemaOption,
            };
        default:
            return state;
    }
}

export type UseFileHandlerHookResult = {
    state: FileHandlerState;
    dispatch: Dispatch<FileHandlerAction>;
};

// this layer of abstraction around the reducer may not be necessary, but following
// the pattern laid down in UsePagination for now, in case we need to make this more
// complex later - DWS
export default function useFileHandler(): UseFileHandlerHookResult {
    const [state, dispatch] = useReducer<FileHandlerReducer>(
        reducer,
        getInitialState(),
    );

    /* TODO: possible future refactors:
      - we could abstract over the dispatch function as UsePagination does and expose individual
      actions as their own functions (ex. fileSelected, requestComplete)
      - we could refactor some logic to keep some state variables internal to the reducer / hook.
      Not sure how much we'd be able to simplify here, or if it'd be worth it, but would be nice
      if we didn't have to send back the entire state to the component
    */

    return {
        state,
        dispatch,
    };
}
