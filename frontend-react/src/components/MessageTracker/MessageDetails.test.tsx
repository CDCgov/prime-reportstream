import { render, screen } from "@testing-library/react";

import { MessageDetails } from "./MessageDetails";

const mockData = {
    id: 1,
    messageId: "12-234567",
    sender: "somebody 1",
    submittedDate: "2022-10-03T18:09:45.129997",
    reportId: "29038fca-e521-4af8-82ac-6b9fafd0fd58",
    fileName: "simple_report_example.csv",
    fileUrl: "https://someurl",
    warnings: [],
    errors: [],
    receiverData: [],
};

jest.mock("react-router-dom", () => ({
    useResource: () => {
        return mockData;
    },
    useNavigate: () => {
        return jest.fn();
    },
    useParams: () => {
        return {
            messageId: "12-234567",
        };
    },
}));

describe("MessageDetails component", () => {
    test("renders expected content", async () => {
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
