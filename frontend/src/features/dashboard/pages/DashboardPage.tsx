import {
    Award,
    FolderPlus,
    Gift,
    Minus,
    Plus,
    Search,
    Star,
    UserPlus,
    Users,
} from "lucide-react";
import { useMemo, useState } from "react";

import { AdjustmentModal } from "../../../features/star-transactions/components/AdjustmentModal";
import { StudentDrawer } from "../../../features/students/components/StudentDrawer";

import { money } from "../../../common/func/common.func";
import { Card } from "../../../common/ui/Card";
import { useAuth } from "../../auth/hooks/useAuth";
import { CreateClassModal } from "../../classes/components/CreateClassModal";
import { useClasses } from "../../classes/hooks/useClasses";
import { RewardRedemptionModal } from "../../reward/components/RewardRedemptionModal";
import { useRedeemStudentReward } from "../../reward/hooks/useRedeemStudentReward";
import { CreateStudentModal } from "../../students/components/CreateStudentModal";
import { useAdjustStudentStars } from "../../students/hooks/useAdjustStudentStars";
import { useStudents } from "../../students/hooks/useStudents";
import type { Student } from "../../students/types/student.types";
import { useDashboard } from "../hooks/useDashboard";

export function DashboardPage() {
    const [classId, setClassId] = useState("");

    const [search, setSearch] = useState("");

    const [selectedStudent, setSelectedStudent] = useState<Student | null>(
        null,
    );

    const [adjustmentAction, setAdjustmentAction] = useState<{
        student: Student;
        mode: "add" | "subtract";
    } | null>(null);

    const [redemptionStudent, setRedemptionStudent] = useState<Student | null>(
        null,
    );

    const dashboard = useDashboard();

    const classes = useClasses();

    const students = useStudents(classId || undefined);

    const adjustmentMutation = useAdjustStudentStars();

    const rewardMutation = useRedeemStudentReward();

    const visible = useMemo(
        () =>
            students.data?.filter((student) =>
                (student.fullName + " " + student.studentCode)
                    .toLowerCase()
                    .includes(search.toLowerCase()),
            ) ?? [],
        [students.data, search],
    );

    const { user } = useAuth();
    const isAdmin = user?.role === "ADMIN";

    const [isCreateClassOpen, setIsCreateClassOpen] = useState(false);

    const [isCreateStudentOpen, setIsCreateStudentOpen] = useState(false);

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

                            <div className="toolbar-actions">
                                {isAdmin && (
                                    <>
                                        <button
                                            type="button"
                                            className="secondary-action"
                                            onClick={() =>
                                                setIsCreateClassOpen(true)
                                            }
                                        >
                                            <FolderPlus />
                                            Thêm lớp
                                        </button>

                                        <button
                                            type="button"
                                            className="primary-action"
                                            onClick={() =>
                                                setIsCreateStudentOpen(true)
                                            }
                                        >
                                            <UserPlus />
                                            Thêm học sinh
                                        </button>
                                    </>
                                )}

                                <label className="search">
                                    <Search />

                                    <input
                                        value={search}
                                        onChange={(e) =>
                                            setSearch(e.target.value)
                                        }
                                        placeholder="Tìm tên hoặc mã HS"
                                    />
                                </label>
                            </div>
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
                                                                setSelectedStudent(
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
                                                                    setAdjustmentAction(
                                                                        {
                                                                            student,
                                                                            mode: "add",
                                                                        },
                                                                    )
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
                                                                    setAdjustmentAction(
                                                                        {
                                                                            student,
                                                                            mode: "subtract",
                                                                        },
                                                                    )
                                                                }
                                                            >
                                                                <Minus />
                                                                Trừ
                                                            </button>

                                                            <button
                                                                type="button"
                                                                className="redeem-button"
                                                                disabled={
                                                                    student.redeemableStars <
                                                                    10
                                                                }
                                                                title={
                                                                    student.redeemableStars <
                                                                    10
                                                                        ? "Học sinh chưa đủ 10 sao"
                                                                        : "Đổi thưởng"
                                                                }
                                                                onClick={() =>
                                                                    setRedemptionStudent(
                                                                        student,
                                                                    )
                                                                }
                                                            >
                                                                <Gift />
                                                                Đổi thưởng
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
            {/* Modal changing student stars */}
            {adjustmentAction && (
                <AdjustmentModal
                    student={adjustmentAction.student}
                    mode={adjustmentAction.mode}
                    busy={adjustmentMutation.isPending}
                    onClose={() => {
                        if (!adjustmentMutation.isPending) {
                            setAdjustmentAction(null);
                        }
                        adjustmentMutation.reset();
                    }}
                    onSubmit={(stars, detail) => {
                        adjustmentMutation.mutate(
                            {
                                studentId: adjustmentAction.student.id,
                                classId: adjustmentAction.student.classId,
                                stars,
                                reasonDetail: detail,
                            },
                            {
                                onSuccess: () => {
                                    setAdjustmentAction(null);
                                },
                            },
                        );
                    }}
                />
            )}{" "}
            {/* reward redemption */}
            {redemptionStudent && (
                <RewardRedemptionModal
                    student={redemptionStudent}
                    busy={rewardMutation.isPending}
                    errorMessage={rewardMutation.error?.message}
                    onClose={() => {
                        if (!rewardMutation.isPending) {
                            setRedemptionStudent(null);
                            rewardMutation.reset();
                        }
                    }}
                    onConfirm={() => {
                        rewardMutation.mutate(
                            {
                                studentId: redemptionStudent.id,
                                classId: redemptionStudent.classId,
                            },
                            {
                                onSuccess: () => {
                                    setRedemptionStudent(null);
                                },
                            },
                        );
                    }}
                />
            )}{" "}
            {/* show student details */}
            {selectedStudent && (
                <StudentDrawer
                    student={
                        visible.find(
                            (student) => student.id === selectedStudent.id,
                        ) ?? selectedStudent
                    }
                    onClose={() => setSelectedStudent(null)}
                />
            )}
            {/* create a new class modal */}
            {isCreateClassOpen && (
                <CreateClassModal onClose={() => setIsCreateClassOpen(false)} />
            )}
            {/* create a new student modal */}
            {isCreateStudentOpen && (
                <CreateStudentModal
                    classes={classes.data ?? []}
                    selectedClassId={classId || undefined}
                    onClose={() => setIsCreateStudentOpen(false)}
                />
            )}
        </div>
    );
}
