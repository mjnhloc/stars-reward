import { useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";

import logo from "../../assets/khai-minh-logo.png";
import { logoutRequest } from "../../features/auth/api/auth.api";
import { useAuth } from "../../features/auth/hooks/useAuth";
import { UserManagementModal } from "../../features/user/components/UserManagementModal";
import { UserMenu } from "../../features/user/components/UserMenu";
import { UserProfileModal } from "../../features/user/components/UserProfileModal";

export function AppLayout() {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [isProfileOpen, setIsProfileOpen] = useState(false);
    const [isUsersOpen, setIsUsersOpen] = useState(false);

    async function handleLogout() {
        try {
            await logoutRequest();
        } finally {
            logout();
            navigate("/login", { replace: true });
        }
    }

    if (!user) {
        return null;
    }

    return (
        <div className="app-shell">
            <header className="app-header">
                <div className="brand">
                    <img src={logo} alt="Khai Minh" className="brand-logo" />

                    <div className="brand-product">
                        <strong>Stars Reward</strong>
                        <small>Quản lí điểm thưởng học sinh</small>
                    </div>
                </div>

                <UserMenu
                    displayName={user.displayName}
                    email={user.email}
                    isOpen={isMenuOpen}
                    onToggle={() => setIsMenuOpen((value) => !value)}
                    onProfileClick={() => {
                        setIsMenuOpen(false);
                        setIsProfileOpen(true);
                    }}
                    onManageUsersClick={
                        user.role === "ADMIN"
                            ? () => {
                                  setIsMenuOpen(false);
                                  setIsUsersOpen(true);
                              }
                            : undefined
                    }
                    onLogout={handleLogout}
                />
            </header>

            <Outlet />

            {isProfileOpen && (
                <UserProfileModal
                    user={user}
                    onClose={() => setIsProfileOpen(false)}
                />
            )}

            {isUsersOpen && (
                <UserManagementModal
                    currentUserId={user.id}
                    onClose={() => setIsUsersOpen(false)}
                />
            )}
        </div>
    );
}
