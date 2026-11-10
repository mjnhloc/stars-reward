import { useMutation, useQueryClient } from "@tanstack/react-query";

import { invalidateTrash } from "../../../common/func/invalidateTrash";
import { restoreClass } from "../api/classes.api";

export function useRestoreClass() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: restoreClass,

        onSuccess: () => invalidateTrash(queryClient),
    });
}
