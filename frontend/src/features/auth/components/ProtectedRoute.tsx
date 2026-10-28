import { Navigate, Outlet, useLocation } from "react-router-dom";

import { LoadingScreen } from "../../../common/ui/LoadingScreen";
import { useAuth } from "../hooks/useAuth";
import { tokenStorage } from "../services/tokenStorage";

export function ProtectedRoute() {
    const location = useLocation();
    const { isAuthenticated, isInitializing } = useAuth();

    if (isInitializing) {
        return <LoadingScreen />;
    }

    const token = tokenStorage.getFromAnyStorage();

    if (!isAuthenticated || !token) {
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
