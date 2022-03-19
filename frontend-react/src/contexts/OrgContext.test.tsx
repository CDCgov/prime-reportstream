import { screen } from "@testing-library/react";
import { useContext } from "react";

import { renderWithOrgContext } from "../utils/CustomRenderUtils";

import { IOrgContext, OrgContext } from "./OrgContext";

export const dummyPayload: IOrgContext = {
    values: {
        oktaGroup: "ignore",
    },
    controller: {
        updateOktaOrg: (val: string) => {
            console.log(val);
        },
    },
};

const OrgConsumer = () => {
    const { values } = useContext(OrgContext);

    return (
        <>
            w<span>{values.oktaGroup || "failed"}</span>
        </>
    );
};

describe("OrgContext", () => {
    beforeEach(() => {
        renderWithOrgContext(<OrgConsumer />);
    });

    test("Values are provided", async () => {
        const name = await screen.findByText("ignore");
        expect(name).toBeInTheDocument();
    });
});
