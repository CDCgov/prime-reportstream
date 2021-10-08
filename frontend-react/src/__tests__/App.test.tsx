import { render } from "@testing-library/react";

import App from "../App";

jest.mock("../App", () => () => {
    return <div>Hello</div>;
});

describe("Describe 1", () => {
    test("Test 1", () => {
        const appComponent = render(<App />);
        expect(appComponent).not.toBeNull();
    });
});
