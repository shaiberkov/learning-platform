import Cookies from "universal-cookie";
import { useContext, useEffect, useState } from "react";
import axios from "axios";
import WeeklySchedule from "../../components/WeeklySchedule.jsx";
import Select from "react-select";
import {UserContext} from "../../context/UserContext.jsx";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";
import {
    AUTH_HEADER, GET_ALL_CLASSES_BY_SCHOOL_CODE,
    QUESTION, SCHOOL_CODE,
    SCHOOL_MANAGER_BASE_PATH, USER_ID
} from "../../constants/pages.constants.js";

function ClassScheduleViewerForManager() {
    const cookies = new Cookies();
    const token = cookies.get("token");
    const [classesNames, setClassesNames] = useState([]);
    const [message, setMessage] = useState("");
    const [selectedClassName, setSelectedClassName] = useState("");

    const {user}=useContext(UserContext)

    useEffect(() => {
        const fetch = async () => {
            if (user?.userId) {
                try {
                        const classRes = await axios.get(
                            `${SCHOOL_MANAGER_BASE_PATH}${GET_ALL_CLASSES_BY_SCHOOL_CODE}${QUESTION}${SCHOOL_CODE}${user.schoolCode}`,
                            { headers:
                                    {
                                        [AUTH_HEADER]: `${BEARER_PREFIX}${token}`
                            }
                            }
                        );

                        if (classRes.data.success) {
                            setClassesNames(classRes.data.data);
                        }

                } catch (err) {
                    console.error(err);
                    setMessage("שגיאה בטעינת הנתונים");
                }
            }
        };

        fetch();
    }, [user?.userId]);

    const classOptions = classesNames.map((cls) => ({
        value: cls,
        label: cls,
    }));

    return (

            <div
                className="page"
                dir="rtl"
            >
                <div className="glass p-6 sm:p-9 reveal">
                    <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-5 pb-6 mb-6 border-b border-dashed border-line">
                        <div className="space-y-2">
                            <span className="eyebrow">Classes · כיתות</span>
                            <h2 className="display text-3xl sm:text-4xl">
                                צפייה במערכת שעות לפי כיתה
                            </h2>
                        </div>

                        <div className="w-full md:w-72">
                            <label className="label">
                                בחר כיתה:
                            </label>

                            <Select
                                options={classOptions}
                                onChange={(sel) => setSelectedClassName(sel?.value || "")}
                                value={classOptions.find((o) => o.value === selectedClassName)}
                                placeholder="בחר כיתה..."
                                isClearable
                                isSearchable
                                classNamePrefix="lp-select"
                                menuPortalTarget={typeof window !== "undefined" ? document.body : null}
                                menuPosition="fixed"
                                menuPlacement="auto"
                                styles={{
                                    control: (base) => ({
                                        ...base,
                                        direction: "rtl",
                                    }),
                                    menu: (base) => ({
                                        ...base,
                                        textAlign: "right",
                                    }),
                                    menuPortal: (base) => ({ ...base, zIndex: 60 }),
                                    option: (base, state) => ({
                                        ...base,
                                        textAlign: "right",
                                        fontWeight: state.isSelected ? "bold" : "normal",
                                    }),
                                    singleValue: (base) => ({
                                        ...base,
                                        textAlign: "right",
                                    }),
                                }}
                            />
                        </div>
                    </div>

                    {message && (
                        <div className="notice">
                            {message}
                        </div>
                    )}

                    {selectedClassName ? (
                        <div className="overflow-x-auto reveal">
                            <WeeklySchedule type="class" classRoomName={selectedClassName} />
                        </div>
                    ) : (
                        <div className="py-16 flex flex-col items-center gap-3 text-center">
                            <span className="w-14 h-14 rounded-2xl border border-dashed border-line-strong grid place-items-center rotate-45">
                                <span className="-rotate-45 w-2.5 h-2.5 rounded-[3px] bg-accent shadow-[0_0_12px_var(--glow)]" />
                            </span>
                            <p className="faint text-sm">בחר כיתה כדי לראות את המערכת השבועית שלה</p>
                        </div>
                    )}
                </div>
            </div>
    );
}

export default ClassScheduleViewerForManager;
