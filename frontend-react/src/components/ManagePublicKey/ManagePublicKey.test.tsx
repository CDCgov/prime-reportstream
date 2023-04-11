import { screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../utils/CustomRenderUtils";
import { UseOrganizationSendersResult } from "../../hooks/UseOrganizationSenders";
import * as useOrganizationSendersExports from "../../hooks/UseOrganizationSenders";
import * as useCreateOrganizationPublicKeyExports from "../../hooks/UseCreateOrganizationPublicKey";
import * as useOrganizationPublicKeysExports from "../../hooks/UseOrganizationPublicKeys";
import { RSApiKeysResponse, RSSender } from "../../config/endpoints/settings";
import { UseCreateOrganizationPublicKeyResult } from "../../hooks/UseCreateOrganizationPublicKey";
import { sendersGenerator } from "../../__mocks__/OrganizationMockServer";
import { UseOrganizationPublicKeysResult } from "../../hooks/UseOrganizationPublicKeys";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";

import { ManagePublicKey } from "./ManagePublicKey";

const DEFAULT_SENDERS: RSSender[] = sendersGenerator(2);

const contentString = "This is the fake file text";
const fakeFile = new File([new Blob([contentString])], "file.pem", {
    type: "application/x-x509-ca-cert",
});
fakeFile.text = () => Promise.resolve(contentString);

const mockRSApiKeysResponse = {
    orgName: "testOrg",
    keys: [
        {
            scope: "testOrg.*.report",
            keys: [
                {
                    kty: "RSA",
                    kid: "testOrg.elr-0",
                    n: "asdfaasdfffffffffffffffffffffffffasdfasdfasdfasdf",
                    e: "AQAB",
                },
            ],
        },
    ],
};

export async function chooseFile(file: File) {
    expect(screen.getByText("Drag file here or")).toBeVisible();
    await userEvent.upload(screen.getByTestId("file-input-input"), file);
    await screen.findByTestId("file-input-preview-image");
}

describe("ManagePublicKey", () => {
    // looking into solution, will resolve
    function mockUseCreateOrganizationPublicKey(
        result: Partial<RSApiKeysResponse> = {}
    ) {
        jest.spyOn(
            useCreateOrganizationPublicKeyExports,
            "useCreateOrganizationPublicKey"
        ).mockReturnValue({
            isLoading: false,
            isSuccess: true,
            mutateAsync: (_) => {
                return Promise.resolve({
                    orgName: "ignore",
                    keys: [
                        {
                            scope: "ignore.*.report",
                            keys: [],
                        },
                    ],
                    ...result,
                });
            },
        } as UseCreateOrganizationPublicKeyResult);
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

    function mockUseOrganizationPublicKeys(
        result: Partial<UseOrganizationPublicKeysResult> = {}
    ) {
        jest.spyOn(
            useOrganizationPublicKeysExports,
            "useOrganizationPublicKeys"
        ).mockReturnValue({
            isLoading: false,
            orgPublicKeys: { orgName: "elr-0", keys: [] },
            ...result,
        });
    }

    beforeEach(() => {
        mockSessionContext.mockReturnValue({
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "testOrg",
                service: "serviceName",
            },
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: false,
            isUserSender: true,
        });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe.skip("by default", () => {
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
                    screen.queryByTestId("file-input-input")
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
                        target: { value: "elr-1" },
                    });

                    expect(selectSender).toHaveValue("elr-1");

                    expect(submit).toBeEnabled();
                    fireEvent.submit(screen.getByTestId("form"));

                    expect(
                        screen.queryByTestId("ManagePublicKeyChooseSender")
                    ).not.toBeInTheDocument();
                    expect(
                        screen.getByTestId("file-input-input")
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
                expect(screen.getByTestId("file-input-input")).toBeVisible();
            });
        });
    });

    describe("when the senders public key has already been configured", () => {
        beforeEach(() => {
            mockUseOrganizationSenders({
                senders: DEFAULT_SENDERS.splice(0, 1),
            });

            mockUseOrganizationPublicKeys({
                orgPublicKeys: mockRSApiKeysResponse,
            });

            renderApp(<ManagePublicKey />);
        });

        test("shows the configured screen and allows the user to upload a new public key", async () => {
            expect(
                screen.getByText("Your public key is already configured.")
            ).toBeVisible();

            await userEvent.click(screen.getByText("Upload new public key"));

            expect(screen.getByTestId("file-input-input")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
        });
    });

    describe.skip("when a valid pem file is being submitted", () => {
        beforeEach(() => {
            mockUseCreateOrganizationPublicKey({
                isLoading: false,
                isSuccess: true,
                mutateAsync: () => Promise.resolve(mockRSApiKeysResponse),
            });

            renderApp(<ManagePublicKey />);
        });

        test("uploads the file and shows the success screen", async () => {
            expect(screen.getByText("Submit")).toBeDisabled();
            await chooseFile(fakeFile);
            expect(screen.getByText("Submit")).toBeEnabled();
            fireEvent.submit(screen.getByTestId("form"));

            expect(
                screen.getByText("You can now submit data to ReportStream.")
            ).toBeVisible();
        });
    });

    describe.skip("when an invalid pem file is being submitted", () => {
        beforeEach(() => {
            mockUseCreateOrganizationPublicKey({
                isLoading: false,
                isSuccess: false,
                mutateAsync: () => Promise.resolve({}),
            });

            renderApp(<ManagePublicKey />);
        });

        test("shows the upload error screen", async () => {
            expect(screen.getByText("Submit")).toBeDisabled();
            await chooseFile(fakeFile);
            expect(screen.getByText("Submit")).toBeEnabled();
            fireEvent.submit(screen.getByTestId("form"));

            expect(
                screen.getByText("Key could not be submitted")
            ).toBeVisible();
        });

        test("allows the user to try again", async () => {
            expect(screen.getByText("Submit")).toBeDisabled();
            await chooseFile(fakeFile);
            expect(screen.getByText("Submit")).toBeEnabled();
            fireEvent.submit(screen.getByTestId("form"));

            expect(
                screen.getByText("Key could not be submitted")
            ).toBeVisible();
            await userEvent.click(screen.getByText("Try Again"));

            expect(screen.getByText("Drag file here or")).toBeVisible();
        });
    });
});
