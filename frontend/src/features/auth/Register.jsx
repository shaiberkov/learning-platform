import React, {useState} from 'react';
import {FaUser, FaEnvelope, FaLock, FaKey, FaPhone} from 'react-icons/fa';
import {useNavigate} from 'react-router-dom';
import axios from "axios";
import AuthShell from "../../components/ui/AuthShell.jsx";
import {getFieldErrors} from "../../api/httpErrors.js";
import {ADD_USER, LOGIN, QUESTION, USER_BASE_PATH} from "../../constants/pages.constants.js";

function Register() {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        username: '',
        userId: '',
        email: '',
        password: '',
        confirmPassword: '',
        phone: '',
    });

    const [errors, setErrors] = useState({
        username: '',
        userId: '',
        email: '',
        password: '',
        confirmPassword: '',
        phone: '',
    });

    const handleChange = (event) => {
        const {name, value} = event.target;

        setFormData({
            ...formData,
            [name]: value,
        });

        let formErrors = {...errors};

        if (name === 'username') {
            if (value !== '' && !/^[a-zA-Zא-ת\s]+$/.test(value)) {
                formErrors.username = 'השם חייב להיות מורכב מאותיות בלבד, ללא מספרים';
            } else if (errors.username === 'Username Taken') {
                formErrors.username = '';
            } else {
                formErrors.username = '';
            }
        }

        if (name === 'userId') {
            const id = value.trim();
            const isValidIsraeliID = (id) => {
                // if (!/^\d{5,9}$/.test(id)) return false;
                // let fullId = id.padStart(9, '0');
                // let sum = 0;
                // for (let i = 0; i < 9; i++) {
                //     let num = Number(fullId[i]) * ((i % 2) + 1);
                //     if (num > 9) num -= 9;
                //     sum += num;
                // }
                // return sum % 10 === 0;
                return true
            };

            if (id !== '' && !isValidIsraeliID(id)) {
                formErrors.userId = 'תעודת זהות לא תקינה';

            } else {
                formErrors.userId = '';
            }
        }


        if (name === 'email') {
            if (value !== '' && !/\S+@\S+\.\S+/.test(value)) {
                formErrors.email = 'האימייל לא תקין, יש לוודא שהוא כולל @';
            } else if (errors.email === 'אימייל זה תפוס' || errors.email === 'אימייל לא תקין') {
                formErrors.email = '';
            } else {
                formErrors.email = '';
            }
        }

        if (name === 'password') {
            if (value !== '' && !/(?=.*[a-z])(?=.*[A-Z]).{6,}/.test(value)) {
                formErrors.password = 'הסיסמא חייבת לכלול לפחות אות גדולה וקטנה, ו-6 תווים לפחות';
            } else {
                formErrors.password = '';
            }
        }

        if (name === 'confirmPassword') {
            if (value !== '' && value !== formData.password) {
                formErrors.confirmPassword = 'הסיסמאות לא תואמות';
            } else if (errors.confirmPassword === 'סיסמאות לא זהות') {
                formErrors.confirmPassword = '';
            } else {
                formErrors.confirmPassword = '';
            }
        }

        if (name === 'phone') {
            if (value !== '' && !/^\d{10}$/.test(value)) {
                formErrors.phone = 'מספר הטלפון חייב לכלול 10 ספרות בלבד';
            } else if (errors.phone === 'טלפון זה תפוס') {
                formErrors.phone = '';
            } else {
                formErrors.phone = '';
            }
        }

        setErrors(formErrors);
    };

    const validateForm = () => {
        return Object.values(errors).every(error => error === '') &&
            Object.values(formData).every(value => value !== '');
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!validateForm()) {
            return;
        }

        try {
            const {username, userId, password, confirmPassword, email, phone} = formData;
            const params = new URLSearchParams({
                username: username,
                userId: userId,
                password: password,
                confirmPassword: confirmPassword,
                email: email,
                phone: phone,
            });

            const response = await axios.post(
                `${USER_BASE_PATH}${ADD_USER}${QUESTION}${params.toString()}`

            );
            console.log("Response:", response.data);
            if (response.data.success) {
                navigate('/login');
            } else {
                setErrors((prevErrors) => ({
                    ...prevErrors,
                    userId: response.data.idTaken || '',
                    email: response.data.emailTaken || '',
                    phone: response.data.phoneTaken || '',
                    password: response.data.passwordDontMatch || '',
                    confirmPassword: response.data.passwordDontMatch || '',
                }));

            }

        } catch (error) {
            // The message itself is shown by ErrorToaster; field errors from the server go next to their fields.
            console.error("Error during the request:", error);
            const fieldErrors = getFieldErrors(error);
            setErrors((prevErrors) => {
                const next = { ...prevErrors };
                Object.keys(prevErrors).forEach((field) => {
                    if (fieldErrors[field]) next[field] = fieldErrors[field];
                });
                return next;
            });
        }
    };

    const hasEmptyFields = () => {
        return Object.values(formData).some(value => value.trim() === '');
    };

    return (

        <AuthShell
            eyebrow="Create account · הרשמה"
            title={<>מרחב שלם <span className="gradient-text">ללמוד בו.</span></>}
            subtitle="חשבון אחד לכל הכלים: מערכת שעות, תרגול אינטראקטיבי, מבחנים והודעות מבית הספר."
        >
            <form
                onSubmit={handleSubmit}
                className="glass glass--strong p-6 sm:p-9 w-full space-y-4"
            >
                <div className="space-y-1 mb-3">
                    <span className="eyebrow">01 — פרטים אישיים</span>
                    <h2 className="title text-3xl">
                        הרשמה
                    </h2>
                </div>

                {/* שם משתמש */}
                <div>
                    <div className="field-wrap">
                        <FaUser/>
                        <input
                            type="text"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            placeholder="שם מלא"
                            required
                            className="field"
                        />
                    </div>
                    {errors.username && formData.username && (
                        <p className="field-error">{errors.username}</p>
                    )}
                </div>

                <div className="grid sm:grid-cols-2 gap-4">
                    {/* ת"ז */}
                    <div>
                        <div className="field-wrap">
                            <FaUser/>
                            <input
                                type="number"
                                name="userId"
                                value={formData.userId}
                                onChange={handleChange}
                                placeholder="תז"
                                required
                                className="field num"
                            />
                        </div>
                        {errors.userId && formData.userId && (
                            <p className="field-error">{errors.userId}</p>
                        )}
                    </div>

                    {/* טלפון */}
                    <div>
                        <div className="field-wrap">
                            <FaPhone/>
                            <input
                                type="tel"
                                name="phone"
                                value={formData.phone}
                                onChange={handleChange}
                                placeholder="מספר טלפון"
                                required
                                className="field num"
                            />
                        </div>
                        {errors.phone && formData.phone && (
                            <p className="field-error">{errors.phone}</p>
                        )}
                    </div>
                </div>

                {/* אימייל */}
                <div>
                    <div className="field-wrap">
                        <FaEnvelope/>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            placeholder="אימייל"
                            required
                            className="field"
                        />
                    </div>
                    {errors.email && formData.email && (
                        <p className="field-error">{errors.email}</p>
                    )}
                </div>

                <div className="grid sm:grid-cols-2 gap-4">
                    {/* סיסמה */}
                    <div>
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
                        {errors.password && formData.password && (
                            <p className="field-error">{errors.password}</p>
                        )}
                    </div>

                    {/* אימות סיסמה */}
                    <div>
                        <div className="field-wrap">
                            <FaKey/>
                            <input
                                type="password"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                placeholder="אימות סיסמא"
                                required
                                className="field"
                            />
                        </div>
                        {errors.confirmPassword && formData.confirmPassword && (
                            <p className="field-error">
                                {errors.confirmPassword}
                            </p>
                        )}
                    </div>
                </div>


                <button
                    type="submit"
                    disabled={hasEmptyFields()}
                    className="btn btn-primary btn-block !mt-6"
                >
                    <span className="z-10 relative">הרשם</span>
                </button>

                <p className="muted text-center text-sm pt-4 border-t border-dashed border-line">
                    <a
                        href="/login"
                        className="link"
                    >
                        התחבר כאן
                    </a>{" "}?
                    אם כבר יש לך חשבון
                </p>


            </form>
        </AuthShell>

    );
}

export default Register;
