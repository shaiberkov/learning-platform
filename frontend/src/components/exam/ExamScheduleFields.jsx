import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { he } from "date-fns/locale";
import "../../Styles/datePicker.css";

// Schedule and audience of an exam, shared by the builder and the clone dialog.
function DateField({ value, onChange, placeholder }) {
    return (
        <DatePicker
            selected={value}
            onChange={onChange}
            showTimeSelect
            timeFormat="HH:mm"
            timeIntervals={5}
            dateFormat="Pp"
            locale={he}
            placeholderText={placeholder}
            className="field num"
            wrapperClassName="w-full"
        />
    );
}

export default function ExamScheduleFields({ value, onChange }) {
    const set = (patch) => onChange({ ...value, ...patch });
    return (
        <div className="grid gap-4">
            <label className="flex items-center gap-2 text-sm text-ink">
                <input type="checkbox" checked={value.template} onChange={(e) => set({ template: e.target.checked })} />
                לשמור כתבנית בלבד (לא נשלח לתלמידים, אפשר לשכפל אותה בהמשך)
            </label>

            <div className="grid sm:grid-cols-2 gap-4">
                {!value.template && (
                    <div>
                        <label className="label">מועד תחילת המבחן</label>
                        <DateField value={value.startTime} onChange={(d) => set({ startTime: d })} placeholder="בחר תאריך ושעה" />
                    </div>
                )}
                <div>
                    <label className="label">משך המבחן (דקות)</label>
                    <input type="number" min="1" max="600" className="field num" value={value.timeLimitMinutes} onChange={(e) => set({ timeLimitMinutes: e.target.value })} />
                </div>
            </div>

            {!value.template && (
                <>
                    <div className="grid sm:grid-cols-2 gap-4 items-end">
                        <label className="flex items-center gap-2 text-sm text-ink">
                            <input type="checkbox" checked={value.scheduledPublish} onChange={(e) => set({ scheduledPublish: e.target.checked })} />
                            פרסום מתוזמן (התלמידים יראו את המבחן רק מהמועד שנבחר)
                        </label>
                        {value.scheduledPublish && (
                            <div>
                                <label className="label">מועד פרסום</label>
                                <DateField value={value.publishAt} onChange={(d) => set({ publishAt: d })} placeholder="מועד פרסום" />
                            </div>
                        )}
                    </div>

                    <div>
                        <label className="label">ת"ז של תלמידים (מופרדים בפסיקים)</label>
                        <textarea rows={3} className="field num resize-none" value={value.studentIds} placeholder="123456789,987654321" onChange={(e) => set({ studentIds: e.target.value })} />
                    </div>

                    <label className="flex items-center gap-2 text-sm text-ink">
                        <input type="checkbox" checked={value.versions} onChange={(e) => set({ versions: e.target.checked })} />
                        גרסאות A/B: חלוקת התלמידים לשתי גרסאות עם סדר שאלות שונה
                    </label>
                </>
            )}
        </div>
    );
}
