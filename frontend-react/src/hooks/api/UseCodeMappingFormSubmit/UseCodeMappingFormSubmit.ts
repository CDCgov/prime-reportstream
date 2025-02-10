import { useMutation } from "@tanstack/react-query";

const useCodeMappingFormSubmit = () => {
    const fn = async () => {
        await new Promise((resolve) => setTimeout(resolve, 2500));
    };
    return useMutation({
        mutationFn: fn,
    });
};

export default useCodeMappingFormSubmit;
