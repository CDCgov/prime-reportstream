import { screen } from "@testing-library/react";

import { CustomerStatus, RsSender } from "../../config/endpoints/settings";
import { renderApp } from "../../utils/CustomRenderUtils";

import ManagePublicKeyChooseSender, {
    ManagePublicKeyChooseSenderProps,
} from "./ManagePublicKeyChooseSender";

const DEFAULT_SENDERS: RsSender[] = [
    {
        name: "default",
        organizationName: "ignore",
        format: "CSV",
        customerStatus: CustomerStatus.INACTIVE,
        schemaName: "primedatainput/pdi-covid-19",
        processingType: "sync",
        allowDuplicates: true,
        topic: "covid-19",
        version: 0,
        createdAt: "",
        createdBy: "",
    },
    {
        name: "ignore-full-elr",
        organizationName: "ignore",
        format: "HL7",
        customerStatus: CustomerStatus.ACTIVE,
        schemaName: "strac/strac-covid-19",
        processingType: "sync",
        allowDuplicates: true,
        topic: "full-elr",
        version: 0,
        createdAt: "",
        createdBy: "",
    },
];

describe("ManagePublicKeyChooseSender", () => {
    const DEFAULT_PROPS: ManagePublicKeyChooseSenderProps = {
        senders: DEFAULT_SENDERS,
        onSenderSelect: () => {},
    };

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe("when the sender options have been loaded", () => {
        describe("and Organizations have more than one sender", () => {
            function setup() {
                renderApp(<ManagePublicKeyChooseSender {...DEFAULT_PROPS} />);
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
