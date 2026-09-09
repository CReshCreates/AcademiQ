import React from "react";
import notification from "../assets/notification.png";
import "./Resource.css";

const Resource = () => {
  return (
    <div className="resource">
      <header className="header">
        <div className="title-left">
          <span>PROJECT LIBRARY</span>
          <h2>Resources</h2>
        </div>

        <div className="title-right">
          
        <button
          className="add-project"
          onClick={() => setShowCreateModal(true)}
        >
          <span>+</span> Upload Resource
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

export default Resource;
