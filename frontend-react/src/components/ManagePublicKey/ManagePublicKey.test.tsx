import { screen, fireEvent } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";
import { UseOrganizationSendersResult } from "../../hooks/UseOrganizationSenders";
import * as useOrganizationSendersExports from "../../hooks/UseOrganizationSenders";
import { RSSender } from "../../config/endpoints/settings";

import { ManagePublicKey } from "./ManagePublicKey";

const DEFAULT_SENDERS: RSSender[] = [
    {
        name: "default",
        organizationName: "ignore",
        format: "CSV",
        customerStatus: "inactive",
        schemaName: "primedatainput/pdi-covid-19",
        processingType: "sync",
        allowDuplicates: true,
        topic: "covid-19",
    },
    {
        name: "ignore-full-elr",
        organizationName: "ignore",
        format: "HL7",
        customerStatus: "active",
        schemaName: "strac/strac-covid-19",
        processingType: "sync",
        allowDuplicates: true,
        topic: "full-elr",
    },
];

describe("ManagePublicKey", () => {
    afterEach(() => {
        jest.restoreAllMocks();
    });

    function mockUseOrganizationSenders(
        result: Partial<UseOrganizationSendersResult> = {}
    ) {
        jest.spyOn(
            useOrganizationSendersExports,
            "useOrganizationSenders"
        ).mockReturnValue({
            isLoading: false,
            senders: DEFAULT_SENDERS,
            ...result,
        });
    }

    describe("on load", () => {
        beforeEach(() => {
            mockUseOrganizationSenders({
                isLoading: false,
                senders: DEFAULT_SENDERS,
            });

            renderApp(<ManagePublicKey />);
        });

        test("renders ManagePublicKeyUpload", () => {
            expect(screen.getByText(/Manage Public Key/)).toBeVisible();
            expect(screen.getByTestId("ManagePublicKeyUpload")).toBeVisible();
        });

        describe.skip("when more than one sender", () => {
            beforeEach(() => {
                mockUseOrganizationSenders({
                    isLoading: false,
                    senders: DEFAULT_SENDERS,
                });

                renderApp(<ManagePublicKey />);
            });

            test("renders ManagePublicKeyChooseSender", () => {
                expect(screen.getByText(/Manage Public Key/)).toBeVisible();
                expect(
                    screen.getByTestId("ManagePublicKeyChooseSender")
                ).toBeVisible();
                expect(
                    screen.queryByTestId("ManagePublicKeyUpload")
                ).not.toBeInTheDocument();
            });

            describe("when sender is selected", () => {
                test("renders ManagePublicKeyUpload", async () => {
                    const submit = await screen.findByRole("button");
                    expect(submit).toHaveAttribute("type", "submit");
                    expect(submit).toBeDisabled();

                    const selectSender = screen.getByRole("combobox");
                    expect(selectSender).toBeInTheDocument();
                    expect(selectSender).toHaveValue("");
                    fireEvent.change(selectSender, {
                        target: { value: "default" },
                    });

                    expect(selectSender).toHaveValue("default");

                    expect(submit).toBeEnabled();
                    // Instead of clicking the submit button, fire submit on the form to prevent console error
                    fireEvent.submit(screen.getByTestId("form"));

                    expect(
                        screen.queryByTestId("ManagePublicKeyChooseSender")
                    ).not.toBeInTheDocument();
                    expect(
                        screen.getByTestId("ManagePublicKeyUpload")
                    ).toBeVisible();
                });
            });
        });

        describe.skip("when only one sender", () => {
            beforeEach(() => {
                mockUseOrganizationSenders({
                    isLoading: false,
                    senders: DEFAULT_SENDERS.splice(0, 1),
                });

                renderApp(<ManagePublicKey />);
            });

            test("renders ManagePublicKeyUpload", () => {
                expect(screen.getByText(/Manage Public Key/)).toBeVisible();
                expect(
                    screen.queryByTestId("ManagePublicKeyChooseSender")
                ).not.toBeInTheDocument();
                expect(
                    screen.getByTestId("ManagePublicKeyUpload")
                ).toBeVisible();
            });
        });
    });
});
