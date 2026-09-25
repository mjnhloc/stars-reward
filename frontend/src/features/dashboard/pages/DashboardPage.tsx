import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Award, Search, Users, Plus, Minus, Star } from "lucide-react";

import { apiClient } from "../../../lib/apiClient";
import { AdjustmentModal } from "../../../features/star-transactions/components/AdjustmentModal";
import { StudentDrawer } from "../../../features/students/components/StudentDrawer";

import type { ClassSummary } from "../../classes/types/class.types";
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
    const students = useQuery({
        queryKey: ["students", classId],
        queryFn: async () =>
            (
                await apiClient.get<Student[]>("/students", {
                    params: { classId: classId || undefined },
                })
            ).data,
    });
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
            reasonCode: string;
            detail: string;
        }) =>
            (
                await apiClient.post(
                    `/students/${v.student.id}/stars/adjustments`,
                    {
                        classId: v.student.classId,
                        stars: v.stars,
                        reasonCode: v.reasonCode,
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
            <header>
                <div className="brand">
                    <Star fill="currentColor" />{" "}
                    <div>
                        <b>Stars reward</b>
                        <small>Quản lý điểm thưởng học sinh</small>
                    </div>
                </div>
                <span className="role">ADMIN</span>
            </header>
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
                                            <th>Điểm BTVN</th>
                                            <th>Điểm KT</th>
                                            <th>Điểm tính</th>
                                            <th>Ngôi sao</th>
                                            <th>Giá trị quy đổi</th>
                                            <th />
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {visible.map((s, i) => (
                                            <tr key={s.id}>
                                                <td>{i + 1}</td>
                                                <td>
                                                    <button
                                                        className="student"
                                                        onClick={() =>
                                                            setSelected(s)
                                                        }
                                                    >
                                                        {s.fullName}
                                                        <small>
                                                            {s.className}
                                                        </small>
                                                    </button>
                                                </td>
                                                <td>{s.studentCode}</td>
                                                <td>
                                                    {s.homeworkScore ?? "–"}
                                                </td>
                                                <td>{s.testScore ?? "–"}</td>
                                                <td>{s.finalScore ?? "–"}</td>
                                                <td>
                                                    <span className="stars">
                                                        <Star
                                                            size={14}
                                                            fill="currentColor"
                                                        />
                                                        {s.totalStars}
                                                    </span>
                                                </td>
                                                <td className="money">
                                                    {money(s.redeemableValue)}
                                                </td>
                                                <td>
                                                    <div className="compact">
                                                        <button
                                                            onClick={() =>
                                                                setAction({
                                                                    student: s,
                                                                    mode: "add",
                                                                })
                                                            }
                                                        >
                                                            <Plus />
                                                            Cộng
                                                        </button>
                                                        <button
                                                            className="danger"
                                                            onClick={() =>
                                                                setAction({
                                                                    student: s,
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
                                        ))}
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
                    onSubmit={(stars, reasonCode, detail) =>
                        mutation.mutate({
                            student: action.student,
                            stars,
                            reasonCode,
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
