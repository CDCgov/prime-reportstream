import { RSOrganization } from "../../../../config/endpoints/settings";
import { renderApp, screen } from "../../../../utils/CustomRenderUtils";

import OrganizationFieldset from "./OrganizationFieldSet";

const fields = [
    "jurisdiction",
    "countyName",
    "stateCode",
    "filters",
] satisfies (keyof RSOrganization)[];

describe("OrganizationFieldset", () => {
    describe("renders fields", () => {
        test.each(fields)("%s", (field) => {
            renderApp(<OrganizationFieldset />);
            expect(screen.getByTestId(field)).toBeInTheDocument();
        });
    });
});
