import { NavLink, useNavigate } from "react-router-dom";

function Sidebar({ onNewChat }) {

    const navigate = useNavigate();

    const user =
        localStorage.getItem("copilotUser") || "Sumanth";

    const logout = () => {

        localStorage.removeItem("copilotUser");

        navigate("/login");
    };

    return (
        <aside className="sidebar">

            <div className="sidebar-brand">

                <div className="brand-icon">
                    ✦
                </div>

                <div>
                    <strong>Knowledge</strong>
                    <span>Copilot</span>
                </div>

            </div>

            <button
                className="new-chat-button"
                onClick={onNewChat}
            >
                <span>＋</span>
                New Chat
            </button>

            <div className="sidebar-section">

                <div className="sidebar-title">
                    WORKSPACE
                </div>

                <NavLink to="/dashboard" className="nav-item">
                    <span>💬</span>
                    Chat
                </NavLink>

                <NavLink to="/documents" className="nav-item">
                    <span>📁</span>
                    Documents
                </NavLink>

                <NavLink to="/evaluation" className="nav-item">
                    <span>📊</span>
                    Evaluation
                </NavLink>

            </div>

            <div className="sidebar-section">

                <div className="sidebar-title">
                    RECENT CHATS
                </div>

                <div className="recent-chat">
                    <span>💬</span>
                    Password reset
                </div>

                <div className="recent-chat">
                    <span>💬</span>
                    Account settings
                </div>

                <div className="recent-chat">
                    <span>💬</span>
                    Product information
                </div>

            </div>

            <div className="sidebar-bottom">

                <NavLink to="/profile" className="profile-mini">

                    <div className="avatar">
                        {user.charAt(0).toUpperCase()}
                    </div>

                    <div>
                        <strong>{user}</strong>
                        <small>Local User</small>
                    </div>

                </NavLink>

                <button
                    className="logout-button"
                    onClick={logout}
                >
                    ↪
                </button>

            </div>

        </aside>
    );
}

export default Sidebar;