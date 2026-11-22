import {useContext, useEffect, useState} from "react";
import {UserContext} from "../../context/UserContext.jsx";
import {FaChartBar, FaFilePdf} from "react-icons/fa";
import BarChart from "../../components/charts/BarChart.jsx";
import LineChart from "../../components/charts/LineChart.jsx";
import OrbitLoader from "../../components/ui/OrbitLoader.jsx";
import {schoolGet} from "../../features/school/schoolApi.js";
import {SCHOOL_ANALYTICS} from "../../constants/pages.constants.js";

// Print rules for "export to PDF": the browser's print dialog saves the page as PDF.
const PRINT_CSS = `
@media print {
  #navbar, #sidebar, footer, .no-print { display: none !important; }
  main { padding-top: 0 !important; }
  .glass { box-shadow: none !important; backdrop-filter: none !important; break-inside: avoid; }
  body { background: #fff !important; }
}`;

function Panel({title, eyebrow, children, i}) {
    return (
        <div className="glass p-5 sm:p-6 reveal" style={{"--i": i}}>
            <div className="mb-4">
                <span className="eyebrow">{eyebrow}</span>
                <h3 className="title text-lg mt-1">{title}</h3>
            </div>
            {children}
        </div>
    );
}

function BucketTable({rows, labelTitle}) {
    if (!rows?.length) return null;
    return (
        <details className="mt-3">
            <summary className="faint text-xs cursor-pointer no-print">הצגה כטבלה</summary>
            <table className="data-table mt-2">
                <thead><tr><th>{labelTitle}</th><th>ממוצע</th><th>ציונים</th></tr></thead>
                <tbody>
                {rows.map((r) => (
                    <tr key={r.label}><td>{r.label}</td><td className="num">{r.average}</td><td className="num">{r.count}</td></tr>
                ))}
                </tbody>
            </table>
        </details>
    );
}

function SchoolAnalytics() {
    const {user} = useContext(UserContext);
    const [data, setData] = useState(null);
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user?.schoolCode) return;
        schoolGet(SCHOOL_ANALYTICS, {schoolCode: user.schoolCode}).then((res) => {
            if (res.success) setData(res.data);
            setMessage(res.errorCode || "");
            setLoading(false);
        });
    }, [user?.schoolCode]);

    return (
        <div className="page page--wide" dir="rtl">
            <style>{PRINT_CSS}</style>
            <div className="glass p-6 sm:p-8 mb-5 reveal flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                    <span className="icon-badge"><FaChartBar/></span>
                    <div>
                        <span className="eyebrow">School dashboard · לוח בית הספר</span>
                        <h2 className="title text-2xl sm:text-3xl mt-1">ציוני בית הספר{data?.schoolName ? ` · ${data.schoolName}` : ""}</h2>
                    </div>
                </div>
                <button className="btn btn-ghost no-print" onClick={() => window.print()} disabled={!data}>
                    <FaFilePdf/> ייצוא ל-PDF
                </button>
            </div>

            {loading && <OrbitLoader title="טוען נתונים"/>}
            {message && <p className="notice mb-5">{message}</p>}

            {data && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                    <div className="glass p-6 reveal lg:col-span-2 grid grid-cols-2 sm:grid-cols-4 gap-4" style={{"--i": 1}}>
                        <div><span className="faint text-xs">ממוצע כללי</span><p className="num text-4xl font-semibold mt-2">{data.overallAverage}</p></div>
                        <div><span className="faint text-xs">ציונים במדגם</span><p className="num text-4xl font-semibold mt-2">{data.resultsCount}</p></div>
                        <div><span className="faint text-xs">שכבות</span><p className="num text-4xl font-semibold mt-2">{data.byGrade.length}</p></div>
                        <div><span className="faint text-xs">מורים</span><p className="num text-4xl font-semibold mt-2">{data.byTeacher.length}</p></div>
                    </div>

                    <Panel i={2} eyebrow="By grade" title="ממוצע לפי שכבה">
                        <BarChart data={data.byGrade}/>
                        <BucketTable rows={data.byGrade} labelTitle="שכבה"/>
                    </Panel>
                    <Panel i={3} eyebrow="By subject" title="ממוצע לפי מקצוע">
                        <BarChart data={data.bySubject}/>
                        <BucketTable rows={data.bySubject} labelTitle="מקצוע"/>
                    </Panel>
                    <Panel i={4} eyebrow="By teacher" title="ממוצע לפי מורה">
                        <BarChart data={data.byTeacher}/>
                        <BucketTable rows={data.byTeacher} labelTitle="מורה"/>
                    </Panel>
                    <Panel i={5} eyebrow="Trend" title="מגמה לאורך זמן (ממוצע חודשי)">
                        <LineChart data={data.trend}/>
                        <BucketTable rows={data.trend} labelTitle="חודש"/>
                    </Panel>

                    <div className="lg:col-span-2 grid grid-cols-1 md:grid-cols-2 gap-5">
                        {data.classComparison.map((g, idx) => (
                            <Panel key={g.gradeName} i={6 + idx} eyebrow="Parallel classes" title={`השוואת כיתות מקבילות · שכבה ${g.gradeName}`}>
                                <BarChart data={g.classes}/>
                            </Panel>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}

export default SchoolAnalytics;
