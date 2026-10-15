import { useState, type SubmitEvent } from "react";

import { CheckCircle2, Eye, EyeOff } from "lucide-react";

import { useAuth } from "../hooks/useAuth";
import { useChangePassword } from "../hooks/useChangePassword";

export function ChangePasswordForm() {
    const { logout } = useAuth();
    const mutation = useChangePassword();

    const [currentPassword, setCurrentPassword] = useState("");

    const [newPassword, setNewPassword] = useState("");

    const [confirmPassword, setConfirmPassword] = useState("");

    const [showPassword, setShowPassword] = useState(false);

    const [validationError, setValidationError] = useState("");

    async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
        event.preventDefault();

        if (!currentPassword || !newPassword || !confirmPassword) {
            setValidationError("Vui lòng nhập đầy đủ thông tin.");

            return;
        }

        if (newPassword.length < 8) {
            setValidationError("Mật khẩu mới phải có ít nhất 8 ký tự.");

            return;
        }

        if (newPassword !== confirmPassword) {
            setValidationError("Xác nhận mật khẩu không khớp.");

            return;
        }

        setValidationError("");

        try {
            await mutation.mutateAsync({
                currentPassword,
                newPassword,
                confirmPassword,
            });

            /*
             * Cho user xem thông báo thành công,
             * sau đó đăng xuất để đăng nhập lại.
             */
            window.setTimeout(() => {
                logout();
                window.location.replace("/login");
            }, 1200);
        } catch (error) {
            // mutation.error được hiển thị phía dưới
            console.error("Change password failed: ", error);
        }
    }

    const errorMessage = validationError || mutation.error?.message;

    return (
        <form className="change-password-form" onSubmit={handleSubmit}>
            <PasswordField
                label="Mật khẩu hiện tại"
                value={currentPassword}
                onChange={setCurrentPassword}
                visible={showPassword}
            />

            <PasswordField
                label="Mật khẩu mới"
                value={newPassword}
                onChange={setNewPassword}
                visible={showPassword}
            />

            <PasswordField
                label="Xác nhận mật khẩu mới"
                value={confirmPassword}
                onChange={setConfirmPassword}
                visible={showPassword}
            />

            <label className="show-password">
                <input
                    type="checkbox"
                    checked={showPassword}
                    onChange={(event) => setShowPassword(event.target.checked)}
                />
                Hiển thị mật khẩu
            </label>

            {errorMessage && (
                <div className="login-error" role="alert">
                    {errorMessage}
                </div>
            )}

            {mutation.isSuccess && (
                <div className="password-success">
                    <CheckCircle2 aria-hidden="true" />
                    Đổi mật khẩu thành công. Bạn sẽ được chuyển về trang đăng
                    nhập.
                </div>
            )}

            <div className="actions">
                <button type="submit" disabled={mutation.isPending}>
                    {mutation.isPending ? "Đang cập nhật..." : "Đổi mật khẩu"}
                </button>
            </div>
        </form>
    );
}

type PasswordFieldProps = {
    label: string;
    value: string;
    visible: boolean;
    onChange: (value: string) => void;
};

function PasswordField({
    label,
    value,
    visible,
    onChange,
}: PasswordFieldProps) {
    return (
        <label className="form-field">
            <span>{label}</span>

            <div className="input-container">
                {visible ? (
                    <EyeOff aria-hidden="true" />
                ) : (
                    <Eye aria-hidden="true" />
                )}

                <input
                    type={visible ? "text" : "password"}
                    value={value}
                    onChange={(event) => onChange(event.target.value)}
                    autoComplete="new-password"
                />
            </div>
        </label>
    );
}
