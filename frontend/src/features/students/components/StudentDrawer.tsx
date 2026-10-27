import { X } from "lucide-react";

import { useStudentScores } from "../hooks/useStudentScores";

import type { Student } from "../types/student.types";

type StudentDrawerProps = {
    student: Student;
    onClose: () => void;
};

export function StudentDrawer({ student, onClose }: StudentDrawerProps) {
    const scores = useStudentScores(student.id);

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
                            {student.studentCode} · {student.className}
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

                <section className="score-history">
                    <div className="section-heading">
                        <div>
                            <h3>Chi tiết điểm học tập</h3>

                            <p>Điểm theo từng lần đánh giá</p>
                        </div>

                        {scores.data && <span>{scores.data.length} lần</span>}
                    </div>

                    {scores.isLoading ? (
                        <p className="muted">Đang tải lịch sử điểm...</p>
                    ) : scores.error ? (
                        <p className="error">{scores.error.message}</p>
                    ) : scores.data?.length === 0 ? (
                        <div className="empty-state">
                            Học sinh chưa có dữ liệu điểm.
                        </div>
                    ) : (
                        <div className="score-list">
                            {scores.data?.map((score, index) => (
                                <article key={score.id} className="score-item">
                                    <div className="score-item-header">
                                        <div>
                                            <strong>
                                                Lần {scores.data.length - index}
                                            </strong>

                                            <span>
                                                {formatDate(
                                                    score.assessmentDate,
                                                )}
                                            </span>
                                        </div>

                                        <span className="score-stars">
                                            +{score.awardedStars} sao
                                        </span>
                                    </div>

                                    <div className="score-values">
                                        <div>
                                            <span>BTVN</span>

                                            <strong>
                                                {formatScore(
                                                    score.homeworkScore,
                                                )}
                                            </strong>
                                        </div>

                                        <div>
                                            <span>Kiểm tra</span>

                                            <strong>
                                                {formatScore(score.testScore)}
                                            </strong>
                                        </div>

                                        <div className="final-score">
                                            <span>Điểm tính</span>

                                            <strong>
                                                {formatScore(score.finalScore)}
                                            </strong>
                                        </div>
                                    </div>
                                </article>
                            ))}
                        </div>
                    )}
                </section>
            </aside>
        </div>
    );
}

function formatScore(value: number | null): string {
    if (value === null) {
        return "–";
    }

    return new Intl.NumberFormat("vi-VN", {
        maximumFractionDigits: 2,
    }).format(value);
}

function formatDate(value: string): string {
    return new Intl.DateTimeFormat("vi-VN").format(new Date(value));
}

function money(value: number): string {
    return new Intl.NumberFormat("vi-VN").format(value) + " ₫";
}
