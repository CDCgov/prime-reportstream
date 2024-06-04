import { fireEvent, screen } from "@testing-library/react";
import { Suspense } from "react";

import {
    dateIsInRange,
    DateRangePickingAtomic,
    dateShortFormat,
    durationFormatShort,
    endOfDayIso,
    initialEndDate,
    initialStartDate,
    MainRender,
    ModalInfoRender,
    SKIP_HOURS,
    sortStatusData,
    startOfDayIso,
    strcmp,
    SuccessRate,
    SuccessRateTracker,
    TimeSlots,
} from "./AdminReceiverDashboard";
import type { RSReceiverStatus } from "../../hooks/api/UseReceiversConnectionStatus/UseReceiversConnectionStatus";
import { renderApp } from "../../utils/CustomRenderUtils";

const mockData: RSReceiverStatus[] = [
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

vi.mock(
    "../../hooks/api/UseReceiversConnectionStatus/UseReceiversConnectionStatus.ts",
    () => ({
        default: () => {
            return { data: mockData };
        },
    }),
);

describe("AdminReceiverDashboard tests", () => {
    test("misc functions", () => {
        // we're checking these don't throw.
        const now = new Date();
        expect(startOfDayIso(now)).toContain("T");
        expect(endOfDayIso(now)).toContain("T");
        expect(initialStartDate().toISOString()).toContain("T");
        expect(initialEndDate().toISOString()).toContain("T");
        expect(strcmp("A", "a")).toBe(-1);
        expect(strcmp("a", "a")).toBe(0);
        expect(strcmp("a", "A")).toBe(1);

        expect(
            dateIsInRange(new Date("1/2/2020"), [
                new Date("1/1/2020"),
                new Date("1/3/2020"),
            ]),
        ).toBe(true);
        expect(
            dateIsInRange(new Date("1/2/2020"), [
                new Date("1/1/2020"),
                new Date("1/1/2020"),
            ]),
        ).toBe(false);
        expect(
            dateIsInRange(new Date("1/1/2020"), [
                new Date("1/1/2020"),
                new Date("1/2/2020"),
            ]),
        ).toBe(true);
    });

    test("TimeSlots", () => {
        // NOTE: all times are ISO with a trailing "Z"
        const timeslots = new TimeSlots(
            [
                new Date("2022-07-11T00:00:00.000Z"),
                new Date("2022-07-13T00:00:00.000Z"),
            ],
            8,
        );

        const resultStart: string[] = [];
        const resultEnd: string[] = [];
        for (const timeslot of timeslots) {
            resultStart.push(timeslot[0].toISOString());
            resultEnd.push(timeslot[1].toISOString());
        }
        expect(JSON.stringify(resultStart)).toBe(
            `["2022-07-11T00:00:00.000Z","2022-07-11T08:00:00.000Z","2022-07-11T16:00:00.000Z",` +
                `"2022-07-12T00:00:00.000Z","2022-07-12T08:00:00.000Z","2022-07-12T16:00:00.000Z"]`,
        );
        expect(JSON.stringify(resultEnd)).toBe(
            `["2022-07-11T08:00:00.000Z","2022-07-11T16:00:00.000Z","2022-07-12T00:00:00.000Z",` +
                `"2022-07-12T08:00:00.000Z","2022-07-12T16:00:00.000Z","2022-07-13T00:00:00.000Z"]`,
        );
    });

    test("dateShortFormat", () => {
        expect(dateShortFormat(new Date("2022-07-11T08:09:22.748Z"))).toBe(
            "Mon, 7/11/2022",
        );
    });

    test("SuccessRateTracker", () => {
        // test two overlapping conditions and variables just to be sure no globals are used
        const testSuccess = new SuccessRateTracker();
        const testFailure = new SuccessRateTracker();

        for (let ii = 0; ii < 2; ii++) {
            // run twice to make sure reset works
            expect(testSuccess.currentState).toBe(SuccessRate.UNDEFINED);
            expect(testFailure.currentState).toBe(SuccessRate.UNDEFINED);

            expect(testSuccess.updateState(true)).toBe(
                SuccessRate.ALL_SUCCESSFUL,
            );
            expect(testFailure.updateState(false)).toBe(
                SuccessRate.ALL_FAILURE,
            );

            expect(testSuccess.updateState(true)).toBe(
                SuccessRate.ALL_SUCCESSFUL,
            );
            expect(testFailure.updateState(false)).toBe(
                SuccessRate.ALL_FAILURE,
            );

            // Flip it so we make results mixed.
            expect(testSuccess.updateState(false)).toBe(
                SuccessRate.MIXED_SUCCESS,
            );
            expect(testFailure.updateState(true)).toBe(
                SuccessRate.MIXED_SUCCESS,
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
            before.getSeconds() - 3,
        );

        const result1 = durationFormatShort(now, before);
        expect(result1).toBe("1h 02m 03s");

        const future2 = new Date(now.getTime() + 5678);
        const result2 = durationFormatShort(future2, now);
        expect(result2).toBe("05.678s");

        future2.setHours(future2.getHours() + 12, future2.getMinutes() + 34);
        const result3 = durationFormatShort(future2, now);
        expect(result3).toBe("12h 34m 05.678s");

        const result4 = durationFormatShort(now, now);
        expect(result4).toBe("");
    });

    test("sortStatusData", () => {
        const data = sortStatusData(mockData); // sorts
        expect(data.length).toBe(6);
        // make sure sortStatusData sorted correctly.
        expect(data[3].organizationName).toBe("oh-doh");
    });

    test("sortStatusData and MainRender tests", async () => {
        const { baseElement } = renderApp(
            <Suspense fallback={<></>}>
                {/*eslint-disable-next-line react/jsx-pascal-case*/}
                <MainRender
                    datesRange={[
                        new Date("2022-07-11"),
                        new Date("2022-07-14"),
                    ]}
                    filterRowStatus={SuccessRate.ALL_SUCCESSFUL}
                    filterErrorText={" "}
                    filterRowReceiver={"-"}
                    onDetailsClick={(_subdata: RSReceiverStatus[]) => void 0}
                />
            </Suspense>,
        );

        const days = screen.getAllByText(/Mon/);
        expect(days.length).toBe(3);
        const orgs = screen.getAllByText(/oh-doh/);
        expect(orgs.length).toBe(1);
        expect(sortStatusData([])).toStrictEqual([]);

        // role options does NOT support "aria-disabled=false". lame.
        // No easy way to find active buttons
        const slices = await screen.findAllByRole("button", {});

        // broken out for readability
        const slicesPerDay = 24 / SKIP_HOURS;
        const numDays = 3; // based on datesRange
        const numReceivers = 3; // based on mockData
        const totalSlices = numReceivers * numDays * slicesPerDay;
        expect(slices.length).toBe(totalSlices); // based on receivers x days x 12 slices/day

        // find a slice that is clickable. How?
        // We can't access className in vi's virtual DOM.
        // We can't access "aria-disabled" for the button with vi's virtual DOM.
        // ONLY solution is to j
        const clickableSlices = baseElement.querySelectorAll(
            `[role="button"][aria-disabled="false"]`,
        );

        expect(clickableSlices.length).toBe(3); // based on Data and slices

        fireEvent.click(clickableSlices[0]);

        // Seems like we need a cypress test to verify modal is shown
        // expect(
        //     screen.getAllByText(/Results for connection verification check/)
        // ).toBeInTheDocument();
    });

    test("ModalInfoRender", () => {
        const data = sortStatusData(mockData); // sorts
        const subData = data[0];
        renderApp(
            // eslint-disable-next-line react/jsx-pascal-case
            <ModalInfoRender subData={[subData]} />,
        );
        const matches = screen.queryAllByText(
            "connectionCheckResult dummy result 2397",
        );
        expect(matches.length).toBe(1);
    });

    test("ModalInfoRender empty", () => {
        renderApp(
            // eslint-disable-next-line react/jsx-pascal-case
            <ModalInfoRender subData={[]} />,
        );
        expect(screen.getByText(/No Data Found/)).toBeInTheDocument();
    });

    test("DateRangePickingAtomic", () => {
        renderApp(
            // eslint-disable-next-line react/jsx-pascal-case
            <DateRangePickingAtomic
                defaultStartDate="2022-07-11T00:00:00.000Z"
                defaultEndDate="2022-07-13T00:00:00.000Z"
                onChange={(_props) => void 0}
            />,
        );
        expect(screen.getByText(/7\/11\/2022/)).toBeInTheDocument();
    });
});
