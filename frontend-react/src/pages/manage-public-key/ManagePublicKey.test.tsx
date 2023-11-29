import {
    screen,
    fireEvent,
    waitFor,
    RenderResult,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useErrorBoundary } from "react-error-boundary";

import { RSSender } from "../../config/endpoints/settings";
import { sendersGenerator } from "../../__mocks__/OrganizationMockServer";
import { render } from "../../utils/Test/render";
import ManagePublicKeyUploadError from "../../components/ManagePublicKey/ManagePublicKeyUploadError";
import silenceVirtualConsole from "../../utils/Test/silenceVirtualConsole";

import { ManagePublicKeyPageBase } from "./ManagePublicKey";

vi.mock("../../components/ManagePublicKey/ManagePublicKeyUploadError");

const mockManagePublicKeyUploadError = vi.mocked(ManagePublicKeyUploadError);
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
                    kid: `testOrg.${DEFAULT_SENDERS[0].name}`,
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
    const mockOnSubmit = vi.fn().mockResolvedValue({});
    const mockOnError = vi.fn();

    function submitHandler(renderResult: RenderResult, key: any) {
        renderResult.rerender(
            <ManagePublicKeyPageBase
                onSubmit={mockOnSubmit}
                onError={mockOnError}
                senders={[DEFAULT_SENDERS[0]]}
                keySets={mockRSApiKeysResponse.keys}
            />,
        );
    }

    async function submitFile(file: File = fakeFile) {
        const input = screen.getByTestId("file-input-input");
        expect(input).toBeVisible();
        expect(screen.getByText("Submit")).toBeDisabled();
        await chooseFile(file);
        expect(await screen.findByText("Submit")).toBeVisible();
        fireEvent.submit(screen.getByTestId("form"));
        await waitFor(() => expect(input).not.toBeInTheDocument());
    }

    function setup() {
        render(
            <ManagePublicKeyPageBase
                onSubmit={mockOnSubmit}
                onError={mockOnError}
                senders={[DEFAULT_SENDERS[0]]}
                keySets={[]}
            />,
        );
    }

    describe("when the key is submitting", async () => {
        test("renders a spinner", async () => {
            let res: (...args: any[]) => void = () => void 0;
            const p = new Promise((_res) => {
                res = _res;
            });
            mockOnSubmit.mockImplementation(async () => await p);
            setup();
            submitFile();
            await waitFor(() => {
                expect(
                    screen.getByLabelText("loading-indicator"),
                ).toBeVisible();
            });
            res();
        });
    });

    describe("when the Organization has more than one Sender", () => {
        function setup() {
            render(
                <ManagePublicKeyPageBase
                    onSubmit={mockOnSubmit}
                    onError={mockOnError}
                    senders={DEFAULT_SENDERS}
                    keySets={[]}
                />,
            );
        }
        test("renders the sender options", () => {
            setup();
            expect(screen.getByText(/Manage public key/)).toBeVisible();
            expect(
                screen.getByTestId("ManagePublicKeyChooseSender"),
            ).toBeVisible();
            expect(
                screen.queryByTestId("file-input-input"),
            ).not.toBeInTheDocument();
        });

        describe("when the Sender is selected", () => {
            async function setup() {
                render(
                    <ManagePublicKeyPageBase
                        onSubmit={mockOnSubmit}
                        onError={mockOnError}
                        senders={DEFAULT_SENDERS}
                        keySets={[]}
                    />,
                );
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
                await waitFor(async () => {
                    fireEvent.submit(screen.getByTestId("form"));
                    expect(
                        screen.queryByTestId("ManagePublicKeyChooseSender"),
                    ).not.toBeInTheDocument();
                });
            }

            test("renders ManagePublicKeyUpload", async () => {
                await setup();
                expect(screen.getByTestId("file-input-input")).toBeVisible();
            });
        });
    });

    describe("when the Organization has one sender", () => {
        test("renders ManagePublicKeyUpload", () => {
            setup();
            expect(screen.getByText(/Manage public key/)).toBeVisible();
            expect(
                screen.queryByTestId("ManagePublicKeyChooseSender"),
            ).not.toBeInTheDocument();
            expect(screen.getByTestId("file-input-input")).toBeVisible();
        });
    });

    describe("when the senders public key has already been configured", () => {
        async function setup() {
            render(
                <ManagePublicKeyPageBase
                    onSubmit={mockOnSubmit}
                    onError={mockOnError}
                    senders={[DEFAULT_SENDERS[0]]}
                    keySets={mockRSApiKeysResponse.keys}
                />,
            );
            await waitFor(() =>
                expect(
                    screen.getByText("Your public key is already configured."),
                ).toBeVisible(),
            );
        }

        // TODO: remove skip when functionality reenabled
        test.todo(
            "shows the configured screen and allows the user to upload a new public key",
            async () => {
                await setup();

                await waitFor(async () => {
                    await userEvent.click(
                        screen.getByText("Upload new public key"),
                    );

                    await new Promise((res) => setTimeout(res, 100));
                });
                expect(screen.getByTestId("file-input-input")).toBeVisible();
                expect(screen.getByText("Submit")).toBeDisabled();
            },
        );

        test("shows the configured screen and displays a message to the user", async () => {
            await setup();
            expect(screen.getByText("Contact ReportStream")).toBeVisible();
            expect(
                screen.getByText(/to upload a new public key./),
            ).toBeVisible();
        });
    });

    describe(
        "when a valid pem file is being submitted",
        () => {
            async function setup() {
                const result = render(
                    <ManagePublicKeyPageBase
                        onSubmit={mockOnSubmit}
                        onError={mockOnError}
                        senders={[DEFAULT_SENDERS[0]]}
                        keySets={[]}
                    />,
                );
                mockOnSubmit.mockImplementation((args: any) =>
                    submitHandler(result, args),
                );
                await submitFile();
            }

            test("uploads the file and shows the success screen", async () => {
                await setup();
                expect(
                    await screen.findByText(
                        "You can now submit data to ReportStream.",
                    ),
                ).toBeVisible();
            });
        },
        { timeout: 60000 },
    );

    describe("when an invalid pem file is being submitted", () => {
        async function submitInvalidFile() {
            await submitFile();
            expect(
                await screen.findByText("Key could not be submitted"),
            ).toBeVisible();
        }
        async function _setup() {
            mockManagePublicKeyUploadError.mockImplementation(() => {
                // eslint-disable-next-line react-hooks/rules-of-hooks
                const { resetBoundary } = useErrorBoundary();
                return (
                    <section>
                        <h1>Key could not be submitted</h1>
                        <button type="button" onClick={resetBoundary}>
                            Try again
                        </button>
                    </section>
                );
            });
            mockOnSubmit.mockRejectedValue(new Error("Test Error"));
            render(
                <ManagePublicKeyPageBase
                    onSubmit={mockOnSubmit}
                    onError={mockOnError}
                    senders={DEFAULT_SENDERS.slice(0, 1)}
                    keySets={[]}
                />,
            );
            const reset = silenceVirtualConsole();
            await submitInvalidFile();
            reset();
        }
        async function setup() {
            await waitFor(_setup);
        }

        test("shows the upload error screen", async () => {
            await setup();
        });

        test("allows the user to try again", async () => {
            await setup();
            await userEvent.click(screen.getByText("Try again"));

            expect(await screen.findByText("Drag file here or")).toBeVisible();
        });
    });
});
