import type { PropsWithChildren } from "react";
import CodeMappingForm from "../../components/CodeMapping/CodeMappingForm";

export type CodeMappingPageProps = PropsWithChildren;

const CodeMappingPage = (props: CodeMappingPageProps) => {
    return (
        <div className="measure-6">
            <h1>Code mapping tool</h1>
            <CodeMappingForm />
            {props.children}
        </div>
    );
};

export default CodeMappingPage;
