import { TriangleAlert, X } from "lucide-react";

import { useRevertPreview } from "../hooks/useRevertPreview";
import { useRevertScoreImport } from "../hooks/useRevertScoreImport";

import type { ImportBatch } from "../types/score-import.types";

type Props = {
    batch: ImportBatch;
    onClose: () => void;
};

export function RevertImportModal({ batch, onClose }: Props) {
    const preview = useRevertPreview(batch.id);

    const mutation = useRevertScoreImport();

    const blocked =
        preview.data?.students.filter((student) => !student.enoughBalance) ??
        [];

    function handleClose() {
        if (!mutation.isPending) {
            onClose();
        }
    }

    return (
        <div className="overlay" role="dialog" aria-modal="true">
            <div className="modal revert-modal">
                <div className="row">
                    <h2>Hoàn tác lần nhập điểm</h2>

                    <button
                        type="button"
                        className="icon"
                        onClick={handleClose}
                        disabled={mutation.isPending}
                        aria-label="Đóng"
                    >
                        <X />
                    </button>
                </div>

                {preview.isLoading ? (
                    <p className="muted">Đang kiểm tra số sao của học sinh...</p>
                ) : preview.error ? (
                    <p className="error">{preview.error.message}</p>
                ) : (
                    preview.data && (
                        <>
                            <p>
                                Hoàn tác lần nhập{" "}
                                <strong>{preview.data.fileName}</strong>? Hệ
                                thống sẽ hủy{" "}
                                <strong>
                                    {preview.data.sessionCount} buổi điểm
                                </strong>{" "}
                                và trừ lại{" "}
                                <strong>{preview.data.starsToReverse} sao</strong>{" "}
                                của {preview.data.students.length} học sinh. Sau
                                đó bạn có thể upload lại file để nhập lại.
                            </p>

                            {preview.data.students.length > 0 && (
                                <ul className="revert-students">
                                    {preview.data.students.map((student) => (
                                        <li
                                            key={student.studentCode}
                                            className={
                                                student.enoughBalance
                                                    ? ""
                                                    : "not-enough"
                                            }
                                        >
                                            <span>
                                                <strong>
                                                    {student.studentCode}
                                                </strong>{" "}
                                                · {student.fullName}
                                            </span>

                                            <span>
                                                −{student.starsToReverse} sao
                                                <small>
                                                    {student.enoughBalance
                                                        ? ` (còn ${student.currentBalance - student.starsToReverse})`
                                                        : ` (hiện chỉ còn ${student.currentBalance})`}
                                                </small>
                                            </span>
                                        </li>
                                    ))}
                                </ul>
                            )}

                            <div className="redemption-notice">
                                <TriangleAlert aria-hidden="true" />

                                <span>
                                    {preview.data.canRevert
                                        ? "Lịch sử giao dịch vẫn được giữ, hệ thống tạo giao dịch hoàn tác để trừ lại số sao."
                                        : `Không thể hoàn tác vì ${blocked.length} học sinh đã đổi thưởng hoặc bị trừ sao nên không đủ sao để trừ lại.`}
                                </span>
                            </div>
                        </>
                    )
                )}

                {mutation.error && (
                    <p className="error" role="alert">
                        {mutation.error.message}
                    </p>
                )}

                <div className="actions">
                    <button
                        type="button"
                        className="secondary"
                        onClick={handleClose}
                        disabled={mutation.isPending}
                    >
                        Hủy
                    </button>

                    <button
                        type="button"
                        className="danger-action"
                        disabled={
                            mutation.isPending || !preview.data?.canRevert
                        }
                        onClick={() =>
                            mutation.mutate(batch.id, { onSuccess: onClose })
                        }
                    >
                        {mutation.isPending ? "Đang hoàn tác..." : "Hoàn tác"}
                    </button>
                </div>
            </div>
        </div>
    );
}
