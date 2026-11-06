import { useState } from "react";

import { RotateCcw } from "lucide-react";

import { useScoreImports } from "../hooks/useScoreImports";
import { RevertImportModal } from "./RevertImportModal";

import { formatDate } from "../../../common/func/common.func";
import type { ImportBatch } from "../types/score-import.types";

export function ImportHistory() {
    const imports = useScoreImports();

    const [revertingBatch, setRevertingBatch] = useState<ImportBatch | null>(
        null,
    );

    if (imports.isLoading) {
        return <p className="muted">Đang tải lịch sử nhập...</p>;
    }

    if (imports.error) {
        return <p className="error">{imports.error.message}</p>;
    }

    if (!imports.data?.length) {
        return <div className="empty-state">Chưa có lần nhập điểm nào.</div>;
    }

    return (
        <>
            <div className="score-list import-history">
                {imports.data.map((batch) => {
                    const reverted = batch.status === "REVERTED";

                    return (
                        <article
                            key={batch.id}
                            className={`score-item ${reverted ? "reverted" : ""}`}
                        >
                            <div className="score-item-header">
                                <div>
                                    <strong>{batch.fileName}</strong>

                                    <span>
                                        {formatDate(batch.importedAt, true)}
                                        {batch.importedBy &&
                                            ` · bởi ${batch.importedBy}`}
                                        {batch.classNames.length > 0 &&
                                            ` · ${batch.classNames.join(", ")}`}
                                    </span>
                                </div>

                                <span
                                    className={`import-batch-status ${reverted ? "reverted" : "completed"}`}
                                >
                                    {reverted ? "Đã hoàn tác" : "Đã nhập"}
                                </span>
                            </div>

                            <div className="import-batch-footer">
                                <p className="transaction-meta">
                                    {batch.sessionCount} buổi ·{" "}
                                    {batch.studentCount} học sinh · +
                                    {batch.awardedStars} sao
                                    {reverted && batch.revertedAt && (
                                        <>
                                            <br />
                                            Hoàn tác lúc{" "}
                                            {formatDate(batch.revertedAt, true)}
                                            {batch.revertedBy &&
                                                ` bởi ${batch.revertedBy}`}
                                        </>
                                    )}
                                </p>

                                {!reverted && (
                                    <button
                                        type="button"
                                        className="secondary-action revert-action"
                                        onClick={() => setRevertingBatch(batch)}
                                    >
                                        <RotateCcw />
                                        Hoàn tác
                                    </button>
                                )}
                            </div>
                        </article>
                    );
                })}
            </div>

            {revertingBatch && (
                <RevertImportModal
                    batch={revertingBatch}
                    onClose={() => setRevertingBatch(null)}
                />
            )}
        </>
    );
}
