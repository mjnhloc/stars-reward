export type UserRole = "ADMIN" | "TEACHER" | "VIEWER";

export type AuthUser = {
    id: string;
    email: string;
    displayName: string;
    role: UserRole;
};

export type LoginRequest = {
    email: string;
    password: string;
    remember: boolean;
};

export type LoginResponse = {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
    user: AuthUser;
};
