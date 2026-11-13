import { CommonModule, registerLocaleData } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import localeFa from '@angular/common/locales/fa';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Locale, messages } from './messages';

registerLocaleData(localeFa);

type ViewId = 'overview' | 'project' | 'model-import' | 'catalog' | 'compatibility' | 'architecture' | 'quality' | 'generate';
type NoticeType = 'info' | 'success' | 'error';
type ApiState = 'unknown' | 'connected' | 'offline';

interface Project {
  id: string;
  name: string;
  description: string;
  status: string;
  createdAt: string;
  updatedAt: string;
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
interface SnapshotDocument extends Snapshot {
  specification: unknown;
}
interface TechnologyRequirement {
  technologyId: string;
  versionRange: string;
  reason: string;
}
interface Technology {
  id: string;
  name: string;
  category: string;
  version: string;
  supportedRange: string;
  status: string;
  capabilities: string[];
  vendor?: string;
  versionScheme?: string;
  requires?: TechnologyRequirement[];
  conflicts?: unknown[];
  platforms?: string[];
  operatingModes?: string[];
  documentation?: string[];
}
interface QualityTestCase {
  id: string;
  category: string;
  title: string;
  objective: string;
  priority: string;
  sourcePath: string;
  steps: string[];
  expectedResult: string;
}
interface QualityPlan {
  projectId: string;
  snapshotId: string;
  snapshotVersion: number;
  generatedAt: string;
  planSha256: string;
  summary: Record<string, number>;
  testCases: QualityTestCase[];
}
interface ArchitectureDecision {
  id: string;
  projectId: string;
  snapshotId: string;
  snapshotVersion: number;
  title: string;
  context: string;
  decision: string;
  alternatives: string[];
  consequences: string[];
  status: string;
  version: number;
  createdAt: string;
  updatedAt: string;
  history: { id: string; fromStatus: string | null; toStatus: string; comment: string; actor: string; createdAt: string }[];
}
interface ArchitectureFinding {
  id: string;
  status: string;
  severity: string;
  message: string;
  rationale: string;
  affectedPaths: string[];
  remediation: string[];
}
interface ArchitectureAssessment {
  selectedStyle: string;
  recommendedStyle: string;
  rationale: string;
  requiresReview: boolean;
  findings: ArchitectureFinding[];
  tradeOffs: string[];
  adrDraft: { title: string; context: string; decision: string; alternatives: string[]; consequences: string[] };
}
interface ModelAttribute {
  name: string;
  type: string;
  required?: boolean;
  unique?: boolean;
  description?: string;
}
interface ModelEntity {
  name: string;
  boundedContext?: string;
  attributes?: ModelAttribute[];
}
interface ModelRelationship {
  name: string;
  from: string;
  to: string;
  cardinality: string;
  ownership?: string;
  optional?: boolean;
}
interface ModelIndex {
  name?: string;
  entity: string;
  columns: string[];
  unique?: boolean;
  method?: string;
}
interface ModelImportResult {
  model: { entities?: ModelEntity[]; relationships?: ModelRelationship[]; indexes?: ModelIndex[] };
  warnings: string[];
}
interface GenerationRun {
  id: string;
  projectId: string;
  snapshotId: string;
  snapshotVersion: number;
  generatorId: string;
  generatorVersion: string;
  status: string;
  outputSha256: string;
  generatedFileCount: number;
  createdAt: string;
}
interface GeneratorDescriptor {
  id: string;
  version: string;
  description: string;
  supportedSpecificationVersions: string[];
  supportedTechnologyVersions: Record<string, string>;
}
interface CompatibilityFinding {
  ruleId: string;
  status: string;
  severity: string;
  message: string;
  rationale: string;
  affectedTechnologyIds: string[];
  remediation: string[];
  evidence: string[];
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
  totalPages: number;
  number: number;
  size: number;
}
interface SpecificationSummary {
  specVersion: string;
  style: string;
  entities: number;
  relationships: number;
  functional: number;
  qualityAttributes: number;
  constraints: number;
  technologies: number;
  profile: string;
  outputLayout: string;
}
interface StackSelection {
  id: string;
  name: string;
  version: string;
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
  private readonly encoder = new TextEncoder();

