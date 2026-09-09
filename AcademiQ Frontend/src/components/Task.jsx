import React from "react";
import notification from '../assets/notification.png'
import "./Task.css";

const Task = () => {
  return (
    <div className="task">
      <header>
        <div className="title-left">
          <span>PROJECT WORK</span>
          <h2>Tasks</h2>
        </div>
        <div className="title-right">
          
        <button
          className="add-project"
          onClick={() => setShowCreateModal(true)}
        >
          <span>+</span> Create Task
        </button>
          <button className="notification">
            <img src={notification} alt="" />
          </button>

          <button className="profile">RR</button>
        </div>
      </header>
    </div>
  );
};
export default Task;
