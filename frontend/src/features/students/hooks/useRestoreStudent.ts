import { useMutation, useQueryClient } from "@tanstack/react-query";

import { invalidateTrash } from "../../../common/func/invalidateTrash";
import { restoreStudent } from "../api/students.api";
import type { RestoreStudentRequest } from "../types/student.types";

type Variables = {
    studentId: string;
    request: RestoreStudentRequest;
};

export function useRestoreStudent() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ studentId, request }: Variables) =>
            restoreStudent(studentId, request),

        onSuccess: () => invalidateTrash(queryClient),
    });
}
