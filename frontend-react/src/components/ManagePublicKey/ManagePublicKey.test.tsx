import { fireEvent, screen, waitFor } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";

import { ManagePublicKeyPage } from "./ManagePublicKey";
import { sendersGenerator } from "../../__mockServers__/OrganizationMockServer";
import { RSSender } from "../../config/endpoints/settings";
import * as useCreateOrganizationPublicKeyExports from "../../hooks/api/organizations/UseCreateOrganizationPublicKey/UseCreateOrganizationPublicKey";
import { UseCreateOrganizationPublicKeyResult } from "../../hooks/api/organizations/UseCreateOrganizationPublicKey/UseCreateOrganizationPublicKey";
import * as useOrganizationPublicKeysExports from "../../hooks/api/organizations/UseOrganizationPublicKeys/UseOrganizationPublicKeys";
import { UseOrganizationPublicKeysResult } from "../../hooks/api/organizations/UseOrganizationPublicKeys/UseOrganizationPublicKeys";
import * as useOrganizationSendersExports from "../../hooks/api/organizations/UseOrganizationSenders/UseOrganizationSenders";
import { UseOrganizationSendersResult } from "../../hooks/api/organizations/UseOrganizationSenders/UseOrganizationSenders";
import { renderApp } from "../../utils/CustomRenderUtils";
import { MemberType } from "../../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../contexts/Session/__mocks__/useSessionContext")
>("../../contexts/Session/useSessionContext");

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

async function chooseFile(file: File) {
    expect(screen.getByText("Drag file here or")).toBeVisible();
    await userEvent.upload(screen.getByTestId("file-input-input"), file);
    await screen.findByTestId("file-input-preview-image");
}

