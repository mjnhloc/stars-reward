export type Student = {
    id: string;
    studentCode: string;
    fullName: string;
    classId: string;
    className: string;
    totalStars: number;
    redeemableStars: number;
    redeemableValue: number;
    remainingStars: number;
};

export type StudentScore = {
    id: string;
    homeworkScore: number | null;
    testScore: number | null;
    finalScore: number | null;
    awardedStars: number;
    assessmentDate: string;
    createdAt: string;
};

export type StudentStarsAdjustmentRequest = {
    classId: string;
    stars: number;
    reasonDetail: string;
};

export type StudentStarsAdjustmentResponse = {
    transactionId: string;
    studentId: string;
    starDelta: number;
    balanceAfter: number;
};

export type CreateStudentRequest = {
    studentCode: string;
    fullName: string;
    classId: string;
    startedAt?: string;
};

export type CreateStudentResponse = {
    id: string;
    studentCode: string;
    fullName: string;
    classId: string;
    className: string;
    startedAt: string;
    totalStars: number;
};

export type UpdateStudentRequest = {
    studentCode: string;
    fullName: string;
    classId: string;
};

export type UpdateStudentResponse = {
    id: string;
    studentCode: string;
    fullName: string;
    classId: string;
    className: string;
    totalStars: number;
};
