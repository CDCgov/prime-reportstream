import { render, screen, within } from "@testing-library/react";

import { WarningsErrors } from "./WarningsErrors";

describe("WarningsErrors component", () => {
    test("renders expected content", async () => {
        const warnings = [
            {
                field: "first field",
                description:
                    "Invalid code: is not a display value in altValues set for Specimen_type_code (specimen_type).",
                type: "Invalid code",
                trackingIds: ["first_id1", "first_id3", "first_id4"],
            },
            {
                field: "second field",
                description:
                    "Missing Ordering_facility_email (ordering_facility_email) header",
                type: "Missing email",
                trackingIds: ["second_id"],
            },
        ];
        render(<WarningsErrors title={"THE HEADING"} data={warnings} />);

        expect(screen.getByText(/THE HEADING/)).toBeInTheDocument();

        const table = screen.queryByRole("table");
        expect(table).toBeInTheDocument();

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(3); // 2 warnings + header

        const firstCells = await within(rows[1]).findAllByRole("cell");
        expect(firstCells).toHaveLength(4);
        expect(firstCells[0]).toHaveTextContent("first field");
        expect(firstCells[1]).toHaveTextContent(
            "Invalid code: is not a display value in altValues set for Specimen_type_code (specimen_type)."
        );
        expect(firstCells[2]).toHaveTextContent("Invalid code");
        expect(firstCells[3]).toHaveTextContent(
            "first_id1, first_id3, first_id4"
        );
    });
});
