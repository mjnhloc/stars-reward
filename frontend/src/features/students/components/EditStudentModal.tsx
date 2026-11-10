import { type FormEvent, useState } from "react";

import { Info, X } from "lucide-react";

import { useUpdateStudent } from "../hooks/useUpdateStudent";

import {
    formatDate,
    todayInputValue,
} from "../../../common/func/common.func";
import type { ClassSummary } from "../../classes/types/class.types";
import type { Student } from "../types/student.types";

type Props = {
    student: Student;
    classes: ClassSummary[];
    onClose: () => void;
};

export function EditStudentModal({ student, classes, onClose }: Props) {
    const [studentCode, setStudentCode] = useState(student.studentCode);

    const [fullName, setFullName] = useState(student.fullName);

    const [classId, setClassId] = useState(student.classId);

    const [startedAt, setStartedAt] = useState(student.startedAt);

    const mutation = useUpdateStudent();

    const today = todayInputValue();

    const isClassChanged = classId !== student.classId;

    const isUnchanged =
        studentCode.trim().toUpperCase() === student.studentCode &&
        fullName.trim() === student.fullName &&
        !isClassChanged &&
        startedAt === student.startedAt;

    function handleClassChange(nextClassId: string) {
        setClassId(nextClassId);

        // moving to another class starts today by default, back to the current class keeps its date
        setStartedAt(nextClassId === student.classId ? student.startedAt : today);
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        try {
            await mutation.mutateAsync({
                studentId: student.id,
                request: {
                    studentCode: studentCode.trim(),
                    fullName: fullName.trim(),
                    classId,
                    startedAt,
                },
            });

            onClose();
        } catch {
            // mutation.error hiển thị trong form
        }
    }

    return (
        <div className="overlay">
            <form className="modal" onSubmit={handleSubmit}>
                <div className="row">
                    <h2>Sửa học sinh</h2>

                    <button
                        type="button"
                        className="icon"
                        onClick={onClose}
                        disabled={mutation.isPending}
                        aria-label="Đóng"
                    >
                        <X />
                    </button>
                </div>

                <label>
                    Mã học sinh
                    <input
                        value={studentCode}
                        onChange={(event) => setStudentCode(event.target.value)}
                        placeholder="Ví dụ: HS001"
                        maxLength={50}
                        required
                    />
                </label>

                <label>
                    Họ và tên
                    <input
                        value={fullName}
                        onChange={(event) => setFullName(event.target.value)}
                        placeholder="Nhập họ và tên"
                        maxLength={200}
                        required
                    />
                </label>

                <label>
                    Lớp học
                    <select
                        value={classId}
                        onChange={(event) =>
                            handleClassChange(event.target.value)
                        }
                        disabled={mutation.isPending}
                        required
                    >
                        {classes.map((classItem) => (
                            <option
                                key={classItem.classId}
                                value={classItem.classId}
                            >
                                {classItem.className}
                                {classItem.classId === student.classId
                                    ? " (lớp hiện tại)"
                                    : ""}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    {isClassChanged ? "Ngày vào lớp mới" : "Ngày bắt đầu học"}
                    <input
                        type="date"
                        value={startedAt}
                        max={today}
                        onChange={(event) => setStartedAt(event.target.value)}
                        disabled={mutation.isPending}
                        required
                    />
                </label>

                {isClassChanged && (
                    <div className="redemption-notice">
                        <Info />

                        <span>
                            {startedAt > student.startedAt
                                ? `Học sinh sẽ được chuyển sang lớp mới từ ngày ${startedAt ? formatDate(startedAt) : ""}.`
                                : "Học sinh được tính là học lớp mới ngay từ ngày bắt đầu học."}{" "}
                            Số sao ({student.totalStars}) và lịch sử giao dịch
                            được giữ nguyên.
                        </span>
                    </div>
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
                        onClick={onClose}
                        disabled={mutation.isPending}
                    >
                        Hủy
                    </button>

                    <button
                        type="submit"
                        disabled={
                            mutation.isPending ||
                            isUnchanged ||
                            !studentCode.trim() ||
                            !fullName.trim() ||
                            !classId ||
                            !startedAt
                        }
                    >
                        {mutation.isPending ? "Đang lưu..." : "Lưu thay đổi"}
                    </button>
                </div>
            </form>
        </div>
    );
}
