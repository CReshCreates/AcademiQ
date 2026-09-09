import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import tape_recorder from "../assets/tape-recorder.png";
import dashboard_img from "../assets/content/dashboard_img.png";
import notifi_img from "../assets/content/notifi_img.png";
import projects_img from "../assets/content/projects_img.png";
import tasks_img from "../assets/content/tasks_img.png";
import submission_img from "../assets/content/submission_img.png";

import "./Login.css";

const Login = () => {
  const navigate = useNavigate();

  const [showLogin, setShowLogin] = useState(false);

  const handleSignIn = () => {
    setShowLogin(true);
  };

  const handleSignUP = () => {
    navigate("/signup");
  };
  const handleSigninSubmit = (e) => {
    e.preventDefault();
    navigate("/dashboard");
  };

  return (
    <div className="login-page">
      <header className="login-header">
        <div className="header-left">
          <img src={tape_recorder} alt="AcademiQ Logo" className="login-logo" />
          <div className="title">
            <h1>AcademiQ</h1>
            <span>PROJECT HUB</span>
          </div>
        </div>

        <div className="header-right">
          <button className="sign-up" onClick={handleSignUP}>
            Sign Up for free
          </button>

          <button className="login" type="button" onClick={handleSignIn}>
            Sign In
          </button>
        </div>
      </header>

      <main className="login-container">
        {/* LEFT SIDE */}
        <div className="container-left">
          <h2>
            Manage. Collaborate.
            <br />
            Build. Succeed.
          </h2>

          <p>
            A smarter way to manage academic projects, collaborate with your
            team, and stay on track from proposal to completion.
          </p>

          <div className="login-buttons">
            <button className="log" type="button" onClick={handleSignIn}>
              Sign In
            </button>

            <button className="sign" onClick={handleSignUP}>
              Sign Up
            </button>
          </div>
        </div>

        {/* RIGHT SIDE */}
        <div className="container-right">
          <div className="container-images">
            <img
              src={notifi_img}
              alt="Notifications"
              className="screen notification-screen"
            />

            <img
              src={dashboard_img}
              alt="Dashboard"
              className="screen dashboard-screen"
            />

            <img
              src={projects_img}
              alt="Projects"
              className="screen projects-screen"
            />

            <img src={tasks_img} alt="Tasks" className="screen tasks-screen" />

            <img
              src={submission_img}
              alt="Submissions"
              className="screen submission-screen"
            />
          </div>
        </div>
      </main>

      {showLogin && (
        <div className="login-overlay">
          <div className="login-modal">
            <button className="close-login" onClick={() => setShowLogin(false)}>
              X
            </button>

            <h2 className="login-welcome">Welcome-back</h2>
            <p className="login-subtitle">Please enter your details</p>
            <form action="">
              <input
                type="email"
                placeholder="Email Address"
                className="login-input"
                required
              />

              <input
                type="password"
                placeholder="Password"
                className="login-input"
                required
              />

              <div className="login-options">
                <label htmlFor="" className="remember">
                  <input type="checkbox" name="" id="" />
                  <span>Remember for 30 days</span>
                </label>

                
              </div>
              <button type="submit" className="modal-signin">
                Sign In
              </button>
            </form>

            <button className="google-login">
              <span className="google-icon">G</span>
              <span>Sign in with Google</span>
            </button>

            <p className="modal-signup">
              Don't have an account?
              <button onClick={handleSignUP}>Sign Up</button>
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default Login;