  readonly navigation: { id: ViewId; group: string; label: string }[] = [
    { id: 'overview', group: 'nav.group.workspace', label: 'nav.overview' },
    { id: 'project', group: 'nav.group.workspace', label: 'nav.specification' },
    { id: 'model-import', group: 'nav.group.design', label: 'nav.model' },
    { id: 'catalog', group: 'nav.group.design', label: 'nav.catalog' },
    { id: 'compatibility', group: 'nav.group.analysis', label: 'nav.compatibility' },
    { id: 'architecture', group: 'nav.group.analysis', label: 'nav.architecture' },
    { id: 'quality', group: 'nav.group.delivery', label: 'nav.quality' },
    { id: 'generate', group: 'nav.group.delivery', label: 'nav.generate' }
  ];

  projects: Project[] = [];
  snapshots: Snapshot[] = [];
  generationRuns: GenerationRun[] = [];
  generators: GeneratorDescriptor[] = [];
  modelImportFormat = 'sql-ddl';
  modelImportContent = 'CREATE TABLE customer (\n  id UUID PRIMARY KEY,\n  email VARCHAR(255) NOT NULL UNIQUE\n);';
  modelPreview: ModelImportResult | null = null;
  catalog: Technology[] = [];
  selectedProject: Project | null = null;
  selectedSnapshot: SnapshotDocument | null = null;
  selectedSnapshotJson = '';
  selectedCategory = 'all';
  catalogQuery = '';
  selectedTechnologyId: string | null = null;
  qualityCategory = 'all';
  activeView: ViewId = 'overview';
  projectName = '';
  projectDescription = '';
  projectQuery = '';
  projectStatus = '';
  projectPage = 0;
  projectPageSize = 8;
  projectTotal = 0;
  projectTotalPages = 1;
  editingProject: Project | null = null;
  editName = '';
  editDescription = '';
  pendingDelete: Project | null = null;
  catalogPage = 0;
  readonly catalogPageSize = 6;
  snapshotPage = 0;
  readonly snapshotPageSize = 6;
  locale: Locale = 'en';
  specificationJson = this.initialSpecification();
  selections: Record<string, string> = {
    'framework.spring-boot': '4.0.0',
    'language.java': '25',
    'database.postgresql': '17'
  };
  compatibility: CompatibilityResult | null = null;
  architectureAssessment: ArchitectureAssessment | null = null;
  architectureDecisions: ArchitectureDecision[] = [];
  qualityPlan: QualityPlan | null = null;
  adrTransitionComment = '';
  validation: { valid: boolean; errors: string[] } | null = null;
  busy = false;
  apiState: ApiState = 'unknown';
  notice = '';
  noticeKey = 'notice.offline';
  noticeValues: Record<string, string | number> = {};
  noticeType: NoticeType = 'info';

  get navGroups(): string[] {
    return [...new Set(this.navigation.map(item => item.group))];
  }

  get activeNavigation(): { id: ViewId; group: string; label: string } {
    return this.navigation.find(item => item.id === this.activeView) ?? this.navigation[0];
  }

  get dateLocale(): string {
    return this.locale === 'fa' ? 'fa' : 'en';
  }

  get pagedCatalog(): Technology[] {
    const start = this.catalogPage * this.catalogPageSize;
    return this.filteredCatalog.slice(start, start + this.catalogPageSize);
  }

  get catalogPageCount(): number {
    return Math.max(1, Math.ceil(this.filteredCatalog.length / this.catalogPageSize));
  }

  get pagedSnapshots(): Snapshot[] {
    const start = this.snapshotPage * this.snapshotPageSize;
    return this.snapshots.slice(start, start + this.snapshotPageSize);
  }

  get snapshotPageCount(): number {
    return Math.max(1, Math.ceil(this.snapshots.length / this.snapshotPageSize));
  }

  get categories(): string[] {
    return [...new Set(this.catalog.map(item => item.category))].sort();
  }

  get filteredCatalog(): Technology[] {
    const query = this.catalogQuery.trim().toLowerCase();
    return this.catalog.filter(item => {
      const categoryMatches = this.selectedCategory === 'all' || item.category === this.selectedCategory;
      if (!categoryMatches) return false;
      if (!query) return true;
      return [item.name, item.id, item.vendor, item.category, item.status, ...item.capabilities]
        .filter((value): value is string => !!value)
        .join(' ')
        .toLowerCase()
        .includes(query);
    });
  }

