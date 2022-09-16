import { fireEvent, render, screen } from "@testing-library/react";
import { NetworkErrorBoundary } from "rest-hooks";
import React, { Suspense } from "react";
import { MemoryRouter } from "react-router-dom";

import { AdmConnStatusDataType } from "../../resources/AdmConnStatusResource";
import { ErrorPage } from "../../pages/error/ErrorPage";

import { _exportForTesting } from "./AdminReceiverDashboard";

// <editor-fold defaultstate="collapsed" desc="mockData: AdmConnStatusDataType[]">
const mockData: AdmConnStatusDataType[] = [
    {
        receiverConnectionCheckResultId: 2397,
        organizationId: 61,
        receiverId: 313,
        connectionCheckResult: "connectionCheckResult dummy result 2397",
        connectionCheckSuccessful: true,
        connectionCheckStartedAt: "2022-07-11T00:00:00.001Z",
        connectionCheckCompletedAt: "2022-07-11T00:00:50.000Z",
        organizationName: "ca-dph",
        receiverName: "elr",
    },
    {
        receiverConnectionCheckResultId: 2398,
        organizationId: 61,
        receiverId: 313,
        connectionCheckResult: "connectionCheckResult dummy result 2398",
        connectionCheckSuccessful: true,
        connectionCheckStartedAt: "2022-07-11T07:59:24.036Z",
        connectionCheckCompletedAt: "2022-07-11T07:59:24.358Z",
        organizationName: "ca-dph",
        receiverName: "elr-secondary",
    },
    {
        receiverConnectionCheckResultId: 2399,
        organizationId: 49,
        receiverId: 311,
        connectionCheckResult: "connectionCheckResult dummy result 2399",
        connectionCheckSuccessful: true,
        connectionCheckStartedAt: "2022-07-11T07:59:23.713Z",
        connectionCheckCompletedAt: "2022-07-11T07:59:24.033Z",
        organizationName: "oh-doh",
        receiverName: "elr",
    },
    {
        // this entry is out-of-sort-order and connectionCheckSuccessful:failed
        receiverConnectionCheckResultId: 2396,
        organizationId: 61,
        receiverId: 312,
        connectionCheckResult: "connectionCheckResult dummy result 2396",
        connectionCheckSuccessful: false,
        connectionCheckStartedAt: "2022-07-11T07:59:23.385Z",
        connectionCheckCompletedAt: "2022-07-11T07:59:23.711Z",
        organizationName: "ca-dph",
        receiverName: "elr",
    },
    {
        receiverConnectionCheckResultId: 2395,
        organizationId: 46,
        receiverId: 310,
        connectionCheckResult: "connectionCheckResult dummy result 2395",
        connectionCheckSuccessful: true,
        connectionCheckStartedAt: "2022-07-11T08:09:23.066Z",
        connectionCheckCompletedAt: "2022-07-11T08:09:23.383Z",
        organizationName: "vt-doh",
        receiverName: "elr-otc",
    },
    {
        receiverConnectionCheckResultId: 2394,
        organizationId: 46,
        receiverId: 309,
        connectionCheckResult: "connectionCheckResult dummy result 2394",
        connectionCheckSuccessful: false,
        connectionCheckStartedAt: "2022-07-11T08:09:22.748Z",
        connectionCheckCompletedAt: "2022-07-11T08:09:23.063Z",
        organizationName: "vt-doh",
        receiverName: "elr-secondary",
    },
];
// </editor-fold>

jest.mock("rest-hooks", () => ({
    useResource: () => {
        return mockData;
    },
    useController: () => {
        // fetch is destructured as fetchController in component
        return { fetch: () => mockData };
    },
    // Must return children when mocking, otherwise nothing inside renders
    NetworkErrorBoundary: ({ children }: { children: JSX.Element[] }) => {
        return <>{children}</>;
    },
}));

