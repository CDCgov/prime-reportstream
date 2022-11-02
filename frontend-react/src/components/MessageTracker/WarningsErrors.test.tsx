import { render, screen, within } from "@testing-library/react";

import { WarningsErrors } from "./WarningsErrors";

describe("WarningsErrors component", () => {
    test("renders expected content", async () => {
        const warnings = [
            {
                class: "gov.cdc.prime.router.InvalidCodeMessage",
                fieldMapping: "Specimen_type_code (specimen_type)",
                scope: "item",
                message:
                    "Invalid code: is not a display value in altValues set for Specimen_type_code (specimen_type).",
            },
            {
                class: "gov.cdc.prime.router.InvalidEquipmentMessage",
                fieldMapping: "Equipment_Model_ID (equipment_model_id)",
                scope: "item",
                message:
                    "Invalid field Equipment_Model_ID (equipment_model_id); please refer to the Department of Health and Human Services' (HHS) LOINC Mapping spreadsheet for acceptable values.",
            },
        ];
        render(<WarningsErrors title={"THE HEADING"} data={warnings} />);

        expect(screen.getByText(/THE HEADING/)).toBeInTheDocument();

        const table = screen.queryByRole("table");
        expect(table).toBeInTheDocument();

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(3); // 2 warnings + header

        const firstCells = await within(rows[1]).findAllByRole("cell");
        expect(firstCells).toHaveLength(3);
        expect(firstCells[0]).toHaveTextContent(
            "Specimen_type_code (specimen_type)"
        );
        expect(firstCells[1]).toHaveTextContent(
            "Invalid code: is not a display value in altValues set for Specimen_type_code (specimen_type)."
        );
        expect(firstCells[2]).toHaveTextContent(
            "gov.cdc.prime.router.InvalidCodeMessage"
        );
    });
});
