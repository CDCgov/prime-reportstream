import { render, screen } from "@testing-library/react";

import { AdmConnStatusDataType } from "../../resources/AdmConnStatusResource";

import { _exportForTesting } from "./AdminReceiverDashboard";

const DATA: AdmConnStatusDataType[] = [
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

describe("AdminReceiverDashboard tests", () => {
    test("misc functions", () => {
        expect(_exportForTesting.defaultStartDateIso()).toContain("T");
        expect(_exportForTesting.defaultEndDateIso()).toContain("Z");
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

    test("roundDateDown", () => {
        expect(
            _exportForTesting.roundIsoDateDown("2022-07-11T08:09:22.748")
        ).toBe("2022-07-11T00:00:00.000");

        expect(
            _exportForTesting.roundIsoDateDown("2022-07-11T08:09:22.748Z")
        ).toBe("2022-07-11T00:00:00.000Z");
    });

    test("roundDateUp", () => {
        expect(
            _exportForTesting.roundIsoDateUp("2022-07-11T08:09:22.748")
        ).toBe("2022-07-12T00:00:00.000");

        expect(
            _exportForTesting.roundIsoDateUp("2022-07-11T08:09:22.748Z")
        ).toBe("2022-07-12T00:00:00.000Z");
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

    test("makeDictionary and MainRender tests", async () => {
        const data = _exportForTesting.makeDictionary(DATA); // sorts
        const keys = Object.keys(data);
        expect(keys.length).toBe(6);
        // make sure makeDictionary sorted correctly.
        expect(data[keys[3]].organizationName).toBe("oh-doh");

        render(
            // eslint-disable-next-line react/jsx-pascal-case
            <_exportForTesting.MainRender
                data={data}
                datesRange={[new Date("2022-07-11"), new Date()]}
                filterRowStatus={_exportForTesting.SuccessRate.ALL_SUCCESSFUL}
                onDetailsClick={(_subdata: AdmConnStatusDataType[]) => {}}
            />
        );

        expect(
            screen.getByText("Mon, 7/11/2022", { exact: false })
        ).toBeInTheDocument();
    });
});
