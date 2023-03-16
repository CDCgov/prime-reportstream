import { screen } from "@testing-library/react";

import {
    ManagePublicKeyState,
    INITIAL_STATE,
} from "../../hooks/network/ManagePublicKey/ManagePublicKeyHooks";
import { UseOrganizationSendersResult } from "../../hooks/UseOrganizationSenders";
import { RSSender } from "../../config/endpoints/settings";
import * as useManagePublicKeyExports from "../../hooks/network/ManagePublicKey/ManagePublicKeyHooks";
import * as useOrganizationSendersExports from "../../hooks/UseOrganizationSenders";
import { renderApp } from "../../utils/CustomRenderUtils";

import { ManagePublicKey } from "./ManagePublicKey";

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
const contentString = "foo,bar\r\nbar,foo";

const fakeFile = new File([new Blob([contentString])], "file.pem", {
    type: "application/x-x509-ca-cert",
});
fakeFile.text = () => Promise.resolve(contentString);

describe("ManagePublicKey", () => {
    afterEach(() => {
        jest.restoreAllMocks();
    });

    function mockUseFileHandler(
        managePublicKeyState: Partial<ManagePublicKeyState> = {}
    ) {
        jest.spyOn(useManagePublicKeyExports, "default").mockReturnValue({
            state: {
                ...managePublicKeyState,
            } as ManagePublicKeyState,
            dispatch: () => {},
        });
    }

    function mockUseOrganizationSenders(
        result: Partial<UseOrganizationSendersResult> = {}
    ) {
        jest.spyOn(
            useOrganizationSendersExports,
            "UseOrganizationSenders"
        ).mockReturnValue({
            isLoading: false,
            senders: DEFAULT_SENDERS,
            ...result,
        });
    }

    describe("when the sender options are loading", () => {
        beforeEach(() => {
            mockUseFileHandler(INITIAL_STATE);
            mockUseOrganizationSenders({ isLoading: true });

            renderApp(<ManagePublicKey />);
        });

        test("renders a spinner", () => {
            expect(screen.getByLabelText("loading-indicator")).toBeVisible();
        });
    });

    describe("when the sender options have been loaded", () => {
        describe("when in the prompt state", () => {
            beforeEach(() => {
                mockUseFileHandler(INITIAL_STATE);
                mockUseOrganizationSenders({
                    isLoading: false,
                    senders: DEFAULT_SENDERS,
                });

                renderApp(<ManagePublicKey />);
            });

            test("renders as expected", () => {
                expect(
                    screen.getByText(/Manage Public Key/)
                ).toBeInTheDocument();
            });
        });
    });
});
