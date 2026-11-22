import { FiVolume2 } from "react-icons/fi";
import { canSpeak, speak } from "../Utils/accessibility.js";

// Speaker button that reads a text aloud in Hebrew (Web Speech API). Hidden when unsupported.
export default function ReadAloudButton({ text, label = "הקרא בקול", className = "" }) {
    if (!canSpeak()) return null;
    return (
        <button
            type="button"
            onClick={() => speak(text)}
            className={`btn btn-ghost btn-sm ${className}`}
            aria-label={label}
            title={label}
        >
            <FiVolume2 />
        </button>
    );
}
