
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

type DailyActivity = {
  date: string;
  submissions: number;
};

type DifficultyStats = {
  difficulty: string;
  solved: number;
};

type RecentSubmission = {
  id: number;
  problemId: number;
  problemTitle: string;
  language: string;
  verdict: string;
  executionTimeMs: number | null;
  submittedAt: string;
};

type DashboardData = {
  dailyActivity: DailyActivity[];
  difficultyStats: DifficultyStats[];
  recentSubmissions: RecentSubmission[];
};

function DashboardPage() {
  const [dashboard, setDashboard] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get<DashboardData>('/api/dashboard')
      .then((response) => {
        setDashboard(response.data);
      })
      .catch((err) => {
        console.error('Dashboard API error:', err);
        setError(
          err.response?.status === 401
            ? 'Your session has expired. Please log in again.'
            : 'Unable to load dashboard. Please check your backend.',
        );
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const totalSubmissions =
    dashboard?.dailyActivity.reduce(
      (total, day) => total + day.submissions,
      0,
    ) ?? 0;

  const totalSolved =
    dashboard?.difficultyStats.reduce(
      (total, item) => total + item.solved,
      0,
    ) ?? 0;

  const getActivityColor = (count: number) => {
    if (count === 0) return '#202b3b';
    if (count <= 2) return '#245b48';
    if (count <= 5) return '#26865c';
    return '#39c982';
  };

  const formatDate = (date: string) =>
    new Date(date).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });

  const formatDateTime = (date: string) =>
    new Date(date).toLocaleString('en-IN', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });

  const difficultyColor = (difficulty: string) => {
    switch (difficulty.toLowerCase()) {
      case 'easy':
        return '#39c982';
      case 'medium':
        return '#eab85b';
      case 'hard':
        return '#f07171';
      default:
        return '#aab5c5';
    }
  };

  return (
    <div className="dashboard-page">
      <header className="dashboard-topbar">
        <Link to="/" className="dashboard-brand">
          <img
            src="/codequest-logo.png"
            alt="CodeQuest"
            className="dashboard-logo"
          />
        </Link>

        <nav className="dashboard-nav">
          <Link to="/">Problems</Link>
          <span className="dashboard-nav-current">Dashboard</span>
          <Link to="/profile">My Profile</Link>
        </nav>
      </header>

      <main className="dashboard-container">
        {loading && (
          <p className="status">Loading your coding dashboard...</p>
        )}

        {error && (
          <section className="dashboard-panel">
            <h2>Dashboard unavailable</h2>
            <p className="error">{error}</p>
            <Link to="/login" className="auth-link">
              Go to Login
            </Link>
          </section>
        )}

        {!loading && !error && !dashboard && (
          <p className="status">No dashboard data available.</p>
        )}

        {!loading && !error && dashboard && (
          <>
            <header className="dashboard-header">
              <div>
                <p className="eyebrow">PRACTICE • SOLVE • GROW</p>
                <h1>Your Coding Dashboard</h1>
                <p className="dashboard-subtitle">
                  Track your consistency, progress and problem-solving activity.
                </p>
              </div>

              <Link to="/" className="auth-link">
                ← Browse Problems
              </Link>
            </header>

            <section className="dashboard-stats">
              <article className="dashboard-stat-card">
                <span>Total Submissions (30 days)</span>
                <strong>{totalSubmissions}</strong>
              </article>

              <article className="dashboard-stat-card">
                <span>Problems Solved</span>
                <strong>{totalSolved}</strong>
              </article>

              <article className="dashboard-stat-card">
                <span>Recent Attempts</span>
                <strong>{dashboard.recentSubmissions.length}</strong>
              </article>
            </section>

            <section className="dashboard-panel">
              <div className="dashboard-section-heading">
                <div>
                  <h2>Daily Coding Activity</h2>
                  <p>Your submission activity over the last 30 days.</p>
                </div>
                <span>{totalSubmissions} submissions</span>
              </div>

              <div className="activity-grid">
                {dashboard.dailyActivity.map((day) => (
                  <div
                    key={day.date}
                    className="activity-cell"
                    style={{
                      backgroundColor: getActivityColor(day.submissions),
                    }}
                    title={`${formatDate(day.date)}: ${day.submissions} submissions`}
                  />
                ))}
              </div>

              <div className="activity-legend">
                <span>Less</span>
                {[0, 1, 3, 6].map((count) => (
                  <span
                    key={count}
                    className="activity-legend-cell"
                    style={{ backgroundColor: getActivityColor(count) }}
                  />
                ))}
                <span>More</span>
              </div>
            </section>

            <section className="dashboard-panel">
              <div className="dashboard-section-heading">
                <div>
                  <h2>Difficulty Breakdown</h2>
                  <p>Unique problems successfully accepted.</p>
                </div>
              </div>

              <div className="difficulty-breakdown">
                {['Easy', 'Medium', 'Hard'].map((level) => {
                  const stat = dashboard.difficultyStats.find(
                    (item) =>
                      item.difficulty.toLowerCase() === level.toLowerCase(),
                  );

                  const solved = stat?.solved ?? 0;
                  const percentage =
                    totalSolved > 0 ? (solved / totalSolved) * 100 : 0;

                  return (
                    <div className="difficulty-row" key={level}>
                      <div className="difficulty-row-heading">
                        <span style={{ color: difficultyColor(level) }}>
                          {level}
                        </span>
                        <strong>{solved}</strong>
                      </div>

                      <div className="difficulty-progress">
                        <div
                          style={{
                            width: `${percentage}%`,
                            backgroundColor: difficultyColor(level),
                          }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>

            <section className="dashboard-panel">
              <div className="dashboard-section-heading">
                <div>
                  <h2>Recent Submissions</h2>
                  <p>Your latest 10 coding attempts.</p>
                </div>
              </div>

              {dashboard.recentSubmissions.length === 0 ? (
                <p className="status">
                  No submissions yet. Start solving problems!
                </p>
              ) : (
                <div className="submission-table-wrapper">
                  <table className="submission-table">
                    <thead>
                      <tr>
                        <th>Problem</th>
                        <th>Language</th>
                        <th>Verdict</th>
                        <th>Runtime</th>
                        <th>Submitted</th>
                      </tr>
                    </thead>

                    <tbody>
                      {dashboard.recentSubmissions.map((submission) => (
                        <tr key={submission.id}>
                          <td>
                            <Link to={`/problems/${submission.problemId}`}>
                              {submission.problemTitle}
                            </Link>
                          </td>

                          <td>{submission.language}</td>

                          <td>
                            <span
                              className={`submission-verdict ${
                                submission.verdict.toLowerCase() === 'accepted'
                                  ? 'accepted'
                                  : 'rejected'
                              }`}
                            >
                              {submission.verdict}
                            </span>
                          </td>

                          <td>
                            {submission.executionTimeMs != null
                              ? `${submission.executionTimeMs} ms`
                              : '—'}
                          </td>

                          <td>{formatDateTime(submission.submittedAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </>
        )}
      </main>

      <footer className="dashboard-footer">
        <span>CodeQuest</span>
        <span>Practice • Solve • Grow</span>
      </footer>
    </div>
  );
}

export default DashboardPage;

