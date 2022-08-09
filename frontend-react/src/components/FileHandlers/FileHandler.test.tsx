import { screen, fireEvent, waitFor } from "@testing-library/react";

import { renderWithSession } from "../../utils/CustomRenderUtils";
import {
    EndpointName,
    ResponseError,
    WatersResponse,
} from "../../network/api/WatersApi";
import {
    INITIAL_STATE,
    FileType,
    FileHandlerActionType,
} from "../../hooks/UseFileHandler";
import { formattedDateFromTimestamp } from "../../utils/DateTimeUtils";

import FileHandler, { FileHandlerType } from "./FileHandler";

let fakeState = {};

const hl7Sender = {
    sender: { format: "HL7" },
    loading: false,
};

const mockState = (state: any) => (fakeState = state);
const fakeStateProvider = () => fakeState;
const mockDispatch = jest.fn();

// again, foiled when trying to use an actual jest mock here
// someday we should get to the bottom of why we can't control
// return values for jest mocks when they are used to mock imported functions this way
let mockUseSenderResource = () => {};

jest.mock("../../hooks/UseFileHandler", () => ({
    ...jest.requireActual("../../hooks/UseFileHandler"),
    default: () => {
        return {
            state: fakeStateProvider(),
            dispatch: mockDispatch,
        };
    },
    __esModule: true,
}));

jest.mock("../../hooks/UseOrganizationResource", () => ({
    useOrganizationResource: () => {
        return {
            organization: { description: "wow, cool organization" },
            loading: false,
        };
    },
}));

jest.mock("../../hooks/UseSenderResource", () => ({
    useSenderResource: () => mockUseSenderResource(),
}));

/* 
  below is an example of a mocked File & mocked React file input change event we can use for future tests
  thx to https://evanteague.medium.com/creating-fake-test-events-with-typescript-jest-778018379d1e
*/

const contentString = "some file content";

// doesn't work out of the box as it somehow doesn't come with a .text method
const fakeFile = new File([new Blob([contentString])], "file.csv", {
    type: "hl7",
});
fakeFile.text = () => Promise.resolve(contentString);

const fakeFileList = {
    length: 1,
    item: () => fakeFile,
    [Symbol.iterator]: function* () {
        yield fakeFile;
    },
} as FileList;

const fileChangeEvent = {
    target: {
        files: fakeFileList,
    },
} as React.ChangeEvent<HTMLInputElement>;

