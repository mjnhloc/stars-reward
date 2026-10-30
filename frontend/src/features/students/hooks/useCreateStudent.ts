import { useMutation, useQueryClient } from "@tanstack/react-query";

import { createStudent } from "../api/students.api";

export function useCreateStudent() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: createStudent,

        onSuccess: (_, request) => {
            queryClient.invalidateQueries({
                queryKey: ["students"],
            });

            queryClient.invalidateQueries({
                queryKey: ["classes"],
            });

            queryClient.invalidateQueries({
                queryKey: ["dashboard"],
            });

            queryClient.invalidateQueries({
                queryKey: ["students", request.classId],
            });
        },
    });
}
