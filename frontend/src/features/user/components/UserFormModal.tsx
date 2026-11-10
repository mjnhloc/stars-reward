import { type FormEvent, useState } from "react";

import { Info, X } from "lucide-react";

import type { UserRole } from "../../auth/types/auth.types";
import type { ClassSummary } from "../../classes/types/class.types";
import { ROLE_LABELS } from "../func/role";
import { useCreateUser } from "../hooks/useCreateUser";
import { useUpdateUser } from "../hooks/useUpdateUser";
import type { ManagedUser } from "../types/user.types";

type Props = {
    // null to create a new user
    user: ManagedUser | null;
    isSelf: boolean;
    classes: ClassSummary[];
    onClose: () => void;
};

const MIN_PASSWORD_LENGTH = 8;

export function UserFormModal({ user, isSelf, classes, onClose }: Props) {
    const isNew = user === null;

    const [displayName, setDisplayName] = useState(user?.displayName ?? "");

    const [email, setEmail] = useState(user?.email ?? "");

    const [role, setRole] = useState<UserRole>(user?.role ?? "TEACHER");

    const [active, setActive] = useState(user?.active ?? true);

    const [password, setPassword] = useState("");

    const [classIds, setClassIds] = useState<string[]>(user?.classIds ?? []);

    const createMutation = useCreateUser();

    const updateMutation = useUpdateUser();

    const mutation = isNew ? createMutation : updateMutation;

    const passwordTooShort =
        password.length > 0 && password.length < MIN_PASSWORD_LENGTH;

    const invalid =
        !displayName.trim() ||
        !email.trim() ||
        passwordTooShort ||
        (isNew && !password);

    // these changes log the user out of every device
    const logsOut =
        user !== null &&
        (role !== user.role || (user.active && !active) || !!password);

    function toggleClass(classId: string) {
        setClassIds((current) =>
            current.includes(classId)
                ? current.filter((id) => id !== classId)
                : [...current, classId],
        );
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        const assignedClasses = role === "TEACHER" ? classIds : [];

        try {
            if (user === null) {
                await createMutation.mutateAsync({
                    displayName: displayName.trim(),
                    email: email.trim(),
                    role,
                    password,
                    classIds: assignedClasses,
                });
            } else {
                await updateMutation.mutateAsync({
                    userId: user.id,
                    request: {
                        displayName: displayName.trim(),
                        email: email.trim(),
                        role,
                        active,
                        newPassword: password || null,
                        classIds: assignedClasses,
                    },
                });
            }

            onClose();
        } catch {
            // mutation.error hiển thị trong form
        }
    }

    return (
        <div className="overlay">
            <form className="modal user-form-modal" onSubmit={handleSubmit}>
                <div className="row">
                    <h2>{isNew ? "Thêm người dùng" : "Sửa người dùng"}</h2>

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
                    Tên hiển thị
                    <input
                        value={displayName}
                        onChange={(event) => setDisplayName(event.target.value)}
                        placeholder="Ví dụ: Cô Lan"
                        maxLength={150}
                        required
                    />
                </label>

                <label>
                    Email đăng nhập
                    <input
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        placeholder="ten@example.com"
                        maxLength={250}
                        required
                    />
                </label>

                <label>
                    Vai trò
                    <select
                        value={role}
                        onChange={(event) =>
                            setRole(event.target.value as UserRole)
                        }
                        disabled={isSelf || mutation.isPending}
                    >
                        {(Object.keys(ROLE_LABELS) as UserRole[]).map(
                            (value) => (
                                <option key={value} value={value}>
                                    {ROLE_LABELS[value]}
                                </option>
                            ),
                        )}
                    </select>
                </label>

                {role === "TEACHER" && (
                    <fieldset className="user-class-list">
                        <legend>
                            Lớp phụ trách (được cộng/trừ sao và đổi thưởng)
                        </legend>

                        {classes.length === 0 ? (
                            <p className="muted">Chưa có lớp nào.</p>
                        ) : (
                            classes.map((classItem) => (
                                <label key={classItem.classId}>
                                    <input
                                        type="checkbox"
                                        checked={classIds.includes(
                                            classItem.classId,
                                        )}
                                        onChange={() =>
                                            toggleClass(classItem.classId)
                                        }
                                        disabled={mutation.isPending}
                                    />
                                    {classItem.className}
                                </label>
                            ))
                        )}
                    </fieldset>
                )}

                <label>
                    {isNew ? "Mật khẩu" : "Mật khẩu mới"}
                    <input
                        type="password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        placeholder={
                            isNew
                                ? `Tối thiểu ${MIN_PASSWORD_LENGTH} ký tự`
                                : "Để trống nếu giữ nguyên mật khẩu"
                        }
                        autoComplete="new-password"
                        maxLength={100}
                        required={isNew}
                    />
                </label>

                {passwordTooShort && (
                    <p className="error">
                        Mật khẩu phải có ít nhất {MIN_PASSWORD_LENGTH} ký tự.
                    </p>
                )}

                {!isNew && (
                    <label className="user-active-toggle">
                        <input
                            type="checkbox"
                            checked={active}
                            onChange={(event) => setActive(event.target.checked)}
                            disabled={isSelf || mutation.isPending}
                        />
                        Tài khoản đang hoạt động
                    </label>
                )}

                {isSelf && (
                    <p className="muted">
                        Bạn không thể tự đổi vai trò hoặc tự khóa tài khoản của
                        mình.
                    </p>
                )}

                {logsOut && (
                    <div className="redemption-notice">
                        <Info />

                        <span>
                            Người dùng này sẽ bị đăng xuất và phải đăng nhập lại.
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
                        disabled={mutation.isPending || invalid}
                    >
                        {mutation.isPending
                            ? "Đang lưu..."
                            : isNew
                              ? "Thêm người dùng"
                              : "Lưu thay đổi"}
                    </button>
                </div>
            </form>
        </div>
    );
}
