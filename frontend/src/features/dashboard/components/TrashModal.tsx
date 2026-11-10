import { useState } from "react";

import { RotateCcw, Trash2, X } from "lucide-react";

import { formatDate } from "../../../common/func/common.func";
import { ConfirmDeleteModal } from "../../../common/ui/ConfirmDeleteModal";
import { useClassTrash } from "../../classes/hooks/useClassTrash";
import { usePermanentDeleteClass } from "../../classes/hooks/usePermanentDeleteClass";
import { useRestoreClass } from "../../classes/hooks/useRestoreClass";
import type { ClassSummary, ClassTrash } from "../../classes/types/class.types";
import { usePermanentDeleteStudent } from "../../students/hooks/usePermanentDeleteStudent";
import { useRestoreStudent } from "../../students/hooks/useRestoreStudent";
import { useStudentTrash } from "../../students/hooks/useStudentTrash";
import type { StudentTrash } from "../../students/types/student.types";

type Props = {
    // active classes, a student whose old class was deleted is restored into one of them
    classes: ClassSummary[];
    onClose: () => void;
};

type TrashTab = "students" | "classes";

type PermanentDelete =
    | { kind: "student"; item: StudentTrash }
    | { kind: "class"; item: ClassTrash };

export function TrashModal({ classes, onClose }: Props) {
    const [activeTab, setActiveTab] = useState<TrashTab>("students");

    // class chosen for every student whose old class was deleted
    const [targetClass, setTargetClass] = useState<Record<string, string>>({});

    const [deleting, setDeleting] = useState<PermanentDelete | null>(null);

    const [confirmCode, setConfirmCode] = useState("");

    const studentTrash = useStudentTrash();

    const classTrash = useClassTrash();

    const restoreStudent = useRestoreStudent();

    const restoreClass = useRestoreClass();

    const deleteStudent = usePermanentDeleteStudent();

    const deleteClass = usePermanentDeleteClass();

    const busy =
        restoreStudent.isPending ||
        restoreClass.isPending ||
        deleteStudent.isPending ||
        deleteClass.isPending;

    const restoreError = restoreStudent.error ?? restoreClass.error;

    const deleteError = deleteStudent.error ?? deleteClass.error;

    const deletingCode =
        deleting?.kind === "student"
            ? deleting.item.studentCode
            : (deleting?.item.code ?? "");

    function openDelete(target: PermanentDelete) {
        deleteStudent.reset();
        deleteClass.reset();
        setConfirmCode("");
        setDeleting(target);
    }

    function confirmDelete() {
        if (!deleting) {
            return;
        }

        const options = { onSuccess: () => setDeleting(null) };

        if (deleting.kind === "student") {
            deleteStudent.mutate(deleting.item.id, options);
        } else {
            deleteClass.mutate(deleting.item.id, options);
        }
    }

    function handleRestoreStudent(student: StudentTrash) {
        restoreClass.reset();

        restoreStudent.mutate({
            studentId: student.id,
            request: student.lastClassArchived
                ? { classId: targetClass[student.id] }
                : {},
        });
    }

    function handleRestoreClass(classItem: ClassTrash) {
        restoreStudent.reset();
        restoreClass.mutate(classItem.id);
    }

    function handleClose() {
        if (!busy) {
            onClose();
        }
    }

    return (
        <div className="overlay">
            <div
                className="modal trash-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="trash-modal-title"
            >
                <div className="row">
                    <div>
                        <h2 id="trash-modal-title">Thùng rác</h2>

                        <p className="muted">
                            Học sinh và lớp đã xóa, có thể khôi phục hoặc xóa
                            vĩnh viễn
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
                        aria-selected={activeTab === "students"}
                        className={activeTab === "students" ? "active" : ""}
                        onClick={() => setActiveTab("students")}
                    >
                        Học sinh
                        {studentTrash.data && ` (${studentTrash.data.length})`}
                    </button>

                    <button
                        type="button"
                        role="tab"
                        aria-selected={activeTab === "classes"}
                        className={activeTab === "classes" ? "active" : ""}
                        onClick={() => setActiveTab("classes")}
                    >
                        Lớp
                        {classTrash.data && ` (${classTrash.data.length})`}
                    </button>
                </div>

                {restoreError && (
                    <p className="error" role="alert">
                        {restoreError.message}
                    </p>
                )}

                {activeTab === "students" ? (
                    studentTrash.isLoading ? (
                        <p className="muted">Đang tải thùng rác...</p>
                    ) : studentTrash.error ? (
                        <p className="error">{studentTrash.error.message}</p>
                    ) : !studentTrash.data?.length ? (
                        <div className="empty-state">
                            Không có học sinh nào trong thùng rác.
                        </div>
                    ) : (
                        <div className="score-list trash-list">
                            {studentTrash.data.map((student) => {
                                const needsClass = student.lastClassArchived;

                                return (
                                    <article
                                        key={student.id}
                                        className="score-item"
                                    >
                                        <div className="score-item-header">
                                            <div>
                                                <strong>
                                                    {student.studentCode} ·{" "}
                                                    {student.fullName}
                                                </strong>

                                                <span>
                                                    Xóa lúc{" "}
                                                    {formatDate(
                                                        student.archivedAt,
                                                        true,
                                                    )}
                                                    {student.lastClassName &&
                                                        ` · Lớp cũ: ${student.lastClassName}`}
                                                    {needsClass &&
                                                        " (đã xóa)"}
                                                </span>
                                            </div>

                                            <span className="score-stars">
                                                {student.totalStars} sao
                                            </span>
                                        </div>

                                        <div className="trash-actions">
                                            {needsClass && (
                                                <select
                                                    value={
                                                        targetClass[
                                                            student.id
                                                        ] ?? ""
                                                    }
                                                    onChange={(event) =>
                                                        setTargetClass(
                                                            (current) => ({
                                                                ...current,
                                                                [student.id]:
                                                                    event.target
                                                                        .value,
                                                            }),
                                                        )
                                                    }
                                                    disabled={busy}
                                                    aria-label={`Lớp khôi phục cho ${student.studentCode}`}
                                                >
                                                    <option value="">
                                                        Chọn lớp để khôi phục
                                                    </option>

                                                    {classes.map(
                                                        (classItem) => (
                                                            <option
                                                                key={
                                                                    classItem.classId
                                                                }
                                                                value={
                                                                    classItem.classId
                                                                }
                                                            >
                                                                {
                                                                    classItem.className
                                                                }
                                                            </option>
                                                        ),
                                                    )}
                                                </select>
                                            )}

                                            <button
                                                type="button"
                                                className="secondary-action"
                                                disabled={
                                                    busy ||
                                                    (needsClass &&
                                                        !targetClass[
                                                            student.id
                                                        ])
                                                }
                                                onClick={() =>
                                                    handleRestoreStudent(
                                                        student,
                                                    )
                                                }
                                            >
                                                <RotateCcw />
                                                Khôi phục
                                            </button>

                                            <button
                                                type="button"
                                                className="secondary-action revert-action"
                                                disabled={busy}
                                                onClick={() =>
                                                    openDelete({
                                                        kind: "student",
                                                        item: student,
                                                    })
                                                }
                                            >
                                                <Trash2 />
                                                Xóa vĩnh viễn
                                            </button>
                                        </div>
                                    </article>
                                );
                            })}
                        </div>
                    )
                ) : classTrash.isLoading ? (
                    <p className="muted">Đang tải thùng rác...</p>
                ) : classTrash.error ? (
                    <p className="error">{classTrash.error.message}</p>
                ) : !classTrash.data?.length ? (
                    <div className="empty-state">
                        Không có lớp nào trong thùng rác.
                    </div>
                ) : (
                    <div className="score-list trash-list">
                        {classTrash.data.map((classItem) => (
                            <article key={classItem.id} className="score-item">
                                <div className="score-item-header">
                                    <div>
                                        <strong>
                                            {classItem.code} · {classItem.name}
                                        </strong>

                                        <span>
                                            Năm học {classItem.schoolYear} · Xóa
                                            lúc{" "}
                                            {formatDate(
                                                classItem.archivedAt,
                                                true,
                                            )}
                                        </span>
                                    </div>

                                    {classItem.studentCount > 0 && (
                                        <span className="transaction-meta">
                                            {classItem.studentCount} học sinh
                                            trong thùng rác
                                        </span>
                                    )}
                                </div>

                                <div className="trash-actions">
                                    <button
                                        type="button"
                                        className="secondary-action"
                                        disabled={busy}
                                        onClick={() =>
                                            handleRestoreClass(classItem)
                                        }
                                    >
                                        <RotateCcw />
                                        Khôi phục
                                    </button>

                                    <button
                                        type="button"
                                        className="secondary-action revert-action"
                                        disabled={
                                            busy || classItem.studentCount > 0
                                        }
                                        title={
                                            classItem.studentCount > 0
                                                ? "Hãy khôi phục hoặc xóa vĩnh viễn các học sinh của lớp trước"
                                                : undefined
                                        }
                                        onClick={() =>
                                            openDelete({
                                                kind: "class",
                                                item: classItem,
                                            })
                                        }
                                    >
                                        <Trash2 />
                                        Xóa vĩnh viễn
                                    </button>
                                </div>
                            </article>
                        ))}
                    </div>
                )}

                <div className="actions">
                    <button type="button" onClick={handleClose} disabled={busy}>
                        Đóng
                    </button>
                </div>
            </div>

            {deleting && (
                <ConfirmDeleteModal
                    title={
                        deleting.kind === "student"
                            ? "Xóa vĩnh viễn học sinh"
                            : "Xóa vĩnh viễn lớp"
                    }
                    message={
                        <>
                            {deleting.kind === "student" ? (
                                <>
                                    Xóa vĩnh viễn{" "}
                                    <strong>
                                        {deleting.item.studentCode} ·{" "}
                                        {deleting.item.fullName}
                                    </strong>{" "}
                                    cùng toàn bộ điểm, giao dịch sao, lịch sử
                                    đổi thưởng và số dư ({deleting.item.totalStars}{" "}
                                    sao).
                                </>
                            ) : (
                                <>
                                    Xóa vĩnh viễn lớp{" "}
                                    <strong>
                                        {deleting.item.code} ·{" "}
                                        {deleting.item.name}
                                    </strong>
                                    . Học sinh từng học lớp này nhưng đã chuyển
                                    sang lớp khác vẫn giữ điểm và số sao, chỉ
                                    không còn hiện tên lớp.
                                </>
                            )}

                            <label className="trash-confirm">
                                Gõ lại mã <strong>{deletingCode}</strong> để xác
                                nhận
                                <input
                                    value={confirmCode}
                                    onChange={(event) =>
                                        setConfirmCode(event.target.value)
                                    }
                                    disabled={busy}
                                    autoFocus
                                />
                            </label>
                        </>
                    }
                    warning="Thao tác này không thể hoàn tác."
                    confirmLabel="Xóa vĩnh viễn"
                    busy={deleteStudent.isPending || deleteClass.isPending}
                    disabled={
                        confirmCode.trim().toUpperCase() !==
                        deletingCode.toUpperCase()
                    }
                    errorMessage={deleteError?.message}
                    onClose={() => setDeleting(null)}
                    onConfirm={confirmDelete}
                />
            )}
        </div>
    );
}
