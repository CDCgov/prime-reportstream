import React from "react";

import { BasicHelmet } from "../../components/header/BasicHelmet";
import Title from "../../components/Title";

import { AgreementBody } from "./TermsOfServiceForm";

const Step = ({
    heading,
    label,
}: {
    number: number;
    heading: string;
    label: string;
    complete: boolean;
}) => {
    return (
        <li className="usa-process-list__item">
            <h4 className="usa-process-list__heading">{heading}</h4>
            <p className="margin-top-05">{label}</p>
        </li>
    );
};

function SuccessPage({ data }: { data: AgreementBody | null }) {
    const steps = [
        {
            number: 1,
            heading: "Account creation",
            label: "The ReportStream team will create and configure your account.",
            complete: false,
        },
        {
            number: 2,
            heading: "Log in to your account",
            label: "The ReportStream team will reach out to you within one week with login credentials.",
            complete: false,
        },
    ];
    return data !== null ? (
        <>
            <BasicHelmet pageTitle="Sign the Terms of Service" />
            <div
                data-testid="success-container"
                className="tablet:grid-col-6 margin-x-auto"
            >
                <Title
                    preTitle="Account registration"
                    title={`You're almost there, ${data.firstName}!`}
                />
                <div className="usa-alert usa-alert--success">
                    <div className="usa-alert__body">
                        <h4 className="usa-alert__heading">
                            Registration request submitted
                        </h4>
                        <p className="usa-alert__text">
                            <strong>Name:</strong>{" "}
                            {`${data.firstName} ${data.lastName}`}
                            <br />
                            <strong>Email:</strong> {data.email}
                            <br />
                            <strong>State or territory:</strong>{" "}
                            {data.territory.toUpperCase()}
                            <br />
                            <strong>Organization name:</strong>{" "}
                            {data.organizationName}
                            <br />
                            <br />A copy of this confirmation has been sent to{" "}
                            {data.email}. If you don't receive confirmation,
                            check your SPAM folder for an email from
                            no-reply@reportstream.gov.
                        </p>
                    </div>
                </div>
                <h2 className="margin-top-6">Next steps</h2>
                <ol className="usa-process-list">
                    {steps.map((step) => {
                        return (
                            <Step
                                key={step.number}
                                number={step.number}
                                heading={step.heading}
                                label={step.label}
                                complete={step.complete}
                            />
                        );
                    })}
                </ol>
            </div>
        </>
    ) : null;
}

export default SuccessPage;
