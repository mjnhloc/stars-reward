import { useMutation, useQueryClient } from "@tanstack/react-query";

import { deleteStudent } from "../api/students.api";

export function useDeleteStudent() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: deleteStudent,

        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ["students"],
            });

            queryClient.invalidateQueries({
                queryKey: ["classes"],
            });

            queryClient.invalidateQueries({
                queryKey: ["dashboard"],
            });
        },
    });
}
