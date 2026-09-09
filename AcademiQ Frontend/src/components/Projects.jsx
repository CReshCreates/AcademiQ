import "./Projects.css";
import notification from "../assets/notification.png";
import { useState } from "react";

const Projects = () => {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [projects, setProjects] = useState([]);
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    course: "",
    supervisor: "",
  });

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleCreateProject = (e) => {
    e.preventDefault();

    if (!formData.title.trim()) return;

    setProjects([...projects, { ...formData, id: Date.now() }]);

    setFormData({
      title: "",
      description: "",
      course: "",
      supervisor: "",
    });
    setShowCreateModal(false);
  };

  return (
    <div className="project">
      <header className="page-header">
          <div className="title-left">
           <span>CURRENT SEMESTER</span>
          <h2>My Projects</h2>
        </div>
         

       <div className="title-right">
                 
               <button
                 className="add-project"
                 onClick={() => setShowCreateModal(true)}
               >
                 <span>+</span> Create Project
               </button>
                 <button className="notification-logo">
                   <img src={notification} alt="" />
                 </button>
       
                 <button className="profile-logo">RR</button>
               </div>
      </header>

      <div className="project-portfolio">
        <div className="portfolio-details">
          <span>Current Semester</span>
          <h2>Your Academic Portfolio</h2>
          <p>
            You can create and manage multiple projects during the semester.
          </p>
        </div>

        <div className="portfolio-count">
          <strong> {projects.length} </strong>
          <span>Projects</span>
        </div>
      </div>

      <div className="body">
        {projects.length === 0 ? (
          <p className="body-details">No Current Projects</p>
        ) : (
          <div className="projects-list">
            {projects.map((project) => (
              <article className="project-card" key={project.id}>
                <h3> {project.title} </h3>
                <p> {project.description}</p>

                <div className="project-meta">
                  <span> {project.course} </span>
                  <span> {project.supervisor} </span>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>

      {showCreateModal && (
        <div
          className="modal-overlay"
          onClick={() => setShowCreateModal(false)}
        >
          <form
            className="create-modal"
            onSubmit={handleCreateProject}
            onClick={(e) => e.stopPropagation()}
          >

            <button
              type="button"
              className="close-modal"
              onClick={() => setShowCreateModal(false)}
            >
              X
            </button>

            <h2>Create New Project</h2>
            <p>Start a new academic project for this semester.</p>

            <input
              type="text"
              name="title"
              placeholder="Project title"
              value={formData.title}
              onChange={handleChange}
              required
            />


            <textarea
              name="description"
              placeholder="Project description"
              rows="4"
              value={formData.description}
              onChange={handleChange}
            />

            <select
              name="course"
              value={formData.course}
              onChange={handleChange}
              required
            >
              <option value="">Select department / course</option>
              <option value="Bsc.CSIT. Software Engineering">
                Bsc.CSIT. Software Engineering
              </option>
              <option value="Other">Other</option>
            </select>

            <select
              name="supervisor"
              value={formData.supervisor}
              onChange={handleChange}
              required
            >
              <option value="">Request a Supervisor</option>
              <option value="Dr. Resh Raj Chaudary">
                Dr. Resh Raj Chaudary
              </option>
              <option value="Er. Resh Raj Chaudary">
                Er. Resh Raj Chaudary
              </option>
            </select>

            <div className="modal-actions">
              <button
                type="button"
                className="cancel"
                onClick={() => setShowCreateModal(false)}
              >
                Cancel
              </button>

              <button type="submit" className="create-btn">
                Create Project
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

export default Projects;
