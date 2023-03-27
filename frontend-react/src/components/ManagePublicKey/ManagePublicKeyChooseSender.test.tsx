import { screen } from "@testing-library/react";

import { UseOrganizationSendersResult } from "../../hooks/UseOrganizationSenders";
import { RSSender } from "../../config/endpoints/settings";
import { renderApp } from "../../utils/CustomRenderUtils";
import * as useOrganizationSendersExports from "../../hooks/UseOrganizationSenders";

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
        onSenderSelect: () => {},
    };

    afterEach(() => {
        jest.restoreAllMocks();
    });

    function mockUseOrganizationSenders(
        result: Partial<UseOrganizationSendersResult> = {}
    ) {
        jest.spyOn(
            useOrganizationSendersExports,
            "useOrganizationSenders"
        ).mockReturnValue({
            isLoading: false,
            senders: DEFAULT_SENDERS,
            ...result,
        });
    }

    describe("when the sender options are loading", () => {
        beforeEach(() => {
            mockUseOrganizationSenders({ isLoading: true });

            renderApp(<ManagePublicKeyChooseSender {...DEFAULT_PROPS} />);
        });

        test("renders a spinner", () => {
            expect(screen.getByLabelText("loading-indicator")).toBeVisible();
        });
    });

    describe("when the sender options have been loaded", () => {
        describe("and Organizations have more than one sender", () => {
            beforeEach(() => {
                mockUseOrganizationSenders({
                    isLoading: false,
                    senders: DEFAULT_SENDERS,
                });

                renderApp(<ManagePublicKeyChooseSender {...DEFAULT_PROPS} />);
            });

            test("renders the sender options", () => {
                expect(
                    screen.getByRole("option", { name: "default" })
                ).toBeVisible();
                expect(
                    screen.getByRole("option", { name: "ignore-full-elr" })
                ).toBeVisible();
            });

            test("renders the submit button", () => {
                expect(screen.getByText("Submit")).toBeVisible();
            });
        });
    });
});
