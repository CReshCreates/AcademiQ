import React from 'react';
import './Dashboard.css';
import star from '../assets/star.png'
import notification from '../assets/notification.png'

const Dashboard = () => {
  return (
    <div className='dashboard'>
        <header>
            <div className='title-left'>
              <span>DASHBOARD</span>
              <h2>Good Morning, Resh<img className='star' src={star} alt="" /></h2>
                
            </div>

            <div className="title-right">

              
        <button
          className="add-project"
          onClick={() => setShowCreateModal(true)}
        >
          <span>+</span> New Task
        </button>
                      <button className="notification">
                        <img src={notification} alt="" />
                      </button>
            
                      <button className="profile">RR</button>
                    </div>
        </header>
    </div>
  )
}

export default Dashboard;