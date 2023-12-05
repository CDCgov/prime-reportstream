import { RSReceiver } from "../../../../config/endpoints/settings";
import { renderApp, screen } from "../../../../utils/CustomRenderUtils";

import ReceiverFieldset from "./ReceiverFieldSet";

const fields = [
    "customerStatus",
    "timeZone",
    "dateTimeFormat",
    "description",
    "translation",
    "jurisdictionalFilter",
    "qualityFilter",
    "reverseTheQualityFilter",
    "routingFilter",
    "processingModeFilter",
    "deidentify",
    "timing",
    "transport",
    "externalName",
] satisfies (keyof RSReceiver)[];

describe("ReceiverFieldset", () => {
    describe("renders fields", () => {
        test.each(fields)("%s", (field) => {
            renderApp(<ReceiverFieldset />);
            expect(screen.getByTestId(field)).toBeInTheDocument();
        });
    });
});
