import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

interface Project {
  id: string;
  name: string;
  description: string;
  status: string;
  createdAt: string;
  version: number;
}
interface Snapshot {
  id: string;
  projectId: string;
  snapshotVersion: number;
  schemaVersion: string;
  contentSha256: string;
  createdAt: string;
}
interface Technology {
  id: string;
  name: string;
  category: string;
  version: string;
  supportedRange: string;
  status: string;
  capabilities: string[];
}
interface CompatibilityFinding {
  ruleId: string;
  status: string;
  severity: string;
  message: string;
  rationale: string;
  affectedTechnologyIds: string[];
  remediation: string[];
  overridePolicy: string;
}
interface CompatibilityResult {
  compatible: boolean;
  requiresReview: boolean;
  findings: CompatibilityFinding[];
}
interface Page<T> {
  content: T[];
  totalElements: number;
}

@Component({
  selector: 'df-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  private readonly http = inject(HttpClient);

  projects: Project[] = [];
  snapshots: Snapshot[] = [];
  catalog: Technology[] = [];
  selectedProject: Project | null = null;
  selectedCategory = 'all';
  activeView = 'overview';
  projectName = '';
  projectDescription = '';
  specificationJson = this.initialSpecification();
  selections: Record<string, string> = {
    'framework.spring-boot': '4.0.0',
    'language.java': '25',
    'database.postgresql': '17'
  };
  compatibility: CompatibilityResult | null = null;
  validation: { valid: boolean; errors: string[] } | null = null;
  busy = false;
  notice = 'Connect to your local DevForge API to begin.';
  noticeType: 'info' | 'success' | 'error' = 'info';

  get categories(): string[] {
    return [...new Set(this.catalog.map(item => item.category))].sort();
  }

  get filteredCatalog(): Technology[] {
    return this.selectedCategory === 'all'
      ? this.catalog
      : this.catalog.filter(item => item.category === this.selectedCategory);
  }

  ngOnInit(): void {
    void this.loadWorkspace();
  }

  async loadWorkspace(): Promise<void> {
    this.busy = true;
    try {
      const results = await Promise.all([
        firstValueFrom(this.http.get<Page<Project>>('/api/v1/projects?page=0&size=50')),
        firstValueFrom(this.http.get<Technology[]>('/api/v1/catalog/technologies'))
      ]);
      this.projects = results[0].content;
      this.catalog = results[1];
      this.setNotice('Connected to DevForge API.', 'success');
    } catch (error) {
      this.handleError(error, 'Could not connect. Start the backend with the local profile.');
    } finally {
      this.busy = false;
    }
  }

  async createProject(): Promise<void> {
    if (!this.projectName.trim()) {
      this.setNotice('Enter a project name first.', 'error');
      return;
    }
    this.busy = true;
    try {
      const project = await firstValueFrom(this.http.post<Project>('/api/v1/projects', {
        name: this.projectName,
        description: this.projectDescription
      }));
      this.projects = [project, ...this.projects];
      await this.selectProject(project);
      this.projectName = '';
      this.projectDescription = '';
      this.setNotice('Project created.', 'success');
    } catch (error) {
      this.handleError(error, 'Project could not be created.');
    } finally {
      this.busy = false;
    }
  }

  async selectProject(project: Project): Promise<void> {
    this.selectedProject = project;
    this.activeView = 'project';
    this.validation = null;
    this.compatibility = null;
    const spec = JSON.parse(this.specificationJson) as Record<string, unknown>;
    const projectNode = spec['project'] as Record<string, unknown>;
    projectNode['id'] = project.id;
    projectNode['name'] = project.name;
    projectNode['description'] = project.description || '';
    this.specificationJson = JSON.stringify(spec, null, 2);
    await this.loadSnapshots();
  }

  async loadSnapshots(): Promise<void> {
    if (!this.selectedProject) return;
    try {
      this.snapshots = await firstValueFrom(
        this.http.get<Snapshot[]>('/api/v1/projects/' + this.selectedProject.id + '/specifications')
      );
    } catch (error) {
      this.handleError(error, 'Could not load specification snapshots.');
    }
  }

  async validateSpecification(): Promise<void> {
    this.busy = true;
    try {
      const payload = JSON.parse(this.specificationJson) as unknown;
      this.validation = await firstValueFrom(
        this.http.post<{ valid: boolean; errors: string[] }>('/api/v1/specifications/validate', payload)
      );
      this.setNotice(this.validation.valid ? 'Specification is valid.' : 'Specification has validation errors.',
        this.validation.valid ? 'success' : 'error');
    } catch (error) {
      if (error instanceof SyntaxError) {
        this.validation = { valid: false, errors: ['The editor content is not valid JSON.'] };
        this.setNotice('Invalid JSON.', 'error');
      } else {
        const httpError = error as HttpErrorResponse;
        const body = httpError.error as { errors?: string[] } | undefined;
        this.validation = body?.errors ? { valid: false, errors: body.errors } : null;
        this.handleError(error, 'Specification validation failed.');
      }
    } finally {
      this.busy = false;
    }
  }

  async saveSnapshot(): Promise<void> {
    if (!this.selectedProject) {
      this.setNotice('Create or select a project before saving a snapshot.', 'error');
      return;
    }
    this.busy = true;
    try {
      const payload = JSON.parse(this.specificationJson) as unknown;
      const snapshot = await firstValueFrom(this.http.post<Snapshot>(
        '/api/v1/projects/' + this.selectedProject.id + '/specifications', payload
      ));
      this.snapshots = [...this.snapshots, snapshot];
      this.setNotice('Specification snapshot v' + snapshot.snapshotVersion + ' saved.', 'success');
    } catch (error) {
      if (error instanceof SyntaxError) {
        this.setNotice('The editor content is not valid JSON.', 'error');
      } else {
        this.handleError(error, 'Snapshot could not be saved. Validate the specification first.');
      }
    } finally {
      this.busy = false;
    }
  }

  async generateProject(): Promise<void> {
    if (!this.selectedProject || this.snapshots.length === 0) {
      this.setNotice('Save a valid specification snapshot before generation.', 'error');
      return;
    }
    const latest = this.snapshots[this.snapshots.length - 1];
    this.busy = true;
    try {
      const archive = await firstValueFrom(this.http.post(
        '/api/v1/projects/' + this.selectedProject.id + '/generations',
        { snapshotVersion: latest.snapshotVersion },
        { responseType: 'blob' }
      ));
      const url = window.URL.createObjectURL(archive);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = this.selectedProject.name.toLowerCase().replace(/[^a-z0-9]+/g, '-') + '.zip';
      anchor.click();
      window.URL.revokeObjectURL(url);
      this.setNotice('Generated project archive downloaded.', 'success');
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.error instanceof Blob) {
        const raw = await error.error.text();
        try {
          const body = JSON.parse(raw) as { message?: string; error?: string };
          this.setNotice(body.message ?? body.error ?? 'Generation failed.', 'error');
        } catch {
          this.setNotice('Generation failed. Review compatibility findings and backend logs.', 'error');
        }
      } else {
        this.handleError(error, 'Generation failed.');
      }
    } finally {
      this.busy = false;
    }
  }

  async evaluateCompatibility(): Promise<void> {
    this.busy = true;
    try {
      this.compatibility = await firstValueFrom(this.http.post<CompatibilityResult>(
        '/api/v1/compatibility/evaluate', { selections: this.selections }
      ));
      this.setNotice(this.compatibility.compatible
        ? 'Selected technologies satisfy the current catalog rules.'
        : 'Compatibility findings require attention.',
        this.compatibility.compatible ? 'success' : 'error');
    } catch (error) {
      this.handleError(error, 'Compatibility evaluation failed.');
    } finally {
      this.busy = false;
    }
  }

  openSnapshot(snapshot: Snapshot): void {
    this.setNotice('Snapshot v' + snapshot.snapshotVersion + ' · SHA-256 '
      + snapshot.contentSha256.slice(0, 12) + '…', 'info');
  }

  private initialSpecification(): string {
    return JSON.stringify({
      specVersion: '1.0.0',
      project: { id: 'new-project', name: 'New Project', description: '' },
      requirements: { functional: [], qualityAttributes: [], constraints: [] },
      model: { entities: [], relationships: [] },
      architecture: { style: 'modular-monolith', decisions: [] },
      technologySelections: {},
      generation: { profile: 'recommended', outputLayout: 'single-project' }
    }, null, 2);
  }

  private setNotice(message: string, type: 'info' | 'success' | 'error'): void {
    this.notice = message;
    this.noticeType = type;
  }

  private handleError(error: unknown, fallback: string): void {
    const httpError = error instanceof HttpErrorResponse ? error : null;
    const body = httpError?.error as { message?: string; error?: string } | undefined;
    this.setNotice(body?.message ?? body?.error ?? fallback, 'error');
  }
}
