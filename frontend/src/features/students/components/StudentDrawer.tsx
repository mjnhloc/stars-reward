import { useState } from "react";

import { X } from "lucide-react";

import { useStudentScores } from "../hooks/useStudentScores";
import { useStudentTransactions } from "../hooks/useStudentTransactions";
import { StudentScoreHistory } from "./StudentScoreHistory";
import { StudentTransactionHistory } from "./StudentTransactionHistory";

import { formatDate, money } from "../../../common/func/common.func";
import type { Student } from "../types/student.types";

type StudentDrawerProps = {
    student: Student;
    onClose: () => void;
};

type DrawerTab = "scores" | "transactions";

export function StudentDrawer({ student, onClose }: StudentDrawerProps) {
    const [activeTab, setActiveTab] = useState<DrawerTab>("scores");

    // chỉ tải giao dịch khi người dùng mở tab lần đầu
    const [hasOpenedTransactions, setHasOpenedTransactions] = useState(false);

    const scores = useStudentScores(student.id);

    const transactions = useStudentTransactions(
        student.id,
        hasOpenedTransactions,
    );

    function openTab(tab: DrawerTab) {
        setActiveTab(tab);

        if (tab === "transactions") {
            setHasOpenedTransactions(true);
        }
    }

    return (
        <div className="drawerWrap">
            <button
                type="button"
                className="backdrop"
                onClick={onClose}
                aria-label="Đóng chi tiết học sinh"
            />

            <aside
                className="drawer student-drawer"
                role="dialog"
                aria-modal="true"
                aria-labelledby="student-drawer-title"
            >
                <div className="row">
                    <div>
                        <h2 id="student-drawer-title">{student.fullName}</h2>

                        <p className="muted">
                            {student.studentCode} · {student.className} · Bắt
                            đầu học {formatDate(student.startedAt)}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="icon"
                        onClick={onClose}
                        aria-label="Đóng"
                    >
                        <X />
                    </button>
                </div>

                <section className="miniGrid">
                    <div>
                        <span>Tổng ngôi sao</span>
                        <strong>{student.totalStars}</strong>
                    </div>

                    <div>
                        <span>Có thể đổi</span>
                        <strong>{money(student.redeemableValue)}</strong>
                    </div>

                    <div>
                        <span>Sao có thể đổi</span>
                        <strong>{student.redeemableStars}</strong>
                    </div>

                    <div>
                        <span>Sao còn dư</span>
                        <strong>{student.remainingStars}</strong>
                    </div>
                </section>

                <div className="profile-tabs" role="tablist">
                    <button
                        type="button"
                        role="tab"
                        aria-selected={activeTab === "scores"}
                        className={activeTab === "scores" ? "active" : ""}
                        onClick={() => openTab("scores")}
                    >
                        Lịch sử điểm
                        {scores.data && ` (${scores.data.length})`}
                    </button>

                    <button
                        type="button"
                        role="tab"
                        aria-selected={activeTab === "transactions"}
                        className={activeTab === "transactions" ? "active" : ""}
                        onClick={() => openTab("transactions")}
                    >
                        Giao dịch sao
                        {transactions.data && ` (${transactions.data.length})`}
                    </button>
                </div>

                {activeTab === "scores" ? (
                    <StudentScoreHistory studentId={student.id} />
                ) : (
                    <StudentTransactionHistory studentId={student.id} />
                )}
            </aside>
        </div>
    );
}
