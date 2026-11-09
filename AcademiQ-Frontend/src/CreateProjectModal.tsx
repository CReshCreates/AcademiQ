import { useEffect, useState } from 'react';
import type { CSSProperties, FormEvent } from 'react';
import {
  createAcademicProject,
  getEligibleProjectSubjects,
  getProjectSupervisors,
  type AvailableProjectSubject,
  type AvailableProjectSupervisor,
} from './api/projectCreation';

interface Props {
  onClose: () => void;
  onCreated: (projectId: number) => void;
}

const fieldStyle: CSSProperties = {
  display: 'block', width: '100%', marginTop: 6, padding: '10px 12px',
  border: '1px solid #dddbea', borderRadius: 10, fontSize: 13,
  background: '#fff', color: '#211f39',
};
const labelStyle: CSSProperties = {
  display: 'block', fontSize: 12, fontWeight: 600, color: '#353151', marginTop: 14,
};

export default function CreateProjectModal({ onClose, onCreated }: Props) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [subjects, setSubjects] = useState<AvailableProjectSubject[]>([]);
  const [subjectId, setSubjectId] = useState('');
  const [supervisors, setSupervisors] = useState<AvailableProjectSupervisor[]>([]);
  const [supervisorId, setSupervisorId] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [loadingSubjects, setLoadingSubjects] = useState(true);
  const [loadingSupervisors, setLoadingSupervisors] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    getEligibleProjectSubjects(controller.signal)
      .then(data => { if (!controller.signal.aborted) setSubjects(data); })
      .catch(err => { if (!controller.signal.aborted) setError(err instanceof Error ? err.message : 'Could not load eligible subjects.'); })
      .finally(() => { if (!controller.signal.aborted) setLoadingSubjects(false); });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    setSupervisors([]);
    setSupervisorId('');
    if (!subjectId) return;
    const controller = new AbortController();
    setLoadingSupervisors(true);
    setError('');
    getProjectSupervisors(Number(subjectId), controller.signal)
      .then(data => { if (!controller.signal.aborted) setSupervisors(data); })
      .catch(err => { if (!controller.signal.aborted) setError(err instanceof Error ? err.message : 'Could not load supervisors.'); })
      .finally(() => { if (!controller.signal.aborted) setLoadingSupervisors(false); });
    return () => controller.abort();
  }, [subjectId]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    if (!title.trim() || !description.trim() || !subjectId || !supervisorId || !file) {
      setError('Complete every field and attach your proposal PDF.');
      return;
    }
    if (!file.name.toLowerCase().endsWith('.pdf') || file.type && file.type !== 'application/pdf') {
      setError('Please select a PDF proposal.');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      setError('The proposal PDF must not exceed 10 MB.');
      return;
    }
    setError('');
    setSaving(true);
    try {
      const created = await createAcademicProject({
        projectTitle: title,
        projectDescription: description,
        programmeSubjectId: Number(subjectId),
        supervisorId: Number(supervisorId),
        uploadFile: file,
      });
      onCreated(created.projectId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to create the project.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ background: 'rgba(20,18,40,.5)' }} onMouseDown={() => { if (!saving) onClose(); }}>
      <div role="dialog" aria-modal="true" aria-label="Create Project" className="rounded-2xl p-6" style={{ width: '100%', maxWidth: 520, maxHeight: '90vh', overflowY: 'auto', background: 'white' }} onMouseDown={event => event.stopPropagation()}>
        <h2 style={{ fontFamily: 'var(--font-display)', fontSize: 20, fontWeight: 700 }}>Create Project</h2>
        <p style={{ fontSize: 12, color: '#6b6890', marginTop: 6 }}>Your proposal will be sent for supervisor approval. Milestones start as Pending.</p>
        <form onSubmit={submit}>
          <label style={labelStyle}>Project title
            <input required maxLength={200} value={title} onChange={e => setTitle(e.target.value)} style={fieldStyle} placeholder="Enter project title" disabled={saving} />
          </label>
          <label style={labelStyle}>Description
            <textarea required rows={3} value={description} onChange={e => setDescription(e.target.value)} style={{ ...fieldStyle, resize: 'vertical' }} placeholder="Project overview and objectives" disabled={saving} />
          </label>
          <label style={labelStyle}>Eligible subject
            <select required value={subjectId} onChange={e => setSubjectId(e.target.value)} style={fieldStyle} disabled={saving || loadingSubjects}>
              <option value="">{loadingSubjects ? 'Loading subjects…' : 'Select subject'}</option>
              {subjects.map(subject => <option key={subject.programmeSubjectId} value={subject.programmeSubjectId}>{subject.subjectName}</option>)}
            </select>
          </label>
          <label style={labelStyle}>Supervisor
            <select required value={supervisorId} onChange={e => setSupervisorId(e.target.value)} style={fieldStyle} disabled={saving || !subjectId || loadingSupervisors}>
              <option value="">{loadingSupervisors ? 'Loading supervisors…' : 'Select supervisor'}</option>
              {supervisors.map(supervisor => <option key={supervisor.subjectSupervisorId} value={supervisor.supervisorId}>{supervisor.supervisorName}</option>)}
            </select>
          </label>
          <label style={labelStyle}>Proposal PDF (maximum 10 MB)
            <input required type="file" accept=".pdf,application/pdf" onChange={e => setFile(e.target.files?.[0] || null)} style={fieldStyle} disabled={saving} />
          </label>
          {error && <p role="alert" style={{ fontSize: 12, color: '#b91c1c', marginTop: 12 }}>{error}</p>}
          {!loadingSubjects && subjects.length === 0 && !error && <p style={{ fontSize: 12, marginTop: 12 }}>No eligible subjects are available in your current semester.</p>}
          <div className="flex justify-end gap-3 mt-6">
            <button type="button" disabled={saving} onClick={onClose} className="rounded-xl px-5 py-2.5" style={{ background: '#f0eff8', color: '#353151', fontSize: 12 }}>Cancel</button>
            <button type="submit" disabled={saving || loadingSubjects || loadingSupervisors || subjects.length === 0} className="rounded-xl px-5 py-2.5" style={{ background: '#6366f1', color: 'white', opacity: saving ? .65 : 1, fontSize: 12, fontWeight: 600 }}>{saving ? 'Creating…' : 'Create Project'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}
