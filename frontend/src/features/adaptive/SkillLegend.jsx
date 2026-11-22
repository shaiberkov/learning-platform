import { SKILL_COLORS } from "./adaptiveApi.js";

function SkillLegend() {
    return (
        <div className="flex flex-wrap gap-3 text-xs">
            {Object.entries(SKILL_COLORS).map(([key, c]) => (
                <span key={key} className="flex items-center gap-1.5 text-ink-2">
                    <span className={`w-2.5 h-2.5 rotate-45 rounded-[3px] ${c.dot}`} />
                    {c.label}
                </span>
            ))}
        </div>
    );
}

export default SkillLegend;
