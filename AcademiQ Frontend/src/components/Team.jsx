import { useState } from "react";
import "./Team.css";
import notification from "../assets/notification.png";

const Team = () => {
  const [showCreateModal, setShowCreateModal] = useState(false);

  return (
    <div className="team">
      <header className="header">
        <div className="title-left">
          <span>COLLABORATION</span>
          <h1>Project Team</h1>
        </div>

        <div className="title-right">
          <button
            className="add-project"
            onClick={() => setShowCreateModal(true)}
          >
            <span>+</span>Invite Member
          </button>

          <button className="notification-logo" aria-label="Notifications">
            <img src={notification} alt="Notifications" />
          </button>

          <button className="profile-logo" aria-label="Profile">
            RR
          </button>
        </div>
      </header>

      <section className="team-empty-state">
        <div className="team-empty-icon">+</div>
        <h2>Build your project team</h2>
        <p>
          Create a project first, then invite members to collaborate with you.
        </p>
      </section>
    </div>
  );
};

export default Team;
