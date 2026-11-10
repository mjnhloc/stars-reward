import { useQuery } from "@tanstack/react-query";

import { getClassTrash } from "../api/classes.api";

export function useClassTrash() {
    return useQuery({
        queryKey: ["trash", "classes"],
        queryFn: getClassTrash,
    });
}
