import { Navigate } from "react-router-dom";
import { Star } from "lucide-react";

import { LoginForm } from "../components/LoginForm";
import { useAuth } from "../hooks/useAuth";
import { LoadingScreen } from "../../../components/ui/LoadingScreen";

export function LoginPage() {
    const { isAuthenticated, isInitializing } = useAuth();

    if (isInitializing) {
        return <LoadingScreen />;
    }

    if (isAuthenticated) {
        return <Navigate to="/dashboard" replace />;
    }

    return (
        <main className="login-page">
            <section className="login-introduction">
                <div className="login-brand">
                    <Star />
                    <div>
                        <strong>Stars Reward</strong>
                        <span>Quản lí điểm thưởng học sinh</span>
                    </div>
                </div>

                <div className="login-message">
                    <h1>
                        Ghi nhận chính xác.
                        <br />
                        Khích lệ đúng lúc.
                    </h1>

                    <p>
                        Theo dõi điểm học tập, ngôi sao thưởng, lịch sử giao
                        dịch và đổi thưởng trong 1 hệ thống nhất quán.
                    </p>
                </div>
            </section>

            <section className="login-content">
                <div className="login-card">
                    <p className="login-eyebrow">Chào mừng trở lại</p>

                    <h2>Đăng nhập</h2>

                    <p className="login-description">
                        Sử dụng tài khoản được quản trị viên cấp để tiếp tục.
                    </p>

                    <LoginForm />

                    <small className="login-help">
                        Nếu chưa có tài khoản, hãy liên hệ quản trị viên hệ
                        thống.
                    </small>
                </div>
            </section>
        </main>
    );
}
