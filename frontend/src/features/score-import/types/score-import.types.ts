export type ImportSessionStatus = "NEW" | "EXISTS" | "CONFLICT";

export type ImportIssueSeverity = "ERROR" | "WARNING";

export type ImportSession = {
    assessmentDate: string;
    homeworkScore: number;
    testScore: number;
    finalScore: number;
    awardedStars: number;
    status: ImportSessionStatus;
};

export type ImportStudent = {
    studentCode: string;
    fullName: string;
    classCode: string;
    newSessions: number;
    skippedSessions: number;
    starsToAward: number;
    sessions: ImportSession[];
};

// a student of the file that is not on the system, added only when ticked
export type ImportNewStudent = {
    studentCode: string;
    fullName: string | null;
    startedAt: string | null;
    classCode: string;
    className: string;
    newSessions: number;
    starsToAward: number;
    sessions: ImportSession[];
    // why it cannot be added, null when it can
    problem: string | null;
};

export type ImportClass = {
    classCode: string;
    className: string | null;
    found: boolean;
    studentCount: number;
};

export type ImportIssue = {
    severity: ImportIssueSeverity;
    location: string;
    message: string;
};

export type ScoreImportPreview = {
    fileName: string;
    sheetName: string;
    classes: ImportClass[];
    students: ImportStudent[];
    newStudents: ImportNewStudent[];
    totals: {
        students: number;
        newSessions: number;
        skippedSessions: number;
        starsToAward: number;
        errors: number;
    };
    issues: ImportIssue[];
};

export type ScoreImportResult = {
    batchId: string;
    importedSessions: number;
    awardedStars: number;
    students: number;
    skippedSessions: number;
    createdStudents: number;
};

export type ImportBatchStatus = "COMPLETED" | "REVERTED";

export type ImportBatch = {
    id: string;
    fileName: string;
    status: ImportBatchStatus;
    importedAt: string;
    importedBy: string | null;
    sessionCount: number;
    studentCount: number;
    awardedStars: number;
    classNames: string[];
    revertedAt: string | null;
    revertedBy: string | null;
};

export type ImportRevertStudent = {
    studentCode: string;
    fullName: string;
    starsToReverse: number;
    currentBalance: number;
    enoughBalance: boolean;
};

export type ImportRevertPreview = {
    batchId: string;
    fileName: string;
    sessionCount: number;
    starsToReverse: number;
    students: ImportRevertStudent[];
    canRevert: boolean;
};

export type ScoreImportRevertResult = {
    batchId: string;
    voidedSessions: number;
    reversedStars: number;
    students: number;
};
