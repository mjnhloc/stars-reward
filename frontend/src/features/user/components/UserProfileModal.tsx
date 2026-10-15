import { X } from "lucide-react";
import { useState } from "react";

import { ChangePasswordForm } from "../../auth/components/ChangePasswordForm";
import type { AuthUser } from "../../auth/types/auth.types";

type Props = {
    user: AuthUser;
    onClose: () => void;
};

type Tab = "profile" | "password";

export function UserProfileModal({ user, onClose }: Props) {
    const [activeTab, setActiveTab] = useState<Tab>("profile");

    return (
        <div className="overlay">
            <div
                className="profile-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="profile-modal-title"
            >
                <div className="profile-modal-header">
                    <div>
                        <h2 id="profile-modal-title">Tài khoản của tôi</h2>

                        <p>Xem thông tin và thay đổi mật khẩu.</p>
                    </div>

                    <button
                        type="button"
                        className="icon"
                        onClick={onClose}
                        aria-label="Đóng"
                    >
                        <X aria-hidden="true" />
                    </button>
                </div>

                <div className="profile-tabs">
                    <button
                        type="button"
                        className={activeTab === "profile" ? "active" : ""}
                        onClick={() => setActiveTab("profile")}
                    >
                        Thông tin
                    </button>

                    <button
                        type="button"
                        className={activeTab === "password" ? "active" : ""}
                        onClick={() => setActiveTab("password")}
                    >
                        Đổi mật khẩu
                    </button>
                </div>

                {activeTab === "profile" ? (
                    <div className="profile-details">
                        <div>
                            <span>Họ và tên</span>
                            <strong>{user.displayName}</strong>
                        </div>

                        <div>
                            <span>Email</span>
                            <strong>{user.email}</strong>
                        </div>

                        <div>
                            <span>Vai trò</span>
                            <strong>{formatRole(user.role)}</strong>
                        </div>
                    </div>
                ) : (
                    <ChangePasswordForm />
                )}
            </div>
        </div>
    );
}

function formatRole(role: string): string {
    switch (role) {
        case "ADMIN":
            return "Quản trị viên";

        case "TEACHER":
            return "Giáo viên";

        case "VIEWER":
            return "Chỉ xem";

        default:
            return role;
    }
}
