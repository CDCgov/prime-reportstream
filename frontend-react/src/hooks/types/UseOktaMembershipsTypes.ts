import { AccessToken } from "@okta/okta-auth-js";

export enum MemberType {
    SENDER = "sender",
    RECEIVER = "receiver",
    PRIME_ADMIN = "prime-admin",
    NON_STAND = "non-standard",
}

export enum MembershipActionType {
    SWITCH = "switch",
    UPDATE = "update",
    ADMIN_OVERRIDE = "override",
}

export interface MembershipSettings {
    // The org header value
    parsedName: string;
    // The type of membership
    memberType: MemberType;
    // Optional sender name
    senderName?: string;
}

export interface MembershipState {
    active?: MembershipSettings;
    // Key is the OKTA group name, settings has parsedName
    memberships?: Map<string, MembershipSettings>;
}

export interface MembershipController {
    state: MembershipState;
    dispatch: React.Dispatch<MembershipAction>;
}

export interface MembershipAction {
    type: MembershipActionType;
    // Only need to pass name of an org to swap to
    payload: string | AccessToken | Partial<MembershipSettings>;
}
