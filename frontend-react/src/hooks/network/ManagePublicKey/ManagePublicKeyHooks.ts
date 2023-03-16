import { useReducer } from "react";
import pick from "lodash.pick";

import {
    PAYLOAD_MAX_BYTES,
    PAYLOAD_MAX_KBYTES,
} from "../../../utils/FileUtils";
import { ContentType, Format } from "../../../utils/TemporarySettingsAPITypes";

// Internal state for the hook.
export interface ManagePublicKeyState {
    fileInputResetValue: number;
    fileContent: string;
    contentType?: ContentType.PEM;
    fileType?: Format.PEM;
    fileName: string;
    selectedSender: string;
    reportId: string;
    successTimestamp?: string;
    localError: string;
}

export enum ManagePublicKeyActionType {
    RESET = "RESET",
    PREPARE_FOR_REQUEST = "PREPARE_FOR_REQUEST",
    FILE_SELECTED = "FILE_SELECTED",
    REQUEST_COMPLETE = "REQUEST_COMPLETE",
}

// TODO: Update with correct class when implementing saving of key
export interface RequestCompletePayload {
    response: {
        id?: string;
        timestamp?: string;
    };
}

interface FileSelectedPayload {
    file: File;
}

type ManagePublicKeyActionPayload =
    | RequestCompletePayload
    | FileSelectedPayload
    | null;

interface ManagePublicKeyAction {
    type: ManagePublicKeyActionType;
    payload?: ManagePublicKeyActionPayload; // reset actions will have no payload
}

type ManagePublicKeyReducer = (
    state: ManagePublicKeyState,
    action: ManagePublicKeyAction
) => ManagePublicKeyState;

export const INITIAL_STATE = {
    fileInputResetValue: 0,
    fileContent: "",
    fileName: "",
    selectedSender: "",
    reportId: "",
    successTimestamp: "",
    localError: "",
};

function getInitialState(): ManagePublicKeyState {
    return INITIAL_STATE;
}

// state for resetting / setting state at beginning of submission
function getPreSubmitState(): Partial<ManagePublicKeyState> {
    return {
        ...pick(INITIAL_STATE, ["reportId", "successTimestamp", "localError"]),
    };
}

// update state when file is selected in form
function calculateFileSelectedState(
    state: ManagePublicKeyState,
    payload: FileSelectedPayload
): Partial<ManagePublicKeyState> {
    const { file } = payload;
    try {
        // look at the filename extension.
        const fileNameArray = file.name.split(".");
        const uploadFileExtension = fileNameArray[fileNameArray.length - 1];

        if (uploadFileExtension.toUpperCase() !== Format.PEM) {
            return {
                ...state,
                localError: "The file type must be of type .pem",
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
        const contentType = ContentType.PEM;
        const fileType = Format.PEM;

        return {
            ...state,
            fileType,
            fileName: file.name,
            contentType,
        };
    } catch (err: any) {
        console.warn(err);
        return {
            ...state,
            localError: `An unexpected error happened: '${err.toString()}'`,
        };
    }
}

// update state when API request is complete
function calculateRequestCompleteState(
    state: ManagePublicKeyState,
    payload: RequestCompletePayload
): Partial<ManagePublicKeyState> {
    const {
        response: { id, timestamp },
    } = payload;

    return {
        fileInputResetValue: state.fileInputResetValue + 1,
        reportId: id || "",
        successTimestamp: timestamp,
    };
}

function reducer(
    state: ManagePublicKeyState,
    action: ManagePublicKeyAction
): ManagePublicKeyState {
    const { type, payload } = action;
    switch (type) {
        case ManagePublicKeyActionType.RESET:
            return getInitialState();
        case ManagePublicKeyActionType.PREPARE_FOR_REQUEST:
            const preSubmitState = getPreSubmitState();
            return { ...state, ...preSubmitState };
        case ManagePublicKeyActionType.FILE_SELECTED:
            const fileSelectedState = calculateFileSelectedState(
                state,
                payload as FileSelectedPayload
            );
            return { ...state, ...fileSelectedState };
        case ManagePublicKeyActionType.REQUEST_COMPLETE:
            const requestCompleteState = calculateRequestCompleteState(
                state,
                payload as RequestCompletePayload
            );
            return { ...state, ...requestCompleteState };
        default:
            return state;
    }
}

export type UseManagePublicKeyHookResult = {
    state: ManagePublicKeyState;
    dispatch: React.Dispatch<ManagePublicKeyAction>;
};

export default function useManagePublicKey(): UseManagePublicKeyHookResult {
    const [state, dispatch] = useReducer<ManagePublicKeyReducer>(
        reducer,
        getInitialState()
    );

    return {
        state,
        dispatch,
    };
}
