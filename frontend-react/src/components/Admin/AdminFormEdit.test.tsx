import { fireEvent, screen } from "@testing-library/react";
import React, { useState } from "react";

import { renderApp } from "../../utils/CustomRenderUtils";
import { ProcessingType } from "../../utils/TemporarySettingsAPITypes";

import { DropdownComponent, DropdownProps } from "./AdminFormEdit";

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
    beforeEach(() => {
        renderApp(<DropdownComponentHelper />);
    });

    test("Check data as object rendered", () => {
        expect(screen.getByLabelText(/Processing Type/)).toBeInTheDocument();
        expect(screen.getByText(ProcessingType.SYNC)).toBeInTheDocument();
        expect(screen.getByText(ProcessingType.ASYNC)).toBeInTheDocument();
        expect(screen.getAllByRole("option")).toHaveLength(3);
        expect(screen.getByText("tooltip")).toBeInTheDocument();
    });

    test("test savefunc", () => {
        fireEvent.change(screen.getByRole("combobox"), {
            target: { value: ProcessingType.SYNC },
        });
        expect(screen.getByText(`${ProcessingType.SYNC} value saved`));
    });
});
