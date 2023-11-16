import { screen } from "@testing-library/react";

import { render } from "../../utils/Test/render";

import ManagePublicKeyUpload, {
    ManagePublicKeyUploadProps,
} from "./ManagePublicKeyUpload";

describe("ManagePublicKeyUpload", () => {
    const DEFAULT_PROPS: ManagePublicKeyUploadProps = {
        onPublicKeySubmit: () => {},
        onFileChange: () => {},
        onBack: () => {},
        hasBack: false,
        publicKey: false,
        file: null,
    };

    const contentString = "This is the fake file text";
    const fakeFile = new File([new Blob([contentString])], "file.pem", {
        type: "application/x-x509-ca-cert",
    });

    function doRender(props: Partial<ManagePublicKeyUploadProps> = {}) {
        return render(<ManagePublicKeyUpload {...DEFAULT_PROPS} {...props} />);
    }

    describe("default state", () => {
        test("renders the input", () => {
            doRender();
            expect(screen.getByTestId("file-input-input")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
        });
    });

    describe("when public key already configured", () => {
        function setup() {
            doRender({
                publicKey: true,
            });
        }

        test("displays message", () => {
            setup();
            expect(
                screen.getByText(/Your public key is already configured./),
            ).toBeVisible();
        });
    });

    describe("when hasBack", () => {
        function setup() {
            doRender({
                hasBack: true,
            });
        }

        test("displays Back button", () => {
            setup();
            expect(screen.queryByText("Back")).toBeVisible();
        });
    });

    describe("when file selected", () => {
        function setup() {
            doRender({
                file: fakeFile,
            });
        }

        test("enables the submit", () => {
            setup();
            expect(screen.queryByText("Submit")).toBeEnabled();
        });

        describe("with accept values", () => {
            test("only allows .pem files", () => {
                setup();
                expect(screen.getByTestId("file-input-input")).toHaveAttribute(
                    "accept",
                    ".pem",
                );
            });
        });
    });
});
