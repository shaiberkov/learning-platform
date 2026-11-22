import React, { useState } from 'react';
import { FaUser, FaLock } from 'react-icons/fa';
import { useNavigate } from "react-router-dom";
import axios from "axios";
import AuthShell from "../../components/ui/AuthShell.jsx";
import {LOGIN, QUESTION, USER_BASE_PATH} from "../../constants/pages.constants.js";
import { useTranslation } from "react-i18next";

function Login() {
    const navigate = useNavigate();
    const { t } = useTranslation();
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
        }
    };

    const hasEmptyFields = () => {
        return Object.values(formData).some(value => value.trim() === '');
    };


    return (
        <AuthShell
            eyebrow={t("login.eyebrow")}
            title={<>{t("login.titleA")} <span className="gradient-text">{t("login.titleB")}</span></>}
            subtitle={t("login.subtitle")}
        >
            <form
                onSubmit={handleSubmit}
                className="glass glass--strong p-6 sm:p-9 w-full h-full flex flex-col justify-center space-y-5"
            >
                <div className="space-y-1 mb-2">
                    <span className="eyebrow">{t("login.step")}</span>
                    <h2 className="title text-3xl">
                        {t("login.heading")}
                    </h2>
                </div>

                <div className="field-wrap">
                    <FaUser aria-hidden="true"/>
                    <label htmlFor="login-userId" className="sr-only">{t("login.userId")}</label>
                    <input
                        id="login-userId"
                        type="text"
                        name="userId"
                        autoComplete="username"
                        value={formData.userId}
                        onChange={handleChange}
                        placeholder={t("login.userId")}
                        required
                        className="field"
                    />
                </div>

                <div className="field-wrap">
                    <FaLock aria-hidden="true"/>
                    <label htmlFor="login-password" className="sr-only">{t("login.password")}</label>
                    <input
                        id="login-password"
                        type="password"
                        name="password"
                        autoComplete="current-password"
                        value={formData.password}
                        onChange={handleChange}
                        placeholder={t("login.password")}
                        required
                        className="field"
                    />
                </div>

                {error && (
                    <p className="notice notice--danger" role="alert">{error}</p>
                )}
                <button
                    type="submit"
                    disabled={hasEmptyFields()}

                    className="btn btn-primary btn-block"
                >
                    <span className="relative z-10">{t("login.submit")}</span>
                </button>

                <div className="pt-4 mt-2 border-t border-dashed border-line space-y-2 text-sm">
                    <p className="muted text-center">
                        <a
                            href="/register"
                            className="link"
                        >
                            {t("login.registerLink")}
                        </a>{" "}
                        {t("login.registerText")}
                    </p>

                    <p className="faint text-center">
                        {t("login.forgot")}{" "}
                        <a
                            href="/reset-password"
                            className="link"
                        >
                            {t("login.resetLink")}
                        </a>
                    </p>
                </div>
            </form>
        </AuthShell>
    );
}

export default Login;
