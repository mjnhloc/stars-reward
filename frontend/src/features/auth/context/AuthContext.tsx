import {
    createContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from "react";
import { getCurrentUser, refreshAccessToken } from "../api/auth.api";
import { tokenStorage } from "../services/tokenStorage";
import type { AuthUser } from "../types/auth.types";

type AuthContextValue = {
    user: AuthUser | null;
    isAuthenticated: boolean;
    isInitializing: boolean;
    setAuthenticatedUser: (
        user: AuthUser,
        accessToken: string,
        remember: boolean,
    ) => void;
    logout: () => void;
};

export const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ 
    children,
}: { 
    children: ReactNode; 
}) {
    const [user, setUser] = useState<AuthUser | null>(null);

    const [isInitializing, setIsInitializing] = useState(true);

    useEffect(() => {
        /*
         * A new tab or a reopened browser has no access token yet, but the
         * refresh cookie may still be valid, so try it before showing login
         */
        const ensureAccessToken = tokenStorage.getFromAnyStorage()
            ? Promise.resolve()
            : refreshAccessToken().then((refreshed) =>
                  tokenStorage.set(refreshed.accessToken, refreshed.remember),
              );

        ensureAccessToken
            .then(getCurrentUser)
            .then(setUser)
            .catch(() => {
                tokenStorage.clear();
                setUser(null);
            })
            .finally(() => {
                setIsInitializing(false);
            });
    }, []);

    function setAuthenticatedUser(
        authenticatedUser: AuthUser,
        accessToken: string,
        remember: boolean,
    ) {
        tokenStorage.set(accessToken, remember);
        setUser(authenticatedUser);
    }

    function logout() {
        tokenStorage.clear();
        setUser(null);
    }

    const value = useMemo<AuthContextValue>(
        () => ({
            user,
            isAuthenticated: user !== null,
            isInitializing,
            setAuthenticatedUser,
            logout,
        }),
        [user, isInitializing],
    );

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}
