import { useNavigate } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";

function Profile() {

    const navigate = useNavigate();

    const user =
        localStorage.getItem("copilotUser") || "Sumanth";

    const logout = () => {

        localStorage.clear();

        navigate("/login");
    };

    return (
        <div className="app-layout">

            <Sidebar />

            <main className="main-content">

                <Header
                    title="Profile"
                    subtitle="Your local copilot profile"
                />

                <div className="page-container">

                    <div className="profile-card">

                        <div className="profile-avatar">
                            {user.charAt(0).toUpperCase()}
                        </div>

                        <h1>{user}</h1>

                        <p>
                            Local Knowledge Copilot User
                        </p>

                        <div className="profile-detail">
                            <span>Account type</span>
                            <strong>Demo / Local</strong>
                        </div>

                        <div className="profile-detail">
                            <span>Authentication</span>
                            <strong>Frontend only</strong>
                        </div>

                        <button
                            className="danger-button"
                            onClick={logout}
                        >
                            Log out
                        </button>

                    </div>

                </div>

            </main>

        </div>
    );
}

export default Profile;