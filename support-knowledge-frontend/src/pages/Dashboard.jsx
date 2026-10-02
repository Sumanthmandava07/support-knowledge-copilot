import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

const API_URL = "http://localhost:8080";

function Dashboard() {
    const navigate = useNavigate();
    const fileInputRef = useRef(null);

    const [selectedFile, setSelectedFile] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [uploadStatus, setUploadStatus] = useState("");
    const [documentReady, setDocumentReady] = useState(false);

    const [question, setQuestion] = useState("");
    const [messages, setMessages] = useState([]);
    const [loading, setLoading] = useState(false);

    const [sessionId, setSessionId] = useState(
        localStorage.getItem("copilotSessionId") || ""
    );

    const suggestions = [
        "What is this document about?",
        "What are the main points?",
        "What are the important requirements?",
        "Give me a summary of this document"
    ];

    const handleFileChange = (event) => {
        const file = event.target.files[0];

        if (!file) {
            return;
        }

        setSelectedFile(file);
        setUploadStatus("");
        setDocumentReady(false);
        setMessages([]);
        setQuestion("");
    };

    const uploadDocument = async () => {
    if (!selectedFile) {
        return;
    }

    setUploading(true);
    setUploadStatus("Uploading and processing document...");
    setDocumentReady(false);

    const formData = new FormData();
    formData.append("file", selectedFile);

    try {
        const response = await fetch(
            `${API_URL}/api/documents/upload`,
            {
                method: "POST",
                body: formData
            }
        );

        const responseText = await response.text();

        console.log("Upload status:", response.status);
        console.log("Upload response:", responseText);

        if (!response.ok) {
            throw new Error(
                `Upload failed (${response.status})`
            );
        }

        // The backend successfully processed the document.
        setUploadStatus(
            "✓ Document uploaded and processed successfully. You can now ask questions."
        );

        setDocumentReady(true);

        // Clear previous conversation for the new document.
        setMessages([]);
        setQuestion("");

    } catch (error) {
        console.error("DOCUMENT UPLOAD ERROR:", error);

        setUploadStatus(
            "Unable to upload the document. Check the backend terminal for the actual error."
        );

        setDocumentReady(false);

    } finally {
        setUploading(false);
    }
};

    const askQuestion = async (questionText = question) => {
        const trimmedQuestion = questionText.trim();

        if (!trimmedQuestion || !documentReady || loading) {
            return;
        }

        const userMessage = {
            role: "user",
            text: trimmedQuestion
        };

        setMessages((previous) => [
            ...previous,
            userMessage
        ]);

        setQuestion("");
        setLoading(true);

        try {
            const response = await fetch(
                `${API_URL}/api/chat`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        question: trimmedQuestion,
                        sessionId: sessionId || null
                    })
                }
            );

            if (!response.ok) {
                throw new Error("Chat request failed");
            }

            const data = await response.json();

            if (data.sessionId) {
                setSessionId(data.sessionId);
                localStorage.setItem(
                    "copilotSessionId",
                    data.sessionId
                );
            }

            const assistantMessage = {
                role: "assistant",
                text:
                    data.answer ||
                    "I could not generate an answer.",
                citations: data.citations || [],
                verified: data.verified
            };

            setMessages((previous) => [
                ...previous,
                assistantMessage
            ]);
        } catch (error) {
            console.error(error);

            setMessages((previous) => [
                ...previous,
                {
                    role: "assistant",
                    text:
                        "Unable to connect to the Spring Boot backend. Make sure the backend is running on port 8080.",
                    citations: []
                }
            ]);
        } finally {
            setLoading(false);
        }
    };

    const handleQuestionKeyDown = (event) => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            askQuestion();
        }
    };

    const startNewChat = () => {
        setMessages([]);
        setQuestion("");
        setSessionId("");
        localStorage.removeItem("copilotSessionId");
    };

    const logout = () => {
        localStorage.removeItem("copilotSessionId");
        localStorage.removeItem("copilotUser");
        navigate("/login");
    };

    return (
        <div className="app-layout">

            {/* SIDEBAR */}
            <aside className="sidebar">

                <div className="sidebar-brand">
                    <div className="brand-icon">✦</div>

                    <div>
                        <h2>Knowledge</h2>
                        <span>Copilot</span>
                    </div>
                </div>

                <button
                    className="new-chat-button"
                    onClick={startNewChat}
                >
                    <span>＋</span>
                    New Chat
                </button>

                <nav className="sidebar-nav">

                    <button className="nav-item active">
                        <span>💬</span>
                        Chat
                    </button>

                    <button
                        className="nav-item"
                        onClick={() => navigate("/evaluation")}
                    >
                        <span>📊</span>
                        Evaluation
                    </button>

                </nav>

                <div className="sidebar-bottom">

                    <div className="sidebar-info">
                        <div className="info-icon">✓</div>

                        <div>
                            <strong>Verified Answers</strong>
                            <p>
                                Answers include supporting
                                citations.
                            </p>
                        </div>
                    </div>

                    <button
                        className="logout-button"
                        onClick={logout}
                    >
                        ↪ Logout
                    </button>

                </div>

            </aside>

            {/* MAIN */}
            <main className="main-content">

                {/* HEADER */}
                <header className="top-header">

                    <div>
                        <h1>Knowledge Copilot</h1>

                        <p>
                            Upload a document and ask questions
                            about its contents.
                        </p>
                    </div>

                    <div className="header-avatar">
                        {(
                            localStorage.getItem("copilotUser") ||
                            "S"
                        )
                            .charAt(0)
                            .toUpperCase()}
                    </div>

                </header>

                <div className="dashboard-content">

                    {/* UPLOAD SECTION */}
                    <section className="upload-card">

                        <div className="section-title-row">

                            <div className="section-icon">
                                📄
                            </div>

                            <div>
                                <h2>Upload your document</h2>

                                <p>
                                    Upload a PDF, DOCX, TXT or
                                    Markdown file to start asking
                                    questions.
                                </p>
                            </div>

                        </div>

                        <div
                            className={`upload-area ${
                                selectedFile
                                    ? "file-selected"
                                    : ""
                            }`}
                            onClick={() =>
                                fileInputRef.current?.click()
                            }
                        >

                            <div className="upload-icon">
                                ↑
                            </div>

                            <h3>
                                {selectedFile
                                    ? selectedFile.name
                                    : "Choose a document"}
                            </h3>

                            <p>
                                {selectedFile
                                    ? "Click to choose another file"
                                    : "PDF, DOCX, TXT or MD"}
                            </p>

                            <input
                                ref={fileInputRef}
                                type="file"
                                accept=".pdf,.docx,.txt,.md"
                                onChange={handleFileChange}
                                hidden
                            />

                        </div>

                        {selectedFile && (
                            <div className="selected-file">

                                <div className="file-details">

                                    <div className="file-icon">
                                        📄
                                    </div>

                                    <div>
                                        <strong>
                                            {selectedFile.name}
                                        </strong>

                                        <span>
                                            {(
                                                selectedFile.size /
                                                1024
                                            ).toFixed(1)}{" "}
                                            KB
                                        </span>
                                    </div>

                                </div>

                                <button
                                    className="upload-button"
                                    onClick={uploadDocument}
                                    disabled={uploading}
                                >
                                    {uploading
                                        ? "Processing..."
                                        : "Upload Document"}
                                </button>

                            </div>
                        )}

                        {uploadStatus && (
                            <div
                                className={`upload-status ${
                                    documentReady
                                        ? "success"
                                        : uploadStatus.includes(
                                              "Unable"
                                          )
                                        ? "error"
                                        : "processing"
                                }`}
                            >
                                <span>
                                    {documentReady
                                        ? "✓"
                                        : uploadStatus.includes(
                                              "Unable"
                                          )
                                        ? "!"
                                        : "⏳"}
                                </span>

                                {uploadStatus}
                            </div>
                        )}

                    </section>

                    {/* QUESTION SECTION */}
                    <section
                        className={`question-card ${
                            !documentReady
                                ? "question-disabled"
                                : ""
                        }`}
                    >

                        <div className="section-title-row">

                            <div className="section-icon">
                                💬
                            </div>

                            <div>
                                <h2>Ask about your document</h2>

                                <p>
                                    Ask questions based on the
                                    uploaded document.
                                </p>
                            </div>

                        </div>

                        {!documentReady && (
                            <div className="question-notice">
                                <span>🔒</span>
                                Upload and process a document
                                before asking questions.
                            </div>
                        )}

                        {/* SUGGESTIONS */}
                        {documentReady &&
                            messages.length === 0 && (
                                <div className="suggestions">

                                    <p>
                                        Try asking:
                                    </p>

                                    <div className="suggestion-list">

                                        {suggestions.map(
                                            (suggestion) => (
                                                <button
                                                    key={suggestion}
                                                    onClick={() =>
                                                        askQuestion(
                                                            suggestion
                                                        )
                                                    }
                                                >
                                                    {suggestion}
                                                    <span>
                                                        →
                                                    </span>
                                                </button>
                                            )
                                        )}

                                    </div>

                                </div>
                            )}

                        {/* CHAT MESSAGES */}
                        {messages.length > 0 && (
                            <div className="chat-messages">

                                {messages.map(
                                    (message, index) => (
                                        <div
                                            key={index}
                                            className={`message ${
                                                message.role ===
                                                "user"
                                                    ? "user-message"
                                                    : "assistant-message"
                                            }`}
                                        >

                                            <div className="message-avatar">
                                                {message.role ===
                                                "user"
                                                    ? "S"
                                                    : "✦"}
                                            </div>

                                            <div className="message-content">

                                                <div className="message-name">
                                                    {message.role ===
                                                    "user"
                                                        ? "You"
                                                        : "Knowledge Copilot"}
                                                </div>

                                                <div className="message-text">
                                                    {message.text}
                                                </div>

                                                {/* CITATIONS */}
                                                {message.citations &&
                                                    message.citations
                                                        .length >
                                                        0 && (
                                                        <div className="citations">

                                                            <div className="citations-heading">
                                                                <span>
                                                                    ✓
                                                                </span>
                                                                Verified
                                                                citations
                                                            </div>

                                                            {message.citations.map(
                                                                (
                                                                    citation,
                                                                    citationIndex
                                                                ) => (
                                                                    <div
                                                                        className="citation-card"
                                                                        key={
                                                                            citationIndex
                                                                        }
                                                                    >

                                                                        <div className="citation-top">

                                                                            <strong>
                                                                                {citation.title ||
                                                                                    citation.source ||
                                                                                    "Document"}
                                                                            </strong>

                                                                            <span className="verified-badge">
                                                                                ✓ Verified
                                                                            </span>

                                                                        </div>

                                                                        <div className="citation-meta">
                                                                            {citation.section && (
                                                                                <span>
                                                                                    Section:{" "}
                                                                                    {
                                                                                        citation.section
                                                                                    }
                                                                                </span>
                                                                            )}

                                                                            {citation.page && (
                                                                                <span>
                                                                                    Page:{" "}
                                                                                    {
                                                                                        citation.page
                                                                                    }
                                                                                </span>
                                                                            )}

                                                                            {citation.supportScore !==
                                                                                undefined && (
                                                                                <span>
                                                                                    Support:{" "}
                                                                                    {(
                                                                                        citation.supportScore *
                                                                                        100
                                                                                    ).toFixed(
                                                                                        0
                                                                                    )}
                                                                                    %
                                                                                </span>
                                                                            )}
                                                                        </div>

                                                                        {citation.text && (
                                                                            <p>
                                                                                "
                                                                                {
                                                                                    citation.text
                                                                                }
                                                                                "
                                                                            </p>
                                                                        )}

                                                                        {citation.source && (
                                                                            <small>
                                                                                Source:{" "}
                                                                                {
                                                                                    citation.source
                                                                                }
                                                                            </small>
                                                                        )}

                                                                    </div>
                                                                )
                                                            )}

                                                        </div>
                                                    )}

                                            </div>

                                        </div>
                                    )
                                )}

                                {loading && (
                                    <div className="message assistant-message">

                                        <div className="message-avatar">
                                            ✦
                                        </div>

                                        <div className="message-content">

                                            <div className="message-name">
                                                Knowledge Copilot
                                            </div>

                                            <div className="typing">
                                                <span></span>
                                                <span></span>
                                                <span></span>
                                            </div>

                                        </div>

                                    </div>
                                )}

                            </div>
                        )}

                        {/* QUESTION INPUT */}
                        <div className="question-input-area">

                            <textarea
                                value={question}
                                onChange={(event) =>
                                    setQuestion(
                                        event.target.value
                                    )
                                }
                                onKeyDown={
                                    handleQuestionKeyDown
                                }
                                placeholder={
                                    documentReady
                                        ? "Ask a question about your document..."
                                        : "Upload a document first..."
                                }
                                disabled={
                                    !documentReady ||
                                    loading
                                }
                                rows="1"
                            />

                            <button
                                className="ask-button"
                                onClick={() =>
                                    askQuestion()
                                }
                                disabled={
                                    !documentReady ||
                                    !question.trim() ||
                                    loading
                                }
                            >
                                {loading ? "..." : "Ask"}
                                <span>→</span>
                            </button>

                        </div>

                        <div className="input-hint">
                            Press Enter to ask • Shift + Enter
                            for a new line
                        </div>

                    </section>

                </div>

            </main>

        </div>
    );
}

export default Dashboard;