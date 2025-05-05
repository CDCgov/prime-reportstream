import { fireEvent, screen } from "@testing-library/react";
import { useState } from "react";

import { DropdownComponent, DropdownProps } from "./AdminFormEdit";
import { renderApp } from "../../utils/CustomRenderUtils";
import { ProcessingType } from "../../utils/TemporarySettingsAPITypes";

const DropdownComponentHelper = () => {
    const [state, setState] = useState("");

    const dropdownObj: DropdownProps = {
        defaultvalue: ProcessingType.SYNC,
        fieldname: "processingType",
        label: "Processing Type",
        savefunc: setState,
        toolTip: <p>tooltip</p>,
        valuesFrom: "processingType",
    };

    return (
        <>
            <DropdownComponent {...dropdownObj} />
            <span>{`${state} value saved`}</span>
        </>
    );
};

describe("Render DropdownComponent", () => {
    function setup() {
        renderApp(<DropdownComponentHelper />);
    }

    test("Check data as object rendered", () => {
        setup();
        expect(screen.getByLabelText(/Processing Type/)).toBeInTheDocument();
        expect(screen.getByText(ProcessingType.SYNC)).toBeInTheDocument();
        expect(screen.getByText(ProcessingType.ASYNC)).toBeInTheDocument();
        expect(screen.getAllByRole("option")).toHaveLength(3);
        expect(screen.getByText("tooltip")).toBeInTheDocument();
    });

    test("savefunc", () => {
        setup();
        fireEvent.change(screen.getByRole("combobox"), {
            target: { value: ProcessingType.SYNC },
        });
        expect(screen.getByText(`${ProcessingType.SYNC} value saved`)).toBeInTheDocument();
    });
});
