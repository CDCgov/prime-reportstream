import { renderHook } from "@testing-library/react-hooks";
import * as AiReact from "@microsoft/applicationinsights-react-js";

import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import { MembershipSettings, MemberType } from "../UseOktaMemberships";

import useTrackEventWithSession from "./UseTrackEventWithSession";

describe("useTrackEventWithSession", () => {
    const loggedOutSessionContextValue = {
        activeMembership: {} as MembershipSettings,
        memberships: new Map(),
        oktaToken: {},
        dispatch: () => {},
        initialized: false,
    };

    const loggedInSessionContextValue = {
        activeMembership: {
            memberType: MemberType.SENDER,
            parsedName: "testOrg",
            service: "testSender",
        },
        memberships: new Map([
            [
                "DHPrimeAdmins",
                {
                    parsedName: "PrimeAdmins",
                    memberType: MemberType.PRIME_ADMIN,
                    service: undefined,
                },
            ],
        ]),
        oktaToken: { accessToken: "TOKEN" },
        dispatch: () => {},
        initialized: true,
    };

    let trackEventSpy: () => void;
    let trackFn: (args?: {}) => void;

    beforeEach(() => {
        trackEventSpy = jest.fn();
        jest.spyOn(AiReact, "useTrackEvent").mockReturnValue(trackEventSpy);
    });

    afterAll(() => {
        jest.restoreAllMocks();
    });

    describe("when App Insights is initialized", () => {
        beforeEach(() => {
            jest.spyOn(AiReact, "useAppInsightsContext").mockReturnValue(
                {} as AiReact.ReactPlugin
            );
        });

        describe("when the user is logged in", () => {
            beforeEach(() => {
                mockSessionContext.mockReturnValueOnce(
                    loggedInSessionContextValue
                );

                const view = renderHook(() => useTrackEventWithSession("Test"));
                trackFn = view.result.current as (args?: {}) => {};
            });

            describe("when the hook function is called with no arguments", () => {
                test("calls the track event function with only the session variables", () => {
                    trackFn();

                    expect(trackEventSpy).toHaveBeenCalledTimes(1);
                    expect(trackEventSpy).toHaveBeenCalledWith({
                        activeMembership:
                            loggedInSessionContextValue.activeMembership,
                        memberships: loggedInSessionContextValue.memberships,
                    });
                });
            });

            describe("when the hook function is called with an object", () => {
                test("calls the track event function with the object and session variables", () => {
                    const obj = { a: 1, b: 2 };
                    trackFn(obj);

                    expect(trackEventSpy).toHaveBeenCalledTimes(1);
                    expect(trackEventSpy).toHaveBeenCalledWith({
                        ...obj,
                        activeMembership:
                            loggedInSessionContextValue.activeMembership,
                        memberships: loggedInSessionContextValue.memberships,
                    });
                });
            });
        });

        describe("when the user is not logged in", () => {
            beforeEach(() => {
                mockSessionContext.mockReturnValueOnce(
                    loggedOutSessionContextValue
                );

                const view = renderHook(() => useTrackEventWithSession("Test"));
                trackFn = view.result.current as (args?: {}) => {};
            });

            test("calls the track event function with the object and empty session variables", () => {
                trackFn();

                expect(trackEventSpy).toHaveBeenCalledTimes(1);
                expect(trackEventSpy).toHaveBeenCalledWith({
                    activeMembership:
                        loggedOutSessionContextValue.activeMembership,
                    memberships: loggedOutSessionContextValue.memberships,
                });
            });
        });
    });

    describe("when App Insights is not initialized", () => {
        let warnSpy: (args: string) => void;

        beforeEach(() => {
            mockSessionContext.mockReturnValueOnce(
                loggedOutSessionContextValue
            );

            warnSpy = jest.fn();
            jest.spyOn(console, "warn").mockImplementation(warnSpy);

            const view = renderHook(() => useTrackEventWithSession("Test"));
            trackFn = view.result.current as (args?: {}) => {};
        });

        test("logs out a warning with the attempted event name once", () => {
            trackFn();

            expect(warnSpy).toHaveBeenCalledWith(
                "useTrackEventWithSession for Test called without appInsights"
            );
        });

        test("warns once for all track event calls", () => {
            trackFn();
            trackFn();
            trackFn();

            expect(warnSpy).toHaveBeenCalledTimes(1);
        });

        test("does not call the track event function", () => {
            trackFn();

            expect(trackEventSpy).not.toHaveBeenCalled();
        });
    });
});
