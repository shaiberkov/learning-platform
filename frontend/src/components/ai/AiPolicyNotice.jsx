// The AI transparency notice shown wherever students or teachers use AI.
// Keep it in sync with the backend: ContentSafetyFilter, ExamGuard, AiRetentionJob (ai.retention.days).
export default function AiPolicyNotice({ retentionDays = 30, audience = "student" }) {
    return (
        <details className="glass--sunk rounded-2xl px-4 py-3 text-sm text-ink-2" dir="rtl">
            <summary className="cursor-pointer font-semibold text-ink">
                איך עוזר ה-AI עובד ומה קורה עם השיחות
            </summary>
            <ul className="mt-3 space-y-2 list-disc pr-5 leading-relaxed">
                {audience === "student" ? (
                    <>
                        <li>העוזר מנחה בשאלות ולא נותן תשובות סופיות. בזמן מבחן הוא כבוי לגמרי.</li>
                        <li>
                            <strong>מי רואה את השיחות:</strong> רק את/ה. צוות בית הספר רואה רק הודעות שמערכת הבטיחות סימנה
                            (למשל מצוקה או תוכן לא הולם), כדי שיוכלו לעזור.
                        </li>
                        <li>
                            <strong>כמה זמן נשמר:</strong> {retentionDays} ימים, ואז השיחות נמחקות אוטומטית. אפשר למחוק את
                            השיחות שלך בכל רגע בכפתור "מחק שיחות".
                        </li>
                        <li>
                            ההודעות נשלחות לספק AI חיצוני בלי השם שלך. מספרי טלפון, ת"ז ומיילים מוסרים אוטומטית – בכל זאת,
                            אל תכתבו פרטים אישיים.
                        </li>
                        <li>ה-AI יכול לטעות. אם משהו נראה לא נכון – שאלו את המורה.</li>
                        <li>קשה לך? אפשר תמיד לפנות למחנך/ת, ליועצ/ת או לער"ן בטלפון 1201.</li>
                    </>
                ) : (
                    <>
                        <li>כל תוצר של ה-AI (שאלות, ציונים, סיכומים) הוא הצעה בלבד ונכנס לשימוש רק אחרי אישור שלך.</li>
                        <li>שיחות תלמידים נשמרות {retentionDays} ימים ונמחקות אוטומטית; צוות רואה רק הודעות שסומנו על ידי מערכת הבטיחות.</li>
                        <li>לספק ה-AI לא נשלחים שמות או מספרי זהות: בסיכומים התלמידים מופיעים בכינוי (S1, S2...).</li>
                        <li>לכל משתמש מכסה יומית, והעלות מתועדת לפי בית ספר.</li>
                    </>
                )}
            </ul>
        </details>
    );
}
