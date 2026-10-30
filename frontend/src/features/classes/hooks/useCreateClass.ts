import { useMutation, useQueryClient } from "@tanstack/react-query";

import { createClass } from "../api/classes.api";

export function useCreateClass() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: createClass,

        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ["classes"],
            });

            queryClient.invalidateQueries({
                queryKey: ["dashboard"],
            });
        },
    });
}
