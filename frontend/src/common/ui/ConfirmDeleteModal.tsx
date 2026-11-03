import type { ReactNode } from "react";

import { TriangleAlert, X } from "lucide-react";

type Props = {
    title: string;
    message: ReactNode;
    warning?: ReactNode;
    confirmLabel?: string;
    busy: boolean;
    disabled?: boolean;
    errorMessage?: string;
    onClose: () => void;
    onConfirm: () => void;
};

export function ConfirmDeleteModal({
    title,
    message,
    warning,
    confirmLabel = "Xóa",
    busy,
    disabled = false,
    errorMessage,
    onClose,
    onConfirm,
}: Props) {
    return (
        <div className="overlay" role="dialog" aria-modal="true">
            <div className="modal">
                <div className="row">
                    <h2>{title}</h2>

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

                <p>{message}</p>

                {warning && (
                    <div className="redemption-notice">
                        <TriangleAlert />

                        <span>{warning}</span>
                    </div>
                )}

                {errorMessage && (
                    <p className="error" role="alert">
                        {errorMessage}
                    </p>
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
                        className="danger-action"
                        onClick={onConfirm}
                        disabled={busy || disabled}
                    >
                        {busy ? "Đang xóa..." : confirmLabel}
                    </button>
                </div>
            </div>
        </div>
    );
}
