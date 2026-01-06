import { useMutation } from "@tanstack/react-query";
import useSessionContext from "../../../contexts/Session/useSessionContext";

const useCreateResend = () => {
    const { authorizedFetch } = useSessionContext();

    const fn = (params: { reportId: string; receiver: string }) => {
        return authorizedFetch({
            url: `/adm/resend`,
            method: "post",
            params,
        });
    };

    return useMutation({
        mutationFn: fn,
    });
};

export default useCreateResend;
