
import React, { useLayoutEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import tape_recorder from "./assets/tape-recorder.png";
import dashboard_img from "./assets/content/dashboard_img.png";
import notifi_img from "./assets/content/notifi_img.png";
import projects_img from "./assets/content/projects_img.png";
import tasks_img from "./assets/content/tasks_img.png";
import submission_img from "./assets/content/submission_img.png";

import "./Login.css";
import "./LandingPage.css";

const Login = () => {
  const navigate = useNavigate();

  const [showLogin, setShowLogin] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState("");
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  useLayoutEffect(() => {
    if ("scrollRestoration" in window.history) {
      window.history.scrollRestoration = "manual";
    }

    window.scrollTo(0, 0);
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;
  }, []);

  const handleSignIn = () => {
    setLoginError("");
    setShowLogin(true);
  };

  const handleSignUp = () => {
    navigate("/signup");
  };

  const handleSigninSubmit = async (event) => {
    event.preventDefault();

    if (isLoggingIn) return;

    setLoginError("");
    setIsLoggingIn(true);

    try {
      const response = await fetch("/users/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: email.trim(),
          password,
        }),
      });

      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          throw new Error("Invalid email or password.");
        }

        throw new Error(
          `Login failed (HTTP ${response.status}). Please try again.`
        );
      }

      const data = await response.json();

      if (!data.token || !data.role || !data.email) {
        throw new Error("Invalid login response from the server.");
      }


      const role = data.role
        .replace(/^ROLE_/, "")
        .toUpperCase();

      // Clear any workspace previously selected
      // by another logged-in account.
      sessionStorage.removeItem("academiq_workspace");

      // Preserve the existing authentication session.
      // The global role is retained for compatibility;
      // workspace roles will control portal navigation.
      sessionStorage.setItem(
        "academiq_auth",
        JSON.stringify({
          token: data.token,
          fullName: data.fullName,
          email: data.email,
          role,
        })
      );

      // Workspace selection now determines which portal
      // the user can enter.
      navigate("/workspaces/select", { replace: true });


    } catch (error) {
      setLoginError(
        error instanceof Error
          ? error.message
          : "Unable to sign in. Please try again."
      );
    } finally {
      setIsLoggingIn(false);
    }
  };

  return (
    <div className="aq-landing">
      <header className="aq-landing__header">
        <div className="aq-landing__nav">
          <div
            className="aq-landing__brand"
            aria-label="AcademiQ Project Hub"
          >
            <img
              src={tape_recorder}
              alt=""
              className="aq-landing__logo"
            />

            <div className="aq-landing__brand-text">
              <strong>AcademiQ</strong>
              <span>PROJECT HUB</span>
            </div>
          </div>

          <nav
            className="aq-landing__nav-actions"
            aria-label="Account navigation"
          >
            <button
              type="button"
              className="aq-landing__nav-signup"
              onClick={handleSignUp}
            >
              Sign up for free
            </button>

            <button
              type="button"
              className="aq-landing__nav-signin"
              onClick={handleSignIn}
            >
              Sign In
            </button>
          </nav>
        </div>
      </header>

      <main className="aq-landing__hero">
        <div className="aq-landing__content">
          <h1>
            Manage. Collaborate.
            <br />
            Build. Succeed.
          </h1>

          <p>
            A smarter way to manage academic projects,
            collaborate with your team, and stay on
            track from proposal to completion.
          </p>

          <div className="aq-landing__actions">
            <button type="button" onClick={handleSignIn}>
              Sign In
            </button>

            <button type="button" onClick={handleSignUp}>
              Sign Up
            </button>
          </div>
        </div>

        <div
          className="aq-landing__preview"
          aria-label="AcademiQ project management previews"
        >
          <img
            src={tasks_img}
            alt="Task management screen"
            className="aq-landing__preview-tasks"
          />

          <img
            src={notifi_img}
            alt="Notifications screen"
            className="aq-landing__preview-notifications"
          />

          <img
            src={dashboard_img}
            alt="Student dashboard screen"
            className="aq-landing__preview-dashboard"
          />

          <img
            src={projects_img}
            alt="Project milestones screen"
            className="aq-landing__preview-projects"
          />

          <img
            src={submission_img}
            alt="Supervisor review screen"
            className="aq-landing__preview-submissions"
          />
        </div>
      </main>

      {showLogin && (
        <div className="login-overlay">
          <div
            className="login-modal"
            role="dialog"
            aria-modal="true"
            aria-label="Sign in"
          >
            <button
              className="close-login"
              type="button"
              aria-label="Close sign in"
              onClick={() => setShowLogin(false)}
            >
              X
            </button>

            <h2 className="login-welcome">
              Welcome-back
            </h2>

            <p className="login-subtitle">
              Please enter your details
            </p>

            <form onSubmit={handleSigninSubmit}>
              <input
                type="email"
                placeholder="Email Address"
                className="login-input"
                value={email}
                onChange={(event) =>
                  setEmail(event.target.value)
                }
                autoComplete="email"
                required
              />

              <input
                type="password"
                placeholder="Password"
                className="login-input"
                value={password}
                onChange={(event) =>
                  setPassword(event.target.value)
                }
                autoComplete="current-password"
                required
              />

              {loginError && (
                <p
                  role="alert"
                  style={{
                    color: "#dc2626",
                    fontSize: "13px",
                    marginBottom: "14px",
                  }}
                >
                  {loginError}
                </p>
              )}

              <button
                type="submit"
                className="modal-signin"
                disabled={isLoggingIn}
              >
                {isLoggingIn ? "Signing In..." : "Sign In"}
              </button>
            </form>

            <button
              type="button"
              className="google-login"
              disabled
              title="Google sign-in is not available yet"
              style={{ opacity: 0.6, cursor: "not-allowed" }}
            >
              <span className="google-icon">G</span>
              <span>Sign in with Google</span>
            </button>

            <p className="modal-signup">
              Don't have an account?{" "}
              <button
                type="button"
                onClick={handleSignUp}
              >
                Sign Up
              </button>
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default Login;
