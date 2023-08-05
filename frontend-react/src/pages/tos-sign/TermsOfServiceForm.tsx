import {
    Alert,
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
import { Helmet } from "react-helmet-async";

import Title from "../../components/Title";
import getStateTerritoryList from "../../utils/StateTerritories";
import config from "../../config";
import { getAppInsightsHeaders } from "../../TelemetryService";
import { USLink } from "../../components/USLink";

import SuccessPage from "./SuccessPage";

const { RS_API_URL } = config;

enum Fields {
    title = "title",
    operatesInMultipleStates = "operatesInMultipleStates",
}

enum RequiredFields {
    firstName = "firstName",
    lastName = "lastName",
    email = "email",
    territory = "territory",
    organizationName = "organizationName",
    agreedToTermsOfService = "agreedToTermsOfService",
}

export interface AgreementBody {
    [Fields.title]: string;
    [RequiredFields.firstName]: string;
    [RequiredFields.lastName]: string;
    [RequiredFields.email]: string;
    [RequiredFields.territory]: string;
    [RequiredFields.organizationName]: string;
    [Fields.operatesInMultipleStates]: boolean;
    [RequiredFields.agreedToTermsOfService]: boolean;
}

function TermsOfServiceForm() {
    const STATES = getStateTerritoryList();

    /* Form field values are stored here */
    const [title, setTitle] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [email, setEmail] = useState("");
    const [territory, setTerritory] = useState(STATES[0].toLowerCase());
    const [organizationName, setOrganizationName] = useState("");
    const [multipleStates, setMultipleStates] = useState(false);
    const [agree, setAgree] = useState(false);
    const [success, setSuccess] = useState(false);

    /* The proper flags are set to true if form is submitted without required fields */
    const [firstNameErrorFlag, setFirstNameErrorFlag] = useState(false);
    const [lastNameErrorFlag, setLastNameErrorFlag] = useState(false);
    const [emailErrorFlag, setemailErrorFlag] = useState(false);
    const [territoryErrorFlag, setterritoryErrorFlag] = useState(false);
    const [organizationNameErrorFlag, setorganizationNameErrorFlag] =
        useState(false);
    const [agreeErrorFlag, setAgreeErrorFlag] = useState(false);
    const [sendGridErrorFlag, setSendGridErrorFlag] = useState({
        isError: false,
        status: 200,
    });

    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async (e: any) => {
        e.preventDefault();

        setSubmitting(true);
        resetAllErrorFlags();

        const body = createBody();
        if (body === null) {
            setSubmitting(false);
            return;
        }

        const response = await fetch(`${RS_API_URL}/api/email-registered`, {
            method: "POST",
            headers: {
                ...getAppInsightsHeaders(),
                "Content-Type": "application/json",
            },
            body: JSON.stringify(body),
        });
        if (response.status < 200 || response.status > 299) {
            setSubmitting(false);
            setSendGridErrorFlag({
                isError: true,
                status: response.status,
            });
            return;
        }

        setSuccess(true);
    };

    /* INFO
       handles the front-end not-null validation and builds the body object of type AgreementBody
       then returns it if no required values are absent. Otherwise, it returns null. */
    function createBody(): AgreementBody | null {
        let bodyHasNoErrors: boolean = true;
        const body: AgreementBody = {
            [Fields.title]: title,
            [RequiredFields.firstName]: firstName,
            [RequiredFields.lastName]: lastName,
            [RequiredFields.email]: email,
            [RequiredFields.territory]: territory,
            [RequiredFields.organizationName]: organizationName,
            [Fields.operatesInMultipleStates]: multipleStates,
            [RequiredFields.agreedToTermsOfService]: agree,
        };
        Object.entries(body).forEach((item) => {
            const [key, value]: [string, string | boolean] = item;
            if (
                Object.keys(RequiredFields).includes(key) &&
                (String(value).trim() === "" || value === false)
            ) {
                bodyHasNoErrors = false;
                setSubmitting(false);
                setErrorFlag(key);
            }
        });

        return bodyHasNoErrors ? body : null;
    }

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
        return <span style={{ color: "#b50909" }}>*</span>;
    };

    const AgreementLabel = () => {
        return (
            <span className="maxw-2">
                I have read and agree to the ReportSteam{" "}
                <USLink href="/terms-of-service" target="_blank" rel="noopener">
                    terms of service
                </USLink>
                . <Required />
            </span>
        );
    };

    const ErrorMessageWithFlag = ({
        message,
        flag,
    }: {
        message: string;
        flag: boolean;
    }) => {
        if (flag) {
            return (
                <ErrorMessage>
                    <span
                        style={{
                            color: "#b50909",
                        }}
                    >
                        {message}
                    </span>
                </ErrorMessage>
            );
        } else {
            return null;
        }
    };

    return success ? (
        <SuccessPage data={createBody()} />
    ) : (
        <>
            <Helmet>
                <title>Sign the Terms of Service</title>
            </Helmet>
            <div
                data-testid="form-container"
                className="grid-container tablet:grid-col-6 margin-x-auto"
            >
                <Title
                    title="Register your organization with ReportStream"
                    preTitle="Account registration"
                />
                <p className="padding-bottom-6 margin-bottom-6 border-bottom-1px border-base-lighter">
                    Required fields are marked with an asterisk (
                    <abbr
                        title="required"
                        className="usa-hint usa-hint--required"
                    >
                        *
                    </abbr>
                    ).
                </p>

                <Form id="tos-agreement" onSubmit={(e) => handleSubmit(e)}>
                    <fieldset className="usa-fieldset margin-bottom-6">
                        <legend className="usa-legend font-body-lg text-bold">
                            Name and contact information
                        </legend>
                        <FormGroup>
                            <Label htmlFor="title">Job Title</Label>
                            <TextInput
                                id="title"
                                data-testid="title"
                                name="title"
                                type="text"
                                value={title}
                                maxLength={255}
                                onChange={(
                                    e: React.ChangeEvent<HTMLInputElement>,
                                ) => setTitle(e.target.value)}
                            />
                        </FormGroup>
                        <FormGroup error={firstNameErrorFlag}>
                            <Label htmlFor="first-name">
                                First name <Required />
                            </Label>
                            <TextInput
                                alt="First name input"
                                id="first-name"
                                data-testid="first-name"
                                name="first-name"
                                type="text"
                                value={firstName}
                                maxLength={255}
                                onChange={(
                                    e: React.ChangeEvent<HTMLInputElement>,
                                ) => setFirstName(e.target.value)}
                            />
                            <ErrorMessageWithFlag
                                flag={firstNameErrorFlag}
                                message="First name is a required field"
                            />
                        </FormGroup>
                        <FormGroup error={lastNameErrorFlag}>
                            <Label htmlFor="last-name">
                                Last name <Required />
                            </Label>
                            <TextInput
                                alt="Last name input"
                                id="last-name"
                                data-testid="last-name"
                                name="last-name"
                                type="text"
                                value={lastName}
                                maxLength={255}
                                onChange={(
                                    e: React.ChangeEvent<HTMLInputElement>,
                                ) => setLastName(e.target.value)}
                            />
                            <ErrorMessageWithFlag
                                flag={lastNameErrorFlag}
                                message="Last name is a required field"
                            />
                        </FormGroup>
                        <FormGroup error={emailErrorFlag}>
                            <Label htmlFor="email">
                                Email <Required />
                            </Label>
                            <TextInput
                                alt="Email input"
                                id="email"
                                data-testid="email"
                                name="email"
                                type="email"
                                value={email}
                                maxLength={255}
                                onChange={(
                                    e: React.ChangeEvent<HTMLInputElement>,
                                ) => setEmail(e.target.value)}
                            />
                            <ErrorMessageWithFlag
                                flag={emailErrorFlag}
                                message="Email is a required field"
                            />
                        </FormGroup>
                    </fieldset>
                    <fieldset className="usa-fieldset margin-bottom-6">
                        <legend className="usa-legend font-body-lg text-bold">
                            About your organization
                        </legend>

                        <FormGroup error={organizationNameErrorFlag}>
                            <Label htmlFor="organization-name">
                                Organization name <Required />
                            </Label>
                            <TextInput
                                alt="Organization input"
                                id="organization-name"
                                data-testid="organization-name"
                                name="organization-name"
                                type="text"
                                value={organizationName}
                                maxLength={255}
                                onChange={(
                                    e: React.ChangeEvent<HTMLInputElement>,
                                ) => setOrganizationName(e.target.value)}
                            />
                            <ErrorMessageWithFlag
                                flag={organizationNameErrorFlag}
                                message="Organization is a required field"
                            />
                        </FormGroup>
                        <FormGroup error={territoryErrorFlag}>
                            <Label htmlFor="states-dropdown">
                                State or territory you're based in <Required />
                            </Label>
                            <Dropdown
                                id="input-dropdown"
                                data-testid="states-dropdown"
                                name="states-dropdown"
                                value={territory}
                                onChange={(
                                    e: React.ChangeEvent<HTMLSelectElement>,
                                ) => setTerritory(e.target.value)}
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
                            <ErrorMessageWithFlag
                                flag={territoryErrorFlag}
                                message="State or Territory is a required field"
                            />
                        </FormGroup>
                        <Checkbox
                            alt="Agreed checkbox"
                            className="padding-top-3"
                            id="multi-state"
                            data-testid="multi-state"
                            name="multi-state"
                            label="My organization operates in multiple states"
                            onChange={(
                                e: React.ChangeEvent<HTMLInputElement>,
                            ) => setMultipleStates(e.target.checked)}
                        />
                    </fieldset>
                    <fieldset className="usa-fieldset">
                        <legend className="usa-legend font-body-lg text-bold">
                            Terms of service
                        </legend>
                        <FormGroup error={agreeErrorFlag}>
                            <Checkbox
                                id="agree"
                                data-testid="agree"
                                name="agree"
                                onChange={(
                                    e: React.ChangeEvent<HTMLInputElement>,
                                ) => setAgree(e.target.checked)}
                                label={<AgreementLabel />}
                            />
                            <ErrorMessage>
                                <span
                                    style={{
                                        color: "#b50909",
                                        visibility: agreeErrorFlag
                                            ? "visible"
                                            : "hidden",
                                    }}
                                >
                                    You must agree to the Terms of Service
                                    before using ReportStream.
                                </span>
                            </ErrorMessage>
                        </FormGroup>
                    </fieldset>
                </Form>
                <div className="border-top-1px border-base-lighter margin-top-2 padding-top-6">
                    <Button
                        form="tos-agreement"
                        type="submit"
                        data-testid="submit"
                        disabled={submitting}
                    >
                        Submit registration
                    </Button>
                    <Alert
                        headingLevel="h4"
                        style={{
                            visibility: sendGridErrorFlag.isError
                                ? "visible"
                                : "hidden",
                        }}
                        type="error"
                    >
                        Oh no! There was an error sending this data. Code:{" "}
                        {sendGridErrorFlag.status}
                    </Alert>
                </div>
            </div>
        </>
    );
}

export default TermsOfServiceForm;
