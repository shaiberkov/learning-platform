import React, { useRef, useEffect } from "react";

const ChatComponent = ({ messages, userMessage, setUserMessage, sendMessage, isLoading, setIsOpen }) => {
    const messagesEndRef = useRef(null);
    const chatRef = useRef(null);
    const inputRef = useRef(null);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (chatRef.current && !chatRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, [setIsOpen]);

    const handleKeyDown = (event) => {
        if (event.key === "Enter" && userMessage.trim()) {
            sendMessage();
        }
    };

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "auto" });
    }, [messages]);

    return (
        <div ref={chatRef} className="flex flex-col h-full overflow-hidden">
            <div className="flex-1 overflow-y-auto p-4 space-y-3 flex flex-col">
                {messages.map((message, index) => (
                    <div
                        key={index}
                        className={`max-w-[82%] px-4 py-2.5 text-sm leading-relaxed shadow-[inset_0_1px_0_var(--inner)] ${
                            message.sender === "user"
                                ? "self-end text-right ml-auto rounded-[18px] rounded-br-md bg-[linear-gradient(135deg,var(--accent),var(--accent-2))] text-[var(--accent-ink)]"
                                : "self-start text-left mr-auto rounded-[18px] rounded-bl-md bg-surface-sunk border border-line text-ink"
                        }`}
                        style={{ animation: "rise .5s var(--ease-out-expo) both" }}
                    >
                        <strong className="block num text-[0.62rem] uppercase tracking-[0.14em] opacity-70 mb-1">{message.sender === "user" ? "אתה" : "מרצה AI"}:</strong>
                        {message.content}
                    </div>
                ))}
                {isLoading && (
                    <div className="self-start flex items-center gap-3 faint text-sm">
                        <span className="dot-pulse"><span /><span /><span /></span>
                        מרצה AI מקליד...
                    </div>
                )}
                <div ref={messagesEndRef}></div>
            </div>

            <div className="p-3 border-t border-dashed border-line flex items-center gap-2"
            >
                <input
                    ref={inputRef}
                    type="text"
                    value={userMessage}
                    onChange={(e) => setUserMessage(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="כתוב הודעה..."
                    className="field !min-h-[2.75rem] !py-2 text-sm"
                    dir="rtl"
                />
                <button
                    onClick={sendMessage}
                    className="btn btn-primary btn-sm !min-h-[2.75rem]"
                >
                    שלח
                </button>
            </div>
        </div>
    );

};

export default ChatComponent;
