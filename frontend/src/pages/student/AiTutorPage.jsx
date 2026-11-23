import AiTutorPanel from "../../components/ai/AiTutorPanel.jsx";

// Student: free chat with the AI tutor, for an exercise from a notebook or a textbook — photograph it or say it.
export default function AiTutorPage() {
    return (
        <div className="page page--medium" dir="rtl">
            <div className="glass glass--strong !rounded-[26px] overflow-hidden flex flex-col h-[75vh] reveal">
                <header className="px-5 py-4 border-b border-dashed border-line">
                    <span className="eyebrow">AI tutor · מורה-עזר</span>
                    <h2 className="title text-2xl mt-1">צלמו, הקליטו או כתבו תרגיל</h2>
                </header>
                <div className="flex-1 min-h-0">
                    <AiTutorPanel />
                </div>
            </div>
        </div>
    );
}
