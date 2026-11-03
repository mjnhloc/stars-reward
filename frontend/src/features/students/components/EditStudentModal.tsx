import { type FormEvent, useState } from "react";

import { Info, X } from "lucide-react";

import { useUpdateStudent } from "../hooks/useUpdateStudent";

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

    const mutation = useUpdateStudent();

    const isClassChanged = classId !== student.classId;

    const isUnchanged =
        studentCode.trim().toUpperCase() === student.studentCode &&
        fullName.trim() === student.fullName &&
        !isClassChanged;

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        try {
            await mutation.mutateAsync({
                studentId: student.id,
                request: {
                    studentCode: studentCode.trim(),
                    fullName: fullName.trim(),
                    classId,
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
                        onChange={(event) => setClassId(event.target.value)}
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

                {isClassChanged && (
                    <div className="redemption-notice">
                        <Info />

                        <span>
                            Học sinh sẽ được chuyển sang lớp mới từ hôm nay. Số
                            sao ({student.totalStars}) và lịch sử giao dịch được
                            giữ nguyên.
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
                            !classId
                        }
                    >
                        {mutation.isPending ? "Đang lưu..." : "Lưu thay đổi"}
                    </button>
                </div>
            </form>
        </div>
    );
}
