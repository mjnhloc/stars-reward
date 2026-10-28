import { useQuery } from "@tanstack/react-query";
import { getClasses } from "../api/classes.api";

export function useClasses() {
    return useQuery({
        queryKey: ["classes"],
        queryFn: getClasses,
    });
}
