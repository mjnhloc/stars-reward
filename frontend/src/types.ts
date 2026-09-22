export type ClassSummary = {
    classId: string;
    classCode: string;
    className: string;
    studentCount: number;
    totalStars: number;
};

export type Student = {
    id: string;
    studentCode: string;
    fullName: string;
    classId: string;
    className: string;
    homeworkScore: number | null;
    testScore: number | null;
    finalScore: number | null;
    totalStars: number;
    redeemableStars: number;
    redeemableValue: number;
    remainingStars: number;
};

export type Dashboard = {
    totalStudents: number;
    totalStars: number;
};

export type Transaction = {
    id: string;
    star_delta: number;
    balance_after: number;
    reason_code: string;
    reason_detail: string;
    created_at: string;
    actor: string;
};
