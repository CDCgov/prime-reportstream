import { render, screen } from "@testing-library/react";

import { mockUseMessageDetails } from "../../hooks/network/MessageTracker/__mocks__/MessageTrackerHooks";
import { RSMessageDetail } from "../../config/endpoints/messageTracker";

import { MessageDetails } from "./MessageDetails";

const TEST_ID = 1;
const MOCK_MESSAGE_WARNINGS = [
    {
        detail: {
            class: "gov.cdc.prime.router.InvalidCodeMessage",
            fieldMapping: "Specimen_type_code (specimen_type)",
            scope: "item",
            message:
                "Invalid code: '' is not a display value in altValues set for Specimen_type_code (specimen_type).",
        },
    },
    {
        detail: {
            class: "gov.cdc.prime.router.InvalidEquipmentMessage",
            fieldMapping: "Equipment_Model_ID (equipment_model_id)",
            scope: "item",
            message:
                "Invalid field Equipment_Model_ID (equipment_model_id); please refer to the Department of Health and Human Services' (HHS) LOINC Mapping spreadsheet for acceptable values.",
        },
    },
];
const MOCK_MESSAGE_ERRORS = [
    {
        detail: {
            class: "first field",
            fieldMapping: "Missing type",
            message: "Missing required HL7 message type",
            scope: "error",
        },
    },
    {
        detail: {
            class: "second field",
            fieldMapping: "Invalid content type",
            message: "Expecting content type of 'application/hl7 -v2",
            scope: "error",
        },
    },
];
const MOCK_RECEIVER_DATA = [
    {
        reportId: "578eae4e-b24d-45aa-bc5c-4d96a0bfef96",
        receivingOrg: "md-phd",
        receivingOrgSvc: "elr",
        transportResult: null,
        fileName:
            "fl-covid-19-45ee5710-08fd-4446-bb89-c106fb4c63ea-20221022025000.hl7",
        fileUrl: null,
        createdAt: "2022-09-28T19:55:12.46782",
        qualityFilters: [],
    },
    {
        reportId: "6d2d1ad0-1bdf-4d0b-804a-1e3494928f0f",
        receivingOrg: "ak-phd",
        receivingOrgSvc: "elr",
        transportResult: "FAILED Sftp Upload blah blah blah",
        fileName: null,
        fileUrl:
            "https://azurite:10000/devstoreaccount1/reports/batch%2Fak-phd.elr%2Fcovid-19-6d2d1ad0-1bdf-4d0b-804a-1e3494928f0f-20220928195512.internal.csv",
        createdAt: "2022-09-28T19:55:12.46782",
        qualityFilters: [
            {
                trackingId: "Alaska1",
                detail: {
                    class: "gov.cdc.prime.router.ReportStreamFilterResult",
                    receiverName: "ak-phd.elr",
                    originalCount: 5,
                    filterName: "hasValidDataFor",
                    filterArgs: ["patient_dob"],
                    filteredTrackingElement: "Alaska1",
                    filterType: "QUALITY_FILTER",
                    scope: "translation",
                    message:
                        "For ak-phd.elr, filter hasValidDataFor[patient_dob] filtered out item Alaska1",
                },
            },
            {
                trackingId: "Alaska1",
                detail: {
                    class: "gov.cdc.prime.router.ReportStreamFilterResult",
                    receiverName: "ak-phd.elr",
                    originalCount: 5,
                    filterName: "isValidCLIA",
                    filterArgs: ["testing_lab_clia", "reporting_facility_clia"],
                    filteredTrackingElement: "Alaska1",
                    filterType: "QUALITY_FILTER",
                    scope: "translation",
                    message:
                        "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska1",
                },
            },
            {
                trackingId: "Alaska2",
                detail: {
                    class: "gov.cdc.prime.router.ReportStreamFilterResult",
                    receiverName: "ak-phd.elr",
                    originalCount: 5,
                    filterName: "isValidCLIA",
                    filterArgs: ["testing_lab_clia", "reporting_facility_clia"],
                    filteredTrackingElement: "Alaska2",
                    filterType: "QUALITY_FILTER",
                    scope: "translation",
                    message:
                        "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska2",
                },
            },
            {
                trackingId: "Alaska4",
                detail: {
                    class: "gov.cdc.prime.router.ReportStreamFilterResult",
                    receiverName: "ak-phd.elr",
                    originalCount: 5,
                    filterName: "isValidCLIA",
                    filterArgs: ["testing_lab_clia", "reporting_facility_clia"],
                    filteredTrackingElement: "Alaska4",
                    filterType: "QUALITY_FILTER",
                    scope: "translation",
                    message:
                        "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska4",
                },
            },
        ],
    },
];
const MOCK_EMPTY_MESSAGE_DETAIL = {
    id: 0,
    messageId: "",
    sender: "",
    submittedDate: "",
    reportId: "",
    fileName: "",
    fileUrl: "",
    warnings: [],
    errors: [],
    receiverData: [],
};
const MOCK_MESSAGE_DETAIL = {
    id: TEST_ID,
    messageId: "12-234567",
    sender: "somebody 1",
    submittedDate: "09/28/2022",
    reportId: "29038fca-e521-4af8-82ac-6b9fafd0fd58",
    fileName: "simple_report_example.csv",
    fileUrl: "https://someurl",
    warnings: MOCK_MESSAGE_WARNINGS,
    errors: MOCK_MESSAGE_ERRORS,
    receiverData: MOCK_RECEIVER_DATA,
};

jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"), // use actual for all non-hook parts
    useNavigate: () => {
        return jest.fn();
    },
    useParams: () => ({
        id: TEST_ID,
    }),
}));

describe("RSMessageDetail component", () => {
    test("url param (messageId) feeds into network hook", () => {
        mockUseMessageDetails.mockReturnValueOnce({
            messageDetails: MOCK_EMPTY_MESSAGE_DETAIL as RSMessageDetail,
        });
        render(<MessageDetails />);
        expect(mockUseMessageDetails).toHaveBeenCalledWith(TEST_ID);
    });

    test("renders expected content", async () => {
        mockUseMessageDetails.mockReturnValueOnce({
            messageDetails: MOCK_MESSAGE_DETAIL as RSMessageDetail,
        });
        render(<MessageDetails />);

        expect(screen.getByText(/Message ID/)).toBeInTheDocument();
        expect(screen.getByText(/12-234567/)).toBeInTheDocument();
        expect(screen.getByText(/somebody 1/)).toBeInTheDocument();
        expect(
            screen.getByText(/29038fca-e521-4af8-82ac-6b9fafd0fd58/)
        ).toBeInTheDocument();
        expect(
            screen.getByText("simple_report_example.csv")
        ).toBeInTheDocument();
        expect(screen.getByText("https://someurl")).toBeInTheDocument();
    });
});
