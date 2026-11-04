export type TransactionType = "EARN" | "DEDUCT" | "REDEEM" | "REVERSAL";

export type TransactionSourceType =
    | "SCORE"
    | "MANUAL"
    | "BEHAVIOR"
    | "REDEMPTION"
    | "SYSTEM";

export type StudentTransaction = {
    id: string;
    transactionType: TransactionType;
    sourceType: TransactionSourceType;
    starDelta: number;
    balanceBefore: number;
    balanceAfter: number;
    reasonDetail: string;
    createdAt: string;
    displayName: string | null;
    className: string | null;
};
