import React from "react";
import notification from "../assets/notification.png";
import "./Submission.css";

const Submission = () => {
  return (
    <div className="submission">
      <header className="header">
        <div className="title-left">
          <span>DOCUMENTS & REVIEWS</span>
          <h2>Submissions</h2>
        </div>

        <div className="title-right">
          
        <button
          className="add-project"
          onClick={() => setShowCreateModal(true)}
        >
          <span>+</span> New Submission
        </button>
          <button className="notification-logo">
            <img src={notification} alt="" />
          </button>

          <button className="profile-logo">RR</button>
        </div>
      </header>
    </div>
  );
};

export default Submission;
  