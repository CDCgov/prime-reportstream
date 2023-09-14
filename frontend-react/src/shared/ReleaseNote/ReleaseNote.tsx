import styles from "./ReleaseNote.module.scss";

type Section = {
    title: string;
    tag: "feature" | "bug" | "announcement" | "improvement";
    text: HTMLElementTagNameMap["p"];
};

interface ReleaseNoteProps {
    header: string;
    sections: Array<Section>;
}

export function ReleaseNote({ header, sections }: ReleaseNoteProps) {
    return (
        <div>
            <p>{header}</p>
            {sections.map((section: Section) => (
                <div>
                    <p>{section.title}</p>
                    <div></div>
                </div>
            ))}
        </div>
    );
}
