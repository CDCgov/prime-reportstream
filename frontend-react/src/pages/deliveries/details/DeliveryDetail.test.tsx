import { render, screen } from "../../../utils/Test/render";

import { DeliveryDetailBase } from "./DeliveryDetail";

vi.mock("./DeliveryFacilitiesTable");
vi.mock("./Summary");

describe("DeliveryDetails", () => {
    test("renders", () => {
        render(
            <DeliveryDetailBase
                report={{
                    batchReadyAt: "",
                    deliveryId: 0,
                    expires: "",
                    fileName: "",
                    fileType: "",
                    receiver: "",
                    reportId: "",
                    reportItemCount: 1,
                    topic: "",
                }}
            >
                Test
            </DeliveryDetailBase>,
        );
        expect(screen.getByText("Test")).toBeInTheDocument();
    });
});
