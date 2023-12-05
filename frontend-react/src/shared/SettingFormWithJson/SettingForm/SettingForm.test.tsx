import { renderApp, screen } from "../../../utils/CustomRenderUtils";

import { SettingFormBase, SettingFormBaseProps } from "./SettingForm";

describe("SettingForm", () => {
    const mockOnCancel = jest.fn();
    const mockOnDelete = jest.fn();
    const mockOnChangeView = jest.fn();
    const mockOnSave = jest.fn();
    function renderSettingForm(props?: Partial<SettingFormBaseProps>) {
        renderApp(
            <SettingFormBase
                ctx={{} as any}
                formView={"form"}
                mode={"edit"}
                onChangeView={mockOnChangeView}
                onDelete={mockOnDelete}
                onCancel={mockOnCancel}
                onSave={mockOnSave}
                isInvalid={false}
                isSave={true}
                {...props}
            >
                <div>Test</div>
            </SettingFormBase>,
        );
    }

    describe("renders", () => {
        test("children", () => {
            renderSettingForm();
            expect(screen.getByText("Test")).toBeInTheDocument();
        });
    });
});
