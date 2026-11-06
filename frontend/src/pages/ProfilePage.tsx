
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getMyProfile } from '../services/profileService';
import type { UserProfile } from '../types/profile';

function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const data = await getMyProfile();
        setProfile(data);
      } catch {
        setError('Unable to load your profile. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const formatDate = (date: string) => {
    if (!date) return 'Not available';

    return new Date(date).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  };

  const acceptanceRate = Math.min(
    100,
    Math.max(0, profile?.acceptanceRate ?? 0),
  );

  return (
    <div className="profile-page">
      <header className="profile-topbar">
        <Link to="/" className="profile-brand">
          <img
            src="/codequest-logo.png"
            alt="CodeQuest"
            className="profile-logo"
          />
        </Link>

        <nav className="profile-nav">
          <Link to="/">Problems</Link>
          <Link to="/dashboard">Dashboard</Link>
          <span className="profile-nav-current">My Profile</span>
          <button onClick={handleLogout}>Logout</button>
        </nav>
      </header>

      <main className="profile-container">
        <div className="profile-heading">
          <div>
            <p className="profile-eyebrow">YOUR ACCOUNT</p>
            <h1>My Profile</h1>
            <p className="profile-subtitle">
              Track your coding journey and submission progress.
            </p>
          </div>

          <Link to="/" className="profile-back-link">
            ← Browse Problems
          </Link>
        </div>

        {loading && (
          <div className="profile-message">
            Loading your profile...
          </div>
        )}

        {error && (
          <div className="profile-error">{error}</div>
        )}

        {profile && !loading && (
          <>
            <section className="profile-user-card">
              <div className="profile-avatar">
                {profile.username.charAt(0).toUpperCase()}
              </div>

              <div className="profile-user-info">
                <span className="profile-member-badge">
                  CODEQUEST MEMBER
                </span>
                <h2>{profile.username}</h2>
                <p>{profile.email}</p>
                <span>
                  Member since {formatDate(profile.memberSince)}
                </span>
              </div>
            </section>

            <section className="profile-stats">
              <article className="profile-stat-card stat-solved">
                <span className="profile-stat-icon">✦</span>
                <span className="profile-stat-label">
                  Problems Solved
                </span>
                <strong>{profile.solvedProblems}</strong>
                <span className="profile-stat-description">
                  Unique problems accepted
                </span>
              </article>

              <article className="profile-stat-card stat-submissions">
                <span className="profile-stat-icon">⌘</span>
                <span className="profile-stat-label">
                  Total Submissions
                </span>
                <strong>{profile.totalSubmissions}</strong>
                <span className="profile-stat-description">
                  All submitted attempts
                </span>
              </article>

              <article className="profile-stat-card stat-accepted">
                <span className="profile-stat-icon">✓</span>
                <span className="profile-stat-label">
                  Accepted
                </span>
                <strong>{profile.acceptedSubmissions}</strong>
                <span className="profile-stat-description">
                  Successful submissions
                </span>
              </article>

              <article className="profile-stat-card stat-rate">
                <span className="profile-stat-icon">↗</span>
                <span className="profile-stat-label">
                  Acceptance Rate
                </span>
                <strong>
                  {profile.acceptanceRate.toFixed(2)}%
                </strong>
                <span className="profile-stat-description">
                  Accepted / total submissions
                </span>
              </article>
            </section>

            <section className="profile-progress-card">
              <div className="profile-section-heading">
                <div>
                  <p className="profile-eyebrow">
                    PERFORMANCE
                  </p>
                  <h2>Your Coding Progress</h2>
                  <p>
                    Your submission acceptance overview.
                  </p>
                </div>

                <div className="profile-rate-circle">
                  <strong>
                    {profile.acceptanceRate.toFixed(1)}%
                  </strong>
                  <span>Success rate</span>
                </div>
              </div>

              <div className="profile-progress-track">
                <div
                  className="profile-progress-fill"
                  style={{ width: `${acceptanceRate}%` }}
                />
              </div>

              <div className="profile-progress-footer">
                <span>
                  <i className="profile-dot accepted-dot" />
                  {profile.acceptedSubmissions} accepted
                </span>

                <span>
                  {profile.totalSubmissions} total submissions
                </span>
              </div>
            </section>

            <div className="profile-bottom-link">
              <p>Ready for your next challenge?</p>
              <Link to="/">Explore Coding Problems →</Link>
            </div>
          </>
        )}
      </main>

      <footer className="profile-footer">
        <span>CodeQuest</span>
        <span>Practice • Solve • Grow</span>
      </footer>
    </div>
  );
}

export default ProfilePage;
