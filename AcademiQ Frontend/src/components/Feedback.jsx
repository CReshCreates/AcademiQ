import React from "react";
import notification from "../assets/notification.png";
import "./Feedback.css";

const Feedback = () => {
  return (
    <div className="feedback">
      <header className="header">
        <div className="title-left">
          <span> ACADEMIC PREVIEW</span>
          <h2>Supervisor Feedback</h2>
        </div>
        <div className="title-right">
          <button className="notification-logo">
            <img src={notification} alt="" />
          </button>

          <button className="profile-logo">RR</button>
        </div>
      </header>
    </div>
  );
};

export default Feedback;
