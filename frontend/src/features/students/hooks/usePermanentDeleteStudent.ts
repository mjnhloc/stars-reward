import { useMutation, useQueryClient } from "@tanstack/react-query";

import { invalidateTrash } from "../../../common/func/invalidateTrash";
import { deleteStudentPermanently } from "../api/students.api";

export function usePermanentDeleteStudent() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: deleteStudentPermanently,

        onSuccess: () => invalidateTrash(queryClient),
    });
}
