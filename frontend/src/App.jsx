import React, { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import AppRoutes from "./routes/AppRoutes.jsx";
import {UserProvider} from "./context/UserContext.jsx";
import {BrowserRouter} from "react-router-dom";
import {NotificationProvider} from "./context/NotificationContext.jsx";
import {QueryClientProvider} from "@tanstack/react-query";
import Footer from "./components/Footer.jsx";
import ScrollToTop from "./components/ScrollToTop.jsx";
import AmbientBackdrop from "./components/ui/AmbientBackdrop.jsx";
import CommandPalette from "./components/CommandPalette.jsx";
import ToastStack from "./components/notifications/ToastStack.jsx";
import OnboardingTour from "./components/onboarding/OnboardingTour.jsx";
import OfflineBanner from "./components/OfflineBanner.jsx";
import {queryClient} from "./lib/queryClient.js";

function App() {


        return (
            <>
                <AmbientBackdrop />

                <QueryClientProvider client={queryClient}>
                <BrowserRouter>
                    <UserProvider>
                        <NotificationProvider>
                            <ScrollToTop />
                                <AppRoutes />
                                <CommandPalette />
                                <OnboardingTour />
                                <ToastStack />
                                <OfflineBanner />
                        </NotificationProvider>
                    </UserProvider>
                </BrowserRouter>
                </QueryClientProvider>
            </>)


}

export default App
