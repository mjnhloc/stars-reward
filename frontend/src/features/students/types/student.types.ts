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
