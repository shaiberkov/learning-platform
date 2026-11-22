// import React, { useEffect, useState } from 'react';
// import { FaPhone, FaKey } from 'react-icons/fa';
// import Cookies from "universal-cookie";
// import { useLocation, useNavigate } from "react-router-dom";
// import axios from "axios";
// import {
//     GET_USER_PHONE,
//     QUESTION,
//     SEND_OTP,
//     USER_BASE_PATH,
//     USER_ID,
//     VERIFY_OTP
// } from "../../constants/pages.constants.js";
//
// function TwoFactorAuthForm() {
//     // const [phoneNumber, setPhoneNumber] = useState('');
//     const [verificationCode, setVerificationCode] = useState('');
//     const [error, setError] = useState('');
//     const location = useLocation();
//     const userId = location.state?.userId;
//     const phoneNumber=location.state?.phoneNumber
//     const navigate = useNavigate();
//     const cookies = new Cookies();
//
//
//
//     useEffect(() => {
//         const sendOtp = async () => {
//             if (phoneNumber) {
//                 try {
//                     const params = new URLSearchParams({ userId, phoneNumber });
//                     const response = await axios.post(
//                         `${USER_BASE_PATH}${SEND_OTP}${QUESTION}${params.toString()}`
//                     );
//
//                     if (!response.data.success) {
//                         setError(response.data.errorCode || 'שגיאה בשליחת קוד אימות');
//                     }
//                 } catch (error) {
//                     console.error('Error sending OTP:', error);
//                     setError('שגיאה בשליחת קוד אימות');
//                 }
//             }
//         };
//         sendOtp();
//     }, []);
//
//     const handleCodeChange = (event) => {
//         setVerificationCode(event.target.value);
//     };
//
//     const handleSubmit = async (event) => {
//         event.preventDefault()
//         setError('');
//
//         const params = new URLSearchParams({
//             userId: userId,
//             otp: verificationCode,
//         });
//
//         try {
//             const response = await axios.post(
//                 `${USER_BASE_PATH}${VERIFY_OTP}${QUESTION}${params.toString()}`
//
//             );
//             if (response.data.success) {
//                 console.log("Response:", response.data.token);
//                 cookies.set("token", response.data.token, { path: "/" });
//
//                  navigate("/login")
//             } else {
//                 setError('הקוד שהוזן לא נכון. נסה שוב.');
//             }
//         } catch (error) {
//             console.error('Error during OTP verification:', error);
//             setError('שגיאה באימות הקוד. נסה שוב מאוחר יותר');
//         }
//     };
//
//     return (
//         <div className="flex justify-center mt-15 px-4 py-10 pt-12 animate-fade-in">
//             <form
//                 onSubmit={handleSubmit}
//                 className="bg-green p-8 rounded-4xl shadow-xl w-full max-w-md space-y-4 transition duration-300"
//             >
//                 <h2 className="text-2xl font-bold text-center text-gray-800 mb-3">
//                     הכנס את קוד האימות
//                 </h2>
//
//                 <div className="relative">
//                     <FaKey
//                         className="absolute top-3 left-3 text-yellow-500 drop-shadow-md hover:scale-120 transition duration-200"/>
//                     <input
//                         type="text"
//                         name="verificationCode"
//                         value={verificationCode}
//                         onChange={handleCodeChange}
//                         placeholder="הכנס קוד אימות"
//                         required
//                         className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-yellow-300 transition"
//                     />
//                     {error && (
//                         <p className="text-red-500 text-sm mt-1 text-center flex items-center justify-center gap-1">
//                              {error}
//                         </p>
//                     )}
//
//                 </div>
//
//                 <button
//                     type="submit"
//                     className="w-full relative py-2 rounded-lg bg-gradient-to-r from-blue-400 to-green-400 text-white font-semibold shadow-md hover:from-blue-600 hover:to-green-600 transition-all duration-300 ease-in-out disabled:opacity-50 overflow-hidden"
//                 >
//                     אמת קוד
//                 </button>
//                 <p className="text-sm text-gray-600 text-center mt-2">
//                     הקוד תקף ל-2 דקות בלבד. הקוד נשלח למספר טלפון
//                     <br/>
//                     {phoneNumber.slice(0, 3)}
//                     ******
//                     {phoneNumber.slice(-3)}
//                 </p>
//             </form>
//         </div>
//     );
// }
//
// export default TwoFactorAuthForm;


import React, { useEffect, useRef, useState } from 'react';
import { FaKey } from 'react-icons/fa';
import Cookies from "universal-cookie";
import { useLocation, useNavigate } from "react-router-dom";
import axios from "axios";
import AuthShell from "../../components/ui/AuthShell.jsx";
import {
    QUESTION,
    SEND_OTP,
    USER_BASE_PATH,
    VERIFY_OTP
} from "../../constants/pages.constants.js";

