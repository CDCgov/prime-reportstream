import { screen } from "@testing-library/react";
import { SetStateAction } from "react";

import { render } from "../../utils/Test/render";
import { RSReceiver } from "../../config/endpoints/settings";

import { FacilitiesProvidersBase } from "./FacilitiesProviders";

vi.mock("./FacilitiesProvidersTable");

describe("FacilitiesProviders", () => {
    test("Breadcrumb displays with link", async () => {
        render(
            <FacilitiesProvidersBase
                services={[]}
                activeService={{} as any}
                submitters={[]}
                meta={{} as any}
                filterManager={{ pageSettings: {} } as any}
                onFilterClick={function (from: string, to: string): void {
                    throw new Error("Function not implemented.");
                }}
                setActiveService={function (
                    value: SetStateAction<RSReceiver | undefined>,
                ): void {
                    throw new Error("Function not implemented.");
                }}
            />,
        );

        expect(
            screen.getByRole("link", {
                name: "Data Dashboard",
            }),
        ).toBeInTheDocument();
    });

    test("if no active service display NoServicesBanner", async () => {
        render(
            <FacilitiesProvidersBase
                services={[]}
                submitters={[]}
                meta={{} as any}
                filterManager={{ pageSettings: {} } as any}
                onFilterClick={function (from: string, to: string): void {
                    throw new Error("Function not implemented.");
                }}
                setActiveService={function (
                    value: SetStateAction<RSReceiver | undefined>,
                ): void {
                    throw new Error("Function not implemented.");
                }}
            />,
        );
        const heading = await screen.findByText(/No available data/i);
        expect(heading).toBeInTheDocument();
    });
});
