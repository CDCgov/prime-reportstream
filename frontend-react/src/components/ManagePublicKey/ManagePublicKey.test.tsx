import { act, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../utils/CustomRenderUtils";
import * as useCreateOrganizationPublicKeyExports from "../../hooks/network/Organizations/PublicKeys/UseCreateOrganizationPublicKey";
import { UseCreateOrganizationPublicKeyResult } from "../../hooks/network/Organizations/PublicKeys/UseCreateOrganizationPublicKey";
import { RSSender } from "../../config/endpoints/settings";
import { sendersGenerator } from "../../__mocks__/OrganizationMockServer";
import * as useOrganizationPublicKeysExports from "../../hooks/network/Organizations/PublicKeys/UseOrganizationPublicKeys";
import { UseOrganizationPublicKeysResult } from "../../hooks/network/Organizations/PublicKeys/UseOrganizationPublicKeys";
import * as useOrganizationSendersExports from "../../hooks/UseOrganizationSenders";
import { UseOrganizationSendersResult } from "../../hooks/UseOrganizationSenders";
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

async function chooseFile(file: File) {
    expect(screen.getByText("Drag file here or")).toBeVisible();
    await userEvent.upload(screen.getByTestId("file-input-input"), file);
    await screen.findByTestId("file-input-preview-image");
}

describe("ManagePublicKey", () => {
    function mockUseCreateOrganizationPublicKey(
        result: Partial<UseCreateOrganizationPublicKeyResult>,
    ) {
        jest.spyOn(
            useCreateOrganizationPublicKeyExports,
            "default",
        ).mockReturnValue({
            mutateAsync: jest.fn(),
            ...result,
        } as UseCreateOrganizationPublicKeyResult);
    }

    function mockUseOrganizationSenders(
        result: Partial<UseOrganizationSendersResult> = {},
    ) {
        jest.spyOn(useOrganizationSendersExports, "default").mockReturnValue({
            isLoading: false,
            data: DEFAULT_SENDERS,
            ...result,
        } as UseOrganizationSendersResult);
    }

    function mockUseOrganizationPublicKeys(
        result: Partial<UseOrganizationPublicKeysResult> = {},
    ) {
        jest.spyOn(useOrganizationPublicKeysExports, "default").mockReturnValue(
            {
                isLoading: false,
                data: { orgName: "elr-0", keys: [] },
                ...result,
            } as UseOrganizationPublicKeysResult,
        );
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
            environment: "test",
        });
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    describe("when the page is loading", () => {
        beforeEach(() => {
            mockUseOrganizationSenders({ isLoading: true });

            renderApp(<ManagePublicKey />);
        });

        test("renders a spinner", () => {
            expect(screen.getByLabelText("loading-indicator")).toBeVisible();
        });
    });

    describe("when the Organization has more than one Sender", () => {
        beforeEach(() => {
            mockUseOrganizationSenders({
                isLoading: false,
                data: DEFAULT_SENDERS,
            });

            renderApp(<ManagePublicKey />);
        });

        test("renders the sender options", () => {
            expect(screen.getByText(/Manage public key/)).toBeVisible();
            expect(
                screen.getByTestId("ManagePublicKeyChooseSender"),
            ).toBeVisible();
            expect(
                screen.queryByTestId("file-input-input"),
            ).not.toBeInTheDocument();
        });

        describe("when the Sender is selected", () => {
            beforeEach(async () => {
                const submit = await screen.findByRole("button");
                expect(submit).toHaveAttribute("type", "submit");
                expect(submit).toBeDisabled();

                const selectSender = screen.getByRole("combobox");
                expect(selectSender).toBeInTheDocument();
                expect(selectSender).toHaveValue("");

                await userEvent.selectOptions(selectSender, ["elr-1"]);

                expect(submit).toBeEnabled();
                fireEvent.submit(screen.getByTestId("form"));
            });

            test("renders ManagePublicKeyUpload", async () => {
                expect(
                    screen.queryByTestId("ManagePublicKeyChooseSender"),
                ).not.toBeInTheDocument();
                expect(screen.getByTestId("file-input-input")).toBeVisible();
            });
        });
    });

    describe("when the Organization has one sender", () => {
        beforeEach(() => {
            mockUseOrganizationSenders({
                isLoading: false,
                data: DEFAULT_SENDERS.slice(0, 1),
            });

            renderApp(<ManagePublicKey />);
        });

        test("renders ManagePublicKeyUpload", () => {
            expect(screen.getByText(/Manage public key/)).toBeVisible();
            expect(
                screen.queryByTestId("ManagePublicKeyChooseSender"),
            ).not.toBeInTheDocument();
            expect(screen.getByTestId("file-input-input")).toBeVisible();
        });
    });

    describe("when the senders public key has already been configured", () => {
        beforeEach(() => {
            mockUseOrganizationSenders({
                isLoading: false,
                data: DEFAULT_SENDERS.slice(0, 1),
            });

            mockUseOrganizationPublicKeys({
                data: mockRSApiKeysResponse,
            });

            renderApp(<ManagePublicKey />);
        });
        /*
        test("shows the configured screen and allows the user to upload a new public key", async () => {
            expect(
                screen.getByText("Your public key is already configured.")
            ).toBeVisible();

            await userEvent.click(screen.getByText("Upload new public key"));

            expect(screen.getByTestId("file-input-input")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
        });
        */

        test("shows the configured screen and displays a message to the user", async () => {
            expect(
                screen.getByText(/Your public key is already configured./),
            ).toBeVisible();
            expect(screen.getByText("Contact ReportStream")).toBeVisible();
            expect(
                screen.getByText(/to upload a new public key./),
            ).toBeVisible();
        });
    });

    describe("when a valid pem file is being submitted", () => {
        beforeEach(() => {
            // Selected sender
            mockUseOrganizationSenders({
                isLoading: false,
                data: DEFAULT_SENDERS.slice(1),
            });

            mockUseCreateOrganizationPublicKey({
                isLoading: false,
                isSuccess: true,
            });

            renderApp(<ManagePublicKey />);
        });

        test("uploads the file and shows the success screen", async () => {
            expect(screen.getByTestId("file-input-input")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
            await chooseFile(fakeFile);
            expect(screen.getByText("Submit")).toBeVisible();
            /* eslint-disable testing-library/no-unnecessary-act */
            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });
            /* eslint-enable testing-library/no-unnecessary-act */

            expect(
                screen.getByText("You can now submit data to ReportStream."),
            ).toBeVisible();
        });
    });

    describe("when an invalid pem file is being submitted", () => {
        beforeEach(async () => {
            // Selected sender
            mockUseOrganizationSenders({
                isLoading: false,
                data: DEFAULT_SENDERS.slice(0, 1),
            });

            mockUseCreateOrganizationPublicKey({
                isLoading: false,
                isSuccess: false,
            });

            renderApp(<ManagePublicKey />);

            expect(screen.getByTestId("file-input-input")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
            await chooseFile(fakeFile);
            expect(screen.getByText("Submit")).toBeVisible();
            /* eslint-disable testing-library/no-unnecessary-act */
            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });
            /* eslint-enable testing-library/no-unnecessary-act */
        });

        test("shows the upload error screen", () => {
            expect(
                screen.getByText("Key could not be submitted"),
            ).toBeVisible();
        });

        test("allows the user to try again", async () => {
            expect(
                screen.getByText("Key could not be submitted"),
            ).toBeVisible();
            await userEvent.click(screen.getByText("Try again"));

            expect(screen.getByText("Drag file here or")).toBeVisible();
        });
    });
});
