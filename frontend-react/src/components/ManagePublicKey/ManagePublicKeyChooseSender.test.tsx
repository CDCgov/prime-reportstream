import { screen } from "@testing-library/react";

import { RSSender } from "../../config/endpoints/settings";
import { render } from "../../utils/Test/render";

import ManagePublicKeyChooseSender, {
    ManagePublicKeyChooseSenderProps,
} from "./ManagePublicKeyChooseSender";

const DEFAULT_SENDERS: RSSender[] = [
    {
        name: "default",
        organizationName: "ignore",
        format: "CSV",
        customerStatus: "inactive",
        schemaName: "primedatainput/pdi-covid-19",
        processingType: "sync",
        allowDuplicates: true,
        topic: "covid-19",
    },
    {
        name: "ignore-full-elr",
        organizationName: "ignore",
        format: "HL7",
        customerStatus: "active",
        schemaName: "strac/strac-covid-19",
        processingType: "sync",
        allowDuplicates: true,
        topic: "full-elr",
    },
];

describe("ManagePublicKeyChooseSender", () => {
    const DEFAULT_PROPS: ManagePublicKeyChooseSenderProps = {
        senders: DEFAULT_SENDERS,
        onSenderSelect: () => {},
    };

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe("when the sender options have been loaded", () => {
        describe("and Organizations have more than one sender", () => {
            function setup() {
                render(<ManagePublicKeyChooseSender {...DEFAULT_PROPS} />);
            }

            test("renders the sender options", () => {
                setup();
                expect(
                    screen.getByRole("option", { name: "default" }),
                ).toBeVisible();
                expect(
                    screen.getByRole("option", { name: "ignore-full-elr" }),
                ).toBeVisible();
            });

            test("renders the submit button", () => {
                setup();
                expect(screen.getByText("Submit")).toBeVisible();
            });
        });
    });
});
