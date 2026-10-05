import React, { useState, useEffect } from 'react';


// Example inside your React components
const JOB_SERVICE_URL = import.meta.env.VITE_JOB_SERVICE_URL || "http://localhost:8080";
const RESUME_SERVICE_URL = import.meta.env.VITE_RESUME_SERVICE_URL || "http://localhost:8000";
const SCORING_SERVICE_URL = import.meta.env.VITE_SCORING_SERVICE_URL || "http://localhost:8081";

export default function App() {
  const [activeTab, setActiveTab] = useState('jobs');
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [newJob, setNewJob] = useState({
    title: '',
    description: '',
    minExperienceYears: 0,
    requiredSkills: ''
  });

  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadResult, setUploadResult] = useState(null);

  const [scoreJobId, setScoreJobId] = useState('');
  const [scoreCandidateId, setScoreCandidateId] = useState('');
  const [scoreResult, setScoreResult] = useState(null);

  useEffect(() => {
    if (activeTab === 'jobs' || activeTab === 'scoring') {
      fetchJobs();
    }
  }, [activeTab]);

  const fetchJobs = async () => {
    try {
      const response = await fetch(`${JOB_SERVICE_URL}/jobs`);
      if (!response.ok) throw new Error('Failed to fetch jobs');
      const data = await response.json();
      setJobs(Array.isArray(data) ? data : []);
      setError('');
    } catch (err) {
      console.error(err);
      setError('Could not connect to Job Service (Port 8080). Make sure CORS is enabled.');
    }
  };

  const handleCreateJob = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    
    const skillsArray = newJob.requiredSkills
      .split(',')
      .map(skill => skill.trim())
      .filter(skill => skill.length > 0);

    const payload = {
      title: newJob.title,
      description: newJob.description,
      minExperienceYears: parseInt(newJob.minExperienceYears) || 0,
      requiredSkills: skillsArray
    };

    try {
      const response = await fetch(`${JOB_SERVICE_URL}/jobs`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      
      if (!response.ok) throw new Error('Failed to create job');
      
      setNewJob({ title: '', description: '', minExperienceYears: 0, requiredSkills: '' });
      fetchJobs();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleUploadResume = async (e) => {
    e.preventDefault();
    if (!selectedFile) return;

    setLoading(true);
    setError('');
    setUploadResult(null);

    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const response = await fetch(`${RESUME_SERVICE_URL}/api/v1/resumes/upload`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
          const errData = await response.json().catch(() => ({}));
          throw new Error(errData.detail || 'Failed to upload resume');
      }

      const data = await response.json();
      setUploadResult(data.data);
      setScoreCandidateId(data.data.candidate_id);
    } catch (err) {
      setError(err.message || 'Could not connect to Resume Service (Port 8000).');
    } finally {
      setLoading(false);
    }
  };

  const handleRunScoring = async (e) => {
    e.preventDefault();
    if (!scoreJobId || !scoreCandidateId) return;

    setLoading(true);
    setError('');
    setScoreResult(null);

    try {
      const response = await fetch(`${SCORING_SERVICE_URL}/api/v1/score?jobId=${scoreJobId}&candidateId=${scoreCandidateId}`, {
        method: 'POST'
      });

      if (!response.ok) throw new Error('Scoring failed.');

      const data = await response.json();
      setScoreResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const cardStyle = { background: '#fff', padding: '20px', marginBottom: '20px', borderRadius: '8px', border: '1px solid #ddd' };
  const inputStyle = { display: 'block', width: '100%', padding: '8px', marginBottom: '10px', boxSizing: 'border-box' };
  const buttonStyle = { background: '#007bff', color: '#fff', border: 'none', padding: '10px 15px', cursor: 'pointer', borderRadius: '4px' };

  return (
    <div style={{ fontFamily: 'Arial, sans-serif', maxWidth: '800px', margin: '0 auto', padding: '20px', color: '#333' }}>
      <header style={{ marginBottom: '20px', borderBottom: '2px solid #eee', paddingBottom: '10px' }}>
        <h1>ATS Dashboard</h1>
        <nav style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
          <button style={{...buttonStyle, background: activeTab === 'jobs' ? '#0056b3' : '#6c757d'}} onClick={() => setActiveTab('jobs')}>1. Job Service</button>
          <button style={{...buttonStyle, background: activeTab === 'resumes' ? '#0056b3' : '#6c757d'}} onClick={() => setActiveTab('resumes')}>2. Resume Service</button>
          <button style={{...buttonStyle, background: activeTab === 'scoring' ? '#0056b3' : '#6c757d'}} onClick={() => setActiveTab('scoring')}>3. Scoring Engine</button>
        </nav>
      </header>

      <main>
        {error && (
          <div style={{ background: '#f8d7da', color: '#721c24', padding: '10px', marginBottom: '20px', borderRadius: '4px', border: '1px solid #f5c6cb' }}>
            <strong>Error:</strong> {error}
            <button style={{ float: 'right', background: 'transparent', border: 'none', cursor: 'pointer', fontWeight: 'bold' }} onClick={() => setError('')}>x</button>
          </div>
        )}

        {activeTab === 'jobs' && (
          <div>
            <section style={cardStyle}>
              <h2>Create New Job</h2>
              <form onSubmit={handleCreateJob}>
                <label>Title:</label>
                <input required type="text" style={inputStyle} value={newJob.title} onChange={e => setNewJob({...newJob, title: e.target.value})} />
                
                <label>Description:</label>
                <textarea required rows="3" style={inputStyle} value={newJob.description} onChange={e => setNewJob({...newJob, description: e.target.value})} />
                
                <label>Min Experience (Years):</label>
                <input required type="number" min="0" style={inputStyle} value={newJob.minExperienceYears} onChange={e => setNewJob({...newJob, minExperienceYears: e.target.value})} />
                
                <label>Required Skills (comma separated):</label>
                <input required type="text" style={inputStyle} placeholder="Java, Spring Boot, MySQL" value={newJob.requiredSkills} onChange={e => setNewJob({...newJob, requiredSkills: e.target.value})} />
                
                <button type="submit" style={buttonStyle} disabled={loading}>{loading ? 'Creating...' : 'Create Job'}</button>
              </form>
            </section>

            <section style={cardStyle}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h2>Active Jobs</h2>
                <button style={{...buttonStyle, background: '#28a745', padding: '5px 10px'}} onClick={fetchJobs}>Refresh</button>
              </div>
              <ul style={{ paddingLeft: '20px', marginTop: '10px' }}>
                {jobs.map(job => (
                  <li key={job.id} style={{ marginBottom: '10px' }}>
                    <strong>ID {job.id}: {job.title}</strong>
                    <p style={{ margin: '5px 0' }}>{job.description}</p>
                    <small style={{ color: '#666' }}>Skills: {job.requiredSkills?.join(', ')}</small>
                  </li>
                ))}
              </ul>
            </section>
          </div>
        )}

        {activeTab === 'resumes' && (
          <div style={cardStyle}>
            <h2>Upload Resume</h2>
            <form onSubmit={handleUploadResume}>
              <input type="file" accept="application/pdf" style={inputStyle} onChange={e => setSelectedFile(e.target.files[0])} />
              <button type="submit" style={buttonStyle} disabled={!selectedFile || loading}>{loading ? 'Uploading...' : 'Upload & Parse PDF'}</button>
            </form>

            {uploadResult && (
              <div style={{ marginTop: '20px', background: '#d4edda', padding: '15px', borderRadius: '4px', border: '1px solid #c3e6cb' }}>
                <h3 style={{ margin: '0 0 10px 0', color: '#155724' }}>Upload Successful!</h3>
                <p><strong>Candidate ID:</strong> {uploadResult.candidate_id}</p>
                <p><strong>Extracted Skills:</strong> {uploadResult.extracted_skills?.join(', ') || 'None found'}</p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'scoring' && (
          <div style={cardStyle}>
            <h2>Run Match Scoring</h2>
            <form onSubmit={handleRunScoring}>
              <label>Select Job:</label>
              <select required style={inputStyle} value={scoreJobId} onChange={e => setScoreJobId(e.target.value)}>
                <option value="">-- Choose Job --</option>
                {jobs.map(job => (
                  <option key={job.id} value={job.id}>ID {job.id}: {job.title}</option>
                ))}
              </select>

              <label>Candidate ID:</label>
              <input required type="text" style={inputStyle} placeholder="Paste candidate ID from upload tab" value={scoreCandidateId} onChange={e => setScoreCandidateId(e.target.value)} />

              <button type="submit" style={buttonStyle} disabled={!scoreJobId || !scoreCandidateId || loading}>{loading ? 'Calculating...' : 'Calculate Score'}</button>
            </form>

            {scoreResult && (
              <div style={{ marginTop: '20px', background: '#e2e3e5', padding: '15px', borderRadius: '4px', border: '1px solid #d6d8db' }}>
                <h3 style={{ margin: '0 0 10px 0' }}>Match Score: {scoreResult.matchScorePercentage}%</h3>
                <p><strong>Matched Skills:</strong> {scoreResult.matchedSkills?.join(', ') || 'None'}</p>
                <p><strong>Missing Skills:</strong> {scoreResult.missingSkills?.join(', ') || 'None'}</p>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}