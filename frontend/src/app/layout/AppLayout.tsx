import { Star } from "lucide-react";
import { useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";

import { logoutRequest } from "../../features/auth/api/auth.api";
import { useAuth } from "../../features/auth/hooks/useAuth";
import { UserMenu } from "../../features/user/components/UserMenu";
import { UserProfileModal } from "../../features/user/components/UserProfileModal";

export function AppLayout() {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [isProfileOpen, setIsProfileOpen] = useState(false);

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
                    <Star aria-hidden="true" />

                    <div>
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
        </div>
    );
}
