import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../utils/CustomRenderUtils";
import { FileType } from "../../utils/TemporarySettingsAPITypes";
import { INITIAL_STATE } from "../../hooks/UseFileHandler";
import { fakeError, fakeWarning } from "../../hooks/UseFileHandler.test";

import type { RequestLevel } from "./FileHandlerMessaging";
import FileHandlerErrorsWarningsStep, {
    FileHandlerErrorsWarningsStepProps,
} from "./FileHandlerErrorsWarningsStep";

function mockRequestedChangesDisplay({ title }: { title: RequestLevel }) {
    return <div data-testid={`RequestedChangesDisplay--${title}`} />;
}

jest.mock("./FileHandlerMessaging", () => ({
    ...jest.requireActual("./FileHandlerMessaging"),
    RequestedChangesDisplay: mockRequestedChangesDisplay,
}));

describe("FileHandlerErrorsWarningsStep", () => {
    const DEFAULT_PROPS: FileHandlerErrorsWarningsStepProps = {
        ...INITIAL_STATE,
        onPrevStepClick: jest.fn(),
        onNextStepClick: jest.fn(),
        onTestAnotherFileClick: jest.fn(),
        selectedSchemaOption: {
            value: "CSV",
            format: FileType.CSV,
            title: "csv",
        },
    };

    describe("when there are only errors", () => {
        beforeEach(() => {
            renderApp(
                <FileHandlerErrorsWarningsStep
                    {...DEFAULT_PROPS}
                    errors={[fakeError]}
                />,
            );
        });

        test("only renders the errors table", () => {
            expect(
                screen.getByTestId("RequestedChangesDisplay--Errors"),
            ).toBeVisible();
            expect(
                screen.queryByTestId("RequestedChangesDisplay--Warnings"),
            ).not.toBeInTheDocument();
        });
    });

    describe("when there are only warnings", () => {
        beforeEach(() => {
            renderApp(
                <FileHandlerErrorsWarningsStep
                    {...DEFAULT_PROPS}
                    warnings={[fakeWarning]}
                />,
            );
        });

        test("only renders the errors table", () => {
            expect(
                screen.queryByTestId("RequestedChangesDisplay--Errors"),
            ).not.toBeInTheDocument();
            expect(
                screen.getByTestId("RequestedChangesDisplay--Warnings"),
            ).toBeVisible();
        });
    });

    describe("when there are both errors and warnings", () => {
        beforeEach(() => {
            renderApp(
                <FileHandlerErrorsWarningsStep
                    {...DEFAULT_PROPS}
                    errors={[fakeError]}
                    warnings={[fakeWarning]}
                />,
            );
        });

        test("renders both the errors table and the warnings table", () => {
            expect(
                screen.getByTestId("RequestedChangesDisplay--Errors"),
            ).toBeVisible();
            expect(
                screen.getByTestId("RequestedChangesDisplay--Warnings"),
            ).toBeVisible();
        });
    });

    describe("when clicking on 'Test another file'", () => {
        const onTestAnotherFileClickSpy = jest.fn();

        beforeEach(async () => {
            renderApp(
                <FileHandlerErrorsWarningsStep
                    {...DEFAULT_PROPS}
                    errors={[fakeError]}
                    warnings={[fakeWarning]}
                    onTestAnotherFileClick={onTestAnotherFileClickSpy}
                />,
            );

            await userEvent.click(screen.getByText("Test another file"));
        });

        test("calls onTestAnotherFileClick", () => {
            expect(onTestAnotherFileClickSpy).toHaveBeenCalled();
        });
    });
});
