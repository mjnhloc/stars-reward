import { useQuery } from "@tanstack/react-query";

import { getStudentTrash } from "../api/students.api";

export function useStudentTrash() {
    return useQuery({
        queryKey: ["trash", "students"],
        queryFn: getStudentTrash,
    });
}
