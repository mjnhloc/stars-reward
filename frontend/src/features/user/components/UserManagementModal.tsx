import { useState } from "react";

import { Pencil, UserPlus, X } from "lucide-react";

import { useClasses } from "../../classes/hooks/useClasses";
import { formatRole } from "../func/role";
import { useUsers } from "../hooks/useUsers";
import type { ManagedUser } from "../types/user.types";
import { UserFormModal } from "./UserFormModal";

type Props = {
    currentUserId: string;
    onClose: () => void;
};

// null: closed, "new": create form, otherwise the user being edited
type FormTarget = ManagedUser | "new" | null;

export function UserManagementModal({ currentUserId, onClose }: Props) {
    const users = useUsers();

    const classes = useClasses();

    const [formTarget, setFormTarget] = useState<FormTarget>(null);

    const classNames = new Map(
        classes.data?.map((classItem) => [
            classItem.classId,
            classItem.className,
        ]) ?? [],
    );

    return (
        <div className="overlay">
            <div
                className="modal user-management-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="users-modal-title"
            >
                <div className="row">
                    <div>
                        <h2 id="users-modal-title">Quản lý người dùng</h2>

                        <p className="muted">
                            Thêm tài khoản, đổi vai trò, khóa hoặc đặt lại mật
                            khẩu
                        </p>
                    </div>

                    <button
                        type="button"
                        className="icon"
                        onClick={onClose}
                        aria-label="Đóng"
                    >
                        <X />
                    </button>
                </div>

                <div className="user-management-toolbar">
                    <span className="muted">
                        {users.data ? `${users.data.length} người dùng` : ""}
                    </span>

                    <button
                        type="button"
                        className="primary-action"
                        onClick={() => setFormTarget("new")}
                    >
                        <UserPlus />
                        Thêm người dùng
                    </button>
                </div>

                {users.isLoading ? (
                    <p className="muted">Đang tải danh sách người dùng...</p>
                ) : users.error ? (
                    <p className="error">{users.error.message}</p>
                ) : (
                    <div className="score-list">
                        {users.data?.map((user) => (
                            <article
                                key={user.id}
                                className={`score-item ${user.active ? "" : "reverted"}`}
                            >
                                <div className="score-item-header">
                                    <div>
                                        <strong>
                                            {user.displayName}
                                            {user.id === currentUserId &&
                                                " (bạn)"}
                                        </strong>

                                        <span>{user.email}</span>
                                    </div>

                                    <div className="user-badges">
                                        <span className="user-role-badge">
                                            {formatRole(user.role)}
                                        </span>

                                        <span
                                            className={`import-batch-status ${user.active ? "completed" : "reverted"}`}
                                        >
                                            {user.active
                                                ? "Hoạt động"
                                                : "Đã khóa"}
                                        </span>
                                    </div>
                                </div>

                                <div className="import-batch-footer">
                                    <p className="transaction-meta">
                                        {user.role === "TEACHER"
                                            ? user.classIds.length > 0
                                                ? `Phụ trách: ${user.classIds
                                                      .map(
                                                          (id) =>
                                                              classNames.get(
                                                                  id,
                                                              ) ?? "lớp đã xóa",
                                                      )
                                                      .join(", ")}`
                                                : "Chưa phụ trách lớp nào"
                                            : user.role === "ADMIN"
                                              ? "Toàn quyền quản trị"
                                              : "Chỉ xem dữ liệu"}
                                    </p>

                                    <button
                                        type="button"
                                        className="secondary-action"
                                        onClick={() => setFormTarget(user)}
                                    >
                                        <Pencil />
                                        Sửa
                                    </button>
                                </div>
                            </article>
                        ))}
                    </div>
                )}

                <div className="actions">
                    <button type="button" onClick={onClose}>
                        Đóng
                    </button>
                </div>
            </div>

            {formTarget && (
                <UserFormModal
                    user={formTarget === "new" ? null : formTarget}
                    isSelf={
                        formTarget !== "new" && formTarget.id === currentUserId
                    }
                    classes={classes.data ?? []}
                    onClose={() => setFormTarget(null)}
                />
            )}
        </div>
    );
}
