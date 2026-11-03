import { useMutation, useQueryClient } from "@tanstack/react-query";

import { deleteClass } from "../api/classes.api";

export function useDeleteClass() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: deleteClass,

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
