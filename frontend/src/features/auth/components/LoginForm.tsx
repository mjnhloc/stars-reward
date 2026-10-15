import { Eye, EyeOff, LoaderCircle, LockKeyhole, Mail } from "lucide-react";
import { useState, type SubmitEvent } from "react";

import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { useLogin } from "../hooks/useLogin";

type LocationState = {
    from?: {
        pathname?: string;
    };
};

export function LoginForm() {
    const navigate = useNavigate();
    const location = useLocation();

    const { setAuthenticatedUser } = useAuth();
    const loginMutation = useLogin();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [remember, setRememeber] = useState(true);
    const [showPassword, setShowPassword] = useState(false);

    const [validationError, setValidationError] = useState("");

    async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
        event.preventDefault();

        const normalizedEmail = email.trim().toLowerCase();

        if (!normalizedEmail || !password) {
            setValidationError("Vui lòng nhập đầy đủ tài khoản và mật khẩu");
            return;
        }

        setValidationError("");

        try {
            const result = await loginMutation.mutateAsync({
                email: normalizedEmail,
                password,
                remember,
            });

            setAuthenticatedUser(result.user, result.accessToken, remember);

            const state = location.state as LocationState | null;

            navigate(state?.from?.pathname ?? "/dashboard", { replace: true });
        } catch {
            // Error from loginMutaton.error
        }
    }

    const errorMessage = validationError || loginMutation.error?.message;

    return (
        <form onSubmit={handleSubmit} className="login-form" noValidate>
            <label className="form-field">
                <span>Tài khoản</span>

                <div className="input-container">
                    <Mail aria-hidden="true" />

                    <input
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        autoComplete="email"
                        placeholder="Nhập email"
                        disabled={loginMutation.isPending}
                    />
                </div>
            </label>

            <label className="form-field">
                <span>Mật Khẩu</span>

                <div className="input-container">
                    <LockKeyhole aria-hidden="true" />

                    <input
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        type={showPassword ? "text" : "password"}
                        autoComplete="current-password"
                        placeholder="Nhập mật khẩu"
                        disabled={loginMutation.isPending}
                    />

                    <button
                        type="button"
                        className="password-toggle"
                        onClick={() => setShowPassword((current) => !current)}
                        aria-label={
                            showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"
                        }
                    >
                        {showPassword ? <EyeOff /> : <Eye />}
                    </button>
                </div>
            </label>

            <label className="remember-login">
                <input
                    type="checkbox"
                    checked={remember}
                    onChange={(event) => setRememeber(event.target.checked)}
                />
                Ghi nhớ đăng nhập
            </label>

            {errorMessage && (
                <div className="login-error" role="alert" aria-live="polite">
                    {errorMessage}
                </div>
            )}

            <button
                type="submit"
                className="login-button"
                disabled={loginMutation.isPending}
            >
                {loginMutation.isPending ? (
                    <>
                        <LoaderCircle className="spinner" aria-hidden="true" />
                        Đang đăng nhập...
                    </>
                ) : (
                    "Đăng nhập"
                )}
            </button>
        </form>
    );
}
