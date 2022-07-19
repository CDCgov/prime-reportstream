import { AdmConnStatusDataType } from "../../resources/AdmConnStatusResource";

import { _exportForTesting } from "./AdminDestinationStatusDashboard";

const DATA: AdmConnStatusDataType[] = [
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
        receiverConnectionCheckResultId: 2397,
        organizationId: 61,
        receiverId: 312,
        connectionCheckResult: "connectionCheckResult dummy result 2397",
        connectionCheckSuccessful: true,
        connectionCheckStartedAt: "2022-07-11T07:59:23.713Z",
        connectionCheckCompletedAt: "2022-07-11T07:59:24.033Z",
        organizationName: "ca-dph",
        receiverName: "elr",
    },
    {
        receiverConnectionCheckResultId: 2396,
        organizationId: 49,
        receiverId: 311,
        connectionCheckResult: "connectionCheckResult dummy result 2396",
        connectionCheckSuccessful: false,
        connectionCheckStartedAt: "2022-07-11T07:59:23.385Z",
        connectionCheckCompletedAt: "2022-07-11T07:59:23.711Z",
        organizationName: "oh-doh",
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

describe("AdminDestinationStatusDashboard tests", () => {
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

    // test("groupDataByReceiver", () => {
    //     const result = _exportForTesting.groupDataByReceiver(DATA);
    //     expect(Object.keys(result).length).toBe(3);
    //     expect(result["elr"]).toBe(2);
    //     expect(result["elr-secondary"]).toBe(2);
    //     expect(result["elr-otc"]).toBe(1);
    // });
    //
    // test("groupDataByHour", () => {
    //     const result = _exportForTesting.groupDataByHour(DATA);
    //     expect(Object.keys(result).length).toBe(2);
    //     expect(result["July 11, 12 AM"]).toBe(3);
    //     expect(result["July 11, 1 AM"]).toBe(2);
    // });
    //
    // test("dataToKeyBoolean", () => {
    //     const result = _exportForTesting.dataToKeyBoolean(DATA);
    //     expect(Object.keys(result).length).toBe(5);
    //     expect(result["2398"]).toBe(true);
    //     expect(result["2397"]).toBe(true);
    //     expect(result["2396"]).toBe(false);
    //     expect(result["2395"]).toBe(true);
    //     expect(result["2394"]).toBe(false);
    // });
    //
    // test("progressIndicator", () => {
    //     const data = { "1": true, "2": false, "3": true };
    //     let clickedItem = "";
    //     const onClick = (item: string) => (clickedItem = item);
    //     const container = render(
    //         <_exportForTesting.ProgressIndicatorComponent
    //             data={data}
    //             onClick={onClick}
    //         />
    //     ).container;
    //     const html = container.innerHTML;
    //
    //     console.log(html);
    // });
});
