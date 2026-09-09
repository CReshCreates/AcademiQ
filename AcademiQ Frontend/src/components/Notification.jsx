import React from "react";
import notification from '../assets/notification.png';
import "./Notification.css";

const Notification = () => {
  return (
    <div className="notifi">
      <header className="header">
        <div className="title-left">
          <span>ACTIVITY CENTER</span>
          <h2>Notification</h2>
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

export default Notification;