  get selectedTechnology(): Technology | null {
    return this.catalog.find(item => item.id === this.selectedTechnologyId) ?? null;
  }

  get activeSelections(): StackSelection[] {
    return Object.entries(this.selections)
      .map(([id, version]) => ({ id, name: this.technologyName(id), version: version.trim() }))
      .filter(item => item.version.length > 0)
      .sort((left, right) => left.name.localeCompare(right.name));
  }

  get latestSnapshot(): Snapshot | null {
    return this.snapshots.reduce<Snapshot | null>((latest, item) =>
      !latest || item.snapshotVersion > latest.snapshotVersion ? item : latest, null);
  }

  get specificationSummary(): SpecificationSummary | null {
    try {
      const spec = this.record(JSON.parse(this.specificationJson));
      const requirements = this.record(spec['requirements']);
      const model = this.record(spec['model']);
      const architecture = this.record(spec['architecture']);
      const generation = this.record(spec['generation']);
      return {
        specVersion: this.text(spec['specVersion']),
        style: this.text(architecture['style']),
        entities: this.countOf(model['entities']),
        relationships: this.countOf(model['relationships']),
        functional: this.countOf(requirements['functional']),
        qualityAttributes: this.countOf(requirements['qualityAttributes']),
        constraints: this.countOf(requirements['constraints']),
        technologies: this.countOf(spec['technologySelections']),
        profile: this.text(generation['profile']),
        outputLayout: this.text(generation['outputLayout'])
      };
    } catch {
      return null;
    }
  }

  get editorBytes(): number {
    return this.encoder.encode(this.specificationJson).length;
  }

  get importBytes(): number {
    return this.encoder.encode(this.modelImportContent).length;
  }

  get previewEntities(): ModelEntity[] {
    return this.modelPreview?.model.entities ?? [];
  }

  get previewRelationships(): ModelRelationship[] {
    return this.modelPreview?.model.relationships ?? [];
  }

  get previewIndexes(): ModelIndex[] {
    return this.modelPreview?.model.indexes ?? [];
  }

  get qualityCategories(): string[] {
    return [...new Set((this.qualityPlan?.testCases ?? []).map(test => test.category))].sort();
  }

  get visibleTests(): QualityTestCase[] {
    const tests = this.qualityPlan?.testCases ?? [];
    return this.qualityCategory === 'all' ? tests : tests.filter(test => test.category === this.qualityCategory);
  }

  get commentRequired(): boolean {
    return this.adrTransitionComment.trim().length === 0;
  }

  ngOnInit(): void {
    const stored = localStorage.getItem('df-locale');
    this.setLocale(stored === 'fa' ? 'fa' : 'en');
    this.noticeKey = '';
    this.notice = '';
    void this.loadWorkspace();
  }

  itemsIn(group: string): { id: ViewId; group: string; label: string }[] {
    return this.navigation.filter(item => item.group === group);
  }

  t(key: string): string {
    return messages[this.locale][key] ?? messages.en[key] ?? key;
  }

  tf(key: string, values: Record<string, string | number>): string {
    return Object.entries(values).reduce(
      (text, [name, value]) => text.replaceAll('{' + name + '}', String(value)),
      this.t(key));
  }

  setLocale(locale: Locale): void {
    this.locale = locale;
    localStorage.setItem('df-locale', locale);
    document.documentElement.lang = locale;
    document.documentElement.dir = locale === 'fa' ? 'rtl' : 'ltr';
    if (this.noticeKey) this.notice = this.tf(this.noticeKey, this.noticeValues);
  }

  categoryCount(category: string): number {
    return category === 'all'
      ? this.catalog.length
      : this.catalog.filter(item => item.category === category).length;
  }

  technologyName(id: string): string {
    return this.catalog.find(item => item.id === id)?.name ?? id;
  }

  readable(value: string): string {
    return value.replace(/[_-]+/g, ' ');
  }

  attributesOf(entity: ModelEntity): ModelAttribute[] {
    return entity.attributes ?? [];
  }

