import { useContext } from "react";
import { FaUserCheck } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import FormShell from "../../components/ui/FormShell.jsx";
import AttendanceReportView from "../../components/AttendanceReportView.jsx";

function AttendanceReport() {
    const { user } = useContext(UserContext);
    return (
        <FormShell eyebrow="Attendance · נוכחות" title="דוח היעדרויות בית ספרי" icon={<FaUserCheck />} width="wide">
            <AttendanceReportView userId={user?.userId} />
        </FormShell>
    );
}

export default AttendanceReport;
