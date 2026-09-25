import { useMutation } from "@tanstack/react-query";
import { login } from "../api/auth.api";
import type { LoginRequest } from "../types/auth.types";

type LoginVariables = {
    credentials: LoginRequest;
    remember: boolean;
};

export function useLogin() {
    return useMutation({
        mutationFn: ({
            credentials,
        }: LoginVariables) => login(credentials),
    });
}
