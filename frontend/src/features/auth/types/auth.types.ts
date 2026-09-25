export type UserRole = "ADMIN" | "TEACHER" | "VIEWER";

export type AuthUser = {
    id: string;
    username: string;
    displayName: string;
    role: UserRole;
};

export type LoginRequest = {
    username: string;
    password: string;
};

export type LoginResponse = {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
    user: AuthUser;
};
