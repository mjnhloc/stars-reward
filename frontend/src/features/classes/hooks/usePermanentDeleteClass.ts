import { useMutation, useQueryClient } from "@tanstack/react-query";

import { invalidateTrash } from "../../../common/func/invalidateTrash";
import { deleteClassPermanently } from "../api/classes.api";

export function usePermanentDeleteClass() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: deleteClassPermanently,

        onSuccess: () => invalidateTrash(queryClient),
    });
}
