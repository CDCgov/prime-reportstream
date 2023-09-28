import { EmailContentBlock } from "../EmailContentBlock/EmailContentBlock";
import { EmailLayout } from "../EmailLayout/EmailLayout";

export function TemplateExample() {
    return (
        <EmailLayout>
            <EmailContentBlock type="header">ReportStream...</EmailContentBlock>
            <EmailContentBlock isFirst>
                Hi {"$!{StringTool.escapeHtml($!{user.profile.firstName})}"},
                ...
            </EmailContentBlock>
            <EmailContentBlock>Your ReportStream account...</EmailContentBlock>
        </EmailLayout>
    );
}

export default TemplateExample;
