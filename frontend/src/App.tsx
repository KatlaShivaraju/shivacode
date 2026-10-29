
import { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

type Problem = {
  id: number;
  title: string;
  difficulty: string;
  description: string;
  sampleInput: string;
  sampleOutput: string;
};

const difficulties = ['All', 'Easy', 'Medium', 'Hard'];

function App() {
  const [problems, setProblems] = useState<Problem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [difficulty, setDifficulty] = useState('All');

  const navigate = useNavigate();

  useEffect(() => {
    let active = true;

    setLoading(true);
    setError('');

    const params: {
      page: number;
      size: number;
      sort: string;
      difficulty?: string;
    } = {
      page: 0,
      size: 50,
      sort: 'title,asc',
    };

    if (difficulty !== 'All') {
      params.difficulty = difficulty;
    }

    axios
      .get('/api/problems', { params })
      .then((response) => {
        if (active) {
          setProblems(response.data.content);
        }
      })
      .catch(() => {
        if (active) {
          setError(
            'Unable to load problems. Check that the backend is running.',
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [difficulty]);

  return (
    <div className="app">
      <header className="topbar">
        <div
          className="brand"
          onClick={() => navigate('/')}
          role="link"
          tabIndex={0}
          onKeyDown={(event) => {
            if (event.key === 'Enter') navigate('/');
          }}
        >
          <span className="brand-mark">&lt;/&gt;</span>
          <span>ShivaCode</span>
        </div>

        <span className="header-label">CODING PRACTICE</span>
      </header>

      <main className="content">
        <section className="hero">
          <p className="eyebrow">YOUR CODING JOURNEY STARTS HERE</p>

          <h1>
            Sharpen your <span>problem-solving</span> skills.
          </h1>

          <p className="subtitle">
            Practice coding problems, explore solutions, and grow as a
            developer.
          </p>
        </section>

        <section className="problem-section">
          <div className="section-heading">
            <div>
              <h2>Problems</h2>
              <p>Choose a challenge and start thinking.</p>
            </div>

            <span className="problem-count">
              {problems.length} loaded
            </span>
          </div>

          <div className="filters" aria-label="Filter by difficulty">
            {difficulties.map((level) => (
              <button
                key={level}
                type="button"
                className={`filter-button ${
                  difficulty === level ? 'active' : ''
                }`}
                onClick={() => setDifficulty(level)}
                aria-pressed={difficulty === level}
              >
                {level}
              </button>
            ))}
          </div>

          {loading && <p className="status">Loading problems...</p>}

          {error && <p className="error">{error}</p>}

          {!loading && !error && problems.length === 0 && (
            <p className="status">
              No problems found for this difficulty.
            </p>
          )}

          {!loading && !error && problems.length > 0 && (
            <div className="problem-list">
              {problems.map((problem, index) => (
                <article
                  className="problem-card"
                  key={problem.id}
                  onClick={() => navigate(`/problems/${problem.id}`)}
                  role="link"
                  tabIndex={0}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      navigate(`/problems/${problem.id}`);
                    }
                  }}
                >
                  <span className="problem-number">
                    {String(index + 1).padStart(2, '0')}
                  </span>

                  <div className="problem-info">
                    <h3>{problem.title}</h3>
                    <p>{problem.description}</p>
                  </div>

                  <span
                    className={`difficulty ${problem.difficulty.toLowerCase()}`}
                  >
                    {problem.difficulty}
                  </span>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>

      <footer>
        <span>ShivaCode</span>
        <span>Built to make you a better problem solver.</span>
      </footer>
    </div>
  );
}

export default App;


