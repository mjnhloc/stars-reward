import { LogOut, Settings, UsersRound } from "lucide-react";

type Props = {
    displayName?: string;
    email?: string;
    isOpen: boolean;
    onToggle: () => void;
    onProfileClick: () => void;
    // only given to admins
    onManageUsersClick?: () => void;
    onLogout: () => void;
};

export function UserMenu({
    displayName,
    email,
    isOpen,
    onToggle,
    onProfileClick,
    onManageUsersClick,
    onLogout,
}: Props) {
    return (
        <div className="user-menu">
            <button
                type="button"
                className="user-menu-trigger"
                onClick={onToggle}
                aria-expanded={isOpen}
            >
                <span className="user-avatar">{getInitials(displayName)}</span>

                <span className="user-menu-name">{displayName}</span>
            </button>

            {isOpen && (
                <div className="user-menu-dropdown">
                    <div className="user-menu-summary">
                        <strong>{displayName}</strong>
                        <span>{email}</span>
                    </div>

                    <button type="button" onClick={onProfileClick}>
                        <Settings aria-hidden="true" />
                        Đổi mật khẩu
                    </button>

                    {onManageUsersClick && (
                        <button type="button" onClick={onManageUsersClick}>
                            <UsersRound aria-hidden="true" />
                            Quản lý người dùng
                        </button>
                    )}

                    <div className="user-menu-divider" />

                    <button
                        type="button"
                        className="user-menu-logout"
                        onClick={onLogout}
                    >
                        <LogOut aria-hidden="true" />
                        Đăng xuất
                    </button>
                </div>
            )}
        </div>
    );
}

function getInitials(displayName?: string): string {
    if (!displayName) {
        return "U";
    }

    return displayName
        .trim()
        .split(/\s+/)
        .slice(-2)
        .map((part) => part[0])
        .join("")
        .toUpperCase();
}