describe("FileHandler", () => {
    test("renders a spinner while loading sender / organization", async () => {
        mockUseSenderResource = () => ({
            ...hl7Sender,
            loading: true,
        });
        mockState(INITIAL_STATE);
        renderWithSession(
            <FileHandler
                headingText="handler heading"
                handlerType={FileHandlerType.VALIDATION}
                fetcher={() => Promise.resolve({} as WatersResponse)}
                successMessage=""
                resetText=""
                submitText=""
                showSuccessMetadata={false}
                showWarningBanner={false}
                warningText=""
                endpointName={EndpointName.WATERS}
            />
        );
        const spinner = await screen.findByLabelText("loading-indicator");
        expect(spinner).toBeInTheDocument();

        const imaginaryHeading = screen.queryByText("handler heading");
        expect(imaginaryHeading).not.toBeInTheDocument();
    });
    describe("post load", () => {
        beforeEach(() => {
            mockUseSenderResource = () => hl7Sender;
        });

        test("renders as expected (initial form)", async () => {
            mockState(INITIAL_STATE);
            renderWithSession(
                <FileHandler
                    headingText="handler heading"
                    handlerType={FileHandlerType.VALIDATION}
                    fetcher={() => Promise.resolve({} as WatersResponse)}
                    successMessage=""
                    resetText=""
                    submitText="SEND SOMEWHERE"
                    showSuccessMetadata={false}
                    showWarningBanner={false}
                    warningText=""
                    endpointName={EndpointName.WATERS}
                />
            );

            const headings = await screen.findAllByRole("heading");
            expect(headings).toHaveLength(2);
            expect(headings[0]).toHaveTextContent("handler heading");
            expect(headings[1]).toHaveTextContent("wow, cool organization");

            // to verify that the form is rendered
            // this test id is added by trussworks, so... hopefully they don't change it?
            const input = screen.queryByTestId("file-input-input"); //
            expect(input).toBeInTheDocument();

            const formLabel = await screen.findByText(
                "Select an HL7 v2.5.1 formatted file to send somewhere. Make sure that your file has a .hl7 extension."
            );
            expect(formLabel).toHaveAttribute("id", "upload-csv-input-label");
        });

        test("renders as expected (submitting)", async () => {
            mockState({ ...INITIAL_STATE, isSubmitting: true });
            renderWithSession(
                <FileHandler
                    headingText="handler heading"
                    handlerType={FileHandlerType.VALIDATION}
                    fetcher={() => Promise.resolve({} as WatersResponse)}
                    successMessage=""
                    resetText=""
                    submitText=""
                    showSuccessMetadata={false}
                    showWarningBanner={false}
                    warningText=""
                    endpointName={EndpointName.WATERS}
                />
            );

            // to verify that the form is not rendered
            const input = screen.queryByTestId("file-input-input"); //
            expect(input).not.toBeInTheDocument();

            const spinner = await screen.findByLabelText("loading-indicator");
            expect(spinner).toBeInTheDocument();
        });

        test("renders as expected (errors)", async () => {
            mockState({
                ...INITIAL_STATE,
                errors: [{ message: "error" } as ResponseError],
            });
            renderWithSession(
                <FileHandler
                    headingText="handler heading"
                    handlerType={FileHandlerType.VALIDATION}
                    fetcher={() => Promise.resolve({} as WatersResponse)}
                    successMessage=""
                    resetText=""
                    submitText=""
                    showSuccessMetadata={false}
                    showWarningBanner={false}
                    warningText=""
                    endpointName={EndpointName.WATERS}
                />
            );

            const errorTable = await screen.findByTestId("error-table");
            expect(errorTable).toBeInTheDocument();

            // to verify that the form is not rendered
            const input = screen.queryByTestId("file-input-input"); //
            expect(input).not.toBeInTheDocument();

            // testing creation of error messaging for validation + file error
            // for now, assuming that if this works, it will work for the other 3 combinations as well
            const message = await screen.findByText(
                "Please review the errors below."
            );
            expect(message).toHaveClass("usa-alert__text");

            const heading = await screen.findByText(
                "Your file has not passed validation"
            );
            expect(heading).toHaveClass("usa-alert__heading");
        });

        test("renders as expected (success)", async () => {
            mockState({
                ...INITIAL_STATE,
                fileType: FileType.HL7,
                destinations: "1, 2",
                reportId: "IDIDID",
                successTimestamp: new Date(0).toString(),
            });
            renderWithSession(
                <FileHandler
                    headingText="handler heading"
                    handlerType={FileHandlerType.UPLOAD}
                    fetcher={() => Promise.resolve({} as WatersResponse)}
                    successMessage="it was a success"
                    resetText=""
                    submitText=""
                    showSuccessMetadata={true}
                    showWarningBanner={false}
                    warningText=""
                    endpointName={EndpointName.WATERS}
                />
            );

            const errorTable = screen.queryByTestId("error-table");
            expect(errorTable).not.toBeInTheDocument();

            // to verify that the form is not rendered
            const input = screen.queryByTestId("file-input-input"); //
            expect(input).not.toBeInTheDocument();

            // testing creation of success messaging for upload + hl7
            // for now, assuming that if this works, it will work for the other 3 combinations as well
            const message = await screen.findByText(
                "Your file meets the ReportStream standard HL7 v2.5.1 schema and will be transmitted."
            );
            expect(message).toHaveClass("usa-alert__text");

            const heading = await screen.findByText("it was a success");
            expect(heading).toHaveClass("usa-alert__heading");

            const destinations = await screen.findByText("1, 2");
            expect(destinations).toHaveClass("margin-top-05");

            const reportLink = await screen.findByRole("link");
            expect(reportLink).toHaveTextContent("IDIDID");
            expect(reportLink).toHaveAttribute("href", "/submissions/IDIDID");

            const timestampDate = await screen.findByText(
                formattedDateFromTimestamp(
                    new Date(0).toString(),
                    "DD MMMM YYYY"
                )
            );
            expect(timestampDate).toHaveClass("margin-top-05");
        });

        test("renders as expected when FileHandlerType = VALIDATION (success)", async () => {
            mockState({
                ...INITIAL_STATE,
                fileType: FileType.HL7,
                destinations: "1, 2",
                reportId: null,
                successTimestamp: new Date(0).toString(),
                overallStatus: "Valid",
            });
            renderWithSession(
                <FileHandler
                    headingText="handler heading"
                    handlerType={FileHandlerType.VALIDATION}
                    fetcher={() => Promise.resolve({} as WatersResponse)}
                    successMessage="it was a success"
                    resetText=""
                    submitText=""
                    showSuccessMetadata={true}
                    showWarningBanner={false}
                    warningText=""
                    endpointName={EndpointName.VALIDATE}
                />
            );

            const errorTable = screen.queryByTestId("error-table");
            expect(errorTable).not.toBeInTheDocument();

            // testing creation of success messaging for upload + hl7
            // for now, assuming that if this works, it will work for the other 3 combinations as well
            const message = await screen.findByText(
                "Your file meets the ReportStream standard HL7 v2.5.1 schema."
            );
            expect(message).toHaveClass("usa-alert__text");

            const heading = await screen.findByText("it was a success");
            expect(heading).toHaveClass("usa-alert__heading");

            const destinations = await screen.findByText("1, 2");
            expect(destinations).toHaveClass("margin-top-05");

            const timestampDate = await screen.findByText(
                formattedDateFromTimestamp(
                    new Date(0).toString(),
                    "DD MMMM YYYY"
                )
            );
            expect(timestampDate).toHaveClass("margin-top-05");
        });

        test("renders as expected (warnings)", async () => {
            mockState({
                ...INITIAL_STATE,
                warnings: [{ message: "error" } as ResponseError],
                reportId: 1,
            });
            renderWithSession(
                <FileHandler
                    headingText="handler heading"
                    handlerType={FileHandlerType.VALIDATION}
                    fetcher={() => Promise.resolve({} as WatersResponse)}
                    successMessage="it was a success"
                    resetText=""
                    submitText=""
                    showSuccessMetadata={false}
                    showWarningBanner={false}
                    warningText=""
                    endpointName={EndpointName.WATERS}
                />
            );

            const errorTable = await screen.findByTestId("error-table");
            expect(errorTable).toBeInTheDocument();

            // to verify that the form is not rendered
            const input = screen.queryByTestId("file-input-input"); //
            expect(input).not.toBeInTheDocument();

            // testing creation of error messaging for upload
            // for now, assuming that if this works, it will work for validation as well
            const message = await screen.findByText(
                "The following warnings were returned while processing your file. Your file has passed validation, but these warning areas can be addressed to enhance clarity."
            );
            expect(message).toHaveClass("usa-alert__text");

            const heading = await screen.findByText(
                "We found non-critical issues in your file"
            );
            expect(heading).toHaveClass("usa-alert__heading");
        });

        test("renders as expected (warning banner)", async () => {
            mockState({
                ...INITIAL_STATE,
            });
            renderWithSession(
                <FileHandler
                    headingText="handler heading"
                    handlerType={FileHandlerType.UPLOAD}
                    fetcher={() => Promise.resolve({} as WatersResponse)}
                    successMessage="it was a success"
                    resetText=""
                    submitText=""
                    showSuccessMetadata={false}
                    showWarningBanner={true}
                    warningText="THIS IS A WARNING"
                    endpointName={EndpointName.WATERS}
                />
            );
            const message = await screen.findByText("THIS IS A WARNING");
            expect(message).toHaveClass("usa-alert__text");

            const heading = await screen.findByText("Warning");
            expect(heading).toHaveClass("usa-alert__heading");
        });

        test("calls dispatch as expected on file change", async () => {
            mockState({
                ...INITIAL_STATE,
            });
            renderWithSession(
                <FileHandler
                    headingText=""
                    handlerType={FileHandlerType.UPLOAD}
                    fetcher={() => Promise.resolve({} as WatersResponse)}
                    successMessage=""
                    resetText=""
                    submitText=""
                    showSuccessMetadata={false}
                    showWarningBanner={false}
                    warningText=""
                    endpointName={EndpointName.WATERS}
                />
            );

            const input = await screen.findByTestId("file-input-input");

            fireEvent.change(input, fileChangeEvent);
            await waitFor(
                () => {
                    expect(mockDispatch).toHaveBeenCalledTimes(1);
                },
                {
                    onTimeout: (e) => {
                        console.error(
                            "dispatch not called on file select handler"
                        );
                        return e;
                    },
                }
            );
            expect(mockDispatch).toHaveBeenCalledWith({
                type: FileHandlerActionType.FILE_SELECTED,
                payload: { file: fakeFile },
            });
        });
        test("calls fetch and dispatch as expected on submit", async () => {
            const fakeResponse: WatersResponse = {};
            const fetchSpy = jest.fn(() => Promise.resolve(fakeResponse));
            mockState({
                ...INITIAL_STATE,
                fileName: "anything",
            });
            renderWithSession(
                <FileHandler
                    headingText="handler heading"
                    handlerType={FileHandlerType.UPLOAD}
                    fetcher={fetchSpy}
                    successMessage=""
                    resetText=""
                    submitText="SUBMIT ME"
                    showSuccessMetadata={false}
                    showWarningBanner={false}
                    warningText=""
                    endpointName={EndpointName.WATERS}
                />
            );

            const input = await screen.findByTestId("file-input-input");
            const submitButton = await screen.findByText("SUBMIT ME");
            expect(submitButton).toHaveAttribute("type", "submit");

            // set file to be uploaded
            fireEvent.change(input, fileChangeEvent);
            await waitFor(
                () => {
                    expect(mockDispatch).toHaveBeenCalledTimes(1);
                },
                {
                    onTimeout: (e) => {
                        console.error(
                            "dispatch not called on file select handler"
                        );
                        return e;
                    },
                }
            );

            expect(submitButton).toBeEnabled();
            fireEvent.click(submitButton);

            await waitFor(
                () => {
                    // expect(mockDispatch).toHaveBeenCalledTimes(2);
                    expect(fetchSpy).toHaveBeenCalledTimes(1);
                },
                {
                    onTimeout: (e) => {
                        console.error("dispatch not called on submit handler");
                        return e;
                    },
                }
            );

            // more complete file mocking would result in better data here, but I'm good with this
            expect(fetchSpy).toHaveBeenCalledWith(
                "undefined.undefined", // client
                "anything", // filename
                undefined, //contentType
                contentString, //fileContent
                "", //parsedName
                "", //accessToken
                EndpointName.WATERS
            );
            expect(mockDispatch).toHaveBeenCalledWith({
                type: FileHandlerActionType.REQUEST_COMPLETE,
                payload: { response: fakeResponse },
            });
        });

        test("calls dispatch as expected on reset", async () => {
            const fakeResponse: WatersResponse = {};
            const fetchSpy = jest.fn(() => Promise.resolve(fakeResponse));
            mockState({
                ...INITIAL_STATE,
                cancellable: true,
            });
            renderWithSession(
                <FileHandler
                    headingText="handler heading"
                    handlerType={FileHandlerType.UPLOAD}
                    fetcher={fetchSpy}
                    successMessage=""
                    resetText=""
                    submitText="SUBMIT ME"
                    showSuccessMetadata={false}
                    showWarningBanner={false}
                    warningText=""
                    endpointName={EndpointName.WATERS}
                />
            );

            const cancelButton = await screen.findByText("Cancel");
            expect(cancelButton).toHaveAttribute("type", "button");

            fireEvent.click(cancelButton);
            expect(mockDispatch).toHaveBeenCalledWith({
                type: FileHandlerActionType.RESET,
            });
        });
    });
});
