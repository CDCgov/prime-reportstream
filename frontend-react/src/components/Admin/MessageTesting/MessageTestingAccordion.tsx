import { Accordion, Icon, Tag } from "@trussworks/react-uswds";
import { RSMessageResult } from "../../../config/endpoints/reports";

export const MessageTestingAccordion = ({
    accordionTitle,
    priority,
    resultData,
    fieldsToRender,
}: {
    accordionTitle: string;
    priority: "error" | "warning";
    resultData: RSMessageResult;
    fieldsToRender: (keyof RSMessageResult)[];
}) => {
    const fieldID = accordionTitle.toLowerCase().split(" ").join("-");
    const existingFields = fieldsToRender.filter((field) => Object.keys(resultData).includes(field));
    const combinedFieldData = existingFields.flatMap((field) => resultData[field]);

    // Immediately return if there's no warning/error data to display
    if (combinedFieldData.length === 0) return;

    return (
        <div key={`${fieldID}-accordion-wrapper`} className="padding-top-4 ">
            <Accordion
                key={`${fieldID}-accordion`}
                items={[
                    {
                        className: "bg-gray-5",
                        title: (
                            <>
                                {priority === "error" && <Icon.Error size={3} className="text-top margin-right-1" />}

                                {priority === "warning" && (
                                    <Icon.Warning size={3} className="text-top margin-right-1" />
                                )}

                                <span className="font-body-lg">{accordionTitle}</span>

                                {priority === "error" && (
                                    <Tag className="margin-left-1 bg-secondary-vivid">{combinedFieldData.length}</Tag>
                                )}

                                {priority === "warning" && (
                                    <Tag className="margin-left-1 bg-accent-warm">{combinedFieldData.length}</Tag>
                                )}
                            </>
                        ),
                        content: (
                            <div className="bg-white font-sans-sm padding-top-2 padding-bottom-2 padding-left-1 padding-right-1">
                                {combinedFieldData.map((item, index) => (
                                    <div key={index}>
                                        <div>{item}</div>
                                        {index < combinedFieldData.length - 1 && <hr className="rs-hr--half-margin" />}
                                    </div>
                                ))}
                            </div>
                        ),
                        expanded: false,
                        headingLevel: "h3",
                        id: `${fieldID}-list`,
                    },
                ]}
            />
        </div>
    );
};
