import { useMutation } from "@tanstack/react-query";

/**
 * TODO: Implement hook
 */
const useCodeMappingFormSubmit = () => {
    const fn = async () => {
        // Fake request until implementation
        await new Promise((resolve) => setTimeout(resolve, 2500));
    };
    return useMutation({
        mutationFn: fn,
    });
};

export default useCodeMappingFormSubmit;