  linksOf(technology: Technology): string[] {
    return (technology.documentation ?? []).filter(link => link.startsWith('https://') || link.startsWith('http://'));
  }

  conflictLabel(conflict: unknown): string {
    return typeof conflict === 'string' ? conflict : JSON.stringify(conflict);
  }

  async loadWorkspace(): Promise<void> {
    this.busy = true;
    try {
      const [projects, catalog, generators] = await Promise.allSettled([
        this.fetchProjects(),
        firstValueFrom(this.http.get<Technology[]>('/api/v1/catalog/technologies')),
        firstValueFrom(this.http.get<GeneratorDescriptor[]>('/api/v1/generators'))
      ]);
      if (projects.status !== 'fulfilled' || catalog.status !== 'fulfilled') {
        this.apiState = 'offline';
        const reason = projects.status === 'rejected' ? projects.reason
          : catalog.status === 'rejected' ? catalog.reason
          : this.t('notice.offline');
        this.handleError(reason, this.t('notice.offline'));
        return;
      }
      this.applyProjectPage(projects.value);
      this.catalog = catalog.value;
      this.catalogPage = 0;
      this.generators = generators.status === 'fulfilled' ? generators.value : [];
      this.apiState = 'connected';
      if (this.selectedProject) {
        const refreshed = this.projects.find(project => project.id === this.selectedProject?.id);
        if (refreshed) this.selectedProject = refreshed;
      }
      this.setNotice(this.t(generators.status === 'fulfilled' ? 'notice.connected' : 'notice.generators'),
        generators.status === 'fulfilled' ? 'success' : 'error');
    } finally {
      this.busy = false;
    }
  }

  async createProject(): Promise<void> {
    const name = this.projectName.trim();
    const description = this.projectDescription.trim();
    if (!name || name.length > 120 || description.length > 2000) {
      this.setNotice(this.t('notice.nameLength'), 'error');
      return;
    }
    this.busy = true;
    try {
      const project = await firstValueFrom(this.http.post<Project>('/api/v1/projects', { name, description }));
      this.projects = [project, ...this.projects];
      this.projectName = '';
      this.projectDescription = '';
      await this.selectProject(project);
      this.setNotice(this.t('notice.created'), 'success');
    } catch (error) {
      this.handleError(error, this.t('notice.createFailed'));
    } finally {
      this.busy = false;
    }
  }

  async switchProject(projectId: string): Promise<void> {
    if (!projectId) {
      this.clearProjectContext();
      this.setNotice(this.t('notice.cleared'), 'info');
      return;
    }
    const project = this.projects.find(item => item.id === projectId);
    if (project) await this.selectProject(project);
  }

  async selectProject(project: Project): Promise<void> {
    const switching = this.selectedProject?.id !== project.id;
    this.busy = true;
    this.selectedProject = project;
    this.activeView = this.activeView === 'overview' ? 'project' : this.activeView;
    this.validation = null;
    this.compatibility = null;
    this.architectureAssessment = null;
    this.qualityPlan = null;
    this.qualityCategory = 'all';
    this.modelPreview = null;
    this.selectedSnapshot = null;
    this.selectedSnapshotJson = '';
    if (switching) {
      this.specificationJson = this.initialSpecification();
      this.applyProjectIdentity(project);
    }
    try {
      await Promise.all([this.loadSnapshots(), this.loadGenerationRuns(), this.loadArchitectureDecisions()]);
      const latest = this.latestSnapshot;
      const opened = latest ? await this.openSnapshot(latest, false) : false;
      if (switching && opened) this.specificationJson = this.selectedSnapshotJson;
      if (switching && (opened || !latest)) {
        this.setNotice(this.tf(opened ? 'notice.openedStored' : 'notice.openedDraft', {
          name: project.name,
          version: latest?.snapshotVersion ?? ''
        }), 'success');
      }
    } finally {
      this.busy = false;
    }
  }

  async loadSnapshots(): Promise<void> {
    if (!this.selectedProject) return;
    try {
      this.snapshots = await firstValueFrom(
        this.http.get<Snapshot[]>('/api/v1/projects/' + this.selectedProject.id + '/specifications')
      );
      this.snapshotPage = Math.min(this.snapshotPage,
        Math.max(0, Math.ceil(this.snapshots.length / this.snapshotPageSize) - 1));
    } catch (error) {
      this.handleError(error, this.t('notice.snapshotsFailed'));
    }
  }

