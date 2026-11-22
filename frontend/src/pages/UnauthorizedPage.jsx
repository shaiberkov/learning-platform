import {FiAlertTriangle} from "react-icons/fi";

const UnauthorizedPage = () => {
    return(

        <div
            className="page page--narrow min-h-[50vh] grid place-items-center"
            dir="rtl"
        >
            <div className="glass p-8 sm:p-10 text-center space-y-4 reveal">
                <span className="icon-badge icon-badge--danger !w-14 !h-14 !rounded-2xl mx-auto">
                    <FiAlertTriangle className="text-2xl" />
                </span>
                <span className="eyebrow block">Access · הרשאה</span>
                <h2 className="title text-2xl">
                    אין לך הרשאה לצפות בדף זה.
                </h2>
            </div>
        </div>


        )

}

export default UnauthorizedPage;
