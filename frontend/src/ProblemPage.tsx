
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import Editor from '@monaco-editor/react';

type Problem = {
  id: number;
  title: string;
  difficulty: string;
  description: string;
  sampleInput: string;
  sampleOutput: string;
};

type ExecutionResponse = {
  status: string;
  output: string;
  error: string;
  executionTimeMs: number;
};

type SubmissionResponse = {
  verdict: string;
  totalTestCases: number;
  passedTestCases: number;
  message: string;
  executionTimeMs: number;
};

type SubmissionHistoryItem = {
  id: number;
  problemId: number;
  language: string;
  verdict: string;
  executionTimeMs: number | null;
  submittedAt: string;
};

const starterCode = `class Solution {
    public static void main(String[] args) {
        // Write your Java solution here

    }
}`;

function ProblemPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [problem, setProblem] = useState<Problem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [code, setCode] = useState(starterCode);
  const [output, setOutput] = useState('');
  const [isRunning, setIsRunning] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [submission, setSubmission] =
    useState<SubmissionResponse | null>(null);

  const [submissionHistory, setSubmissionHistory] =
    useState<SubmissionHistoryItem[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState('');

  // Load the selected problem.
  useEffect(() => {
    let active = true;

    const fetchProblem = async () => {
      setLoading(true);
      setError('');
      setProblem(null);
      setCode(starterCode);
      setOutput('');
      setSubmission(null);
      setSubmissionHistory([]);
      setHistoryError('');

      try {
        const response = await axios.get<Problem>(
          `/api/problems/${id}`,
        );

        if (active) {
          setProblem(response.data);
        }
      } catch (err) {
        if (active) {
          if (axios.isAxiosError(err)) {
            setError(
              err.response?.data?.message ??
                `Unable to load problem. ${err.message}`,
            );
          } else {
            setError(
              'An unexpected error occurred while loading the problem.',
            );
          }
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    if (id) {
      void fetchProblem();
    } else {
      setError('Invalid problem ID.');
      setLoading(false);
    }

    return () => {
      active = false;
    };
  }, [id]);

  // Load submission history for the current problem.
  const fetchSubmissionHistory = async () => {
    if (!id) return;

    setHistoryLoading(true);
    setHistoryError('');

    try {
      const response = await axios.get<SubmissionHistoryItem[]>(
        `/api/problems/${id}/submissions`,
      );

      setSubmissionHistory(response.data);
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setHistoryError(
          `Unable to load submission history: ${err.message}`,
        );
      } else {
        setHistoryError('Unable to load submission history.');
      }
    } finally {
      setHistoryLoading(false);
    }
  };

  // Fetch history whenever the selected problem changes.
  useEffect(() => {
    setSubmissionHistory([]);
    setHistoryError('');

    if (id) {
      void fetchSubmissionHistory();
    }
  }, [id]);

  // Run code against the problem's sample input.
  const runCode = async () => {
    if (isRunning || isSubmitting) return;

    setIsRunning(true);
    setOutput('Running your Java code...');
    setSubmission(null);

    try {
      const response = await axios.post<ExecutionResponse>(
        '/api/execute',
        {
          code,
          language: 'java',
          input: problem?.sampleInput ?? '',
        },
      );

      const result = response.data;

      const sections = [
        `Status: ${result.status}`,
        `Execution time: ${result.executionTimeMs} ms`,
      ];

      if (result.output) {
        sections.push(`Output:\n${result.output.trimEnd()}`);
      }

      if (result.error) {
        sections.push(`Error:\n${result.error.trimEnd()}`);
      }

      if (!result.output && !result.error) {
        sections.push('No output produced.');
      }

      setOutput(sections.join('\n\n'));
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const message =
          err.response?.data?.message ??
          err.response?.data?.error ??
          err.message;

        setOutput(`Request failed:\n${message}`);
      } else {
        setOutput(
          'An unexpected error occurred while executing your code.',
        );
      }
    } finally {
      setIsRunning(false);
    }
  };

  // Submit code for evaluation against all test cases.
  const submitCode = async () => {
    if (isRunning || isSubmitting) return;

    if (!id || !code.trim()) {
      setOutput('Please enter your Java solution before submitting.');
      return;
    }

    setIsSubmitting(true);
    setOutput('Submitting your solution for evaluation...');
    setSubmission(null);

    try {
      const response = await axios.post<SubmissionResponse>(
        `/api/problems/${id}/submit`,
        {
          code,
          language: 'java',
        },
      );

      setSubmission(response.data);
      setOutput('');

      // Refresh history after the submission has been saved.
      await fetchSubmissionHistory();
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const message =
          err.response?.data?.message ??
          err.response?.data?.error ??
          err.message;

        setOutput(`Submission failed:\n${message}`);
      } else {
        setOutput(
          'An unexpected error occurred while submitting your solution.',
        );
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Restore the starter code.
  const resetCode = () => {
    setCode(starterCode);
    setOutput('');
    setSubmission(null);
  };

  if (loading) {
    return (
      <div className="problem-page">
        <p>Loading problem...</p>
      </div>
    );
  }

  if (error || !problem) {
    return (
      <div className="problem-page">
        <button
          className="back-button"
          onClick={() => navigate('/')}
        >
          Back to Problems
        </button>

        <p>{error || 'Problem not found.'}</p>
      </div>
    );
  }

  return (
    <div className="problem-page">
      <button
        className="back-button"
        onClick={() => navigate('/')}
      >
        ← Back to Problems
      </button>

      <div className="problem-detail">
        {/* Problem description */}
        <section className="problem-description">
          <h1>
            {problem.id}. {problem.title}
          </h1>

          <p
            className={`difficulty ${problem.difficulty.toLowerCase()}`}
          >
            {problem.difficulty}
          </p>

          <div className="detail-block">
            <h3>Description</h3>
            <p>{problem.description}</p>
          </div>

          <div className="detail-block">
            <h3>Sample Input</h3>
            <pre>{problem.sampleInput || '(No sample input)'}</pre>
          </div>

          <div className="detail-block">
            <h3>Sample Output</h3>
            <pre>{problem.sampleOutput || '(No sample output)'}</pre>
          </div>
        </section>

        {/* Code editor and actions */}
        <section className="editor-section">
          <div className="editor-header">
            <h2>Code Editor</h2>
            <span>Java</span>
          </div>

          <div className="monaco-container">
            <Editor
              height="400px"
              language="java"
              theme="vs-dark"
              value={code}
              onChange={(value) => {
                setCode(value ?? '');
                setSubmission(null);
              }}
              options={{
                minimap: { enabled: false },
                fontSize: 14,
                automaticLayout: true,
                scrollBeyondLastLine: false,
                wordWrap: 'on',
                tabSize: 4,
              }}
            />
          </div>

          <div className="editor-actions">
            <button
              className="run-button"
              onClick={runCode}
              disabled={isRunning || isSubmitting}
            >
              {isRunning ? 'Running...' : 'Run Code'}
            </button>

            <button
              className="run-button"
              onClick={submitCode}
              disabled={isRunning || isSubmitting}
            >
              {isSubmitting ? 'Submitting...' : 'Submit'}
            </button>

            <button
              className="reset-button"
              onClick={resetCode}
              disabled={isRunning || isSubmitting}
            >
              Reset Code
            </button>
          </div>

          {/* Current execution or submission result */}
          <div className="output-panel">
            <h3>Execution Result</h3>

            {submission && (
              <div className="submission-result">
                <h2
                  className={
                    submission.verdict === 'ACCEPTED'
                      ? 'verdict-accepted'
                      : 'verdict-failed'
                  }
                >
                  {submission.verdict.replaceAll('_', ' ')}
                </h2>

                <p>
                  Test cases passed: {submission.passedTestCases}/
                  {submission.totalTestCases}
                </p>

                <p>{submission.message}</p>

                <p>
                  Execution time: {submission.executionTimeMs} ms
                </p>
              </div>
            )}

            <pre>
              {output ||
                (submission
                  ? 'Submission evaluation completed.'
                  : 'Your program output will appear here.')}
            </pre>
          </div>
        </section>

        {/* Submission history */}
        <section className="submission-history">
          <div className="history-header">
            <h2>Submission History</h2>

            <button
              type="button"
              className="reset-button"
              onClick={() => void fetchSubmissionHistory()}
              disabled={historyLoading}
            >
              {historyLoading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>

          {historyError && <p>{historyError}</p>}

          {historyLoading && submissionHistory.length === 0 && (
            <p>Loading submission history...</p>
          )}

          {!historyLoading &&
            !historyError &&
            submissionHistory.length === 0 && (
              <p>
                No submissions yet. Submit your solution to see it here.
              </p>
            )}

          {submissionHistory.length > 0 && (
            <div className="history-table-wrapper">
              <table className="history-table">
                <thead>
                  <tr>
                    <th>Verdict</th>
                    <th>Language</th>
                    <th>Execution Time</th>
                    <th>Submitted At</th>
                  </tr>
                </thead>

                <tbody>
                  {submissionHistory.map((item) => (
                    <tr key={item.id}>
                      <td
                        className={
                          item.verdict === 'ACCEPTED'
                            ? 'verdict-accepted'
                            : 'verdict-failed'
                        }
                      >
                        {item.verdict.replaceAll('_', ' ')}
                      </td>

                      <td>{item.language.toUpperCase()}</td>

                      <td>
                        {item.executionTimeMs != null
                          ? `${item.executionTimeMs} ms`
                          : 'N/A'}
                      </td>

                      <td>
                        {new Date(item.submittedAt).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

export default ProblemPage;




