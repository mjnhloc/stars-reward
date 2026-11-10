import type { QueryClient } from "@tanstack/react-query";

/*
 * Restoring or permanently deleting changes the trash, the lists, the dashboard
 * and the import history numbers
 */
export function invalidateTrash(queryClient: QueryClient) {
    ["trash", "classes", "students", "dashboard", "score-imports"].forEach(
        (key) => queryClient.invalidateQueries({ queryKey: [key] }),
    );
}
