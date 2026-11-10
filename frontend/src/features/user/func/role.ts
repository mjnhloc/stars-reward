import type { UserRole } from "../../auth/types/auth.types";

export const ROLE_LABELS: Record<UserRole, string> = {
    ADMIN: "Quản trị viên",
    TEACHER: "Giáo viên",
    VIEWER: "Chỉ xem",
};

export function formatRole(role: string): string {
    return ROLE_LABELS[role as UserRole] ?? role;
}
