import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import ManagePublicKeyUpload, {
    ManagePublicKeyUploadProps,
} from "./ManagePublicKeyUpload";

describe("ManagePublicKeyUpload", () => {
    const DEFAULT_PROPS: ManagePublicKeyUploadProps = {
        onPublicKeySubmit: () => {},
        onFileChange: () => {},
        file: null,
    };

    const contentString = "This is the fake file text";
    const fakeFile = new File([new Blob([contentString])], "file.pem", {
        type: "application/x-x509-ca-cert",
    });

    function doRender(props: Partial<ManagePublicKeyUploadProps> = {}) {
        return renderApp(
            <ManagePublicKeyUpload {...DEFAULT_PROPS} {...props} />
        );
    }

    describe("when unsubmitted (default state)", () => {
        beforeEach(() => {
            renderApp(<ManagePublicKeyUpload {...DEFAULT_PROPS} />);
        });

        test("renders the input", () => {
            expect(screen.getByTestId("file-input-input")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
        });
    });

    describe("when file selected", () => {
        beforeEach(() => {
            doRender({
                file: fakeFile,
            });
        });

        test("enables the submit", () => {
            expect(screen.queryByText("Submit")).toBeEnabled();
        });

        describe("with accept values", () => {
            test("only allows .pem files", () => {
                expect(screen.getByTestId("file-input-input")).toHaveAttribute(
                    "accept",
                    ".pem"
                );
            });
        });
    });
});
