import { useReducer } from "react";

import { RSService } from "../config/endpoints/settings";

export interface ServiceSettings {
    active: string | undefined;
    senders: RSService[];
    receivers: RSService[];
}
export enum ServiceActionType {
    SET_ACTIVE = "set-active",
    STASH_SERVICES = "stash-services",
}
export interface ServiceAction {
    type: ServiceActionType;
    payload?: Partial<ServiceSettings>;
}

const settingsReducer = (
    state: ServiceSettings,
    action: ServiceAction
): ServiceSettings => {
    const { type, payload } = action;
    switch (type) {
        // Used for setting active service, defaults to undefined
        case ServiceActionType.SET_ACTIVE: {
            return {
                ...state,
                active: payload?.active || state?.active || undefined,
            };
        }
        // Used for setting both senders and receivers arrays, defaults to empty array
        case ServiceActionType.STASH_SERVICES: {
            return {
                ...state,
                senders: payload?.senders || state?.senders || [], // or [] is redundant but safe
                receivers: payload?.receivers || state?.receivers || [], // or [] is redundant but safe
            };
        }
        default:
            return state;
    }
};

const initialState: ServiceSettings = {
    active: undefined,
    senders: [],
    receivers: [],
};
export const useServiceSettings = () => {
    const [state, dispatch] = useReducer(settingsReducer, initialState);
    return {
        state,
        dispatch,
    };
};
