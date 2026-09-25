const ACCESS_TOKEN_KEY = "stars_reward_access_token";

export const tokenStorage = {
    clear(): void {
        sessionStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(ACCESS_TOKEN_KEY);
    },

    get(): string | null {
        return sessionStorage.getItem(ACCESS_TOKEN_KEY);
    },

    set(token: string, remember: boolean): void {
        this.clear();

        const storage = remember ? localStorage : sessionStorage;
        storage.setItem(ACCESS_TOKEN_KEY, token);
    },

    getFromAnyStorage(): string | null {
        return (
            sessionStorage.getItem(ACCESS_TOKEN_KEY) ??
            localStorage.getItem(ACCESS_TOKEN_KEY)
        );
    },
};
