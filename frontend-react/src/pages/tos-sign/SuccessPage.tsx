import React from "react";

import Title from "../../components/Title";
import { AgreementBody } from "./SigningForm";

const classNames = "usa-prose margin-bottom-4";

const NumberCircle = ({
    number,
    filled,
    className,
}: {
    number: number;
    filled?: boolean;
    className?: string;
}) => {
    const cssStyles: React.CSSProperties = {
        borderRadius: "50%",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        background: filled ? "#122946" : "#fff",
        color: filled ? "#fff" : "#122946",
        height: "30px",
        width: "30px",
        border: "2px solid #122946",
    };

    return (
        <div className={className || ""} style={cssStyles}>
            {number}
        </div>
    );
};

const Step = ({
    number,
    label,
    complete,
}: {
    number: number;
    label: string;
    complete: boolean;
}) => {
    return (
        <div className="display-flex margin-top-4">
            <NumberCircle
                className="flex-align-self-center"
                number={number}
                filled={complete}
            />
            <span className="flex-align-self-center margin-left-3">
                {label}
            </span>
        </div>
    );
};

function SuccessPage({ data }: { data: AgreementBody }) {
    const steps = [
        {
            number: 1,
            label: "Register to upload",
            complete: true,
        },
        {
            number: 2,
            label: "Confirm identity with jurisdiction name",
            complete: false,
        },
        {
            number: 3,
            label: "Log in with credentials",
            complete: false,
        },
        {
            number: 4,
            label: "Start submitting data",
            complete: false,
        },
    ];
    return (
        <div className="width-tablet margin-x-auto margin-bottom-5">
            <Title
                preTitle="Account registration"
                title={`You're almost there, ${data.firstName}!`}
            />
            <p className={classNames}>
                Our team will reach out to you within one week with credentials
                to log into ReportStream.
            </p>
            <p className={classNames}>
                A copy of this confirmation has been sent to {data.email}. If
                you don't receive confirmation, check your SPAM folder for an
                email from reportstream@cdc.gov.
            </p>
            <p className={classNames}>
                Full name: {data.firstName} {data.lastName}
                <br />
                Email: {data.email}
                <br />
                State or territory: {data.territory.toUpperCase()}
                <br />
                Organization name: {data.organizationName}
            </p>
            <h3 className="padding-top-7 margin-top-7 margin-bottom-7 text-normal border-top-05 border-base-lighter">
                Next steps
            </h3>
            {steps.map((step) => {
                return (
                    <Step
                        key={step.number}
                        number={step.number}
                        label={step.label}
                        complete={step.complete}
                    />
                );
            })}
        </div>
    );
}

export default SuccessPage;