function TwoFactorAuthForm() {
    const [verificationCode, setVerificationCode] = useState('');
    const [error, setError] = useState('');
    const location = useLocation();
    const userId = location.state?.userId;
    const phoneNumber = location.state?.phoneNumber;
    const navigate = useNavigate();
    const cookies = new Cookies();
    const [loading, setLoading] = useState(false);

    const inputsRef = useRef([]);

    useEffect(() => {
        const sendOtp = async () => {
            if (phoneNumber) {
                try {
                    const params = new URLSearchParams({ userId, phoneNumber });
                    const response = await axios.post(
                        `${USER_BASE_PATH}${SEND_OTP}${QUESTION}${params.toString()}`
                    );
                    if (!response.data.success) {
                        setError(response.data.errorCode || 'שגיאה בשליחת קוד אימות');
                    }
                } catch (error) {
                    console.error('Error sending OTP:', error);
                    setError('שגיאה בשליחת קוד אימות');
                }
            }
        };
        sendOtp();
    }, []);

    const handleInputChange = (e, index) => {
        const value = e.target.value.replace(/[^0-9a-zA-Z]/g, '').slice(0, 1);
        const newCode = verificationCode.split('');
        newCode[index] = value;
        setVerificationCode(newCode.join(''));

        if (value && index < 4) {
            inputsRef.current[index + 1]?.focus();
        }
    };

    const handleKeyDown = (e, index) => {
        if (e.key === "Backspace" && !verificationCode[index] && index > 0) {
            inputsRef.current[index - 1]?.focus();
        }
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setLoading(true)
        setError('');
        const params = new URLSearchParams({
            userId: userId,
            otp: verificationCode,
        });

        try {
            const response = await axios.post(
                `${USER_BASE_PATH}${VERIFY_OTP}${QUESTION}${params.toString()}`
            );
            if (response.data.success) {
                setLoading(false)
                cookies.set("token", response.data.token, { path: "/" });
                navigate("/login");
            } else {
                setLoading(false)
                setError('הקוד שהוזן לא נכון. נסה שוב.');
            }
        } catch (error) {
            console.error('Error during OTP verification:', error);
            setError('שגיאה באימות הקוד. נסה שוב מאוחר יותר');
        }
    };

    return (
        <AuthShell
            eyebrow="Verification · אימות"
            title={<>צעד אחד <span className="gradient-text">ואתה בפנים.</span></>}
            subtitle="שלחנו לך קוד חד-פעמי. הזן אותו כדי לאשר שזה באמת אתה."
        >
            {loading && (
                <div className="modal-backdrop">
                    <div className="modal flex flex-col items-center justify-center gap-6 !max-w-[18rem] py-10">
                        <div className="orbit-loader"><span /></div>
                        <p className="title text-lg text-center">
                            מאמת קוד...
                        </p>
                        <div className="dot-pulse"><span /><span /><span /></div>
                    </div>
                </div>
            )}
            <form
                onSubmit={handleSubmit}
                className="glass glass--strong p-6 sm:p-9 w-full h-full flex flex-col justify-center space-y-7"
            >
                <div className="space-y-1">
                    <span className="eyebrow">02 — קוד חד-פעמי</span>
                    <h2 className="title text-3xl flex items-center gap-3">
                        <span className="icon-badge icon-badge--amber"><FaKey /></span>
                        הכנס את קוד האימות
                    </h2>
                </div>

                <div className="flex justify-center gap-2 sm:gap-3" dir="ltr">
                    {[...Array(5)].map((_, index) => (
                        <input
                            key={index}
                            ref={el => inputsRef.current[index] = el}
                            type="text"
                            maxLength="1"
                            value={verificationCode[index] || ''}
                            onChange={e => handleInputChange(e, index)}
                            onKeyDown={e => handleKeyDown(e, index)}
                            className={`field num !w-12 sm:!w-14 !h-16 !px-0 text-center !text-2xl font-semibold !rounded-2xl
    ${verificationCode[index] ? 'animate-pop !border-accent shadow-[0_0_0_4px_var(--accent-soft)]' : ''}`}                        />
                    ))}
                </div>

                {error && (
                    <p className="notice notice--danger">
                        {error}
                    </p>
                )}

                <button
                    type="submit"
                    className="btn btn-primary btn-block"
                >
                    אמת קוד
                </button>

                <p className="faint text-sm text-center leading-relaxed">
                    הקוד תקף ל-2 דקות בלבד. הקוד נשלח למספר טלפון<br />
                    <span className="num text-ink-2" dir="ltr">{phoneNumber?.slice(0, 3)}******{phoneNumber?.slice(-3)}</span>
                </p>
            </form>
        </AuthShell>
    );
}

export default TwoFactorAuthForm;
