import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { ReceiverData } from "../../config/endpoints/messageTracker";
import { renderApp } from "../../utils/CustomRenderUtils";

import {
    FilterOption,
    formatMessages,
    MessageReceivers,
    NormalizedReceiverData,
    NormalizedReceiverKey,
    sortMessages,
} from "./MessageReceivers";

const MOCK_RECEIVER_DATA: ReceiverData[] = [
    {
        reportId: "578eae4e-b24d-45aa-bc5c-4d96a0bfef96",
        receivingOrg: "md-phd",
        receivingOrgSvc: "elr",
        transportResult: "Transport result",
        fileName: "fl-covid-19.hl7",
        fileUrl:
            "https://azurite:10000/devstoreaccount1/reports/batch%2Fignore.HL7_NULL%2Ftx-covid-19-4b3c73df-83b1-48f9-a5a2-ce0c38662f7c-20230203182255.internal.csv",
        createdAt: "2022-09-28T19:55:12.46782",
        qualityFilters: [],
    },
    {
        reportId: "400eae4e-b24d-45aa-bc5c-4d96a0bfef96",
        receivingOrg: "ak-phd",
        receivingOrgSvc: "pdf",
        transportResult: null,
        fileName: "fl-covid-19.pdf",
        fileUrl:
            "https://azurite:10000/devstoreaccount1/reports/batch%2Fignore.HL8_NULL%2Ftx-covid-19-4b3c73df-83b1-48f9-a5a2-ce0c38662f7c-400.internal.csv",
        createdAt: "2022-01-28T19:55:12.46782",
        qualityFilters: [],
    },
];

const formattedMockData: NormalizedReceiverData[] = [
    {
        reportId: "578eae4e-b24d-45aa-bc5c-4d96a0bfef96",
        name: "md-phd",
        service: "elr",
        transportResults: "Transport result",
        date: "09/28/2022, 07:55:12 PM",
        fileName:
            "tx-covid-19-4b3c73df-83b1-48f9-a5a2-ce0c38662f7c-20230203182255.internal.csv",
        main: "batch",
        sub: "ignore.HL7_NULL",
    },
    {
        reportId: "400eae4e-b24d-45aa-bc5c-4d96a0bfef96",
        name: "ak-phd",
        service: "pdf",
        transportResults: "N/A",
        date: "01/28/2022, 07:55:12 PM",
        fileName:
            "tx-covid-19-4b3c73df-83b1-48f9-a5a2-ce0c38662f7c-400.internal.csv",
        main: "batch",
        sub: "ignore.HL8_NULL",
    },
];

const sortScenarios: {
    sortOrder: FilterOption;
    column: NormalizedReceiverKey;
    data: NormalizedReceiverData[];
    sortedData: NormalizedReceiverData[];
}[] = [
    // date sorts
    {
        sortOrder: "asc",
        column: "date",
        data: formattedMockData,
        sortedData: [formattedMockData[1], formattedMockData[0]],
    },
    {
        sortOrder: "desc",
        column: "date",
        data: formattedMockData,
        sortedData: formattedMockData,
    },
    // non-date sorts
    {
        sortOrder: "asc",
        column: "reportId",
        data: formattedMockData,
        sortedData: [formattedMockData[1], formattedMockData[0]],
    },
    {
        sortOrder: "asc",
        column: "reportId",
        data: formattedMockData,
        sortedData: formattedMockData,
    },
];

describe("formatMessages function", () => {
    test("formats data properly", () => {
        expect(formatMessages(MOCK_RECEIVER_DATA)).toEqual(formattedMockData);
    });
});

describe("sortMessages function", () => {
    test.each(sortScenarios)(
        "sorts $column $sortOrder properly",
        ({ data, sortedData, column, sortOrder }) => {
            expect(sortMessages(data, column, sortOrder)).toEqual(sortedData);
        }
    );
});

describe("MessageReceivers component", () => {
    test("renders expected content", async () => {
        renderApp(<MessageReceivers receiverDetails={MOCK_RECEIVER_DATA} />);

        expect(screen.getByText(/Receivers/)).toBeInTheDocument();
        expect(screen.getAllByText(/Name/)[0]).toBeInTheDocument();
        expect(screen.getByText(/md-phd/)).toBeInTheDocument();
        expect(screen.getByText(/Service/)).toBeInTheDocument();
        expect(screen.getByText("elr")).toBeInTheDocument();
        expect(screen.getByText("Date")).toBeInTheDocument();
        expect(
            screen.getByText(/09\/28\/2022, 07:55:12 PM/)
        ).toBeInTheDocument();
        expect(screen.getByText(/Report Id/)).toBeInTheDocument();
        expect(
            screen.getByText(/578eae4e-b24d-45aa-bc5c-4d96a0bfef96/)
        ).toBeInTheDocument();
        expect(screen.getByText(/Main/)).toBeInTheDocument();
        expect(screen.getAllByText(/BATCH/)[0]).toBeInTheDocument();
        expect(screen.getByText(/Sub/)).toBeInTheDocument();
        expect(screen.getByText(/ignore.HL7_NULL/)).toBeInTheDocument();
        expect(screen.getByText(/File Name/)).toBeInTheDocument();
        expect(
            screen.getByText(
                /tx-covid-19-4b3c73df-83b1-48f9-a5a2-ce0c38662f7c-20230203182255.internal.csv/
            )
        ).toBeInTheDocument();

        expect(screen.getByText("Transport Results")).toBeInTheDocument();
        expect(screen.getByText(/Transport result/)).toBeInTheDocument();

        expect(
            screen
                .getByRole("table")
                .getElementsByClassName("message-receiver-break-word")[0]
        ).toHaveTextContent("578eae4e-b24d-45aa-bc5c-4d96a0bfef96");

        await userEvent.click(screen.getByText(/^Report Id$/));
        expect(
            screen
                .getByRole("table")
                .getElementsByClassName("message-receiver-break-word")[0]
        ).toHaveTextContent("400eae4e-b24d-45aa-bc5c-4d96a0bfef96");
    });
});
