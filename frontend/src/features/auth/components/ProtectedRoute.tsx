import { Navigate, Outlet, useLocation } from "react-router-dom";

import { useAuth } from "../hooks/useAuth";
import { LoadingScreen } from "../../../components/ui/LoadingScreen";

export function ProtectedRoute() {
    const location = useLocation();
    const { isAuthenticated, isInitializing } = useAuth();

    if (isInitializing) {
        return <LoadingScreen />;
    }

    if (!isAuthenticated) {
        return (
            <Navigate
                to="/login"
                replace
                state={{
                    from: location,
                }}
            />
        );
    }

    return <Outlet />;
}
