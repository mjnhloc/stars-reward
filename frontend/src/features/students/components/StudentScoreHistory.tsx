import { useStudentScores } from "../hooks/useStudentScores";

import { formatDate, formatScore } from "../../../common/func/common.func";

type Props = {
    studentId: string;
};

export function StudentScoreHistory({ studentId }: Props) {
    const scores = useStudentScores(studentId);

    return (
        <section className="score-history">
            <div className="section-heading">
                <div>
                    <h3>Chi tiết điểm học tập</h3>

                    <p>Điểm theo từng lần đánh giá</p>
                </div>

                {scores.data && <span>{scores.data.length} lần</span>}
            </div>

            {scores.isLoading ? (
                <p className="muted">Đang tải lịch sử điểm...</p>
            ) : scores.error ? (
                <p className="error">{scores.error.message}</p>
            ) : scores.data?.length === 0 ? (
                <div className="empty-state">Học sinh chưa có dữ liệu điểm.</div>
            ) : (
                <div className="score-list">
                    {scores.data?.map((score, index) => (
                        <article key={score.id} className="score-item">
                            <div className="score-item-header">
                                <div>
                                    <strong>
                                        Lần {scores.data.length - index}
                                    </strong>

                                    <span>{formatDate(score.assessmentDate)}</span>
                                </div>

                                <span className="score-stars">
                                    +{score.awardedStars} sao
                                </span>
                            </div>

                            <div className="score-values">
                                <div>
                                    <span>BTVN</span>

                                    <strong>
                                        {formatScore(score.homeworkScore)}
                                    </strong>
                                </div>

                                <div>
                                    <span>Kiểm tra</span>

                                    <strong>{formatScore(score.testScore)}</strong>
                                </div>

                                <div className="final-score">
                                    <span>Điểm tính</span>

                                    <strong>{formatScore(score.finalScore)}</strong>
                                </div>
                            </div>
                        </article>
                    ))}
                </div>
            )}
        </section>
    );
}
