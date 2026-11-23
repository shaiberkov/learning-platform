import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { PageSkeleton } from "../../components/ui/Skeleton.jsx";
import { errorMessage } from "../../lib/api.js";
import { fetchSimResult } from "../../features/examsim/examSimApi.js";
import SimResult from "../../features/examsim/SimResult.jsx";

// A finished simulation, opened again from the past-exams bank.
export default function ExamSimResult() {
    const { attemptId } = useParams();
    const { data, isLoading, isError, error } = useQuery({
        queryKey: ["examSimResult", attemptId],
        queryFn: () => fetchSimResult(attemptId),
    });

    if (isLoading) return <PageSkeleton />;
    if (isError || !data) {
        return (
            <div className="page page--medium" dir="rtl">
                <div className="glass">
                    <EmptyState title="לא ניתן להציג את התוצאות" body={errorMessage(error, "התוצאות לא נמצאו")}
                                actionLabel="חזרה למאגר הבחינות" actionTo="/exam-simulations" />
                </div>
            </div>
        );
    }
    return <SimResult result={data} />;
}
