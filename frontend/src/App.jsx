import React, { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import AppRoutes from "./routes/AppRoutes.jsx";
import {UserProvider} from "./context/UserContext.jsx";
import {BrowserRouter} from "react-router-dom";
import {NotificationProvider} from "./context/NotificationContext.jsx";
import Footer from "./components/Footer.jsx";
import ScrollToTop from "./components/ScrollToTop.jsx";
import AmbientBackdrop from "./components/ui/AmbientBackdrop.jsx";

function App() {


        return (
            <>
                <AmbientBackdrop />

                <BrowserRouter>
                    <UserProvider>
                        <NotificationProvider>
                            <ScrollToTop />
                                <AppRoutes />
                        </NotificationProvider>
                    </UserProvider>
                </BrowserRouter>
            </>)


}

export default App
