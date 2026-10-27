import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Award, Minus, Plus, Search, Star, Users } from "lucide-react";
import { useMemo, useState } from "react";

import { AdjustmentModal } from "../../../features/star-transactions/components/AdjustmentModal";
import { StudentDrawer } from "../../../features/students/components/StudentDrawer";
import { apiClient } from "../../../lib/apiClient";

import type { ClassSummary } from "../../classes/types/class.types";
import { useStudents } from "../../students/hooks/useStudents";
import type { Student } from "../../students/types/student.types";
import type { Dashboard } from "../types/dashboard.types";

export function DashboardPage() {
    const qc = useQueryClient();
    const [classId, setClassId] = useState("");
    const [search, setSearch] = useState("");
    const [selected, setSelected] = useState<Student | null>(null);

    const [action, setAction] = useState<{
        student: Student;
        mode: "add" | "subtract";
    } | null>(null);

    const dashboard = useQuery({
        queryKey: ["dashboard"],
        queryFn: async () =>
            (await apiClient.get<Dashboard>("/dashboard")).data,
    });

    const classes = useQuery({
        queryKey: ["classes"],
        queryFn: async () =>
            (await apiClient.get<ClassSummary[]>("/classes")).data,
    });

    const students = useStudents(classId || undefined);

    const visible = useMemo(
        () =>
            students.data?.filter((s) =>
                (s.fullName + " " + s.studentCode)
                    .toLowerCase()
                    .includes(search.toLowerCase()),
            ) ?? [],
        [students.data, search],
    );

    const mutation = useMutation({
        mutationFn: async (v: {
            student: Student;
            stars: number;
            detail: string;
        }) =>
            (
                await apiClient.post(
                    `/students/${v.student.id}/stars/adjustments`,
                    {
                        classId: v.student.classId,
                        stars: v.stars,
                        reasonDetail: v.detail,
                    },
                    { headers: { "Idempotency-Key": crypto.randomUUID() } },
                )
            ).data,
        onSuccess: () => {
            setAction(null);
            qc.invalidateQueries({ queryKey: ["students"] });
            qc.invalidateQueries({ queryKey: ["dashboard"] });
            qc.invalidateQueries({ queryKey: ["classes"] });
        },
        onError: (e) => alert(e.message),
    });

    return (
        <div>
            <main>
                <h1>Tổng quan điểm thưởng</h1>

                <p className="muted">Dữ liệu được lấy từ sổ cái giao dịch</p>

                {dashboard.isLoading ? (
                    <p>Đang tải...</p>
                ) : dashboard.error ? (
                    <p className="error">{dashboard.error.message}</p>
                ) : (
                    <section className="cards">
                        <Card
                            icon={<Users />}
                            label="Tổng học sinh"
                            value={String(dashboard.data?.totalStudents ?? 0)}
                        />

                        <Card
                            icon={<Award />}
                            label="Tổng ngôi sao"
                            value={String(dashboard.data?.totalStars ?? 0)}
                        />
                    </section>
                )}
                <div className="layout">
                    <aside className="classes">
                        <h2>Lớp học</h2>
                        <button
                            className={!classId ? "active" : ""}
                            onClick={() => setClassId("")}
                        >
                            Tất cả lớp
                        </button>
                        {classes.data?.map((c) => (
                            <button
                                className={
                                    classId === c.classId ? "active" : ""
                                }
                                key={c.classId}
                                onClick={() => setClassId(c.classId)}
                            >
                                <strong>{c.className}</strong>
                                <span>
                                    {c.studentCount} học sinh · {c.totalStars}{" "}
                                    sao
                                </span>
                            </button>
                        ))}
                    </aside>
                    <section className="panel">
                        <div className="toolbar">
                            <div>
                                <h2>Danh sách học sinh</h2>
                                <span>{visible.length} kết quả</span>
                            </div>

                            <label className="search">
                                <Search />

                                <input
                                    value={search}
                                    onChange={(e) => setSearch(e.target.value)}
                                    placeholder="Tìm tên hoặc mã HS"
                                />
                            </label>
                        </div>
                        {students.isLoading ? (
                            <p className="pad">Đang tải...</p>
                        ) : students.error ? (
                            <p className="error pad">
                                {students.error.message}
                            </p>
                        ) : (
                            <div className="tableWrap">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>STT</th>
                                            <th>Học sinh</th>
                                            <th>Mã HS</th>
                                            <th>Ngôi sao</th>
                                            <th>Giá trị quy đổi</th>
                                            <th>Số sao có thể đổi</th>
                                            <th>Số sao còn dư sau khi đổi</th>
                                            <th />
                                        </tr>
                                    </thead>

                                    <tbody>
                                        {visible.length === 0 ? (
                                            <tr>
                                                <td
                                                    colSpan={7}
                                                    className="empty-cell"
                                                >
                                                    Không tìm thấy học sinh phù
                                                    hợp
                                                </td>
                                            </tr>
                                        ) : (
                                            visible.map((student, index) => (
                                                <tr key={student.id}>
                                                    <td>{index + 1}</td>

                                                    <td>
                                                        <button
                                                            type="button"
                                                            className="student"
                                                            onClick={() =>
                                                                setSelected(
                                                                    student,
                                                                )
                                                            }
                                                        >
                                                            {student.fullName}

                                                            <small>
                                                                {
                                                                    student.className
                                                                }
                                                            </small>
                                                        </button>
                                                    </td>

                                                    <td>
                                                        {student.studentCode}
                                                    </td>

                                                    <td>
                                                        <span className="stars">
                                                            <Star
                                                                size={14}
                                                                fill="currentColor"
                                                            />

                                                            {student.totalStars}
                                                        </span>
                                                    </td>

                                                    <td className="money">
                                                        {student.redeemableValue >
                                                        0
                                                            ? money(
                                                                  student.redeemableValue,
                                                              )
                                                            : "Chưa đủ 10 sao"}
                                                    </td>

                                                    <td>
                                                        <span className="stars">
                                                            <Star
                                                                size={14}
                                                                fill="currentColor"
                                                            />

                                                            {
                                                                student.redeemableStars
                                                            }
                                                        </span>
                                                    </td>

                                                    <td>
                                                        <span className="stars">
                                                            <Star
                                                                size={14}
                                                                fill="currentColor"
                                                            />

                                                            {
                                                                student.remainingStars
                                                            }
                                                        </span>
                                                    </td>

                                                    <td>
                                                        <div className="compact">
                                                            <button
                                                                type="button"
                                                                onClick={() =>
                                                                    setAction({
                                                                        student,
                                                                        mode: "add",
                                                                    })
                                                                }
                                                            >
                                                                <Plus />
                                                                Cộng
                                                            </button>

                                                            <button
                                                                type="button"
                                                                className="danger"
                                                                disabled={
                                                                    student.totalStars ===
                                                                    0
                                                                }
                                                                onClick={() =>
                                                                    setAction({
                                                                        student,
                                                                        mode: "subtract",
                                                                    })
                                                                }
                                                            >
                                                                <Minus />
                                                                Trừ
                                                            </button>
                                                        </div>
                                                    </td>
                                                </tr>
                                            ))
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </section>
                </div>
            </main>
            {action && (
                <AdjustmentModal
                    student={action.student}
                    mode={action.mode}
                    busy={mutation.isPending}
                    onClose={() => setAction(null)}
                    onSubmit={(stars, detail) =>
                        mutation.mutate({
                            student: action.student,
                            stars,
                            detail,
                        })
                    }
                />
            )}{" "}
            {selected && (
                <StudentDrawer
                    student={
                        visible.find((s) => s.id === selected.id) ?? selected
                    }
                    onClose={() => setSelected(null)}
                />
            )}
        </div>
    );
}

function Card({
    icon,
    label,
    value,
}: {
    icon: React.ReactNode;
    label: string;
    value: string;
}) {
    return (
        <article className="card">
            <div>
                <span>{label}</span>
                <strong>{value}</strong>
            </div>
            <i>{icon}</i>
        </article>
    );
}

const money = (v: number) => new Intl.NumberFormat("vi-VN").format(v) + " ₫";
