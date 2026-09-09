import React from "react";
import notification from "../assets/notification.png";
import "./Calendar.css";

const Calendar = () => {
  const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  const dates = Array.from({ length: 30 }, (_, i) => i + 1);
  const upcomingEvents = [];

  return (
    <div className="calendar">
      <header className="header">
        <div className="title-left">
          <span>DATE & MEETING</span>
          <h2>Calendar</h2>
        </div>

        <div className="title-right">
          <button className="notification-logo">
            <img src={notification} alt="" />
          </button>

          <button className="profile-logo">RR</button>
        </div>
      </header>
      <main className="calendar-content">
        <section className="calendar-card">
          <div className="weekdays">
            {days.map((day) => (
              <div className="weekday" key={day}>
                {day}
              </div>
            ))}
          </div>

          <div className="dates-grid">
            {dates.map((date) => (
              <div
                key={date}
                className={`date-box ${date === 11 ? "active-date" : ""}`}
              >
                {date}
              </div>
            ))}
          </div>
        </section>

        <section className="upcoming-events">
          <h2>Upcoming Events</h2>
          <div className="events">
            {upcomingEvents.length === 0 ? (
              <div className="no-events">No events available</div>
            ) : (
              upcomingEvents.map((event, index) => (
                <div className="event-card" key={index}>
                  <div className={`event-date ${event.color}`}>
                    {event.date}
                  </div>
                  <div className="event-info">
                    <h3>{event.title} </h3>
                    <p>{event.type} </p>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>
      </main>
    </div>
  );
};

export default Calendar;
