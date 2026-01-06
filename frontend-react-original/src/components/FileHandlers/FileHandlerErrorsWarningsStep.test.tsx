import { screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";

import FileHandlerErrorsWarningsStep, {
    FileHandlerErrorsWarningsStepProps,
} from "./FileHandlerErrorsWarningsStep";
import type { RequestLevel } from "./FileHandlerMessaging";
import { INITIAL_STATE } from "../../hooks/UseFileHandler/UseFileHandler";
import {
    fakeError,
    fakeWarning,
} from "../../hooks/UseFileHandler/UseFileHandler.fixtures";
import { renderApp } from "../../utils/CustomRenderUtils";
import { FileType } from "../../utils/TemporarySettingsAPITypes";

function mockRequestedChangesDisplay({ title }: { title: RequestLevel }) {
    return <div data-testid={`RequestedChangesDisplay--${title}`} />;
}

vi.mock("./FileHandlerMessaging", async (importActual) => ({
    ...(await importActual<typeof import("./FileHandlerMessaging")>()),
    RequestedChangesDisplay: mockRequestedChangesDisplay,
}));

describe("FileHandlerErrorsWarningsStep", () => {
    const DEFAULT_PROPS: FileHandlerErrorsWarningsStepProps = {
        ...INITIAL_STATE,
        onPrevStepClick: vi.fn(),
        onNextStepClick: vi.fn(),
        onTestAnotherFileClick: vi.fn(),
        selectedSchemaOption: {
            value: "CSV",
            format: FileType.CSV,
            title: "csv",
        },
    };

    describe("when there are only errors", () => {
        function setup() {
            renderApp(
                <FileHandlerErrorsWarningsStep
                    {...DEFAULT_PROPS}
                    errors={[fakeError]}
                />,
            );
        }

        test("only renders the errors table", () => {
            setup();
            expect(
                screen.getByTestId("RequestedChangesDisplay--Errors"),
            ).toBeVisible();
            expect(
                screen.queryByTestId("RequestedChangesDisplay--Warnings"),
            ).not.toBeInTheDocument();
        });
    });

    describe("when there are only warnings", () => {
        function setup() {
            renderApp(
                <FileHandlerErrorsWarningsStep
                    {...DEFAULT_PROPS}
                    warnings={[fakeWarning]}
                />,
            );
        }

        test("only renders the errors table", () => {
            setup();
            expect(
                screen.queryByTestId("RequestedChangesDisplay--Errors"),
            ).not.toBeInTheDocument();
            expect(
                screen.getByTestId("RequestedChangesDisplay--Warnings"),
            ).toBeVisible();
        });
    });

    describe("when there are both errors and warnings", () => {
        function setup() {
            renderApp(
                <FileHandlerErrorsWarningsStep
                    {...DEFAULT_PROPS}
                    errors={[fakeError]}
                    warnings={[fakeWarning]}
                />,
            );
        }

        test("renders both the errors table and the warnings table", () => {
            setup();
            expect(
                screen.getByTestId("RequestedChangesDisplay--Errors"),
            ).toBeVisible();
            expect(
                screen.getByTestId("RequestedChangesDisplay--Warnings"),
            ).toBeVisible();
        });
    });

    describe("when clicking on 'Test another file'", () => {
        const onTestAnotherFileClickSpy = vi.fn();

        async function setup() {
            renderApp(
                <FileHandlerErrorsWarningsStep
                    {...DEFAULT_PROPS}
                    errors={[fakeError]}
                    warnings={[fakeWarning]}
                    onTestAnotherFileClick={onTestAnotherFileClickSpy}
                />,
            );

            await userEvent.click(screen.getByText("Test another file"));
        }

        test("calls onTestAnotherFileClick", async () => {
            await setup();
            expect(onTestAnotherFileClickSpy).toHaveBeenCalled();
        });
    });
});
