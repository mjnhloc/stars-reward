import { useMutation, useQueryClient } from "@tanstack/react-query";

import { updateUser } from "../api/users.api";
import type { UpdateUserRequest } from "../types/user.types";

type Variables = {
    userId: string;
    request: UpdateUserRequest;
};

export function useUpdateUser() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ userId, request }: Variables) =>
            updateUser(userId, request),

        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ["users"],
            });
        },
    });
}
