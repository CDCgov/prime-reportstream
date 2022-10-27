import { renderHook } from "@testing-library/react-hooks";

import { useSenderResource } from "./UseSenderResource";
import { MembershipSettings } from "./UseOktaMemberships";

const fakeSender = {
    allowDuplicates: false,
    customerStatus: "active",
    format: "CSV",
    name: "senderName",
    organizationName: "orgName",
    processingType: "sync",
    schemaName: "senderSchema",
    topic: "covid-19",
};

const mockUseRequestConfig = jest.fn();
const mockUseSessionContext = jest.fn();

jest.mock("../contexts/SessionContext", () => ({
    useSessionContext: () => mockUseSessionContext(),
}));

jest.mock("./network/UseRequestConfig", () => ({
    default: () => mockUseRequestConfig(),
    __esModule: true,
}));

describe("useSenderResource", () => {
    test("returns null while loading", () => {
        mockUseRequestConfig.mockReturnValue({
            data: fakeSender,
            loading: true,
        });
        mockUseSessionContext.mockReturnValue({
            activeMembership: {
                service: "senderName",
            } as MembershipSettings,
            dispatch: () => {},
        });

        const {
            result: {
                current: { sender },
            },
        } = renderHook(() => useSenderResource());
        expect(sender).toEqual(null);
    });
    test("returns null if no sender available on membership", () => {
        mockUseRequestConfig.mockReturnValue({
            data: fakeSender,
        });
        mockUseSessionContext.mockReturnValue({
            activeMembership: {},
            dispatch: () => {},
        });

        const {
            result: {
                current: { sender },
            },
        } = renderHook(() => useSenderResource());
        expect(sender).toEqual(null);
    });
    test("returns null if no sender returned from API", () => {
        mockUseRequestConfig.mockReturnValue({
            data: undefined,
        });
        mockUseSessionContext.mockReturnValue({
            activeMembership: {
                service: "a different name",
            } as MembershipSettings,
            dispatch: () => {},
        });

        const {
            result: {
                current: { sender },
            },
        } = renderHook(() => useSenderResource());
        expect(sender).toEqual(null);
    });
    test("returns correct sender match", () => {
        mockUseRequestConfig.mockReturnValue({
            data: fakeSender,
        });
        mockUseSessionContext.mockReturnValue({
            activeMembership: {
                service: "senderName",
            } as MembershipSettings,
            dispatch: () => {},
        });

        const {
            result: {
                current: { sender },
            },
        } = renderHook(() => useSenderResource());
        expect(sender).toEqual(fakeSender);
    });
});
