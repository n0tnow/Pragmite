/**
 * Pragmite Analysis Models - TypeScript interfaces for JSON responses
 */

export interface CodeSmell {
    type: string;
    severity: 'MINOR' | 'MAJOR' | 'CRITICAL' | 'INFO';
    description: string;
    message?: string; // Fallback for older versions
    filePath: string;
    startLine: number;
    endLine: number;
    suggestion?: string;
    affectedElement?: string;
    autoFixAvailable?: boolean;
}

export interface ComplexityInfo {
    filePath: string;
    methodName: string;
    lineNumber: number;
    complexity: string; // O_1, O_N, O_N_SQUARED, etc.
    nestedLoopDepth: number;
    cyclomaticComplexity: number;
    reason?: string;
}

export interface MethodInfo {
    name: string;
    signature: string;
    startLine: number;
    endLine: number;
    lineCount: number;
    parameterCount: number;
    cyclomaticComplexity: number;
}

export interface CKMetrics {
    className: string;
    filePath: string;
    lineNumber: number;
    wmc: number;  // Weighted Methods per Class
    dit: number;  // Depth of Inheritance Tree
    noc: number;  // Number of Children
    cbo: number;  // Coupling Between Objects
    rfc: number;  // Response For a Class
    lcom: number; // Lack of Cohesion in Methods
}

export interface FileAnalysis {
    filePath: string;
    className?: string;
    lineCount: number;
    methodCount: number;
    methods: MethodInfo[];
    smells: CodeSmell[];
    complexities: ComplexityInfo[];
    ckMetrics?: CKMetrics;
}

export interface QualityScore {
    overallScore: number;
    grade: 'A' | 'B' | 'C' | 'D' | 'F';
    dryScore: number;
    orthogonalityScore: number;
    correctnessScore: number;
    perfScore: number;  // Backend uses 'perfScore', not 'performanceScore'
    pragmaticScore: number;
}

export interface RefactoringSuggestion {
    title: string;
    description: string;
    difficulty: 'EASY' | 'MEDIUM' | 'HARD';
    steps: string[];
    beforeCode: string;
    afterCode: string;
    autoFixAvailable: boolean;
}

export interface ProfileReport {
    topCpuMethods: Array<{key: string, value: number}>;
    topAllocationSites: Array<{key: string, value: number}>;
    totalCpuSamples: number;
    totalAllocations: number;
    averageCpuLoad: number;
    maxCpuLoad: number;
}

export interface BenchmarkResult {
    benchmarks: Array<{
        name: string;
        score: number;
        scoreUnit: string;
        mode: string;
    }>;
    slowestMethod?: string;
    fastestMethod?: string;
}

export interface AnalysisResult {
    projectPath: string;
    analyzedAt: string;
    totalFiles: number;
    totalLines: number;
    analysisDurationMs: number;
    fileAnalyses: FileAnalysis[];
    codeSmells: CodeSmell[];
    complexities: ComplexityInfo[];
    qualityScore: QualityScore;
    suggestions?: RefactoringSuggestion[];
    profileReport?: ProfileReport;
    benchmarkResult?: BenchmarkResult;
}
