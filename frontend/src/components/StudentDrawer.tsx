import { useQuery } from "@tanstack/react-query";
import { X } from "lucide-react";
import { api } from "../lib/api";
import type { Student, Transaction } from "../types";
export function StudentDrawer({
    student,
    onClose,
}: {
    student: Student;
    onClose: () => void;
}) {
    const q = useQuery({
        queryKey: ["history", student.id],
        queryFn: async () =>
            (
                await api.get<Transaction[]>(
                    `/students/${student.id}/transactions`,
                )
            ).data,
    });
    return (
        <div className="drawerWrap">
            <button className="backdrop" onClick={onClose} />
            <aside className="drawer">
                <div className="row">
                    <h2>Chi tiết học sinh</h2>
                    <button className="icon" onClick={onClose}>
                        <X />
                    </button>
                </div>
                <h3>{student.fullName}</h3>
                <p>
                    {student.studentCode} · {student.className}
                </p>
                <div className="miniGrid">
                    <div>
                        <span>Tổng sao</span>
                        <strong>{student.totalStars}</strong>
                    </div>
                    <div>
                        <span>Quy đổi</span>
                        <strong>{money(student.redeemableValue)}</strong>
                    </div>
                </div>
                <h3>Lịch sử giao dịch</h3>
                {q.isLoading && <p>Đang tải...</p>}
                {q.error && <p className="error">{q.error.message}</p>}
                <div className="history">
                    {q.data?.map((t) => (
                        <article key={t.id}>
                            <b
                                className={
                                    t.star_delta > 0 ? "positive" : "negative"
                                }
                            >
                                {t.star_delta > 0 ? "+" : ""}
                                {t.star_delta} sao
                            </b>
                            <p>{t.reason_detail}</p>
                            <small>
                                {new Date(t.created_at).toLocaleString("vi-VN")}{" "}
                                · {t.actor}
                            </small>
                        </article>
                    ))}
                </div>
            </aside>
        </div>
    );
}
const money = (v: number) => new Intl.NumberFormat("vi-VN").format(v) + " ₫";
