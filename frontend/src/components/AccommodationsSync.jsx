import { useContext, useEffect } from "react";
import { UserContext } from "../context/UserContext.jsx";
import { applyDyslexiaFont, storedDyslexiaFont, useAccommodations } from "../Utils/accessibility.js";

// Applies the student's dyslexia-friendly font on every page: the local copy at once, then the
// server's setting once it arrives. Renders nothing.
export default function AccommodationsSync() {
    const { user } = useContext(UserContext);
    const { accommodations, loaded } = useAccommodations(user);

    useEffect(() => {
        applyDyslexiaFont(storedDyslexiaFont());
    }, []);

    useEffect(() => {
        if (!loaded) return;
        if (user?.role === "STUDENT") applyDyslexiaFont(accommodations.dyslexiaFont);
        else if (user) applyDyslexiaFont(false);
    }, [loaded, user, accommodations.dyslexiaFont]);

    return null;
}
