import { BrowserRouter, Navigate, Routes, Route } from "react-router-dom";
import Sidebar from "./components/Sidebar";
import Login from "./components/Login";
import Projects from "./components/Projects";
import Team from "./components/Team.jsx";
import Task from "./components/Task.jsx";
import Resource from "./components/Resource.jsx";
import Feedback from "./components/Feedback.jsx";
import Calendar from "./components/Calendar.jsx";
import Notification from "./components/Notification.jsx";
import Dashboard from "./components/Dashboard.jsx";
import Submission from "./components/Submission.jsx";
import Signup from "./components/Signup.jsx";

import "./App.css";

function DashboardLayout() {
  return (
    <div className="app">
      <Sidebar />

      <main className="main-content">
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/projects" element={<Projects />} />
          <Route path="/team" element={<Team />} />
          <Route path="/task" element={<Task />} />
          <Route path="/resource" element={<Resource />} />
          <Route path="/feedback" element={<Feedback />} />
          <Route path="/notification" element={<Notification />} />
          <Route path="/calendar" element={<Calendar />} />
          <Route path="/submission" element={<Submission />} />
        </Routes>
      </main>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Login page */}
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />

        <Route path="/*" element={<DashboardLayout />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
