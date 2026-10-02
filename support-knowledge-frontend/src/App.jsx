import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Documents from "./pages/Documents";
import Evaluation from "./pages/Evaluation";
import Profile from "./pages/Profile";

function App() {
    return (
        <BrowserRouter>
            <Routes>

                <Route path="/" element={<Navigate to="/login" />} />

                <Route path="/login" element={<Login />} />

                <Route path="/dashboard" element={<Dashboard />} />

                <Route path="/documents" element={<Documents />} />

                <Route path="/evaluation" element={<Evaluation />} />

                <Route path="/profile" element={<Profile />} />

            </Routes>
        </BrowserRouter>
    );
}

export default App;