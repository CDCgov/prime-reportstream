import EmailButton from "../EmailButton/EmailButton";
import { EmailContainerCentered } from "../EmailContainer/EmailContainer";
import { EmailContentBlock } from "../EmailContentBlock/EmailContentBlock";
import { EmailLayout } from "../EmailLayout/EmailLayout";

export function TemplateEmailChallenge() {
    return (
        <EmailLayout>
            #if(!{"${appName}"})
            <EmailContentBlock type="header">
                {"${org.name}"} - Action Required: One-time verification code
            </EmailContentBlock>
            #end
            <EmailContentBlock isFirst>
                Hi {"$!{StringTool.escapeHtml($!{user.profile.firstName})}"},
            </EmailContentBlock>
            #if({"${appName}"})
            <EmailContentBlock>
                You have requested an email link to sign in to {"${appName}"}.
                To finish signing in, click the button below or enter the
                provided code. If you did not request this email, please contact
                an administrator at
                <span style={{ color: "#0074b3" }}>
                    {"${orgTechSupportEmail}"}
                </span>
                .
            </EmailContentBlock>
            #end #if({"${emailAuthenticationLink}"})
            <EmailContainerCentered>
                <EmailContainerCentered.ContentBlock>
                    <EmailButton
                        id="email-authentication-button"
                        href="${emailAuthenticationLink}"
                    >
                        Sign In
                    </EmailButton>
                </EmailContainerCentered.ContentBlock>

                <EmailContainerCentered.ContentBlock isInfo>
                    This link expires in
                    {
                        "${f.formatTimeDiffDateNowInUserLocale(${tokenExpirationDate})}"
                    }
                    .
                </EmailContainerCentered.ContentBlock>
                <EmailContainerCentered.ContentBlock>
                    Can't use the link? Enter a code instead:
                    <b>{"${verificationToken}"}</b>
                </EmailContainerCentered.ContentBlock>
            </EmailContainerCentered>
            #else
            <EmailContentBlock>
                You are receiving this email because a request was made for a
                one-time code that can be used for authentication.
            </EmailContentBlock>
            <EmailContentBlock>
                Please enter the following code for verification:
            </EmailContentBlock>
            <EmailContentBlock>
                <span id="verification-code" style={{ fontSize: "18px" }}>
                    {"${verificationToken}"}
                </span>
            </EmailContentBlock>
            <EmailContentBlock>
                If you believe you have received this email in error, please
                reach out to your system administrator.
            </EmailContentBlock>
            #end
        </EmailLayout>
    );
}

export default TemplateEmailChallenge;
