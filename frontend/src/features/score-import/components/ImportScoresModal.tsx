import { type DragEvent, useMemo, useRef, useState } from "react";

import {
    AlertTriangle,
    CheckCircle2,
    ChevronDown,
    ChevronRight,
    FileSpreadsheet,
    Upload,
    X,
} from "lucide-react";

import { useImportScores } from "../hooks/useImportScores";
import { usePreviewScoreImport } from "../hooks/usePreviewScoreImport";
import { useScoreImports } from "../hooks/useScoreImports";
import { ImportHistory } from "./ImportHistory";

import { formatDate, formatScore } from "../../../common/func/common.func";
import type {
    ImportSessionStatus,
    ImportStudent,
} from "../types/score-import.types";

type Props = {
    onClose: () => void;
};

type ModalTab = "upload" | "history";

const STATUS_LABELS: Record<ImportSessionStatus, string> = {
    NEW: "Mới",
    EXISTS: "Đã nhập",
    CONFLICT: "Khác điểm đã nhập",
};

export function ImportScoresModal({ onClose }: Props) {
    const [activeTab, setActiveTab] = useState<ModalTab>("upload");

    const [file, setFile] = useState<File | null>(null);

    const [isDragging, setIsDragging] = useState(false);

    const [expandedCode, setExpandedCode] = useState<string | null>(null);

    const inputRef = useRef<HTMLInputElement>(null);

    const previewMutation = usePreviewScoreImport();

    const importMutation = useImportScores();

    const imports = useScoreImports();

    const preview = previewMutation.data;

    const result = importMutation.data;

    const busy = previewMutation.isPending || importMutation.isPending;

    // students grouped by class, in the same order as in the file
    const studentsByClass = useMemo(() => {
        const groups = new Map<string, ImportStudent[]>();

        preview?.students.forEach((student) => {
            groups.set(student.classCode, [
                ...(groups.get(student.classCode) ?? []),
                student,
            ]);
        });

        return groups;
    }, [preview]);

    const errors =
        preview?.issues.filter((issue) => issue.severity === "ERROR") ?? [];

    const warnings =
        preview?.issues.filter((issue) => issue.severity === "WARNING") ?? [];

    function selectFile(selected: File | undefined) {
        if (!selected || busy) {
            return;
        }

        setFile(selected);
        setExpandedCode(null);
        importMutation.reset();
        previewMutation.mutate(selected);
    }

    function chooseAnotherFile() {
        setFile(null);
        previewMutation.reset();
        importMutation.reset();

        if (inputRef.current) {
            inputRef.current.value = "";
        }
    }

    function handleDrop(event: DragEvent<HTMLLabelElement>) {
        event.preventDefault();
        setIsDragging(false);
        selectFile(event.dataTransfer.files[0]);
    }

    function handleClose() {
        if (!busy) {
            onClose();
        }
    }

    return (
        <div className="overlay">
            <div
                className="modal import-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="import-modal-title"
            >
                <div className="row">
                    <div>
                        <h2 id="import-modal-title">Nhập điểm từ Excel</h2>

                        <p className="muted">
                            Hệ thống tự tính điểm và cộng sao cho từng buổi học
                        </p>
                    </div>

                    <button
                        type="button"
                        className="icon"
                        onClick={handleClose}
                        disabled={busy}
                        aria-label="Đóng"
                    >
                        <X />
                    </button>
                </div>

                <div className="profile-tabs import-tabs" role="tablist">
                    <button
                        type="button"
                        role="tab"
                        aria-selected={activeTab === "upload"}
                        className={activeTab === "upload" ? "active" : ""}
                        disabled={busy}
                        onClick={() => setActiveTab("upload")}
                    >
                        Nhập file mới
                    </button>

                    <button
                        type="button"
                        role="tab"
                        aria-selected={activeTab === "history"}
                        className={activeTab === "history" ? "active" : ""}
                        disabled={busy}
                        onClick={() => setActiveTab("history")}
                    >
                        Lịch sử nhập
                        {imports.data && ` (${imports.data.length})`}
                    </button>
                </div>

                {activeTab === "history" ? (
                    <>
                        <p className="import-formula">
                            Hoàn tác một lần nhập sẽ hủy điểm và trừ lại số sao
                            đã cộng, sau đó có thể upload lại file.
                        </p>

                        <ImportHistory />

                        <div className="actions">
                            <button type="button" onClick={onClose}>
                                Đóng
                            </button>
                        </div>
                    </>
                ) : result ? (
                    /* step 3: done */
                    <>
                        <div className="import-success" role="status">
                            <CheckCircle2 aria-hidden="true" />

                            <span>
                                Đã nhập <strong>{result.importedSessions}</strong>{" "}
                                buổi cho <strong>{result.students}</strong> học
                                sinh và cộng{" "}
                                <strong>{result.awardedStars} sao</strong>.
                                {result.skippedSessions > 0 &&
                                    ` ${result.skippedSessions} buổi đã nhập trước đó được bỏ qua.`}
                            </span>
                        </div>

                        <div className="actions">
                            <button
                                type="button"
                                className="secondary"
                                onClick={chooseAnotherFile}
                            >
                                Nhập file khác
                            </button>

                            <button
                                type="button"
                                className="secondary"
                                onClick={() => setActiveTab("history")}
                            >
                                Xem lịch sử nhập
                            </button>

                            <button type="button" onClick={onClose}>
                                Đóng
                            </button>
                        </div>
                    </>
                ) : !file || previewMutation.isError ? (
                    /* step 1: choose file */
                    <>
                        <label
                            className={`import-dropzone ${isDragging ? "dragging" : ""}`}
                            onDragOver={(event) => {
                                event.preventDefault();
                                setIsDragging(true);
                            }}
                            onDragLeave={() => setIsDragging(false)}
                            onDrop={handleDrop}
                        >
                            <Upload aria-hidden="true" />

                            <strong>Kéo thả file vào đây hoặc bấm để chọn</strong>

                            <span>
                                File Quản lý học tập (.xlsx). Hệ thống chỉ đọc
                                sheet Bảng điểm: mã học sinh, điểm 15p và BTVN
                                của từng buổi.
                            </span>

                            <input
                                ref={inputRef}
                                type="file"
                                accept=".xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                onChange={(event) =>
                                    selectFile(event.target.files?.[0])
                                }
                            />
                        </label>

                        <p className="import-formula">
                            Điểm tính = BTVN × 0,7 + 15p × 0,3 · Điểm 10: 3 sao ·
                            Từ 9: 2 sao · Từ 8: 1 sao
                        </p>

                        {previewMutation.error && (
                            <div className="login-error" role="alert">
                                {previewMutation.error.message}
                            </div>
                        )}

                        <div className="actions">
                            <button
                                type="button"
                                className="secondary"
                                onClick={onClose}
                            >
                                Hủy
                            </button>
                        </div>
                    </>
                ) : !preview ? (
                    <div className="import-loading">
                        <FileSpreadsheet aria-hidden="true" />
                        Đang đọc file {file.name}...
                    </div>
                ) : (
                    /* step 2: preview */
                    <>
                        <div className="import-file">
                            <FileSpreadsheet aria-hidden="true" />

                            <span>
                                <strong>{preview.fileName}</strong> · sheet{" "}
                                {preview.sheetName}
                            </span>
                        </div>

                        <div className="import-summary">
                            <div>
                                <span>Học sinh</span>
                                <strong>{preview.totals.students}</strong>
                            </div>

                            <div>
                                <span>Buổi mới</span>
                                <strong>{preview.totals.newSessions}</strong>
                            </div>

                            <div>
                                <span>Đã nhập trước đó</span>
                                <strong>{preview.totals.skippedSessions}</strong>
                            </div>

                            <div>
                                <span>Sao sẽ cộng</span>
                                <strong className="positive">
                                    +{preview.totals.starsToAward}
                                </strong>
                            </div>
                        </div>

                        {errors.length > 0 && (
                            <details className="import-issues error-list" open>
                                <summary>
                                    <AlertTriangle aria-hidden="true" />
                                    {errors.length} lỗi, các dòng này sẽ không
                                    được nhập
                                </summary>

                                <ul>
                                    {errors.map((issue, index) => (
                                        <li key={index}>
                                            <small>{issue.location}</small>
                                            {issue.message}
                                        </li>
                                    ))}
                                </ul>
                            </details>
                        )}

                        {warnings.length > 0 && (
                            <details className="import-issues warning-list">
                                <summary>
                                    <AlertTriangle aria-hidden="true" />
                                    {warnings.length} cảnh báo
                                </summary>

                                <ul>
                                    {warnings.map((issue, index) => (
                                        <li key={index}>
                                            <small>{issue.location}</small>
                                            {issue.message}
                                        </li>
                                    ))}
                                </ul>
                            </details>
                        )}

                        {preview.students.length === 0 ? (
                            <div className="empty-state">
                                Không có học sinh nào khớp với hệ thống.
                            </div>
                        ) : (
                            [...studentsByClass.entries()].map(
                                ([classCode, students]) => (
                                    <section
                                        key={classCode}
                                        className="import-class"
                                    >
                                        <h3>
                                            {preview.classes.find(
                                                (c) => c.classCode === classCode,
                                            )?.className ?? classCode}
                                            <span>
                                                {students.length} học sinh
                                            </span>
                                        </h3>

                                        <div className="tableWrap">
                                            <table className="import-table">
                                                <thead>
                                                    <tr>
                                                        <th />
                                                        <th>Mã HS</th>
                                                        <th>Họ tên</th>
                                                        <th>Buổi mới</th>
                                                        <th>Đã có</th>
                                                        <th>Sao</th>
                                                    </tr>
                                                </thead>

                                                <tbody>
                                                    {students.map((student) => {
                                                        const expanded =
                                                            expandedCode ===
                                                            student.studentCode;

                                                        return (
                                                            <StudentRows
                                                                key={
                                                                    student.studentCode
                                                                }
                                                                student={student}
                                                                expanded={
                                                                    expanded
                                                                }
                                                                onToggle={() =>
                                                                    setExpandedCode(
                                                                        expanded
                                                                            ? null
                                                                            : student.studentCode,
                                                                    )
                                                                }
                                                            />
                                                        );
                                                    })}
                                                </tbody>
                                            </table>
                                        </div>
                                    </section>
                                ),
                            )
                        )}

                        {importMutation.error && (
                            <div className="login-error" role="alert">
                                {importMutation.error.message}
                            </div>
                        )}

                        <div className="actions">
                            <button
                                type="button"
                                className="secondary"
                                onClick={chooseAnotherFile}
                                disabled={busy}
                            >
                                Chọn file khác
                            </button>

                            <button
                                type="button"
                                disabled={
                                    busy || preview.totals.newSessions === 0
                                }
                                title={
                                    preview.totals.newSessions === 0
                                        ? "Không có buổi điểm mới để nhập"
                                        : undefined
                                }
                                onClick={() => importMutation.mutate(file)}
                            >
                                {importMutation.isPending
                                    ? "Đang nhập..."
                                    : `Nhập ${preview.totals.newSessions} buổi · +${preview.totals.starsToAward} sao`}
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}

type StudentRowsProps = {
    student: ImportStudent;
    expanded: boolean;
    onToggle: () => void;
};

function StudentRows({ student, expanded, onToggle }: StudentRowsProps) {
    const hasConflict = student.sessions.some(
        (session) => session.status === "CONFLICT",
    );

    return (
        <>
            <tr
                className={`import-student ${expanded ? "expanded" : ""}`}
                onClick={onToggle}
            >
                <td>
                    <button
                        type="button"
                        className="icon"
                        aria-expanded={expanded}
                        aria-label={`Xem chi tiết ${student.studentCode}`}
                    >
                        {expanded ? <ChevronDown /> : <ChevronRight />}
                    </button>
                </td>

                <td>{student.studentCode}</td>

                <td>
                    {student.fullName}
                    {hasConflict && (
                        <AlertTriangle
                            className="import-conflict-icon"
                            aria-label="Có buổi khác điểm đã nhập"
                        />
                    )}
                </td>

                <td>{student.newSessions}</td>

                <td className="muted">{student.skippedSessions}</td>

                <td>
                    {student.starsToAward > 0 ? (
                        <span className="score-stars">
                            +{student.starsToAward} sao
                        </span>
                    ) : (
                        <span className="muted">0</span>
                    )}
                </td>
            </tr>

            {expanded && (
                <tr className="import-sessions">
                    <td colSpan={6}>
                        {student.sessions.length === 0 ? (
                            <p className="muted">Học sinh chưa có điểm.</p>
                        ) : (
                            <table>
                                <thead>
                                    <tr>
                                        <th>Ngày</th>
                                        <th>BTVN</th>
                                        <th>15p</th>
                                        <th>Điểm tính</th>
                                        <th>Sao</th>
                                        <th>Trạng thái</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {student.sessions.map((session) => (
                                        <tr
                                            key={session.assessmentDate}
                                            className={`status-${session.status.toLowerCase()}`}
                                        >
                                            <td>
                                                {formatDate(
                                                    session.assessmentDate,
                                                )}
                                            </td>

                                            <td>
                                                {formatScore(
                                                    session.homeworkScore,
                                                )}
                                            </td>

                                            <td>
                                                {formatScore(session.testScore)}
                                            </td>

                                            <td>
                                                <strong>
                                                    {formatScore(
                                                        session.finalScore,
                                                    )}
                                                </strong>
                                            </td>

                                            <td>
                                                {session.awardedStars > 0
                                                    ? `+${session.awardedStars}`
                                                    : "0"}
                                            </td>

                                            <td>
                                                <span className="import-status">
                                                    {
                                                        STATUS_LABELS[
                                                            session.status
                                                        ]
                                                    }
                                                </span>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </td>
                </tr>
            )}
        </>
    );
}
