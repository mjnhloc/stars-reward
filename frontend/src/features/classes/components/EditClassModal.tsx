import { type FormEvent, useMemo, useState } from "react";

import { X } from "lucide-react";

import { useClassDetail } from "../hooks/useClassDetail";
import { useUpdateClass } from "../hooks/useUpdateClass";
import type { ClassDetail } from "../types/class.types";

type Props = {
    classId: string;
    onClose: () => void;
};

const CURRENT_YEAR = new Date().getFullYear();

export function EditClassModal({ classId, onClose }: Props) {
    const detail = useClassDetail(classId);

    if (!detail.data) {
        return (
            <div className="overlay">
                <div className="modal">
                    <div className="row">
                        <h2>Sửa lớp học</h2>

                        <button
                            type="button"
                            className="icon"
                            onClick={onClose}
                            aria-label="Đóng"
                        >
                            <X />
                        </button>
                    </div>

                    {detail.error ? (
                        <p className="error" role="alert">
                            {detail.error.message}
                        </p>
                    ) : (
                        <p className="muted">Đang tải...</p>
                    )}
                </div>
            </div>
        );
    }

    return <EditClassForm classDetail={detail.data} onClose={onClose} />;
}

type FormProps = {
    classDetail: ClassDetail;
    onClose: () => void;
};

function EditClassForm({ classDetail, onClose }: FormProps) {
    const [code, setCode] = useState(classDetail.code);
    const [name, setName] = useState(classDetail.name);

    const [schoolYear, setSchoolYear] = useState(classDetail.schoolYear);

    const mutation = useUpdateClass();

    const schoolYearOptions = useMemo(() => {
        const years = Array.from(
            { length: 20 },
            (_, index) => CURRENT_YEAR - 10 + index,
        );

        // giữ năm học hiện tại của lớp nếu nằm ngoài khoảng mặc định
        const currentStartYear = Number(classDetail.schoolYear);
        if (
            Number.isInteger(currentStartYear) &&
            !years.includes(currentStartYear)
        ) {
            years.push(currentStartYear);
            years.sort((a, b) => a - b);
        }

        return years;
    }, [classDetail.schoolYear]);

    const isUnchanged =
        code.trim().toUpperCase() === classDetail.code &&
        name.trim() === classDetail.name &&
        schoolYear === classDetail.schoolYear;

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        try {
            await mutation.mutateAsync({
                classId: classDetail.id,
                request: {
                    code: code.trim(),
                    name: name.trim(),
                    schoolYear: schoolYear,
                },
            });

            onClose();
        } catch {
            // mutation.error được hiển thị bên dưới.
        }
    }

    return (
        <div className="overlay">
            <form className="modal" onSubmit={handleSubmit}>
                <div className="row">
                    <h2>Sửa lớp học</h2>

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
                    Mã lớp
                    <input
                        value={code}
                        onChange={(event) => setCode(event.target.value)}
                        placeholder="Ví dụ: 7A"
                        maxLength={50}
                        required
                    />
                </label>

                <label>
                    Tên lớp
                    <input
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                        placeholder="Ví dụ: Lớp 7A"
                        maxLength={150}
                        required
                    />
                </label>

                <label>
                    Năm học bắt đầu
                    <select
                        value={schoolYear}
                        onChange={(event) => setSchoolYear(event.target.value)}
                        disabled={mutation.isPending}
                        required
                    >
                        {schoolYearOptions.map((startYear) => (
                            <option key={startYear} value={String(startYear)}>
                                {startYear} - {startYear + 1}
                            </option>
                        ))}
                    </select>
                </label>

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
                            !code.trim() ||
                            !name.trim() ||
                            !schoolYear
                        }
                    >
                        {mutation.isPending ? "Đang lưu..." : "Lưu thay đổi"}
                    </button>
                </div>
            </form>
        </div>
    );
}