  async openSnapshot(snapshot: Snapshot, manageBusy = true): Promise<boolean> {
    if (!this.selectedProject) return false;
    if (manageBusy) this.busy = true;
    try {
      const document = await firstValueFrom(this.http.get<SnapshotDocument>(
        '/api/v1/projects/' + this.selectedProject.id + '/specifications/' + snapshot.snapshotVersion
      ));
      this.selectedSnapshot = document;
      this.selectedSnapshotJson = JSON.stringify(document.specification, null, 2);
      if (manageBusy) this.setNotice(this.tf('notice.snapshotOpened', { version: document.snapshotVersion }), 'info');
      return true;
    } catch (error) {
      this.handleError(error, this.t('notice.snapshotReadFailed'));
      return false;
    } finally {
      if (manageBusy) this.busy = false;
    }
  }

  loadSnapshotIntoEditor(): void {
    if (!this.selectedSnapshotJson) return;
    this.specificationJson = this.selectedSnapshotJson;
    this.validation = null;
    this.setNotice(this.t('notice.snapshotCopied'), 'info');
  }

  async importModel(): Promise<void> {
    if (!this.selectedProject) {
      this.setNotice(this.t('notice.importNeedProject'), 'error');
      return;
    }
    if (!this.modelImportContent.trim()) {
      this.setNotice(this.t('notice.importEmpty'), 'error');
      return;
    }
    if (this.importBytes > 524_288) {
      this.setNotice(this.t('notice.importTooLarge'), 'error');
      return;
    }
    this.busy = true;
    try {
      const result = await firstValueFrom(this.http.post<ModelImportResult>(
        '/api/v1/projects/' + this.selectedProject.id + '/model/import',
        { format: this.modelImportFormat, content: this.modelImportContent }
      ));
      const spec = this.record(JSON.parse(this.specificationJson));
      spec['model'] = result.model;
      this.specificationJson = JSON.stringify(spec, null, 2);
      this.modelPreview = result;
      this.validation = null;
      this.setNotice(this.t('notice.importMerged'), 'success');
    } catch (error) {
      if (error instanceof SyntaxError) {
        this.setNotice(this.t('notice.importEditor'), 'error');
      } else {
        this.handleError(error, this.t('notice.importFailed'));
      }
    } finally {
      this.busy = false;
    }
  }

  async loadGenerationRuns(): Promise<void> {
    if (!this.selectedProject) return;
    try {
      this.generationRuns = await firstValueFrom(this.http.get<GenerationRun[]>(
        '/api/v1/projects/' + this.selectedProject.id + '/generations'
      ));
    } catch (error) {
      this.handleError(error, this.t('notice.runsFailed'));
    }
  }

  formatSpecification(): void {
    try {
      this.specificationJson = JSON.stringify(JSON.parse(this.specificationJson), null, 2);
      this.setNotice(this.t('notice.formatted'), 'success');
    } catch {
      this.setNotice(this.t('notice.invalidJson'), 'error');
    }
  }

  async validateSpecification(): Promise<void> {
    this.busy = true;
    try {
      const payload = JSON.parse(this.specificationJson) as unknown;
      this.validation = await firstValueFrom(
        this.http.post<{ valid: boolean; errors: string[] }>('/api/v1/specifications/validate', payload)
      );
      this.setNotice(this.t(this.validation.valid ? 'notice.specValid' : 'notice.specInvalid'),
        this.validation.valid ? 'success' : 'error');
    } catch (error) {
      if (error instanceof SyntaxError) {
        this.validation = { valid: false, errors: [this.t('notice.invalidJsonShort')] };
        this.setNotice(this.t('notice.invalidJsonShort'), 'error');
      } else {
        const httpError = error as HttpErrorResponse;
        const body = httpError.error as { errors?: string[] } | undefined;
        this.validation = body?.errors ? { valid: false, errors: body.errors } : null;
        this.handleError(error, this.t('notice.validationFailed'));
      }
    } finally {
      this.busy = false;
    }
  }

