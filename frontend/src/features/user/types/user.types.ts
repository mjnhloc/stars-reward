import type { UserRole } from "../../auth/types/auth.types";

export type ManagedUser = {
    id: string;
    displayName: string;
    email: string;
    role: UserRole;
    active: boolean;
    createdAt: string;
    classIds: string[];
};

export type CreateUserRequest = {
    displayName: string;
    email: string;
    role: UserRole;
    password: string;
    classIds: string[];
};

export type UpdateUserRequest = {
    displayName: string;
    email: string;
    role: UserRole;
    active: boolean;
    // null keeps the current password
    newPassword: string | null;
    classIds: string[];
};
