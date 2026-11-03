import { useMutation, useQueryClient } from "@tanstack/react-query";

import { updateClass } from "../api/classes.api";
import type { UpdateClassRequest } from "../types/class.types";

type UpdateClassVariables = {
    classId: string;
    request: UpdateClassRequest;
};

export function useUpdateClass() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ classId, request }: UpdateClassVariables) =>
            updateClass(classId, request),

        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ["classes"],
            });

            // tên lớp hiển thị trong danh sách học sinh
            queryClient.invalidateQueries({
                queryKey: ["students"],
            });
        },
    });
}
