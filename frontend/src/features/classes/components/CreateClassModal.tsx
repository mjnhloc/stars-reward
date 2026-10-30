import { type FormEvent, useMemo, useState } from "react";

import { X } from "lucide-react";

import { useCreateClass } from "../hooks/useCreateClass";

type Props = {
    onClose: () => void;
};

const CURRENT_YEAR = new Date().getFullYear();

export function CreateClassModal({ onClose }: Props) {
    const [code, setCode] = useState("");
    const [name, setName] = useState("");

    const [schoolYear, setSchoolYear] = useState(String(CURRENT_YEAR));

    const mutation = useCreateClass();

    const schoolYearOptions = useMemo(
        () =>
            Array.from({ length: 20 }, (_, index) => CURRENT_YEAR - 10 + index),
        [],
    );

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        try {
            await mutation.mutateAsync({
                code: code.trim(),
                name: name.trim(),
                schoolYear: schoolYear,
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
                    <h2>Thêm lớp học</h2>

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
                            <option key={startYear} value={startYear}>
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
                            !code.trim() ||
                            !name.trim() ||
                            !schoolYear
                        }
                    >
                        {mutation.isPending ? "Đang tạo..." : "Tạo lớp"}
                    </button>
                </div>
            </form>
        </div>
    );
}
