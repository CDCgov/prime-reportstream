const openAsBlob = (data: string) => {
    let formattedContent;

    // Check if the content is JSON and format it
    try {
        formattedContent = JSON.stringify(JSON.parse(data), null, 2);
    } catch {
        formattedContent = data;
    }

    const blob = new Blob([formattedContent], { type: "text/plain" });

    const url = URL.createObjectURL(blob);

    window.open(url, "_blank");

    // Revoke the URL to free up memory
    URL.revokeObjectURL(url);
};

export default openAsBlob;
