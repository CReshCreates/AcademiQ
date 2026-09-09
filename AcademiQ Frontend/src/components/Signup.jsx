import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import tape_recorder from "../assets/tape-recorder.png";
import people from "../assets/people.png";
import line_chart from "../assets/line-chart.png";
import checked from "../assets/checked.png";
import background_img from "../assets/background_img.png";
import Signup_logo from "../assets/Signup_logo.png";
import "./Signup.css";

const Signup = () => {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  useEffect(() => {
    const previousBodyOverflow = document.body.style.overflow;
    const previousDocumentOverflow = document.documentElement.style.overflow;

    document.body.style.overflow = "hidden";
    document.documentElement.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousBodyOverflow;
      document.documentElement.style.overflow = previousDocumentOverflow;
    };
  }, []);

  return (
    <div
      className="signup-page"
      style={{ backgroundImage: `url(${background_img})` }}
    >
      <div className="signup-layout">
        <section className="signup-intro">
          <div>
            <div className="signup-brand">
              <img
                className="signup-brand-logo"
                src={tape_recorder}
                alt="AcademiQ logo"
              />
              <div>
                <h1 className="signup-brand-name">AcademiQ</h1>
                <span className="signup-brand-subtitle">PROJECT HUB</span>
              </div>
            </div>

            <div className="signup-copy">
              <div className="signup-badge">
                <span className="signup-badge-dot" />
                Smart Project Management for Students
              </div>
              <h2 className="signup-headline">
                From Ideas to <span>Successful Projects.</span>
              </h2>
              <p className="signup-description">
                Plan, collaborate, track, and complete your academic
                projects--all in one place.
              </p>

              <div className="signup-features">
                <Feature
                  icon={people}
                  title="Collaborate Seamlessly"
                  text="Work together with your team in real time."
                />
                <Feature
                  icon={checked}
                  title="Stay Organized"
                  text="Manage tasks, milestones, and deadlines easily."
                />
                <Feature
                  icon={line_chart}
                  title="Track Your Progress"
                  text="Monitor updates and feedback in one dashboard."
                />
              </div>
            </div>
          </div>

          <img
            className="signup-illustration"
            src={Signup_logo}
            alt="Students collaborating on an academic project"
          />
        </section>

        <section className="signup-card">
          <div className="signup-form-content">
            <h2 className="signup-form-title">Create your account</h2>
            <p className="signup-form-subtitle">
              Join AcademiQ and start managing your academic projects with ease.
            </p>

            <form
              className="signup-form"
              onSubmit={(event) => event.preventDefault()}
            >
              <Field
                label="Full Name"
                icon={<UserIcon />}
                type="text"
                placeholder="Enter your full name"
              />
              <Field
                label="Email Address"
                icon={<MailIcon />}
                type="email"
                placeholder="Enter your email address"
              />
              <div className="signup-field-row">
                <label className="signup-field">
                  Batch Year
                  <span className="signup-input-wrap">
                    <span className="signup-input-icon">
                      <CalendarIcon />
                    </span>
                    <select
                      required
                      name="batch"
                      defaultValue=""
                      className="signup-input signup-select"
                    >
                      <option value="" disabled>
                        Select batch year
                      </option>
                      <option value="2080">2080</option>
                      <option value="2081">2081</option>
                    </select>
                  </span>
                </label>
                <label className="signup-field">
                  Section
                  <span className="signup-input-wrap">
                    <span className="signup-input-icon">
                      <UserIcon />
                    </span>
                    <select
                      required
                      name="section"
                      defaultValue=""
                      className="signup-input signup-select"
                    >
                      <option value="" disabled>
                        Select section
                      </option>
                      <option value="A">A</option>
                      <option value="B">B</option>
                      <option value="C">C</option>
                    </select>
                  </span>
                </label>
              </div>

              <PasswordField
                label="Password"
                placeholder="Create a password"
                visible={showPassword}
                onToggle={() => setShowPassword(!showPassword)}
              />
              <p className="signup-password-hint">
                Must be at least 8 characters
              </p>
              <PasswordField
                label="Confirm Password"
                placeholder="Confirm your password"
                visible={showConfirmPassword}
                onToggle={() => setShowConfirmPassword(!showConfirmPassword)}
              />

              <div className="signup-account-note">
                <ShieldIcon />
                <span>
                  Your account will be created as a <b>Student</b> by default.
                </span>
              </div>

              <label className="signup-terms">
                <input required type="checkbox" className="signup-checkbox" />
                <span>
                  I agree to the <a href="#terms">Terms &amp; Conditions</a> and{" "}
                  <a href="#privacy">Privacy Policy</a>
                </span>
              </label>

              <button type="submit" className="signup-submit">
                Create Account
              </button>
            </form>

            <div className="signup-divider">
              <span />
              OR
              <span />
            </div>
            <p className="signup-signin">
              Already have an account?{" "}
              <button
                type="button"
                onClick={() => navigate("/login")}
                className="signup-link-button"
              >
                Sign In
              </button>
            </p>
          </div>
        </section>
      </div>
    </div>
  );
};