describe("ManagePublicKey", () => {
    function mockUseCreateOrganizationPublicKey(result: Partial<UseCreateOrganizationPublicKeyResult>) {
        vi.spyOn(useCreateOrganizationPublicKeyExports, "default").mockReturnValue({
            mutateAsync: vi.fn(),
            ...result,
        } as UseCreateOrganizationPublicKeyResult);
    }

    function mockUseOrganizationSenders(result: Partial<UseOrganizationSendersResult> = {}) {
        vi.spyOn(useOrganizationSendersExports, "default").mockReturnValue({
            data: DEFAULT_SENDERS,
            ...result,
        } as UseOrganizationSendersResult);
    }

    function mockUseOrganizationPublicKeys(result: Partial<UseOrganizationPublicKeysResult> = {}) {
        vi.spyOn(useOrganizationPublicKeysExports, "default").mockReturnValue({
            data: { orgName: "elr-0", keys: [] },
            ...result,
        } as UseOrganizationPublicKeysResult);
    }

    beforeEach(() => {
        mockSessionContentReturnValue({
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "testOrg",
                service: "serviceName",
            },
            user: {
                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: true,
                isUserTransceiver: false,
            } as any,
        });
    });

    describe("when the Organization has more than one Sender", () => {
        function renderSetup() {
            mockUseOrganizationSenders({
                isLoading: false,
                data: DEFAULT_SENDERS,
            });
            mockUseOrganizationPublicKeys();

            renderApp(<ManagePublicKeyPage />);
        }

        test("renders the sender options", () => {
            renderSetup();
            expect(screen.getByText(/Manage public key/)).toBeVisible();
            expect(screen.getByTestId("ManagePublicKeyChooseSender")).toBeVisible();
            expect(screen.queryByTestId("file-input-input")).not.toBeInTheDocument();
        });

        describe("when the Sender is selected", () => {
            async function setup() {
                const submit = await screen.findByRole("button");
                expect(submit).toHaveAttribute("type", "submit");
                expect(submit).toBeDisabled();

                const selectSender = screen.getByRole("combobox");
                expect(selectSender).toBeInTheDocument();
                expect(selectSender).toHaveValue("");

                await waitFor(async () => {
                    await userEvent.selectOptions(selectSender, ["elr-1"]);
                    expect(submit).toBeEnabled();
                });
                await waitFor(() => {
                    // eslint-disable-next-line testing-library/no-wait-for-side-effects
                    fireEvent.submit(screen.getByTestId("form"));
                    expect(screen.queryByTestId("ManagePublicKeyChooseSender")).not.toBeInTheDocument();
                });
            }

            test("renders ManagePublicKeyUpload", async () => {
                renderSetup();
                await setup();
                expect(screen.getByTestId("file-input-input")).toBeVisible();
            });
        });
    });

    describe("when the Organization has one sender", () => {
        function setup() {
            mockUseOrganizationSenders({
                isLoading: false,
                data: DEFAULT_SENDERS.slice(0, 1),
            });
            mockUseOrganizationPublicKeys();

            renderApp(<ManagePublicKeyPage />);
        }

        test("renders ManagePublicKeyUpload", () => {
            setup();
            expect(screen.getByText(/Manage public key/)).toBeVisible();
            expect(screen.queryByTestId("ManagePublicKeyChooseSender")).not.toBeInTheDocument();
            expect(screen.getByTestId("file-input-input")).toBeVisible();
        });
    });

    describe("when the senders public key has already been configured", () => {
        function setup() {
            mockUseOrganizationSenders({
                isLoading: false,
                data: DEFAULT_SENDERS.slice(0, 1),
            });

            mockUseOrganizationPublicKeys({
                data: mockRSApiKeysResponse,
            });

            renderApp(<ManagePublicKeyPage />);
        }

        test.skip("shows the configured screen and allows the user to upload a new public key", async () => {
            setup();
            expect(screen.getByText("Your public key is already configured.")).toBeVisible();

            await waitFor(async () => {
                await userEvent.click(screen.getByText("Upload new public key"));

                await new Promise((res) => setTimeout(res, 1000));
            });
            expect(screen.getByTestId("file-input-input")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
        });

        test("shows the configured screen and displays a message to the user", () => {
            setup();
            expect(screen.getByText(/Your public key is already configured./)).toBeVisible();
            expect(screen.getByText("Contact ReportStream")).toBeVisible();
            expect(screen.getByText(/to upload a new public key./)).toBeVisible();
        });
    });

    describe("when a valid pem file is being submitted", () => {
        function setup() {
            mockUseOrganizationPublicKeys();
            // Selected sender
            mockUseOrganizationSenders({
                isLoading: false,
                data: DEFAULT_SENDERS.slice(1),
            });

            mockUseCreateOrganizationPublicKey({
                isPending: false,
                isSuccess: true,
            });

            renderApp(<ManagePublicKeyPage />);
        }

        test("uploads the file and shows the success screen", async () => {
            setup();
            expect(screen.getByTestId("file-input-input")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
            await waitFor(async () => {
                await chooseFile(fakeFile);
                expect(screen.getByText("Submit")).toBeVisible();
            });
            await waitFor(() => {
                // eslint-disable-next-line testing-library/no-wait-for-side-effects
                fireEvent.submit(screen.getByTestId("form"));
                expect(screen.getByText("You can now submit data to ReportStream.")).toBeVisible();
            });
        });
    });

    describe("when an invalid pem file is being submitted", () => {
        async function setup() {
            mockUseOrganizationPublicKeys();
            // Selected sender
            mockUseOrganizationSenders({
                isLoading: false,
                data: DEFAULT_SENDERS.slice(0, 1),
            });

            mockUseCreateOrganizationPublicKey({
                isPending: false,
                isSuccess: false,
            });

            renderApp(<ManagePublicKeyPage />);

            expect(screen.getByTestId("file-input-input")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
            await waitFor(async () => {
                await chooseFile(fakeFile);
                expect(screen.getByText("Submit")).toBeVisible();
            });
            await waitFor(async () => {
                // eslint-disable-next-line testing-library/no-wait-for-side-effects
                fireEvent.submit(screen.getByTestId("form"));
                await new Promise((res) => setTimeout(res, 100));
            });
        }

        test("shows the upload error screen", async () => {
            await setup();
            expect(screen.getByText("Key could not be submitted")).toBeVisible();
        });

        test("allows the user to try again", async () => {
            await setup();
            expect(screen.getByText("Key could not be submitted")).toBeVisible();

            await waitFor(async () => {
                await userEvent.click(screen.getByText("Try again"));

                expect(screen.getByText("Drag file here or")).toBeVisible();
            });
        });
    });
});
