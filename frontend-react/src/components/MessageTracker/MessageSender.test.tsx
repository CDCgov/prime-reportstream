import { render, screen } from "@testing-library/react";

import { MessageSender } from "./MessageSender";

describe("MessageSender component", () => {
    test("renders expected content", async () => {
        const senderDetails = {
            id: 1,
            messageId: "12-234567",
            sender: "somebody 1",
            submittedDate: "09/28/2022",
            reportId: "29038fca-e521-4af8-82ac-6b9fafd0fd58",
            fileUrl: "https://someurl",
        };
        render(<MessageSender senderDetails={senderDetails} />);

        expect(screen.getByText(/12-234567/)).toBeInTheDocument();
        expect(screen.getByText(/somebody 1/)).toBeInTheDocument();
        expect(
            screen.getByText(/29038fca-e521-4af8-82ac-6b9fafd0fd58/)
        ).toBeInTheDocument();
        expect(screen.getByText("https://someurl")).toBeInTheDocument();
    });
});