describe("AdminReceiverDashboard tests", () => {
    test("misc functions", () => {
        // we're checking these don't throw.
        const now = new Date();
        expect(_exportForTesting.startOfDayIso(now)).toContain("T");
        expect(_exportForTesting.endOfDayIso(now)).toContain("T");
        expect(_exportForTesting.initialStartDate().toISOString()).toContain(
            "T"
        );
        expect(_exportForTesting.initialEndDate().toISOString()).toContain("T");
        expect(_exportForTesting.strcmp("A", "a")).toBe(-1);
        expect(_exportForTesting.strcmp("a", "a")).toBe(0);
        expect(_exportForTesting.strcmp("a", "A")).toBe(1);

        expect(
            _exportForTesting.dateIsInRange(new Date("1/2/2020"), [
                new Date("1/1/2020"),
                new Date("1/3/2020"),
            ])
        ).toBe(true);
        expect(
            _exportForTesting.dateIsInRange(new Date("1/2/2020"), [
                new Date("1/1/2020"),
                new Date("1/1/2020"),
            ])
        ).toBe(false);
        expect(
            _exportForTesting.dateIsInRange(new Date("1/1/2020"), [
                new Date("1/1/2020"),
                new Date("1/2/2020"),
            ])
        ).toBe(true);
    });

    test("TimeSlots", () => {
        // NOTE: all times are ISO with a trailing "Z"
        const timeslots = new _exportForTesting.TimeSlots(
            [
                new Date("2022-07-11T00:00:00.000Z"),
                new Date("2022-07-13T00:00:00.000Z"),
            ],
            8
        );

        const resultStart: string[] = [];
        const resultEnd: string[] = [];
        for (let timeslot of timeslots) {
            resultStart.push(timeslot[0].toISOString());
            resultEnd.push(timeslot[1].toISOString());
        }
        expect(JSON.stringify(resultStart)).toBe(
            `["2022-07-11T00:00:00.000Z","2022-07-11T08:00:00.000Z","2022-07-11T16:00:00.000Z",` +
                `"2022-07-12T00:00:00.000Z","2022-07-12T08:00:00.000Z","2022-07-12T16:00:00.000Z"]`
        );
        expect(JSON.stringify(resultEnd)).toBe(
            `["2022-07-11T08:00:00.000Z","2022-07-11T16:00:00.000Z","2022-07-12T00:00:00.000Z",` +
                `"2022-07-12T08:00:00.000Z","2022-07-12T16:00:00.000Z","2022-07-13T00:00:00.000Z"]`
        );
    });

    test("dateShortFormat", () => {
        expect(
            _exportForTesting.dateShortFormat(
                new Date("2022-07-11T08:09:22.748Z")
            )
        ).toBe("Mon, 7/11/2022");
    });

    test("SuccessRateTracker", () => {
        // test two overlapping conditions and variables just to be sure no globals are used
        const testSuccess = new _exportForTesting.SuccessRateTracker();
        const testFailure = new _exportForTesting.SuccessRateTracker();

        for (let ii = 0; ii < 2; ii++) {
            // run twice to make sure reset works
            expect(testSuccess.currentState).toBe(
                _exportForTesting.SuccessRate.UNDEFINED
            );
            expect(testFailure.currentState).toBe(
                _exportForTesting.SuccessRate.UNDEFINED
            );

            expect(testSuccess.updateState(true)).toBe(
                _exportForTesting.SuccessRate.ALL_SUCCESSFUL
            );
            expect(testFailure.updateState(false)).toBe(
                _exportForTesting.SuccessRate.ALL_FAILURE
            );

            expect(testSuccess.updateState(true)).toBe(
                _exportForTesting.SuccessRate.ALL_SUCCESSFUL
            );
            expect(testFailure.updateState(false)).toBe(
                _exportForTesting.SuccessRate.ALL_FAILURE
            );

            // Flip it so we make results mixed.
            expect(testSuccess.updateState(false)).toBe(
                _exportForTesting.SuccessRate.MIXED_SUCCESS
            );
            expect(testFailure.updateState(true)).toBe(
                _exportForTesting.SuccessRate.MIXED_SUCCESS
            );

            // test reset
            testSuccess.reset();
            testFailure.reset();
        }
    });

    test("durationFormatShort", () => {
        const now = new Date();
        const before = new Date(now);
        before.setHours(
            before.getHours() - 1,
            before.getMinutes() - 2,
            before.getSeconds() - 3
        );

        const result1 = _exportForTesting.durationFormatShort(now, before);
        expect(result1).toBe("1h 02m 03s");

        const future2 = new Date(now.getTime() + 5678);
        const result2 = _exportForTesting.durationFormatShort(future2, now);
        expect(result2).toBe("05.678s");

        future2.setHours(future2.getHours() + 12, future2.getMinutes() + 34);
        const result3 = _exportForTesting.durationFormatShort(future2, now);
        expect(result3).toBe("12h 34m 05.678s");

        const result4 = _exportForTesting.durationFormatShort(now, now);
        expect(result4).toBe("");
    });

    test("sortStatusData", async () => {
        const data = _exportForTesting.sortStatusData(mockData); // sorts
        expect(data.length).toBe(6);
        // make sure sortStatusData sorted correctly.
        expect(data[3].organizationName).toBe("oh-doh");
    });

    test("sortStatusData and MainRender tests", async () => {
        const { baseElement } = render(
            <MemoryRouter>
                <Suspense fallback={<></>}>
                    <NetworkErrorBoundary
                        fallbackComponent={() => <ErrorPage type="message" />}
                    >
                        {/*eslint-disable-next-line react/jsx-pascal-case*/}
                        <_exportForTesting.MainRender
                            datesRange={[
                                new Date("2022-07-11"),
                                new Date("2022-07-14"),
                            ]}
                            filterRowStatus={
                                _exportForTesting.SuccessRate.ALL_SUCCESSFUL
                            }
                            filterErrorText={" "}
                            filterRowReceiver={"-"}
                            onDetailsClick={(
                                _subdata: AdmConnStatusDataType[]
                            ) => {}}
                        />
                    </NetworkErrorBoundary>
                </Suspense>
            </MemoryRouter>
        );

        const days = screen.getAllByText(/Mon/);
        expect(days.length).toBe(3);
        const orgs = screen.getAllByText(/oh-doh/);
        expect(orgs.length).toBe(1);
        expect(_exportForTesting.sortStatusData([])).toStrictEqual([]);

        // role options does NOT support "aria-disabled=false". lame.
        // No easy way to find active buttons
        const slices = await screen.findAllByRole("button", {});

        // broken out for readability
        const slicesPerDay = 24 / _exportForTesting.SKIP_HOURS;
        const numDays = 3; // based on datesRange
        const numReceivers = 3; // based on mockData
        const totalSlices = numReceivers * numDays * slicesPerDay;
        expect(slices.length).toBe(totalSlices); // based on receivers x days x 12 slices/day

        // find a slice that is clickable. How?
        // We can't access className in jest's virtual DOM.
        // We can't access "aria-disabled" for the button with jest's virtual DOM.
        // ONLY solution is to j
        const clickableSlices = baseElement.querySelectorAll(
            `[role="button"][aria-disabled="false"]`
        );

        expect(clickableSlices.length).toBe(3); // based on Data and slices

        fireEvent.click(clickableSlices[0]);

        // Seems like we need a cypress test to verify modal is shown
        // expect(
        //     screen.getAllByText(/Results for connection verification check/)
        // ).toBeInTheDocument();
    });

    test("ModalInfoRender", async () => {
        const data = _exportForTesting.sortStatusData(mockData); // sorts
        const subData = data[0];
        render(
            // eslint-disable-next-line react/jsx-pascal-case
            <_exportForTesting.ModalInfoRender subData={[subData]} />
        );
        const matches = screen.queryAllByText(
            "connectionCheckResult dummy result 2397"
        );
        expect(matches.length).toBe(1);
    });

    test("ModalInfoRender empty", async () => {
        render(
            // eslint-disable-next-line react/jsx-pascal-case
            <_exportForTesting.ModalInfoRender subData={[]} />
        );
        expect(screen.getByText(/No Data Found/)).toBeInTheDocument();
    });

    test("DateRangePickingAtomic", async () => {
        render(
            // eslint-disable-next-line react/jsx-pascal-case
            <_exportForTesting.DateRangePickingAtomic
                defaultStartDate="2022-07-11T00:00:00.000Z"
                defaultEndDate="2022-07-13T00:00:00.000Z"
                onChange={(_props) => {}}
            />
        );
        expect(screen.getByText(/7\/11\/2022/)).toBeInTheDocument();
    });
});
