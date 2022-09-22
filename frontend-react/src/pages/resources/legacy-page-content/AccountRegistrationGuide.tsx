import DOMPurify from "dompurify";

import site from "../../../content/site.json";
import { BasicHelmet } from "../../../components/header/BasicHelmet";

/* eslint-disable jsx-a11y/anchor-has-content */
export const AccountRegistrationGuideIa = () => {
    return (
        <>
            <BasicHelmet pageTitle="Account registration guide | Resources" />
            <h1 id="anchor-top">Account registration guide</h1>
            <h2>
                The ReportStream team will help you set up a new user account.
                Follow the steps below to reach out and get the process started.
            </h2>
            <p className="text-base text-italic">Last updated: January 2022</p>

            <section>
                <h3>Register for a ReportStream account</h3>
                <div className="usa-alert usa-alert--info margin-y-6 measure-6">
                    <div className="usa-alert__body">
                        <h3 className="usa-alert__heading font-body-md margin-top-05">
                            Communications during registration
                        </h3>
                        <p>
                            ReportStream, as part of the{" "}
                            <a
                                href="https://www.cdc.gov/surveillance/projects/pandemic-ready-it-systems.html"
                                className="usa-link"
                                target="_blank"
                                rel="noreferrer noopener"
                            >
                                Pandemic-Ready Interoperability Modernization
                                Effort (PRIME)
                            </a>
                            , shares some resources with other projects operated
                            by the CDC.
                        </p>
                        <p className="usa-alert__text">
                            Some automated communications you receive during the
                            registration process may contain references to{" "}
                            <a
                                href="https://simplereport.gov"
                                className="usa-link"
                                target="_blank"
                                rel="noreferrer noopener"
                            >
                                SimpleReport
                            </a>
                            , a PRIME project that ReportStream closely
                            collaborates with. If you have questions about any
                            step of the registration process, contact us at{" "}
                            <a
                                href={
                                    "mailto:" +
                                    DOMPurify.sanitize(site.orgs.RS.email) +
                                    "?subject=Getting started with ReportStream"
                                }
                                className="usa-link"
                            >
                                reportstream@cdc.gov
                            </a>
                            .
                        </p>
                    </div>
                </div>
                <ol className="usa-process-list rs-process-list__documentation">
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Contact ReportStream
                        </h4>
                        <p className="margin-top-05">
                            Send an email to{" "}
                            <a
                                href={
                                    "mailto:" +
                                    DOMPurify.sanitize(site.orgs.RS.email) +
                                    "?subject=Register a new account: organization or testing facility"
                                }
                                className="usa-link"
                            >
                                reportstream@cdc.gov
                            </a>
                            . Use the example text below as a template for your
                            message:
                        </p>
                        <blockquote className="rs-blockquote__documentation">
                            <p>
                                To: reportstream@cdc.gov
                                <br />
                                Subject: Register a new account - organization
                                or testing facility
                            </p>

                            <p>Hi,</p>

                            <p>
                                I'd like to set up a new user account with
                                ReportStream.
                            </p>

                            <p>
                                Name:
                                <br />
                                Job title:
                                <br />
                                Organization:
                                <br />
                                Email address:
                                <br />
                                Phone number:
                                <br />
                            </p>
                            <p>
                                Organization that referred me to ReportStream:
                                <br />
                                Public health department(s) I report to:
                                <br />
                                Current reporting method(s):
                            </p>
                        </blockquote>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Accept the terms of service
                        </h4>
                        <ul className="margin-top-2">
                            <li>
                                A ReportStream representative will process your
                                registration request and respond via email with
                                a link to an online form.{" "}
                                <em>
                                    It can take 2-3 business days to receive
                                    this email
                                </em>
                                .
                            </li>
                            <li>
                                Open the link to view the form and review the
                                ReportStream{" "}
                                <a href="/terms-of-service">terms of service</a>
                            </li>
                            <li>
                                Fill out and submit the form.{" "}
                                <em>
                                    You must accept the terms of service and
                                    submit this form in order to move to step 3.
                                </em>
                            </li>
                        </ul>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Receive account activation email
                        </h4>
                        <ul className="margin-top-2">
                            <li>
                                An email will arrive in your inbox from
                                support@simplereport.gov with the subject line
                                "Welcome to SimpleReport."{" "}
                                <em>
                                    It can take up to 1 business day to receive
                                    the activation email.
                                </em>{" "}
                            </li>
                            <li>
                                Open the email and click "Activate your account"
                            </li>
                        </ul>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Activate your account
                        </h4>
                        <p>
                            After opening the link in the activation email,
                            follow the prompts to set up your account.
                        </p>
                        <p>
                            <strong>
                                i. Create a secure password that includes:
                            </strong>
                        </p>
                        <ul>
                            <li>A minimum of 8 characters</li>
                            <li>An uppercase and a lowercase letter</li>
                            <li>A number</li>
                        </ul>
                        <p>
                            <strong>
                                ii. Choose a "Forgot password" question
                            </strong>
                        </p>
                        <p>
                            Use the drop-down list to select your preferred
                            security question and enter your response.
                        </p>

                        <p>
                            <strong>
                                iii. Set up multi-factor authentication
                            </strong>
                        </p>
                        <p>
                            Pick a multi-factor authentication option, and click
                            "Setup" below it. These authentication options are
                            meant to secure your account. Below, you can review
                            instructions for setting up{" "}
                            <a href="#anchor-mfa" className="usa-link">
                                SMS authentication and Google Authenticator/Okta
                                Verify
                            </a>
                            .
                        </p>
                        <p>
                            <em>
                                If you choose biometric authentication, make
                                sure that you'll only log in to ReportStream
                                from the device you're currently using.
                                Biometric authentication may be difficult to use
                                across multiple devices.
                            </em>
                        </p>

                        <li>
                            <strong>iv. Click "Continue" </strong>
                        </li>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Close window
                        </h4>
                        <ul>
                            <li>
                                A prompt will appear confirming your account set
                                up is complete.{" "}
                                <em>
                                    It can take up to 1 business day before your
                                    account receives access to upload CSV files
                                </em>
                                .{" "}
                            </li>
                            <li>
                                Ignore the "Continue to SimpleReport" button
                            </li>
                            <li>Close out of the window</li>
                        </ul>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Log in to ReportStream
                        </h4>

                        <ul>
                            <li>
                                Go to{" "}
                                <a href="/login" className="usa-link">
                                    https://reportstream.cdc.gov/login
                                </a>{" "}
                            </li>
                            <li>Enter your username and password </li>
                            <li>
                                Contact{" "}
                                <a
                                    href={
                                        "mailto:" +
                                        DOMPurify.sanitize(site.orgs.RS.email) +
                                        "?subject=Getting started with ReportStream"
                                    }
                                    className="usa-link"
                                >
                                    reportstream@cdc.gov
                                </a>{" "}
                                if you run into any login issues{" "}
                            </li>
                        </ul>
                    </li>
                </ol>
            </section>
            <section>
                <h3 id="anchor-mfa">Multi-factor authentication options</h3>
                <p>
                    If you choose SMS or Google Authenticator/Okta Verify as
                    your multi-factor authentication, follow the instructions
                    below to get set up.
                </p>

                <h4 id="sms-authentication">SMS authentication</h4>
                <ol className="usa-process-list rs-process-list__documentation">
                    <li className="usa-process-list__item">
                        Enter your phone number, then click{" "}
                        <strong>Send code</strong>. (Make sure to click{" "}
                        <strong>Send code</strong>, or you won't be able to
                        continue.)
                        <img
                            src="/assets/img/getting-started/sms-step-1.png"
                            alt='Okta page asking for your phone number and the blue "Send code" button'
                        />
                    </li>
                    <li className="usa-process-list__item">
                        Check your text messages for a 6-digit authentication
                        code.
                    </li>
                    <li className="usa-process-list__item">
                        Enter the code in the “Enter Code” field, then click{" "}
                        <strong>Verify</strong>.
                        <img
                            src="/assets/img/getting-started/sms-step-3.png"
                            alt='Okta page with the "Enter Code" field and blue "Verify" button'
                        />
                    </li>
                </ol>

                <h4 id="google-authenticator-or-okta-verify">
                    Google Authenticator or Okta Verify
                </h4>
                <ol className="usa-process-list rs-process-list__documentation">
                    <li className="usa-process-list__item">
                        Select the kind of phone that you use (either iPhone or
                        Android). You'll be asked to download an app. Download
                        it on your phone and wait for it to install. (The page
                        on your device might vary a bit from the screenshot
                        below, based on your device type and whether you chose
                        Google Authenticator or Okta Verify.)
                        <br />
                        <img
                            src="/assets/img/getting-started/authenticator-verify-step-1.png"
                            alt="Okta page that asks you to choose iPhone or Android, with directions for downloading the app"
                        />
                    </li>
                    <li className="usa-process-list__item">
                        Once the app is installed, click <strong>Next</strong>.
                    </li>
                    <li className="usa-process-list__item">
                        Open the app and scan the QR code that appears on your
                        ReportStream registration page. Once you've successfully
                        scanned the QR code, click <strong>Next</strong>.
                        <img
                            src="/assets/img/getting-started/authenticator-verify-step-3.png"
                            alt="Okta page with the QR code"
                        />
                    </li>
                    <li className="usa-process-list__item">
                        Back on your phone, the app will show you a code. Enter
                        the code on the registration page, then click{" "}
                        <strong>Verify</strong>. (The code changes regularly, so
                        you'll need to check the app each time you log in to
                        ReportStream.)
                        <img
                            src="/assets/img/getting-started/authenticator-verify-step-4.png"
                            alt='Okta page with the "Enter Code" field and blue "Verify" button'
                        />
                    </li>
                </ol>
            </section>
        </>
    );
};
