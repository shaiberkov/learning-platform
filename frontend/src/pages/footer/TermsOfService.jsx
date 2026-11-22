import React from "react";
import ProsePage from "../../components/ui/ProsePage.jsx";

const Step = ({ n }) => <span className="num text-accent text-sm ml-3">{String(n).padStart(2, "0")}</span>;

export default function TermsOfService() {
    return (
        <ProsePage eyebrow="Terms · תנאים" title="תנאי שימוש">

                <section>
                    <h3 className="!mt-0"><Step n={1} />שימוש הוגן ואחראי</h3>
                    <p>
                        השימוש באתר מותר לכל אדם, בתנאי שישתמש בו למטרות לימודיות והוגנות בלבד.
                        אין להעלות תכנים פוגעניים, מזיקים או בלתי חוקיים.
                    </p>
                </section>

                <section>
                    <h3><Step n={2} />הרשמה ושמירת מידע</h3>
                    <p>
                        אם תבחר להירשם, עליך לספק מידע נכון ועדכני. הפרטים שלך נשמרים בצורה
                        מאובטחת ואינם נמסרים לגורמים חיצוניים ללא הסכמתך.
                    </p>
                </section>

                <section>
                    <h3><Step n={3} />זכויות יוצרים</h3>
                    <p>
                        כל התכנים באתר מוגנים בזכויות יוצרים. אין להעתיק, להפיץ או להשתמש
                        בהם ללא אישור מראש ובכתב מבעלי האתר.
                    </p>
                </section>

                <section>
                    <h3><Step n={4} />אחריות על השימוש</h3>
                    <p>
                        הנהלת האתר אינה אחראית לכל נזק ישיר או עקיף שעלול להיגרם כתוצאה
                        מהשימוש במערכת או מהסתמכות על תכני האתר.
                    </p>
                </section>

                <section>
                    <h3><Step n={5} />שינויים ועדכונים</h3>
                    <p>
                        תנאי השימוש עשויים להתעדכן מעת לעת. השימוש המתמשך באתר מהווה הסכמה
                        לתנאים המעודכנים.
                    </p>
                </section>

                <section>
                    <h3><Step n={6} />יצירת קשר</h3>
                    <p>
                        בכל שאלה בנוגע לתנאים אלו ניתן לפנות אלינו דרך טופס יצירת קשר
                        באתר או למייל התמיכה המופיע בתחתית העמוד.
                    </p>
                </section>

                <p className="num !text-xs !text-ink-3 pt-6 mt-6 border-t border-dashed border-line">עודכן לאחרונה: מאי 2025</p>
        </ProsePage>)


}
