
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import Editor from '@monaco-editor/react';

import api from './services/api';
import { useAuth } from './context/AuthContext';

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

type TestCaseResult = {
  testCaseNumber: number;
  verdict: string;
  message: string;
};

type SubmissionResponse = {
  verdict: string;
  totalTestCases: number;
  passedTestCases: number;
  message: string;
  executionTimeMs: number;
  testCaseResults: TestCaseResult[];
};

type SubmissionHistoryItem = {
  id: number;
  problemId: number;
  language: string;
  verdict: string;
  executionTimeMs: number | null;
  submittedAt: string;
};

type PublicTestCase = {
  id: number;
  input: string;
  expectedOutput: string;
};

const normalizeOutput = (value: string) =>
  value.replace(/\r\n/g, '\n').trim();

const starterCode = `class Solution {
    public static void main(String[] args) {
        // Write your Java solution here

    }
}`;

function ProblemPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { isAuthenticated } = useAuth();

  const [problem, setProblem] = useState<Problem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [code, setCode] = useState(starterCode);
  const [customInput, setCustomInput] = useState('');
  const [output, setOutput] = useState('');
  const [runVerdict, setRunVerdict] = useState<string | null>(null);

  const [isRunning, setIsRunning] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [submission, setSubmission] =
    useState<SubmissionResponse | null>(null);

  const [submissionHistory, setSubmissionHistory] =
    useState<SubmissionHistoryItem[]>([]);

  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState('');

  const [testCases, setTestCases] = useState<PublicTestCase[]>([]);
  const [selectedTestCase, setSelectedTestCase] = useState(-1);

  const expectedOutput =
    selectedTestCase >= 0
      ? testCases[selectedTestCase]?.expectedOutput ?? ''
      : '';

  // Load problem and public test cases.
  useEffect(() => {
    let active = true;

    const fetchProblem = async () => {
      setLoading(true);
      setError('');
      setProblem(null);
      setCode(starterCode);
      setCustomInput('');
      setOutput('');
      setRunVerdict(null);
      setSubmission(null);
      setSubmissionHistory([]);
      setHistoryError('');
      setTestCases([]);
      setSelectedTestCase(-1);

      try {
        const response = await axios.get<Problem>(
          `/api/problems/${id}`
        );

        if (!active) return;

        setProblem(response.data);

        const testCaseResponse = await axios.get<PublicTestCase[]>(
          `/api/problems/${id}/test-cases`
        );

        if (!active) return;

        const publicCases = testCaseResponse.data;

        setTestCases(publicCases);

        if (publicCases.length > 0) {
          setSelectedTestCase(0);
          setCustomInput(publicCases[0].input);
        } else {
          setSelectedTestCase(-1);
          setCustomInput(response.data.sampleInput ?? '');
        }
      } catch (err) {
        if (active) {
          setError(
            axios.isAxiosError(err)
              ? err.response?.data?.message ?? err.message
              : 'Unable to load problem or public test cases.'
          );
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

  // Load submission history for authenticated users.
  const fetchSubmissionHistory = async () => {
    if (!id || !isAuthenticated) return;

    setHistoryLoading(true);
    setHistoryError('');

    try {
      const response = await api.get<SubmissionHistoryItem[]>(
        `/api/problems/${id}/submissions`
      );

      setSubmissionHistory(response.data);
    } catch (err) {
      setHistoryError(
        axios.isAxiosError(err)
          ? `Unable to load history: ${
              err.response?.data?.message ?? err.message
            }`
          : 'Unable to load submission history.'
      );
    } finally {
      setHistoryLoading(false);
    }
  };

  useEffect(() => {
    if (id && isAuthenticated) {
      void fetchSubmissionHistory();
    } else {
      setSubmissionHistory([]);
      setHistoryError('');
    }
  }, [id, isAuthenticated]);

  // Select a public example or custom input.
  const selectTestCase = (index: number) => {
    setSelectedTestCase(index);

    if (index === -1) {
      setCustomInput('');
    } else {
      setCustomInput(testCases[index].input);
    }

    setOutput('');
    setRunVerdict(null);
    setSubmission(null);
  };

  // Run code using the selected input.
  const runCode = async () => {
    if (isRunning || isSubmitting) return;

    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    setIsRunning(true);
    setOutput('Running your Java code...');
    setRunVerdict(null);
    setSubmission(null);

    try {
      const response = await api.post<ExecutionResponse>(
        '/api/execute',
        {
          code,
          language: 'java',
          input: customInput,
        }
      );

      const result = response.data;

      let verdict = 'EXECUTION_ERROR';

      switch (result.status) {
        case 'EXECUTED':
          if (selectedTestCase >= 0) {
            verdict =
              normalizeOutput(result.output) ===
              normalizeOutput(expectedOutput)
                ? 'PASSED'
                : 'WRONG_ANSWER';
          } else {
            verdict = 'EXECUTED';
          }
          break;

        case 'COMPILATION_ERROR':
          verdict = 'COMPILATION_ERROR';
          break;

        case 'RUNTIME_ERROR':
          verdict = 'RUNTIME_ERROR';
          break;

        case 'TIME_LIMIT_EXCEEDED':
          verdict = 'TIME_LIMIT_EXCEEDED';
          break;

        default:
          verdict = 'EXECUTION_ERROR';
      }

      setRunVerdict(verdict);

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
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        navigate('/login');
        return;
      }

      const message = axios.isAxiosError(err)
        ? err.response?.data?.message ??
          err.response?.data?.error ??
          err.message
        : 'Unexpected execution error.';

      setRunVerdict('EXECUTION_ERROR');
      setOutput(`Request failed:\n${message}`);
    } finally {
      setIsRunning(false);
    }
  };

  // Submit against all hidden test cases.
  const submitCode = async () => {
    if (isRunning || isSubmitting) return;

    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    if (!id || !code.trim()) {
      setOutput('Please enter your Java solution.');
      return;
    }

    setIsSubmitting(true);
    setOutput('Submitting your solution...');
    setRunVerdict(null);
    setSubmission(null);

    try {
      const response = await api.post<SubmissionResponse>(
        `/api/problems/${id}/submit`,
        {
          code,
          language: 'java',
        }
      );

      setSubmission(response.data);
      setOutput('');

      await fetchSubmissionHistory();
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        navigate('/login');
        return;
      }

      const message = axios.isAxiosError(err)
        ? err.response?.data?.message ??
          err.response?.data?.error ??
          err.message
        : 'Unexpected submission error.';

      setOutput(`Submission failed:\n${message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Restore starter code.
  const resetCode = () => {
    setCode(starterCode);
    setOutput('');
    setRunVerdict(null);
    setSubmission(null);
  };

  if (loading) {
    return (
      <div className="workspace-loading">
        Loading problem...
      </div>
    );
  }

  if (error || !problem) {
    return (
      <div className="workspace-loading">
        <button
          className="back-button"
          onClick={() => navigate('/')}
        >
          ← Back to Problems
        </button>

        <p>{error || 'Problem not found.'}</p>
      </div>
    );
  }

  const accepted = submission?.verdict === 'ACCEPTED';

  return (
    <div className="workspace">

      {/* TOP NAVIGATION */}
      <header className="workspace-topbar">
        <button
          className="workspace-brand"
          onClick={() => navigate('/')}
        >
          <span className="brand-mark">S</span>
          <span>ShivaCode</span>
        </button>

        <div className="workspace-nav">
          <span>Problems</span>
          <span className="nav-divider">/</span>
          <span className="current-page">
            {problem.title}
          </span>
        </div>

        <button
          className="workspace-back"
          onClick={() => navigate('/')}
        >
          All Problems
        </button>
      </header>

      <main className="workspace-main">

        <div className="workspace-grid">

          {/* LEFT: PROBLEM DESCRIPTION */}
          <section className="description-panel">

            <div className="panel-heading">
              <span className="panel-number">
                Problem {problem.id}
              </span>

              <span
                className={`difficulty-tag ${problem.difficulty.toLowerCase()}`}
              >
                {problem.difficulty}
              </span>
            </div>

            <h1>{problem.title}</h1>

            <div className="description-content">
              <h3>Problem Description</h3>

              <p>{problem.description}</p>

              <div className="example-card">
                <h3>Sample Input</h3>
                <pre>
                  {problem.sampleInput || '(No sample input)'}
                </pre>
              </div>

              <div className="example-card">
                <h3>Expected Output</h3>
                <pre>
                  {problem.sampleOutput || '(No sample output)'}
                </pre>
              </div>

              <div className="description-tip">
                <strong>Tip</strong>
                <p>
                  Write your solution in Java and test it
                  before submitting.
                </p>
              </div>
            </div>
          </section>

          {/* RIGHT: CODE EDITOR */}
          <section className="coding-panel">

            <div className="editor-toolbar">
              <div>
                <span className="editor-language-icon">J</span>
                <strong>Code</strong>
                <span className="language-label">Java</span>
              </div>

              <button
                className="toolbar-reset"
                onClick={resetCode}
              >
                Reset
              </button>
            </div>

            <div className="workspace-editor">
              <Editor
                height="100%"
                language="java"
                theme="vs-dark"
                value={code}
                onChange={(value) => {
                  setCode(value ?? '');
                  setRunVerdict(null);
                  setSubmission(null);
                }}
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  automaticLayout: true,
                  scrollBeyondLastLine: false,
                  wordWrap: 'on',
                  tabSize: 4,
                  padding: { top: 18 },
                  lineNumbersMinChars: 3,
                  renderLineHighlight: 'all',
                }}
              />
            </div>

            <div className="editor-footer">
              <span className="editor-status">
                ● Java 21
              </span>

              <span className="editor-status">
                Auto layout enabled
              </span>
            </div>

            {/* ACTION BUTTONS */}
            <div className="action-bar">
              <button
                className="workspace-run"
                onClick={runCode}
                disabled={isRunning || isSubmitting}
              >
                {isRunning ? 'Running...' : '▶ Run Code'}
              </button>

              <button
                className="workspace-submit"
                onClick={submitCode}
                disabled={isRunning || isSubmitting}
              >
                {isSubmitting ? 'Submitting...' : '✓ Submit'}
              </button>
            </div>

            {/* TEST CASE PANEL */}
            <section className="result-panel">

              <div className="result-heading">
                <h3>Test Cases</h3>
                <span className="result-indicator">
                  {testCases.length} public examples
                </span>
              </div>

              <div className="test-case-tabs">

                {testCases.map((testCase, index) => (
                  <button
                    key={testCase.id}
                    className={
                      selectedTestCase === index
                        ? 'test-case-tab active'
                        : 'test-case-tab'
                    }
                    onClick={() => selectTestCase(index)}
                  >
                    Example {index + 1}
                  </button>
                ))}

                <button
                  className={
                    selectedTestCase === -1
                      ? 'test-case-tab active'
                      : 'test-case-tab'
                  }
                  onClick={() => selectTestCase(-1)}
                >
                  Custom Input
                </button>

              </div>

              {/* INPUT */}
              <div className="test-input-section">
                <label htmlFor="custom-test-input">
                  Input
                </label>

                <textarea
                  id="custom-test-input"
                  className="custom-input"
                  value={customInput}
                  onChange={(event) => {
                    setCustomInput(event.target.value);
                    setSelectedTestCase(-1);
                    setOutput('');
                    setRunVerdict(null);
                    setSubmission(null);
                  }}
                  placeholder="Enter your test input here..."
                  rows={5}
                  spellCheck={false}
                />
              </div>

              {/* EXPECTED OUTPUT */}
              {selectedTestCase >= 0 && (
                <div className="expected-output-section">
                  <h4>Expected Output</h4>
                  <pre className="result-output">
                    {expectedOutput}
                  </pre>
                </div>
              )}

              {/* SUBMISSION VERDICT */}
              {submission && (
                <div
                  className={`verdict-card ${accepted ? 'accepted' : 'failed'}`}
                >
                  <div>
                    <strong>
                      {submission.verdict.replaceAll('_', ' ')}
                    </strong>

                    <p>{submission.message}</p>
                  </div>

                  <div className="verdict-stats">
                    <span>
                      {submission.passedTestCases}/
                      {submission.totalTestCases}
                    </span>

                    <small>Test cases passed</small>

                    <small>
                      {submission.executionTimeMs} ms
                    </small>
                  </div>
                </div>
              )}

              {/* INDIVIDUAL SUBMISSION TEST CASE RESULTS */}
              {submission && submission.testCaseResults && (
                <div className="submission-test-results">
                  <div className="result-heading">
                    <h3>Test Case Results</h3>
                    <span className="result-indicator">
                      {submission.passedTestCases} / {submission.totalTestCases} passed
                    </span>
                  </div>

                  <div className="submission-test-case-list">
                    {submission.testCaseResults.map((testCase) => (
                      <div
                        className="submission-test-case"
                        key={testCase.testCaseNumber}
                      >
                        <div className="submission-test-case-info">
                          <strong>
                            Test Case {testCase.testCaseNumber}
                          </strong>
                          <small>{testCase.message}</small>
                        </div>

                        <span
                          className={`test-case-verdict ${
                            testCase.verdict === 'PASSED' ? 'passed' : 'failed'
                          }`}
                        >
                          {testCase.verdict.replaceAll('_', ' ')}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* RUN VERDICT */}
              {runVerdict && (
                <div
                  className={`verdict-card ${
                    runVerdict === 'PASSED' ? 'accepted' : 'failed'
                  }`}
                >
                  <div>
                    <strong>{runVerdict.replaceAll('_', ' ')}</strong>
                    <p>
                      {runVerdict === 'PASSED'
                        ? 'Your output matches the expected output!'
                        : runVerdict === 'WRONG_ANSWER'
                          ? 'Your output does not match the expected output.'
                          : runVerdict === 'EXECUTED'
                            ? 'Code executed successfully. Custom input is not compared.'
                            : runVerdict === 'COMPILATION_ERROR'
                              ? 'Your Java code could not be compiled.'
                              : runVerdict === 'RUNTIME_ERROR'
                                ? 'Your program encountered a runtime error.'
                                : runVerdict === 'TIME_LIMIT_EXCEEDED'
                                  ? 'Your program exceeded the execution time limit.'
                                  : 'An execution error occurred.'}
                    </p>
                  </div>
                </div>
              )}

              {/* ACTUAL OUTPUT */}
              <div className="result-heading">
                <h3>Actual Output</h3>
                <span className="result-indicator">
                  {output ? 'Completed' : 'Waiting for execution'}
                </span>
              </div>

              <pre className="result-output">
                {output ||
                  'Run your code to see the output here.'}
              </pre>

            </section>
          </section>
        </div>

        {/* SUBMISSION HISTORY */}
        <section className="history-panel">

          <div className="history-title">
            <div>
              <span className="section-eyebrow">
                YOUR ACTIVITY
              </span>

              <h2>Submission History</h2>
            </div>

            {isAuthenticated && (
              <button
                className="toolbar-reset"
                onClick={() => void fetchSubmissionHistory()}
                disabled={historyLoading}
              >
                {historyLoading ? 'Refreshing...' : '↻ Refresh'}
              </button>
            )}
          </div>

          {!isAuthenticated && (
            <p className="history-message">
              <button
                className="auth-link"
                onClick={() => navigate('/login')}
              >
                Sign in
              </button>
              {' '}to view your submission history.
            </p>
          )}

          {isAuthenticated && historyError && (
            <p className="history-message">
              {historyError}
            </p>
          )}

          {isAuthenticated &&
            historyLoading &&
            submissionHistory.length === 0 && (
              <p className="history-message">
                Loading submissions...
              </p>
            )}

          {isAuthenticated &&
            !historyLoading &&
            !historyError &&
            submissionHistory.length === 0 && (
              <p className="history-message">
                No submissions yet. Your attempts will appear here.
              </p>
            )}

          {isAuthenticated && submissionHistory.length > 0 && (
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
      </main>
    </div>
  );
}

export default ProblemPage;
