export type ClassSummary = {
    classId: string;
    classCode: string;
    className: string;
    studentCount: number;
    totalStars: number;
};

export type CreateClassRequest = {
    code: string;
    name: string;
    schoolYear: string;
};

export type CreateClassResponse = {
    id: string;
    code: string;
    name: string;
    schoolYear: string;
};

export type ClassDetail = {
    id: string;
    code: string;
    name: string;
    schoolYear: string;
};

export type UpdateClassRequest = {
    code: string;
    name: string;
    schoolYear: string;
};