  async saveSnapshot(): Promise<void> {
    if (!this.selectedProject) {
      this.say('notice.saveNeedProject', 'error');
      return;
    }
    if (this.editorBytes > 1_048_576) {
      this.say('notice.specTooLarge', 'error');
      return;
    }
    this.busy = true;
    try {
      const payload = JSON.parse(this.specificationJson) as unknown;
      const snapshot = await firstValueFrom(this.http.post<Snapshot>(
        '/api/v1/projects/' + this.selectedProject.id + '/specifications', payload
      ));
      this.snapshots = [...this.snapshots.filter(item => item.id !== snapshot.id), snapshot]
        .sort((left, right) => left.snapshotVersion - right.snapshotVersion);
      const opened = await this.openSnapshot(snapshot, false);
      this.say(opened ? 'notice.snapshotSaved' : 'notice.snapshotPartial', opened ? 'success' : 'error',
        { version: snapshot.snapshotVersion });
    } catch (error) {
      if (error instanceof SyntaxError) {
        this.say('notice.invalidJson', 'error');
      } else {
        const httpError = error as HttpErrorResponse;
        const body = httpError.error as { errors?: string[] } | undefined;
        if (body?.errors?.length) this.validation = { valid: false, errors: body.errors };
        this.handleError(error, this.t('notice.snapshotSaveFailed'));
      }
    } finally {
      this.busy = false;
    }
  }

