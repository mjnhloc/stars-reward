import { Outlet, useNavigate } from "react-router-dom";
import { Star } from "lucide-react";

import { useAuth } from "../../features/auth/hooks/useAuth";

export function AppLayout() {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    function handleLogout() {
        logout();
        navigate("/login", { replace: true });
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

                <div className="header-user">
                    <span>{user?.displayName}</span>

                    <button type="button" onClick={handleLogout}>
                        Đăng xuất
                    </button>
                </div>
            </header>

            <Outlet />
        </div>
    );
}
