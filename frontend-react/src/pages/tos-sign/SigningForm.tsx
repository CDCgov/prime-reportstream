import {
    Button,
    Checkbox,
    Dropdown,
    Form,
    Label,
    TextInput,
} from "@trussworks/react-uswds";
import React from "react";
import { Link } from "react-router-dom";

import Title from "../../components/Title";

const Required = () => {
    return <span style={{ color: "red" }}>*</span>;
};

const AgreementLabel = () => {
    return (
        <span className="maxw-2">
            By submitting your information, you're agreeing to the ReportSteam{" "}
            <Link to="/terms-of-service">terms of service</Link>. <Required />
        </span>
    );
};

function SigningForm() {
    return (
        <div className="width-tablet margin-x-auto">
            <Title
                title="Register your organization with ReportStream"
                preTitle="Account registration"
            />
            <p className="usa-prose padding-bottom-9 border-bottom-05 border-base-lighter">
                Required fields are marked with an asterisk (
                <span style={{ color: "red" }}>*</span>).
            </p>

            <Form
                id="tos-agreement"
                onSubmit={() => {
                    console.log("Testing");
                }}
            >
                <h3 className="padding-top-7 text-normal">
                    Name and contact information
                </h3>
                <Label htmlFor="title">Title</Label>
                <TextInput
                    id="title"
                    name="title"
                    type="text"
                    inputSize="small"
                />
                <Label htmlFor="first-name">
                    First name <Required />
                </Label>
                <TextInput id="first-name" name="first-name" type="text" />
                <Label htmlFor="last-name">
                    Last name <Required />
                </Label>
                <TextInput id="last-name" name="last-name" type="text" />
                <Label htmlFor="email">
                    Email <Required />
                </Label>
                <TextInput id="email" name="email" type="email" />

                <h3 className="padding-top-5 text-normal">
                    About your organization
                </h3>
                <Label htmlFor="states-dropdown">
                    [HQ] State or territory <Required />
                </Label>
                <Dropdown id="input-dropdown" name="states-dropdown">
                    <option>- Select - </option>
                    <option value="value1">Option A</option>
                    <option value="value2">Option B</option>
                    <option value="value3">Option C</option>
                </Dropdown>
                <Label htmlFor="organization-name">
                    Organization name <Required />
                </Label>
                <TextInput
                    id="organization-name"
                    name="organization-name"
                    type="text"
                />
                <Checkbox
                    className="padding-top-3"
                    id="multi-state"
                    name="multi-state"
                    label="[My org reports to multiple states]"
                />
            </Form>

            <section className="usa-section usa-prose font-sans-2xs text-base-darker border-top-05 border-base-lighter margin-top-9">
                <p>
                    ReportStream will use the information you’ve provided to
                    communicate with you for the purpose of setting up a
                    connection to the ReportStream platform, and to provide
                    support.
                </p>
                <Checkbox
                    className="padding-top-3"
                    id="multi-state"
                    name="multi-state"
                    label={<AgreementLabel />}
                />
            </section>

            <Button
                form="tos-agreement"
                className="margin-bottom-10"
                type="button"
            >
                Submit registration
            </Button>
        </div>
    );
}

export default SigningForm;
