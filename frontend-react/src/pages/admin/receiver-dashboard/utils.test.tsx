import { mockReceiversStatuses, mockReceiversStatusesTimePeriod } from "./fixtures";
import { filterStatuses, sortStatusData, SuccessRate } from "./utils";

describe("AdminReceiverDashboard utils tests", () => {
    test("sortStatusData", () => {
        const data = sortStatusData(mockReceiversStatuses); // sorts
        expect(data.length).toBe(6);
        // make sure sortStatusData sorted correctly.
        expect(data[3].organizationName).toBe("oh-doh");
    });

    test("filterStatuses", () => {
        const filteredData = filterStatuses(mockReceiversStatusesTimePeriod, "-", SuccessRate.UNDEFINED);
        expect(filteredData).toHaveLength(3);
    });
});
