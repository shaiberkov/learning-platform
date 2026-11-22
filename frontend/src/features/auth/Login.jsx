import React, { useState } from 'react';
import { FaUser, FaLock } from 'react-icons/fa';
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { serverErrorMessage } from "../../Utils/httpSetup.js";
import AuthShell from "../../components/ui/AuthShell.jsx";
import {LOGIN, QUESTION, USER_BASE_PATH} from "../../constants/pages.constants.js";

function Login() {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        userId: '',
        password: '',
    });

    const [error, setError] = useState('');

    const handleChange = (event) => {
        const { name, value } = event.target;
        setFormData({
            ...formData,
            [name]: value
        });
        setError('');
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        const { userId, password } = formData;
        const params = new URLSearchParams({
            userId: userId,
            password: password,
        });

        try {
            const response = await axios.post(
                `${USER_BASE_PATH}${LOGIN}${QUESTION}${params.toString()}`
            );
            if (response.data.success) {

                navigate('/two-factor-auth', { state: { userId: formData.userId, phoneNumber: response.data.data } });
            } else {
                setError(response.data.errorCode);
            }
        } catch (error) {
            console.error('Error during login:', error);
            setError(serverErrorMessage(error) || 'אירעה שגיאה במהלך ההתחברות');
        }
    };

    const hasEmptyFields = () => {
        return Object.values(formData).some(value => value.trim() === '');
    };


    return (
        <AuthShell
            eyebrow="Sign in · כניסה"
            title={<>חוזרים <span className="gradient-text">לזרימה.</span></>}
            subtitle="המרחב האישי שלך ללמידה, תרגול וניהול — שקט, ממוקד ומוכן בדיוק מאיפה שעצרת."
        >
            <form
                onSubmit={handleSubmit}
                className="glass glass--strong p-6 sm:p-9 w-full h-full flex flex-col justify-center space-y-5"
            >
                <div className="space-y-1 mb-2">
                    <span className="eyebrow">01 — זיהוי</span>
                    <h2 className="title text-3xl">
                        התחברות
                    </h2>
                </div>

                <div className="field-wrap">
                    <FaUser/>
                    <input
                        type="text"
                        name="userId"
                        value={formData.userId}
                        onChange={handleChange}
                        placeholder="תעודת זהות"
                        required
                        className="field"
                    />
                </div>

                <div className="field-wrap">
                    <FaLock/>
                    <input
                        type="password"
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        placeholder="סיסמא"
                        required
                        className="field"
                    />
                </div>

                {error && (
                    <p className="notice notice--danger">{error}</p>
                )}
                <button
                    type="submit"
                    disabled={hasEmptyFields()}

                    className="btn btn-primary btn-block"
                >
                    <span className="relative z-10">התחבר</span>
                </button>

                <div className="pt-4 mt-2 border-t border-dashed border-line space-y-2 text-sm">
                    <p className="muted text-center">
                        <a
                            href="/register"
                            className="link"
                        >
                            הרשם כאן
                        </a>{" "}
                        אם עדיין אין לך חשבון
                    </p>

                    <p className="faint text-center">
                        שכחת סיסמה?{" "}
                        <a
                            href="/reset-password"
                            className="link"
                        >
                            לחץ כאן לשחזור
                        </a>
                    </p>
                </div>
            </form>
        </AuthShell>
    );
}

export default Login;
