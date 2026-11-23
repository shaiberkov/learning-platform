import { lazy, Suspense, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaArrowRight, FaCheck, FaShapes } from "react-icons/fa";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";
import RealWorldCard from "../../features/realworld/RealWorldCard.jsx";
import { fetchExploredSimulations, recordSimulation } from "../../features/realworld/realWorldApi.js";
import { SIMULATIONS, simulationByKey } from "../../features/simulations/simulationCatalog.js";

const COMPONENTS = {
    parabola: lazy(() => import("../../features/simulations/ParabolaSim.jsx")),
    "line-slope": lazy(() => import("../../features/simulations/LineSlopeSim.jsx")),
    triangle: lazy(() => import("../../features/simulations/TriangleSim.jsx")),
    motion: lazy(() => import("../../features/simulations/MotionSim.jsx")),
};

const EXPLORED_KEY = ["simulations-explored"];

// Gallery of interactive simulations. ?sim=<key> opens one; the first interaction with a
// simulation on each visit is recorded, so the gallery marks it as explored.
export default function Simulations() {
    const [params, setParams] = useSearchParams();
    const queryClient = useQueryClient();
    const recorded = useRef(new Set());
    const selected = simulationByKey(params.get("sim"));

    const explored = useQuery({ queryKey: EXPLORED_KEY, queryFn: fetchExploredSimulations });
    const record = useMutation({
        mutationFn: recordSimulation,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: EXPLORED_KEY }),
    });

    const exploredCount = (key) => explored.data?.find((e) => e.simulationKey === key)?.exploreCount || 0;

    const onExplore = (key) => () => {
        if (recorded.current.has(key)) return;
        recorded.current.add(key);
        record.mutate(key);
    };

    if (selected) {
        const Sim = COMPONENTS[selected.key];
        return (
            <div className="page page--wide space-y-5" dir="rtl">
                <button type="button" className="link inline-flex items-center gap-2" onClick={() => setParams({})}>
                    <FaArrowRight aria-hidden="true" /> לכל הסימולציות
                </button>
                <div className="glass p-6 sm:p-8 space-y-5 reveal">
                    <div className="space-y-1">
                        <span className="eyebrow">Simulation · סימולציה</span>
                        <h1 className="display text-3xl sm:text-4xl">{selected.title}</h1>
                        <p className="title text-lg text-accent">{selected.question}</p>
                        <p className="muted text-sm">{selected.description}</p>
                    </div>
                    <Suspense fallback={<SkeletonList rows={2} />}>
                        <Sim onExplore={onExplore(selected.key)} />
                    </Suspense>
                </div>
                <div className="grid md:grid-cols-2 gap-4">
                    {selected.topics.map((topic) => <RealWorldCard key={topic} topic={topic} />)}
                </div>
            </div>
        );
    }

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-2 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge" aria-hidden="true"><FaShapes /></span>
                    <span className="eyebrow">Simulations · סימולציות</span>
                </div>
                <h1 className="display text-3xl sm:text-4xl">מעבדת הסימולציות</h1>
                <p className="muted">גוררים, מזיזים ומגלים בעצמכם איך המתמטיקה מתנהגת. בחרו סימולציה כדי להתחיל.</p>
            </div>

            {explored.isLoading ? (
                <SkeletonList rows={3} />
            ) : SIMULATIONS.length === 0 ? (
                <EmptyState title="אין סימולציות עדיין" body="סימולציות חדשות יופיעו כאן." />
            ) : (
                <ul className="grid sm:grid-cols-2 gap-4" aria-label="רשימת הסימולציות">
                    {SIMULATIONS.map((s, i) => {
                        const count = exploredCount(s.key);
                        return (
                            <li key={s.key} className="reveal" style={{ "--i": i }}>
                                <button type="button" onClick={() => setParams({ sim: s.key })}
                                        className="glass tile w-full h-full text-right p-5 space-y-2">
                                    <div className="flex items-center justify-between gap-2">
                                        <h2 className="title text-lg">{s.title}</h2>
                                        {count > 0 && (
                                            <span className="chip"><FaCheck aria-hidden="true" className="text-mint" /> נחקרה <span className="num">{count}</span></span>
                                        )}
                                    </div>
                                    <p className="text-accent text-sm font-semibold">{s.question}</p>
                                    <p className="muted text-sm">{s.description}</p>
                                    <p className="faint text-xs">נושאים: {s.topics.join(" · ")}</p>
                                </button>
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
}
