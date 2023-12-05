import { RSSender } from "../../../../config/endpoints/settings";
import { renderApp, screen } from "../../../../utils/CustomRenderUtils";

import SenderFieldset from "./SenderFieldSet";

const fields = [
    "format",
    "customerStatus",
    "schemaName",
    "keys",
    "processingType",
    "allowDuplicates",
] satisfies (keyof RSSender)[];

describe("SenderFieldset", () => {
    describe("renders fields", () => {
        test.each(fields)("%s", (field) => {
            renderApp(<SenderFieldset />);
            expect(screen.getByTestId(field)).toBeInTheDocument();
        });
    });
});
