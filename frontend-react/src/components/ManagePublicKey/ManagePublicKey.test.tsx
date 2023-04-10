import { screen, fireEvent, act, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../utils/CustomRenderUtils";
import { UseOrganizationSendersResult } from "../../hooks/UseOrganizationSenders";
import * as useOrganizationSendersExports from "../../hooks/UseOrganizationSenders";
import * as useCreateOrganizationPublicKeyExports from "../../hooks/UseCreateOrganizationPublicKey";
import { RSApiKeysResponse, RSSender } from "../../config/endpoints/settings";

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

// TODO: move for re-usability
const contentString = "This is the fake file text";
const fakeFile = new File([new Blob([contentString])], "file.pem", {
    type: "application/x-x509-ca-cert",
});

export async function chooseFile(file: File) {
    expect(screen.getByText("Drag file here or")).toBeVisible();
    await userEvent.upload(screen.getByTestId("file-input-input"), file);
    await screen.findByTestId("file-input-preview-image");
}

describe("ManagePublicKey", () => {
    afterEach(() => {
        jest.restoreAllMocks();
    });

    // looking into solution, will resolve
    function mockUseCreateOrganizationPublicKey(
        result: Partial<RSApiKeysResponse> = {}
    ) {
        jest.spyOn(
            useCreateOrganizationPublicKeyExports,
            "useCreateOrganizationPublicKey"
        ).mockReturnValue({
            mutateAsync: () =>
                Promise.resolve({
                    orgName: "test",
                    keys: [],
                    ...result,
                } as RSApiKeysResponse),
            isLoading: false,
            isSuccess: false,
        });
    }

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
        describe("when more than one sender", () => {
            beforeEach(() => {
                mockUseOrganizationSenders({
                    isLoading: false,
                    senders: DEFAULT_SENDERS,
                });

                renderApp(<ManagePublicKey />);
            });

            test("renders ManagePublicKeyChooseSender", () => {
                expect(screen.getByText(/Manage public key/)).toBeVisible();
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

        describe("when only one sender", () => {
            beforeEach(() => {
                mockUseOrganizationSenders({
                    isLoading: false,
                    senders: DEFAULT_SENDERS.splice(0, 1),
                });

                renderApp(<ManagePublicKey />);
            });

            test("renders ManagePublicKeyUpload", () => {
                expect(screen.getByText(/Manage public key/)).toBeVisible();
                expect(
                    screen.queryByTestId("ManagePublicKeyChooseSender")
                ).not.toBeInTheDocument();
                expect(
                    screen.getByTestId("ManagePublicKeyUpload")
                ).toBeVisible();
            });
        });
    });

    describe.skip("when a valid pem file is being submitted", () => {
        beforeEach(() => {
            mockUseCreateOrganizationPublicKey({
                isSuccess: true,
                isLoading: false,
                mutateAsync: () => Promise.resolve({}), // TODO: return saved values here
            });

            renderApp(<ManagePublicKey />);
        });

        test("uploads the file and shows the success screen", async () => {
            expect(screen.getByText("Submit")).toBeDisabled();
            await chooseFile(fakeFile);
            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });

            await waitFor(() => {
                return screen.getByText(
                    "You can now submit data to ReportStream."
                );
            });
        });
    });
});
