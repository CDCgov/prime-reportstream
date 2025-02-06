import { Accordion, Icon, Tag } from "@trussworks/react-uswds";

export const MessageTestingAccordion = ({
    accordionTitle,
    priority,
    fieldData,
}: {
    accordionTitle: string;
    priority: "error" | "warning";
    fieldData: (string | boolean | undefined)[];
}) => {
    const fieldID = accordionTitle.toLowerCase().split(" ").join("-");

    // Immediately return if there's no warning/error data to display
    if (fieldData.length === 0) return;

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
                                    <Tag className="margin-left-1 bg-secondary-vivid">{fieldData.length}</Tag>
                                )}

                                {priority === "warning" && (
                                    <Tag className="margin-left-1 bg-accent-warm">{fieldData.length}</Tag>
                                )}
                            </>
                        ),
                        content: (
                            <div
                                aria-label={accordionTitle}
                                className="bg-white font-sans-sm padding-top-2 padding-bottom-2 padding-left-1 padding-right-1"
                            >
                                {fieldData.map((item, index) => (
                                    <div key={index}>
                                        <div>{item}</div>
                                        {index < fieldData.length - 1 && <hr className="rs-hr--half-margin" />}
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
