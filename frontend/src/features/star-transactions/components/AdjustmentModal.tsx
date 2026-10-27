import { X } from "lucide-react";
import { useState } from "react";
import type { Student } from "../../students/types/student.types";

type Props = {
    student: Student;
    mode: "add" | "subtract";
    busy: boolean;
    onClose: () => void;
    onSubmit: (stars: number, detail: string) => void;
};

export function AdjustmentModal({
    student,
    mode,
    busy,
    onClose,
    onSubmit,
}: Props) {
    const [amount, setAmount] = useState(1);
    const [detail, setDetail] = useState("");
    const delta = mode === "subtract" ? -amount : amount;
    return (
        <div className="overlay" role="dialog" aria-modal="true">
            <form
                className="modal"
                onSubmit={(e) => {
                    e.preventDefault();
                    onSubmit(delta, detail);
                }}
            >
                <div className="row">
                    <h2>{mode === "add" ? "Cộng" : "Trừ"} ngôi sao</h2>
                    <button type="button" className="icon" onClick={onClose}>
                        <X />
                    </button>
                </div>
                <div className="studentBox">
                    <strong>{student.fullName}</strong>
                    <span>
                        {student.className} · {student.studentCode} · Hiện có{" "}
                        {student.totalStars} sao
                    </span>
                </div>
                <label>
                    Số ngôi sao
                    <input
                        type="number"
                        min="1"
                        max="1000"
                        value={amount}
                        onChange={(e) => setAmount(Number(e.target.value))}
                        required
                    />
                </label>
                <label>
                    Lý do chi tiết
                    <textarea
                        value={detail}
                        maxLength={500}
                        onChange={(e) => setDetail(e.target.value)}
                        required
                        placeholder="Nhập lý do có thể kiểm tra lại"
                    />
                </label>
                {mode === "subtract" && amount > student.totalStars && (
                    <p className="error">Không đủ số dư để trừ.</p>
                )}
                <div className="actions">
                    <button
                        type="button"
                        className="secondary"
                        onClick={onClose}
                    >
                        Hủy
                    </button>
                    <button
                        disabled={
                            busy ||
                            (mode === "subtract" && amount > student.totalStars)
                        }
                    >
                        {busy ? "Đang xử lý..." : "Xác nhận"}
                    </button>
                </div>
            </form>
        </div>
    );
}
