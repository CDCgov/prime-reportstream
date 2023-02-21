import { Helmet } from "react-helmet-async";

import site from "../../../content/site.json";
import { ResourcesDirectories } from "../../../content/resources";
import { USExtLink, USLink } from "../../../components/USLink";

/* eslint-disable jsx-a11y/anchor-has-content */
export const AccountRegistrationGuideIa = () => {
    return (
        <>
            <Helmet>
                <title>{`${ResourcesDirectories.ACCOUNT_REGISTRATION} | Resources`}</title>
            </Helmet>
            <h1 id="anchor-top">{ResourcesDirectories.ACCOUNT_REGISTRATION}</h1>
            <h2>
                Follow these steps to set up a new user account with
                ReportStream.
            </h2>
            <p className="text-base text-italic">Last updated: December 2022</p>

            <section>
                <div className="usa-alert usa-alert--info margin-bottom-6 measure-6">
                    <div className="usa-alert__body">
                        <h3 className="usa-alert__heading font-body-md margin-top-05">
                            Why create a ReportStream account?
                        </h3>
                        <br />
                        <p className="usa-alert__text">
                            Entities that receive ReportStream data have the
                            option to download reports manually by logging into
                            the ReportStream application.
                        </p>
                        <br />
                        <p className="usa-alert__text">
                            Register for an account to:
                            <ul>
                                <li>
                                    Have access to your data if needed as a
                                    back-up plan
                                </li>
                                <li>
                                    Start downloading data sooner while we are
                                    setting up your ELR connection
                                </li>
                            </ul>
                        </p>
                        <p className="usa-alert__text">
                            Questions? Get in touch at{" "}
                            <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                                reportstream@cdc.gov
                            </USExtLink>
                        </p>
                    </div>
                </div>

                <p className="text-bold">Jump to:</p>
                <ul>
                    <li>
                        <USLink href="#anchor-mfa">
                            Set up multi-factor authentication
                        </USLink>
                    </li>
                    <li>
                        <USLink href="#anchor-acct-mngt">
                            Account management
                        </USLink>
                    </li>
                </ul>
                <h1>Register for a ReportStream account</h1>
                <ol className="usa-process-list rs-process-list__documentation">
                    <li className="usa-process-list__item">
                        <h3 className="usa-process-list__heading">
                            Contact ReportStream
                        </h3>
                        <p className="margin-top-05">
                            Send an email to{" "}
                            <USExtLink
                                href={`mailto: ${site.orgs.RS.email}?subject=Register a new account: organization or testing facility`}
                            >
                                reportstream@cdc.gov
                            </USExtLink>
                            . Use the example text below as a template for your
                            message:
                        </p>
                        <blockquote className="rs-blockquote__documentation">
                            <p>
                                To: {site.orgs.RS.email}
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
                        <h3 className="usa-process-list__heading">
                            Accept the terms of service
                        </h3>
                        <div className="margin-top-2">
                            <p>
                                A ReportStream representative will process your
                                registration request and respond via email with
                                a link to an online form.{" "}
                                <em>
                                    It can take 2-3 business days to receive
                                    this email
                                </em>
                                .
                            </p>
                            <p>
                                Open the link to view the form and review the
                                ReportStream{" "}
                                <USLink href="/terms-of-service">
                                    terms of service
                                </USLink>
                            </p>
                            <p>
                                Fill out and submit the form.{" "}
                                <em>
                                    You must accept the terms of service and
                                    submit this form in order to move to step 3.
                                </em>
                            </p>
                        </div>
                    </li>
                    <li className="usa-process-list__item">
                        <h3 className="usa-process-list__heading">
                            Activate your account
                        </h3>
                        <div className="margin-top-2">
                            <p>
                                An email will arrive in your inbox from
                                support@simplereport.gov with the subject line
                                "Welcome to SimpleReport."{" "}
                                <em>
                                    It can take up to 1 business day to receive
                                    the activation email.
                                </em>{" "}
                            </p>
                            <p>
                                Open the email and click "Activate your
                                account."
                            </p>
                        </div>
                    </li>
                    <li className="usa-process-list__item">
                        <h3 className="usa-process-list__heading">
                            Set your account password
                        </h3>
                        <p className="margin-top-2">
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
                            <USLink href="#anchor-mfa">
                                SMS authentication and Google Authenticator/Okta
                                Verify
                            </USLink>
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
                            <strong>iv. Click "continue"</strong>
                        </li>
                        <p>
                            A prompt will appear confirming your account set-up
                            is complete.
                        </p>
                        <p>
                            Ignore the "continue to SimpleReport" button and
                            simply close out of the window.
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h3 className="usa-process-list__heading">
                            Log in to ReportStream
                        </h3>

                        <div className="margin-top-2">
                            <p>
                                Go to{" "}
                                <USLink href="/login">
                                    https://reportstream.cdc.gov/login
                                </USLink>{" "}
                                to log in with your username and password.
                            </p>
                            <p>
                                Need help? Contact us at{" "}
                                <USExtLink
                                    href={`mailto: ${site.orgs.RS.email}?subject=Getting started with ReportStream`}
                                >
                                    reportstream@cdc.gov
                                </USExtLink>
                            </p>
                        </div>
                    </li>
                </ol>
            </section>
            <section>
                <h1 id="anchor-mfa">Set up multi-factor authentication</h1>
                <p>
                    If you choose SMS or Google Authenticator/Okta Verify as
                    your multi-factor authentication, follow the instructions
                    below to get set up.
                </p>

                <h3 id="sms-authentication">SMS authentication</h3>
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
            <section>
                <h1 id="anchor-acct-mngt">Account management</h1>
                <p>
                    ReportStream will manually manage user accounts for your
                    team. To add or remove team members,{" "}
                    <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                        contact us
                    </USExtLink>
                    .
                </p>

                <h3 id="sms-authentication">Password reset</h3>
                <p>
                    If you forgot your password, follow the instructions under
                    "Need help signing in?" on the login page at{" "}
                    <USLink href={`${site.orgs.RS.url}/login`}>
                        {`${site.orgs.RS.url}/login`}
                    </USLink>
                    .
                </p>
                <p>
                    If you want to update your password, log out of the
                    application and use the password reset process outlined
                    above.
                </p>
            </section>
        </>
    );
};
