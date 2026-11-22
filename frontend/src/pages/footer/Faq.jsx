import React from "react";
import ProsePage from "../../components/ui/ProsePage.jsx";

const summaryClass =
    "cursor-pointer list-none flex items-center justify-between gap-4 py-5 title text-lg transition-colors group-open:text-accent [&::-webkit-details-marker]:hidden";

function Marker() {
    return (
        <span className="grid place-items-center w-9 h-9 shrink-0 rounded-xl border border-line bg-surface-strong text-ink-3 transition-all duration-500 ease-[var(--ease-spring)] group-open:rotate-45 group-open:text-accent group-open:border-accent">
            +
        </span>
    );
}

export default function Faq() {
    return (

        <ProsePage eyebrow="FAQ · שאלות נפוצות" title="שאלות נפוצות">
            <div className="divide-y divide-dashed divide-line -my-5">

                <details className="group">
                    <summary
                        className={summaryClass}>
                        מהו אתר הלמידה האישי?
                        <Marker />
                    </summary>
                    <p className="pb-5 -mt-1">
                        האתר מאפשר לכל אדם ללמוד, לתרגל ולשפר את הידע שלו בנושאים שונים
                        בצורה עצמאית ובקצב אישי.
                    </p>
                </details>

                <details className="group">
                    <summary
                        className={summaryClass}>
                        האם צריך להירשם כדי להשתמש באתר?
                        <Marker />
                    </summary>
                    <p className="pb-5 -mt-1">
                        לא חובה להירשם כדי לצפות בתכנים בסיסיים. עם זאת, הרשמה מאפשרת
                        לשמור את ההתקדמות האישית שלך, לקבל תרגולים מותאמים ולצבור הישגים.
                    </p>
                </details>

                <details className="group">
                    <summary
                        className={summaryClass}>
                        איך בוחרים תחום לימוד?
                        <Marker />
                    </summary>
                    <p className="pb-5 -mt-1">
                        ניתן לעיין בתחומים הזמינים בדף הבית או בתפריט הראשי. לחיצה על תחום
                        תוביל אותך לעמוד תרגול מותאם.
                    </p>
                </details>

                <details className="group">
                    <summary
                        className={summaryClass}>
                        האם השימוש באתר עולה כסף?
                        <Marker />
                    </summary>
                    <p className="pb-5 -mt-1">
                        השימוש בתכנים הבסיסיים באתר הוא ללא תשלום. ייתכן שבעתיד יתווספו
                        תכנים מתקדמים בתשלום סמלי.
                    </p>
                </details>

                <details className="group">
                    <summary
                        className={summaryClass}>
                        איך שומרים על פרטיות המשתמשים?
                        <Marker />
                    </summary>
                    <p className="pb-5 -mt-1">
                        אנו שומרים על פרטיותך בהתאם לתקנות הגנת המידע. מידע אישי נשמר
                        בצורה מאובטחת ולא מועבר לגורמים חיצוניים.
                    </p>
                </details>

                <details className="group">
                    <summary
                        className={summaryClass}>
                        למי פונים במקרה של שאלה או בעיה?
                        <Marker />
                    </summary>
                    <p className="pb-5 -mt-1">
                        ניתן לפנות אלינו באמצעות טופס “צור קשר” בתפריט הראשי או לשלוח מייל
                        לכתובת התמיכה שלנו. אנחנו כאן כדי לעזור.
                    </p>
                </details>

            </div>
        </ProsePage>
    );

}
