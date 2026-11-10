import { apiClient } from "../../../lib/apiClient";
import type {
    CreateUserRequest,
    ManagedUser,
    UpdateUserRequest,
} from "../types/user.types";

export async function getUsers(): Promise<ManagedUser[]> {
    const response = await apiClient.get<ManagedUser[]>("/users");

    return response.data;
}

export async function createUser(
    request: CreateUserRequest,
): Promise<ManagedUser> {
    const response = await apiClient.post<ManagedUser>("/users", request);

    return response.data;
}

export async function updateUser(
    userId: string,
    request: UpdateUserRequest,
): Promise<ManagedUser> {
    const response = await apiClient.put<ManagedUser>(
        `/users/${userId}`,
        request,
    );

    return response.data;
}
