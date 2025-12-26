import { screen } from "@testing-library/react";
import TimePeriodModalInner from "./TimePeriodModal";
import { renderApp } from "../../../../utils/CustomRenderUtils";
import { mockReceiversStatusesParsed } from "../fixtures";
import { sortStatusData } from "../utils";

describe("TimePeriodModal", () => {
    test("With Data", () => {
        const data = sortStatusData(mockReceiversStatusesParsed); // sorts
        const statuses = [data[0]];
        renderApp(<TimePeriodModalInner receiverStatuses={statuses} />);
        const matches = screen.queryAllByText(
            "connectionCheckResult dummy result 2397",
        );
        expect(matches.length).toBe(1);
    });

    test("With No Data", () => {
        renderApp(<TimePeriodModalInner />);
        expect(screen.getByText(/No Data Found/)).toBeInTheDocument();
    });
});
