import React, { useState } from "react";
import axios from "axios";
import AuthShell from "../../components/ui/AuthShell.jsx";
import { useNavigate } from "react-router-dom";
import {FaKey, FaLock, FaUser} from "react-icons/fa";
import {
    AND,
    FORGOT_PASSWORD,
    LOGIN, NEW_PASSWORD, OTP,
    QUESTION,
    RESET_PASSWORD,
    USER_BASE_PATH,
    USER_ID
} from "../../constants/pages.constants.js";

function ResetPassword() {
    const [formData, setFormData] = useState({
        userId: "",
        otp: "",
        newPassword: "",
    });

    const [errors, setErrors] = useState({
        userId: "",
        otp: "",
        password: "",
    });

    const [step, setStep] = useState(1);
    const navigate = useNavigate();

    const validatePassword = (value) => {
        if (value !== '' && !/(?=.*[a-z])(?=.*[A-Z]).{6,}/.test(value)) {
            setErrors(prevErrors => ({
                ...prevErrors,
                password: "הסיסמא חייבת לכלול לפחות אות גדולה וקטנה, ו-6 תווים לפחות"
            }));
        } else {
            setErrors(prevErrors => ({
                ...prevErrors,
                password: ""
            }));
        }
    };

    const setData = (event) => {
        const {name, value} = event.target;
        setFormData(prevData => ({
            ...prevData,
            [name]: value,
        }));

        if (name === "newPassword") {
            validatePassword(value);
        }
    };

    const requestOtp = async () => {
        try {
            const response = await axios.post(
                `${USER_BASE_PATH}${FORGOT_PASSWORD}${QUESTION}${USER_ID}${formData.userId}`
            );
            if (response.data.success) setStep(2);
            else {
                setErrors(prevErrors => ({
                    ...prevErrors,
                    userId: response.data.userIdError || ''
                }));
            }
        } catch (error) {
            console.error(error);
        }
    };

    const resetPassword = async () => {
        try {
            const response = await axios.post(
                `${USER_BASE_PATH}${RESET_PASSWORD}${QUESTION}${USER_ID}${formData.userId}${AND}${OTP}${formData.otp}${AND}newPassword=${formData.newPassword}`
            );

            if (response.data.success) {
                navigate('/login');
            } else {
                setErrors(prevErrors => ({
                    ...prevErrors,
                    otp: response.data.otpError || '',
                    password: response.data.passwordError || ''
                }));
            }
        } catch (error) {
            console.error(error);
        }
    };

    const isButtonDisabled = errors.password || !formData.newPassword || !formData.otp || !formData.userId;
    return (
        <AuthShell
            eyebrow="Recovery · שחזור"
            title={<>מתחילים <span className="gradient-text">מחדש.</span></>}
            subtitle="נשלח קוד לאיפוס, תבחר סיסמה חדשה, וחוזרים ללמוד תוך דקה."
        >
            <form
                onSubmit={(e) => e.preventDefault()}
                className="glass glass--strong p-6 sm:p-9 w-full h-full flex flex-col justify-center space-y-5"
            >
                <div className="space-y-1 mb-2">
                    <span className="eyebrow">{step === 1 ? "שלב 1 / 2" : "שלב 2 / 2"}</span>
                    <h2 className="title text-3xl">
                        איפוס סיסמה
                    </h2>
                    <div className="track !h-1.5 mt-4"><span style={{ width: step === 1 ? "50%" : "100%" }} /></div>
                </div>

                {step === 1 ? (
                    <>
                        {/* ת״ז */}
                        <div>
                            <div className="field-wrap">
                                <FaUser/>
                                <input
                                    name="userId"
                                    type="text"
                                    placeholder="תעודת זהות"
                                    value={formData.userId}
                                    onChange={setData}
                                    required
                                    className="field"
                                />
                            </div>
                            {errors.userId && (
                                <p className="field-error">{errors.userId}</p>
                            )}
                        </div>

                        <button
                            onClick={requestOtp}
                            disabled={formData.userId === ""}
                            className="btn btn-primary btn-block"
                        >
                            <span className="relative z-10">שלח קוד לאיפוס סיסמה</span>
                        </button>
                    </>
                ) : (
                    <>
                        {/* קוד אימות */}
                        <div>
                            <div className="field-wrap">
                                <FaKey/>
                                <input
                                    name="otp"
                                    type="text"
                                    placeholder="קוד אימות"
                                    value={formData.otp}
                                    onChange={setData}
                                    required
                                    className="field num"
                                />
                            </div>
                            {errors.otp && (
                                <p className="field-error">{errors.otp}</p>
                            )}
                        </div>

                        {/* סיסמה חדשה */}
                        <div>
                            <div className="field-wrap">
                                <FaLock/>
                                <input
                                    name="newPassword"
                                    type="password"
                                    placeholder="סיסמה חדשה"
                                    value={formData.newPassword}
                                    onChange={setData}
                                    required
                                    className="field"
                                />
                            </div>
                            {errors.password && (
                                <p className="field-error">{errors.password}</p>
                            )}
                        </div>

                        <button
                            onClick={resetPassword}
                            disabled={isButtonDisabled}
                            className="btn btn-primary btn-block"
                        >
                            <span className="relative z-10">אפס סיסמה</span>
                        </button>
                    </>
                )}
            </form>
        </AuthShell>
    );
}
export default ResetPassword;