const Feature = ({ icon, title, text }) => (
  <div className="signup-feature">
    <span className="signup-feature-icon">
      <img src={icon} alt="" />
    </span>
    <div>
      <h3>{title}</h3>
      <p>{text}</p>
    </div>
  </div>
);

const Field = ({ label, icon, ...props }) => (
  <label className="signup-field">
    {label}
    <span className="signup-input-wrap">
      <span className="signup-input-icon">{icon}</span>
      <input required {...props} className="signup-input" />
    </span>
  </label>
);

const PasswordField = ({ label, visible, onToggle, ...props }) => (
  <label className="signup-field">
    {label}
    <span className="signup-input-wrap">
      <span className="signup-input-icon">
        <LockIcon />
      </span>
      <input
        required
        {...props}
        type={visible ? "text" : "password"}
        className="signup-input signup-password-input"
      />
      <button
        type="button"
        onClick={onToggle}
        aria-label={visible ? "Hide password" : "Show password"}
        className="signup-eye-button"
      >
        <EyeIcon />
      </button>
    </span>
  </label>
);

const UserIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    className="signup-icon"
  >
    <circle cx="12" cy="8" r="3.5" />
    <path d="M5 20c.7-3.3 3-5 7-5s6.3 1.7 7 5" />
  </svg>
);
const MailIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    className="signup-icon"
  >
    <rect x="3" y="5" width="18" height="14" rx="2" />
    <path d="m4 7 8 6 8-6" />
  </svg>
);
const CalendarIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    className="signup-icon"
  >
    <rect x="3.5" y="5" width="17" height="16" rx="2" />
    <path d="M7.5 3.5v3M16.5 3.5v3M3.5 9.5h17" />
  </svg>
);
const LockIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    className="signup-icon"
  >
    <rect x="5" y="10" width="14" height="10" rx="2" />
    <path d="M8 10V7a4 4 0 0 1 8 0v3" />
  </svg>
);
const EyeIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    className="signup-icon"
  >
    <path d="M2.5 12s3.2-5 9.5-5 9.5 5 9.5 5-3.2 5-9.5 5-9.5-5-9.5-5Z" />
    <circle cx="12" cy="12" r="2.2" />
  </svg>
);
const ShieldIcon = () => (
  <svg viewBox="0 0 24 24" fill="currentColor" className="signup-shield-icon">
    <path d="M12 2 20 5v6c0 5.2-3.4 9.3-8 11-4.6-1.7-8-5.8-8-11V5l8-3Zm0 3.1L8 6.6v4.2c0 3.6 2.1 6.5 4 7.6 1.9-1.1 4-4 4-7.6V6.6l-4-1.5Z" />
  </svg>
);

export default Signup;
