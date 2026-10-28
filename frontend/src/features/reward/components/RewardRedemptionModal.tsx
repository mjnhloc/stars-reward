import { Award, X } from "lucide-react";

import { money } from "../../../common/func/common.func";
import type { Student } from "../../students/types/student.types";

type RewardRedemptionModalProps = {
    student: Student;
    busy: boolean;
    errorMessage?: string;
    onClose: () => void;
    onConfirm: () => void;
};

export function RewardRedemptionModal({
    student,
    busy,
    errorMessage,
    onClose,
    onConfirm,
}: RewardRedemptionModalProps) {
    return (
        <div className="overlay">
            <div
                className="modal redemption-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="redemption-modal-title"
            >
                <div className="row">
                    <div>
                        <h2 id="redemption-modal-title">Xác nhận đổi thưởng</h2>

                        <p className="muted">
                            Kiểm tra thông tin trước khi xác nhận
                        </p>
                    </div>

                    <button
                        type="button"
                        className="icon"
                        onClick={onClose}
                        disabled={busy}
                        aria-label="Đóng"
                    >
                        <X />
                    </button>
                </div>

                <div className="studentBox">
                    <strong>{student.fullName}</strong>

                    <span>
                        {student.studentCode} · {student.className}
                    </span>
                </div>

                <div className="redemption-summary">
                    <div>
                        <span>Tổng sao hiện tại</span>
                        <strong>{student.totalStars} sao</strong>
                    </div>

                    <div>
                        <span>Số sao sẽ đổi</span>
                        <strong>{student.redeemableStars} sao</strong>
                    </div>

                    <div>
                        <span>Giá trị nhận được</span>
                        <strong className="money">
                            {money(student.redeemableValue)}
                        </strong>
                    </div>

                    <div>
                        <span>Sao còn lại</span>
                        <strong>{student.remainingStars} sao</strong>
                    </div>
                </div>

                <div className="redemption-notice">
                    <Award aria-hidden="true" />

                    <span>
                        Sau khi xác nhận, hệ thống sẽ trừ{" "}
                        <strong>{student.redeemableStars} sao</strong> khỏi số
                        dư của học sinh.
                    </span>
                </div>

                {errorMessage && (
                    <div className="login-error" role="alert">
                        {errorMessage}
                    </div>
                )}

                <div className="actions">
                    <button
                        type="button"
                        className="secondary"
                        onClick={onClose}
                        disabled={busy}
                    >
                        Hủy
                    </button>

                    <button
                        type="button"
                        onClick={onConfirm}
                        disabled={busy || student.redeemableStars < 10}
                    >
                        {busy ? "Đang xử lý..." : "Xác nhận đổi thưởng"}
                    </button>
                </div>
            </div>
        </div>
    );
}
