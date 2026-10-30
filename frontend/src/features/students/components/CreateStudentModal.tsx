import { SubmitEvent, useState } from "react";

import { X } from "lucide-react";

import { useCreateStudent } from "../hooks/useCreateStudent";

import type { ClassSummary } from "../../classes/types/class.types";

type Props = {
    classes: ClassSummary[];
    selectedClassId?: string;
    onClose: () => void;
};

export function CreateStudentModal({
    classes,
    selectedClassId,
    onClose,
}: Props) {
    const [studentCode, setStudentCode] = useState("");

    const [fullName, setFullName] = useState("");

    const [classId, setClassId] = useState(selectedClassId ?? "");

    const [startedAt, setStartedAt] = useState(
        new Date().toISOString().slice(0, 10),
    );

    const mutation = useCreateStudent();

    async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
        event.preventDefault();

        try {
            await mutation.mutateAsync({
                studentCode: studentCode.trim(),
                fullName: fullName.trim(),
                classId,
                startedAt,
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
                    <h2>Thêm học sinh</h2>

                    <button
                        type="button"
                        className="icon"
                        onClick={onClose}
                        disabled={mutation.isPending}
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
                        required
                    >
                        <option value="">Chọn lớp học</option>

                        {classes.map((classItem) => (
                            <option
                                key={classItem.classId}
                                value={classItem.classId}
                            >
                                {classItem.className}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    Ngày bắt đầu học
                    <input
                        type="date"
                        value={startedAt}
                        onChange={(event) => setStartedAt(event.target.value)}
                        required
                    />
                </label>

                {mutation.error && (
                    <p className="error">{mutation.error.message}</p>
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
                            !studentCode.trim() ||
                            !fullName.trim() ||
                            !classId
                        }
                    >
                        {mutation.isPending ? "Đang thêm..." : "Thêm học sinh"}
                    </button>
                </div>
            </form>
        </div>
    );
}
