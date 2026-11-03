import { useMutation, useQueryClient } from "@tanstack/react-query";

import { updateStudent } from "../api/students.api";
import type { UpdateStudentRequest } from "../types/student.types";

type UpdateStudentVariables = {
    studentId: string;
    request: UpdateStudentRequest;
};

export function useUpdateStudent() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ studentId, request }: UpdateStudentVariables) =>
            updateStudent(studentId, request),

        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ["students"],
            });

            // số học sinh / số sao của lớp thay đổi khi chuyển lớp
            queryClient.invalidateQueries({
                queryKey: ["classes"],
            });

            queryClient.invalidateQueries({
                queryKey: ["dashboard"],
            });
        },
    });
}
