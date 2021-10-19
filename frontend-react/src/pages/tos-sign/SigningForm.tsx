import {
    Button,
    Checkbox,
    Dropdown,
    ErrorMessage,
    Form,
    FormGroup,
    Label,
    TextInput,
} from "@trussworks/react-uswds";
import React, { useState } from "react";
import { Link } from "react-router-dom";

import Title from "../../components/Title";
import AuthResource from "../../resources/AuthResource";
import { STATES } from "../../utils/OrganizationUtils";

interface AgreementBody {
    title: string;
    firstName: string;
    lastName: string;
    email: string;
    territory: string;
    organizationName: string;
    operatesInMultipleStates: boolean;
    agreedToTermsOfService: boolean;
}

function SigningForm({ signedCallback }: { signedCallback: () => void }) {
    /* Form field values are stored here */
    const [title, setTitle] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [email, setEmail] = useState("");
    const [territory, setTerritory] = useState(STATES[0].toLowerCase());
    const [organizationName, setOrganizationName] = useState("");
    const [multipleStates, setMultipleStates] = useState(false);
    const [agree, setAgree] = useState(false);

    /* The proper flags are set to true if form is submitted without required fields */
    const [firstNameErrorFlag, setFirstNameErrorFlag] = useState(false);
    const [lastNameErrorFlag, setLastNameErrorFlag] = useState(false);
    const [emailErrorFlag, setemailErrorFlag] = useState(false);
    const [territoryErrorFlag, setterritoryErrorFlag] = useState(false);
    const [organizationNameErrorFlag, setorganizationNameErrorFlag] =
        useState(false);
    const [agreeErrorFlag, setAgreeErrorFlag] = useState(false);

    const handleSubmit = async (e: any) => {
        e.preventDefault();
        resetAllErrorFlags();
        const body = createBody(
            title,
            firstName,
            lastName,
            email,
            territory,
            organizationName,
            multipleStates,
            agree
        );
        if (body) {
            const response = await fetch(
                `${AuthResource.getBaseUrl()}/api/email-registered`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify(body),
                }
            );
            if (response.status === 200) {
                signedCallback();
            } else {
                console.log(response);
            }
        }
    };

    /* INFO
       handles the front-end validation and builds the body object of type AgreementBody
       then returns it if no required values are absent. Otherwise, it returns null. */
    const createBody = (
        title: string,
        firstName: string,
        lastName: string,
        email: string,
        territory: string,
        organizationName: string,
        operatesInMultipleStates: boolean,
        agreedToTermsOfService: boolean
    ) => {
        let goodToGo: boolean = true;
        const required: string[] = [
            "firstName",
            "lastName",
            "email",
            "territory",
            "organizationName",
            "agreedToTermsOfService",
        ];
        const body: AgreementBody = {
            title: title,
            firstName: firstName,
            lastName: lastName,
            email: email,
            territory: territory,
            organizationName: organizationName,
            operatesInMultipleStates: operatesInMultipleStates,
            agreedToTermsOfService: agreedToTermsOfService,
        };
        Object.entries(body).forEach((item) => {
            const [key, value]: [string, string | boolean] = item;
            if (
                required.includes(key) &&
                (String(value).trim() === "" || value === false)
            ) {
                console.log(
                    `${key} cannot be ${value === "" ? "empty" : "false"}.`
                );
                goodToGo = false;
                setErrorFlag(key);
            }
        });
        if (goodToGo) {
            return body;
        }
        return null;
    };

    /* INFO
       When resubmitting, this will be called to eliminate all the previous flags
       prior to re-flagging the ones still throwing errors. */
    const resetAllErrorFlags = () => {
        setFirstNameErrorFlag(false);
        setLastNameErrorFlag(false);
        setemailErrorFlag(false);
        setterritoryErrorFlag(false);
        setorganizationNameErrorFlag(false);
        setAgreeErrorFlag(false);
    };

    /* INFO
       Here is where we set flags */
    const setErrorFlag = (key: string) => {
        switch (key) {
            case "firstName":
                setFirstNameErrorFlag(true);
                break;
            case "lastName":
                setLastNameErrorFlag(true);
                break;
            case "email":
                setemailErrorFlag(true);
                break;
            case "territory":
                setterritoryErrorFlag(true);
                break;
            case "organizationName":
                setorganizationNameErrorFlag(true);
                break;
            case "agreedToTermsOfService":
                setAgreeErrorFlag(true);
                break;
            default:
                break;
        }
    };

    const Required = () => {
        return <span style={{ color: "red" }}>*</span>;
    };

    const AgreementLabel = () => {
        return (
            <span className="maxw-2">
                By submitting your information, you're agreeing to the
                ReportSteam <Link to="/terms-of-service">terms of service</Link>
                . <Required />
            </span>
        );
    };

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

            <Form id="tos-agreement" onSubmit={handleSubmit}>
                <h3 className="padding-top-7 text-normal">
                    Name and contact information
                </h3>
                <FormGroup>
                    <Label htmlFor="title">Title</Label>
                    <TextInput
                        id="title"
                        name="title"
                        type="text"
                        inputSize="small"
                        value={title}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setTitle(e.target.value)
                        }
                    />
                </FormGroup>
                <FormGroup error={firstNameErrorFlag}>
                    <Label htmlFor="first-name">
                        First name <Required />
                    </Label>
                    <TextInput
                        id="first-name"
                        name="first-name"
                        type="text"
                        value={firstName}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setFirstName(e.target.value)
                        }
                    />
                </FormGroup>
                <FormGroup error={lastNameErrorFlag}>
                    <Label htmlFor="last-name">
                        Last name <Required />
                    </Label>
                    <TextInput
                        id="last-name"
                        name="last-name"
                        type="text"
                        value={lastName}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setLastName(e.target.value)
                        }
                    />
                </FormGroup>
                <FormGroup error={emailErrorFlag}>
                    <Label htmlFor="email">
                        Email <Required />
                    </Label>
                    <TextInput
                        id="email"
                        name="email"
                        type="email"
                        value={email}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setEmail(e.target.value)
                        }
                    />
                </FormGroup>

                <h3 className="padding-top-5 text-normal">
                    About your organization
                </h3>
                <FormGroup error={territoryErrorFlag}>
                    <Label htmlFor="states-dropdown">
                        [HQ] State or territory <Required />
                    </Label>
                    <Dropdown
                        id="input-dropdown"
                        name="states-dropdown"
                        value={territory}
                        onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                            setTerritory(e.target.value)
                        }
                    >
                        {STATES.map((state) => {
                            return (
                                <option
                                    key={state.toLowerCase()}
                                    value={state.toLowerCase()}
                                >
                                    {state}
                                </option>
                            );
                        })}
                    </Dropdown>
                </FormGroup>
                <FormGroup error={organizationNameErrorFlag}>
                    <Label htmlFor="organization-name">
                        Organization name <Required />
                    </Label>
                    <TextInput
                        id="organization-name"
                        name="organization-name"
                        type="text"
                        value={organizationName}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setOrganizationName(e.target.value)
                        }
                    />
                </FormGroup>
                <Checkbox
                    className="padding-top-3"
                    id="multi-state"
                    name="multi-state"
                    label="My org reports to multiple states"
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setMultipleStates(e.target.checked)
                    }
                />
            </Form>

            <section className="usa-section usa-prose font-sans-2xs text-base-darker border-top-05 border-base-lighter margin-top-9">
                <p>
                    ReportStream will use the information you’ve provided to
                    communicate with you for the purpose of setting up a
                    connection to the ReportStream platform, and to provide
                    support.
                </p>
                <FormGroup error={agreeErrorFlag}>
                    <Checkbox
                        className="padding-top-3"
                        id="agree"
                        name="agree"
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setAgree(e.target.checked)
                        }
                        label={<AgreementLabel />}
                    />
                    <ErrorMessage>
                        <span
                            style={{
                                color: "red",
                                visibility: agreeErrorFlag
                                    ? "visible"
                                    : "hidden",
                            }}
                        >
                            You must agree to the Terms of Service before using
                            ReportStream.
                        </span>
                    </ErrorMessage>
                </FormGroup>
            </section>

            <Button
                form="tos-agreement"
                className="margin-bottom-10"
                type="submit"
            >
                Submit registration
            </Button>
        </div>
    );
}

export default SigningForm;
