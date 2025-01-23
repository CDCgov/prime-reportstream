interface HL7MessageProps {
    message: string;
}

const HL7Message = ({ message }: HL7MessageProps) => {
    const normalized = message.replace(/\r/g, "\n");
    const lines = normalized.split("\n");

    return (
        <div>
            {lines.map((line, index) => {
                const delimiterIndex = line.indexOf("|");

                if (delimiterIndex === -1) {
                    return <div key={index}>{line}</div>;
                }

                const segmentCode = line.slice(0, delimiterIndex);
                const message = line.slice(delimiterIndex);

                return (
                    <div key={index}>
                        <span className="text-bold text-indigo-40">{segmentCode}</span>
                        {message}
                    </div>
                );
            })}
        </div>
    );
};

export default HL7Message;
