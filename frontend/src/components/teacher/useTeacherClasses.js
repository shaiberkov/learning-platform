import { useEffect, useState } from "react";
import { apiGet } from "../../Utils/authRequest.js";
import { GET_GRADEBOOK_CLASSES, GRADEBOOK_BASE_PATH } from "../../constants/pages.constants.js";

// The classes the teacher teaches: [{ classRoomId, className, gradeName, studentCount }].
export default function useTeacherClasses(teacherId) {
    const [classes, setClasses] = useState([]);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!teacherId) return;
        let cancelled = false;
        apiGet(`${GRADEBOOK_BASE_PATH}${GET_GRADEBOOK_CLASSES}`, { teacherId }).then((res) => {
            if (cancelled) return;
            if (res.success) {
                setClasses(res.data || []);
                setError(res.errorCode || "");
            } else {
                setError(res.errorCode);
            }
        });
        return () => { cancelled = true; };
    }, [teacherId]);

    return { classes, error };
}
