import { AccessToken } from "@okta/okta-auth-js";
import { screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
export const mockAccessToken = (mock?: Partial<AccessToken>): AccessToken => {
    return {
        authorizeUrl: mock?.authorizeUrl ?? "",
        expiresAt: mock?.expiresAt ?? 0,
        scopes: mock?.scopes ?? [],
        userinfoUrl: mock?.userinfoUrl ?? "",
        accessToken: mock?.accessToken ?? "",
        claims: mock?.claims ?? { sub: "" },
        tokenType: mock?.tokenType ?? "",
    };
};

export const mockEvent = (mock?: Partial<any>) => {
    return {
        response: mock?.response ?? null,
    };
};

export const selectDatesFromRange = async (dayOne: string, dayTwo: string) => {
    /* Borrowed some of this from Trussworks' own tests: their
     * components are tricky to test. */
    const datePickerButtons = screen.getAllByTestId("date-picker-button");
    const startDatePickerButton = datePickerButtons[0];
    const endDatePickerButton = datePickerButtons[1];

    /* Select Start Date */
    await userEvent.click(startDatePickerButton);
    const newStartDateButton = screen.getByText(`${dayOne}`);
    await userEvent.click(newStartDateButton);

    /* Select End Date */
    await userEvent.click(endDatePickerButton);
    const newEndDateButton = screen.getByText(`${dayTwo}`);
    await userEvent.click(newEndDateButton);
};
