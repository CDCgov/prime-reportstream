import { screen } from "@testing-library/react";

import { ReceiversStatusesDisplay } from "./ReceiversStatusesDisplay";
import { renderApp } from "../../../../utils/CustomRenderUtils";
import { SKIP_HOURS } from "../constants";
import { mockReceiversStatusesTimePeriod } from "../fixtures";

describe("ReceiversStatusesDisplay", () => {
    test("renders", async () => {
        const { baseElement } = renderApp(
            <ReceiversStatusesDisplay
                receiverStatuses={mockReceiversStatusesTimePeriod}
            />,
        );
        const days = screen.getAllByText(/Mon/);
        expect(days.length).toBe(5);
        const orgs = screen.getAllByText(/oh-doh/);
        expect(orgs.length).toBe(1);

        // role options does NOT support "aria-disabled=false". lame.
        // No easy way to find active buttons
        const slices = await screen.findAllByRole("button", {});

        // broken out for readability
        const slicesPerDay = 24 / SKIP_HOURS;
        const numDays = 3; // based on datesRange
        const numReceivers = mockReceiversStatusesTimePeriod.length; // based on mockData
        const totalSlices = numReceivers * numDays * slicesPerDay;
        const clickableSlices = mockReceiversStatusesTimePeriod.reduce(
            (prev, curr) => {
                return (
                    prev +
                    curr.days.reduce((prev, curr) => {
                        return prev + curr.entries.length;
                    }, 0)
                );
            },
            0,
        );
        expect(slices.length).toBe(totalSlices); // based on receivers x days x 12 slices/day

        // find a slice that is clickable. How?
        // We can't access className in vi's virtual DOM.
        // We can't access "aria-disabled" for the button with vi's virtual DOM.
        // ONLY solution is to j
        const clickableSliceElements = baseElement.querySelectorAll(
            `[role="button"][aria-disabled="false"]`,
        );

        expect(clickableSliceElements.length).toBe(clickableSlices); // based on Data and slices
    });
});
