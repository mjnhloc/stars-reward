import { useMemo, useState } from "react";

import { useStudentTransactions } from "../hooks/useStudentTransactions";

import { formatDate } from "../../../common/func/common.func";
import type {
    TransactionSourceType,
    TransactionType,
} from "../../star-transactions/types/star-transaction.types";

type Props = {
    studentId: string;
};

type TransactionFilter = "ALL" | "EARN" | "DEDUCT" | "REDEEM" | "REVERSAL";

const FILTERS: { value: TransactionFilter; label: string }[] = [
    { value: "ALL", label: "Tất cả" },
    { value: "EARN", label: "Cộng" },
    { value: "DEDUCT", label: "Trừ" },
    { value: "REDEEM", label: "Đổi thưởng" },
    { value: "REVERSAL", label: "Hoàn tác" },
];

const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
    EARN: "Cộng sao",
    DEDUCT: "Trừ sao",
    REDEEM: "Đổi thưởng",
    REVERSAL: "Hoàn tác",
};

const SOURCE_TYPE_LABELS: Record<TransactionSourceType, string> = {
    SCORE: "Điểm học tập",
    MANUAL: "Thủ công",
    BEHAVIOR: "Hành vi",
    REDEMPTION: "Đổi thưởng",
    SYSTEM: "Hệ thống",
};

export function StudentTransactionHistory({ studentId }: Props) {
    const transactions = useStudentTransactions(studentId);

    const [filter, setFilter] = useState<TransactionFilter>("ALL");

    const visible = useMemo(
        () =>
            transactions.data?.filter(
                (transaction) =>
                    filter === "ALL" || transaction.transactionType === filter,
            ) ?? [],
        [transactions.data, filter],
    );

    return (
        <section className="transaction-history">
            <div className="section-heading">
                <div>
                    <h3>Lịch sử giao dịch sao</h3>

                    <p>Cộng, trừ và đổi thưởng, mới nhất ở trên</p>
                </div>

                {transactions.data && (
                    <span>{transactions.data.length} giao dịch</span>
                )}
            </div>

            <div className="transaction-filters" role="group">
                {FILTERS.map((item) => (
                    <button
                        key={item.value}
                        type="button"
                        className={filter === item.value ? "active" : ""}
                        aria-pressed={filter === item.value}
                        onClick={() => setFilter(item.value)}
                    >
                        {item.label}
                    </button>
                ))}
            </div>

            {transactions.isLoading ? (
                <p className="muted">Đang tải lịch sử giao dịch...</p>
            ) : transactions.error ? (
                <p className="error">{transactions.error.message}</p>
            ) : transactions.data?.length === 0 ? (
                <div className="empty-state">
                    Học sinh chưa có giao dịch sao.
                </div>
            ) : visible.length === 0 ? (
                <div className="empty-state">
                    Không có giao dịch phù hợp với bộ lọc.
                </div>
            ) : (
                <div className="score-list">
                    {visible.map((transaction) => (
                        <article key={transaction.id} className="score-item">
                            <div className="score-item-header">
                                <div>
                                    <strong>
                                        {TRANSACTION_TYPE_LABELS[
                                            transaction.transactionType
                                        ] ?? transaction.transactionType}
                                    </strong>

                                    <span>
                                        {formatDate(
                                            transaction.createdAt,
                                            true,
                                        )}
                                        {transaction.className &&
                                            ` · ${transaction.className}`}
                                    </span>
                                </div>

                                <span
                                    className={`transaction-delta ${
                                        transaction.starDelta > 0
                                            ? "positive"
                                            : "negative"
                                    }`}
                                >
                                    {transaction.starDelta > 0 ? "+" : ""}
                                    {transaction.starDelta} sao
                                </span>
                            </div>

                            <p className="transaction-reason">
                                {transaction.reasonDetail}
                            </p>

                            <p className="transaction-meta">
                                Số dư: {transaction.balanceBefore} →{" "}
                                {transaction.balanceAfter} · Nguồn:{" "}
                                {SOURCE_TYPE_LABELS[transaction.sourceType] ??
                                    transaction.sourceType}
                                {transaction.displayName &&
                                    ` · Bởi: ${transaction.displayName}`}
                            </p>
                        </article>
                    ))}
                </div>
            )}
        </section>
    );
}