  async generateProject(): Promise<void> {
    const latest = this.latestSnapshot;
    if (!this.selectedProject || !latest) {
      this.say('notice.generateNeed', 'error');
      return;
    }
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
      anchor.download = this.slug(this.selectedProject.name) + '-v' + latest.snapshotVersion + '.zip';
      anchor.click();
      window.URL.revokeObjectURL(url);
      await this.loadGenerationRuns();
      this.say('notice.generated', 'success', { version: latest.snapshotVersion });
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.error instanceof Blob) {
        const raw = await error.error.text();
        try {
          const body = JSON.parse(raw) as { message?: string; error?: string };
          this.setNotice(body.message ?? body.error ?? this.t('notice.generateFailed'), 'error');
        } catch {
          this.say('notice.generateReview', 'error');
        }
      } else {
        this.handleError(error, this.t('notice.generateFailed'));
      }
    } finally {
      this.busy = false;
    }
  }

  async generateQualityPlan(): Promise<void> {
    const latest = this.latestSnapshot;
    if (!this.selectedProject || !latest) {
      this.say('notice.qualityNeed', 'error');
      return;
    }
    this.busy = true;
    try {
      this.qualityPlan = await firstValueFrom(this.http.post<QualityPlan>(
        '/api/v1/projects/' + this.selectedProject.id + '/quality/plan',
        { snapshotVersion: latest.snapshotVersion }
      ));
      this.qualityCategory = 'all';
      this.say('notice.qualityDone', 'success', { version: latest.snapshotVersion });
    } catch (error) {
      this.handleError(error, this.t('notice.qualityFailed'));
    } finally {
      this.busy = false;
    }
  }

  async loadArchitectureDecisions(): Promise<void> {
    if (!this.selectedProject) return;
    try {
      this.architectureDecisions = await firstValueFrom(this.http.get<ArchitectureDecision[]>(
        '/api/v1/projects/' + this.selectedProject.id + '/architecture/decisions'
      ));
    } catch (error) {
      this.handleError(error, this.t('notice.decisionsFailed'));
    }
  }

  async createAdrDraft(): Promise<void> {
    const latest = this.latestSnapshot;
    if (!this.selectedProject || !latest) return;
    this.busy = true;
    try {
      await firstValueFrom(this.http.post<ArchitectureDecision>(
        '/api/v1/projects/' + this.selectedProject.id + '/architecture/decisions',
        { snapshotVersion: latest.snapshotVersion }
      ));
      await this.loadArchitectureDecisions();
      this.say('notice.adrSaved', 'success', { version: latest.snapshotVersion });
    } catch (error) {
      this.handleError(error, this.t('notice.adrSaveFailed'));
    } finally {
      this.busy = false;
    }
  }

  async transitionAdr(decision: ArchitectureDecision, targetStatus: string): Promise<void> {
    if (!this.selectedProject) return;
    if ((targetStatus === 'REJECTED' || targetStatus === 'SUPERSEDED') && this.commentRequired) {
      this.say('notice.adrComment', 'error');
      return;
    }
    this.busy = true;
    try {
      await firstValueFrom(this.http.post<ArchitectureDecision>(
        '/api/v1/projects/' + this.selectedProject.id + '/architecture/decisions/' + decision.id + '/transition',
        { targetStatus, comment: this.adrTransitionComment.trim() }
      ));
      this.adrTransitionComment = '';
      await this.loadArchitectureDecisions();
      this.say('notice.adrMoved', 'success', { status: targetStatus });
    } catch (error) {
      this.handleError(error, this.t('notice.adrFailed'));
    } finally {
      this.busy = false;
    }
  }

  async evaluateArchitecture(): Promise<void> {
    const latest = this.latestSnapshot;
    if (!this.selectedProject || !latest) {
      this.say('notice.archNeed', 'error');
      return;
    }
    this.busy = true;
    try {
      this.architectureAssessment = await firstValueFrom(this.http.post<ArchitectureAssessment>(
        '/api/v1/projects/' + this.selectedProject.id + '/architecture/evaluate',
        { snapshotVersion: latest.snapshotVersion }
      ));
      this.say('notice.archDone', 'success', { version: latest.snapshotVersion });
    } catch (error) {
      this.handleError(error, this.t('notice.archFailed'));
    } finally {
      this.busy = false;
    }
  }

  async evaluateCompatibility(): Promise<void> {
    if (this.activeSelections.length === 0) {
      this.say('notice.compatNeed', 'error');
      return;
    }
    this.busy = true;
    try {
      const selections = Object.fromEntries(this.activeSelections.map(item => [item.id, item.version]));
      this.compatibility = await firstValueFrom(this.http.post<CompatibilityResult>(
        '/api/v1/compatibility/evaluate', { selections }
      ));
      this.say(this.compatibility.compatible ? 'notice.compatOk' : 'notice.compatAttention',
        this.compatibility.compatible ? 'success' : 'error');
    } catch (error) {
      this.handleError(error, this.t('notice.compatFailed'));
    } finally {
      this.busy = false;
    }
  }

  async applyProjectFilter(): Promise<void> {
    this.projectPage = 0;
    await this.reloadProjects();
  }

  async changeProjectPage(page: number): Promise<void> {
    this.projectPage = Math.min(Math.max(0, page), Math.max(0, this.projectTotalPages - 1));
    await this.reloadProjects();
  }

  chooseCategory(category: string): void {
    this.selectedCategory = category;
    this.catalogPage = 0;
  }

  onCatalogQuery(value: string): void {
    this.catalogQuery = value;
    this.catalogPage = 0;
  }

  changeCatalogPage(page: number): void {
    this.catalogPage = Math.min(Math.max(0, page), this.catalogPageCount - 1);
  }

  changeSnapshotPage(page: number): void {
    this.snapshotPage = Math.min(Math.max(0, page), this.snapshotPageCount - 1);
  }

  beginEdit(project: Project): void {
    this.pendingDelete = null;
    this.editingProject = project;
    this.editName = project.name;
    this.editDescription = project.description || '';
  }

  askDelete(project: Project): void {
    this.editingProject = null;
    this.pendingDelete = project;
  }

  async saveProject(): Promise<void> {
    if (!this.editingProject) return;
    const name = this.editName.trim();
    const description = this.editDescription.trim();
    if (!name || name.length > 120 || description.length > 2000) {
      this.say('notice.nameLength', 'error');
      return;
    }
    this.busy = true;
    try {
      const updated = await firstValueFrom(this.http.put<Project>(
        '/api/v1/projects/' + this.editingProject.id,
        { name, description, version: this.editingProject.version }
      ));
      this.projects = this.projects.map(project => project.id === updated.id ? updated : project);
      if (this.selectedProject?.id === updated.id) {
        this.selectedProject = updated;
        this.applyProjectIdentity(updated);
      }
      this.editingProject = null;
      this.say('notice.savedProject', 'success');
    } catch (error) {
      this.handleError(error, this.t('notice.saveProjectFailed'));
    } finally {
      this.busy = false;
    }
  }

  async confirmDelete(): Promise<void> {
    if (!this.pendingDelete) return;
    const removed = this.pendingDelete;
    this.busy = true;
    try {
      await firstValueFrom(this.http.delete('/api/v1/projects/' + removed.id));
      if (this.selectedProject?.id === removed.id) this.clearProjectContext();
      this.pendingDelete = null;
      if (this.projects.length === 1 && this.projectPage > 0) this.projectPage -= 1;
      await this.reloadProjects();
      this.say('notice.deletedProject', 'success');
    } catch (error) {
      this.handleError(error, this.t('notice.deleteProjectFailed'));
    } finally {
      this.busy = false;
    }
  }

  async copyText(value: string, label: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(value);
      this.say('notice.copied', 'success', { label });
    } catch {
      this.say('notice.clipboard', 'error');
    }
  }

  private async fetchProjects(): Promise<Page<Project>> {
    const params = new URLSearchParams({
      page: String(this.projectPage),
      size: String(this.projectPageSize)
    });
    const query = this.projectQuery.trim();
    if (query) params.set('query', query);
    if (this.projectStatus) params.set('status', this.projectStatus);
    return firstValueFrom(this.http.get<Page<Project>>('/api/v1/projects?' + params.toString()));
  }

  private applyProjectPage(page: Page<Project>): void {
    this.projects = page.content;
    this.projectTotal = page.totalElements;
    this.projectTotalPages = Math.max(1, page.totalPages ?? 1);
    this.projectPage = page.number ?? this.projectPage;
  }

  private async reloadProjects(): Promise<void> {
    this.busy = true;
    try {
      this.applyProjectPage(await this.fetchProjects());
      this.apiState = 'connected';
    } catch (error) {
      this.apiState = 'offline';
      this.handleError(error, this.t('notice.offline'));
    } finally {
      this.busy = false;
    }
  }

  private clearProjectContext(): void {
    this.selectedProject = null;
    this.snapshots = [];
    this.generationRuns = [];
    this.architectureDecisions = [];
    this.architectureAssessment = null;
    this.qualityPlan = null;
    this.selectedSnapshot = null;
    this.selectedSnapshotJson = '';
    this.modelPreview = null;
    this.validation = null;
  }

  private applyProjectIdentity(project: Project): void {
    try {
      const spec = this.record(JSON.parse(this.specificationJson));
      const projectNode = this.record(spec['project']);
      projectNode['id'] = project.id;
      projectNode['name'] = project.name;
      projectNode['description'] = project.description || '';
      spec['project'] = projectNode;
      this.specificationJson = JSON.stringify(spec, null, 2);
    } catch {
      this.say('notice.identity', 'error');
    }
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

  private record(value: unknown): Record<string, unknown> {
    return value !== null && typeof value === 'object' && !Array.isArray(value)
      ? value as Record<string, unknown> : {};
  }

  private countOf(value: unknown): number {
    if (Array.isArray(value)) return value.length;
    if (value !== null && typeof value === 'object') return Object.keys(value).length;
    return 0;
  }

  private text(value: unknown): string {
    return typeof value === 'string' && value.trim() ? value : '—';
  }

  private slug(value: string): string {
    const slug = value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
    return slug || 'devforge-project';
  }

  private say(key: string, type: NoticeType, values: Record<string, string | number> = {}): void {
    this.noticeKey = key;
    this.noticeValues = values;
    this.notice = this.tf(key, values);
    this.noticeType = type;
  }

  private setNotice(message: string, type: NoticeType): void {
    this.noticeKey = '';
    this.noticeValues = {};
    this.notice = message;
    this.noticeType = type;
  }

  private handleError(error: unknown, fallback: string): void {
    const httpError = error instanceof HttpErrorResponse ? error : null;
    const body = httpError?.error;
    const detail = typeof body === 'string' ? body
      : body && typeof body === 'object'
        ? (body as { message?: string; error?: string }).message ?? (body as { error?: string }).error
        : undefined;
    const normalized = (detail ?? '').trim().toLowerCase();
    const generic = !detail || httpError?.status === 0 || normalized === 'not found'
      || normalized.startsWith('http failure');
    this.setNotice(generic ? fallback : detail, 'error');
  }
}
