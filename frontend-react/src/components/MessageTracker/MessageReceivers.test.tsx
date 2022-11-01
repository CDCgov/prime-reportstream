import { render, screen } from "@testing-library/react";

import { MessageReceivers } from "./MessageReceivers";

describe("MessageReceivers component", () => {
    test("renders expected content", async () => {
        const MOCK_RECEIVER_DATA = [
            {
                reportId: "578eae4e-b24d-45aa-bc5c-4d96a0bfef96",
                receivingOrg: "md-phd",
                receivingOrgSvc: "elr",
                transportResult: "Transport result",
                fileName: "fl-covid-19.hl7",
                fileUrl: "https://azurite:10000/devstoreaccount1.csv",
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
                            filterArgs: [
                                "testing_lab_clia",
                                "reporting_facility_clia",
                            ],
                            filteredTrackingElement: "Alaska1",
                            filterType: "QUALITY_FILTER",
                            scope: "translation",
                            message:
                                "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska1",
                        },
                    },
                ],
            },
        ];
        render(<MessageReceivers receiverDetails={MOCK_RECEIVER_DATA} />);

        expect(screen.getByText(/Receivers/)).toBeInTheDocument();
        expect(screen.getByText(/Receiver Name/)).toBeInTheDocument();
        expect(screen.getByText(/md-phd/)).toBeInTheDocument();
        expect(screen.getByText(/Receiver Service/)).toBeInTheDocument();
        expect(screen.getByText("elr")).toBeInTheDocument();
        expect(screen.getByText(/Incoming Report Id/)).toBeInTheDocument();
        expect(
            screen.getByText(/578eae4e-b24d-45aa-bc5c-4d96a0bfef96/)
        ).toBeInTheDocument();
        expect(screen.getByText(/Incoming File Name/)).toBeInTheDocument();
        expect(screen.getByText(/fl-covid-19.hl7/)).toBeInTheDocument();
        expect(screen.getByText(/Incoming File URL/)).toBeInTheDocument();
        expect(
            screen.getByText("https://azurite:10000/devstoreaccount1.csv")
        ).toBeInTheDocument();
        expect(screen.getByText("Date/Time Submitted")).toBeInTheDocument();
        expect(screen.getByText(/September 28 2022/)).toBeInTheDocument();
        expect(screen.getByText("Transport Results")).toBeInTheDocument();
        expect(screen.getByText(/Transport result/)).toBeInTheDocument();

        expect(screen.getByText(/Quality Filters/)).toBeInTheDocument();
        expect(
            screen.getByText(
                "For ak-phd.elr, filter hasValidDataFor[patient_dob] filtered out item Alaska1"
            )
        ).toBeInTheDocument();
        expect(
            screen.getByText(
                "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska1"
            )
        ).toBeInTheDocument();
    });
});
