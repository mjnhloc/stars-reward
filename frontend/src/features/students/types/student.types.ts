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
