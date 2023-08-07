import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { ReceiverData } from "../../config/endpoints/messageTracker";
import { renderApp } from "../../utils/CustomRenderUtils";

import { MessageReceivers } from "./MessageReceivers";

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
            screen.getByText(/09\/28\/2022, 07:55:12 PM/),
        ).toBeInTheDocument();
        expect(screen.getByText(/Report Id/)).toBeInTheDocument();
        expect(
            screen.getByText(/578eae4e-b24d-45aa-bc5c-4d96a0bfef96/),
        ).toBeInTheDocument();
        expect(screen.getByText(/Main/)).toBeInTheDocument();
        expect(screen.getAllByText(/BATCH/)[0]).toBeInTheDocument();
        expect(screen.getByText(/Sub/)).toBeInTheDocument();
        expect(screen.getByText(/ignore.HL7_NULL/)).toBeInTheDocument();
        expect(screen.getByText(/File Name/)).toBeInTheDocument();
        expect(
            screen.getByText(
                /tx-covid-19-4b3c73df-83b1-48f9-a5a2-ce0c38662f7c-20230203182255.internal.csv/,
            ),
        ).toBeInTheDocument();

        expect(screen.getByText("Transport Results")).toBeInTheDocument();
        expect(screen.getByText(/Transport result/)).toBeInTheDocument();
        expect(
            screen
                .getAllByRole("row")[1]
                .getElementsByClassName("column-data")[0],
        ).toHaveTextContent("md-phd");

        await userEvent.click(screen.getByText(/^Report Id$/));
        expect(
            screen
                .getAllByRole("row")[1]
                .getElementsByClassName("column-data")[0],
        ).toHaveTextContent("ak-phd");
    });
});
