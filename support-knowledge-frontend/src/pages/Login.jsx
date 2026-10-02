import { useState } from "react";
import { useNavigate } from "react-router-dom";

function Login() {

    const navigate = useNavigate();

    const [name, setName] = useState("");

    const handleLogin = (event) => {

        event.preventDefault();

        const username = name.trim() || "Sumanth";

        localStorage.setItem("copilotUser", username);

        navigate("/dashboard");
    };

    return (
        <div className="login-page">

            <div className="login-left">

                <div className="brand-large">
                    ✦ Knowledge Copilot
                </div>

                <h1>
                    Your intelligent
                    <span> knowledge assistant.</span>
                </h1>

                <p>
                    Ask questions from your documents and get
                    answers backed by verified evidence.
                </p>

                <div className="login-features">

                    <div>
                        <strong>✓ Verified Citations</strong>
                        <small>Every answer can be traced to evidence.</small>
                    </div>

                    <div>
                        <strong>⚡ Hybrid Retrieval</strong>
                        <small>Vector + keyword search for better results.</small>
                    </div>

                    <div>
                        <strong>📚 Knowledge Base</strong>
                        <small>Search your uploaded documents instantly.</small>
                    </div>

                </div>

            </div>

            <div className="login-card">

                <div className="login-logo">✦</div>

                <h2>Welcome back</h2>

                <p>Enter your name to continue</p>

                <form onSubmit={handleLogin}>

                    <label>Name</label>

                    <input
                        type="text"
                        placeholder="Enter your name"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                    />

                    <button type="submit">
                        Continue
                        <span>→</span>
                    </button>

                </form>

                <div className="demo-login">
                    Demo / Local Login
                </div>

            </div>

        </div>
    );
}

export default Login;