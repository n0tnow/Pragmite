package com.pragmite.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.rules.smells.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tüm kod kokusu kurallarını çalıştıran motor.
 */
public class RuleEngine {

    private final List<SmellDetector> detectors;

    public RuleEngine() {
        this.detectors = new ArrayList<>();
        registerDefaultDetectors();
    }

    private void registerDefaultDetectors() {
        // Metot seviyesi kokular
        detectors.add(new LongMethodDetector());
        detectors.add(new LongParameterListDetector());
        detectors.add(new HighComplexityDetector());
        detectors.add(new DeepNestingDetector());

        // Sınıf seviyesi kokular
        detectors.add(new LargeClassDetector());
        detectors.add(new GodClassDetector());
        detectors.add(new DataClassDetector());

        // Kod kalitesi kokulari
        detectors.add(new MagicNumberDetector());
        detectors.add(new EmptyCatchBlockDetector());
        detectors.add(new StringConcatInLoopDetector());
        detectors.add(new UnusedImportDetector());
        detectors.add(new UnusedVariableDetector());
        detectors.add(new MissingTryWithResourcesDetector());

        // Faz 2 - Gelişmiş kokular
        detectors.add(new DuplicateCodeDetector());
        detectors.add(new DataClumpsDetector());
        detectors.add(new FeatureEnvyDetector());
        detectors.add(new InappropriateIntimacyDetector());
        detectors.add(new LazyClassDetector());
        detectors.add(new SpeculativeGeneralityDetector());

        // Faz 3 - Production-ready detectors
        detectors.add(new PrimitiveObsessionDetector());
        detectors.add(new SwitchStatementDetector());
        detectors.add(new MessageChainDetector());
        detectors.add(new MiddleManDetector());

        // Faz 4 - Advanced detectors
        detectors.add(new ParallelInheritanceDetector());
        detectors.add(new ShotgunSurgeryDetector());
        detectors.add(new InconsistentNamingDetector());

        // Faz 5 - Additional quality detectors
        detectors.add(new LongLineDetector());
        detectors.add(new CommentedCodeDetector());
        detectors.add(new TooManyLiteralsDetector());
        detectors.add(new ExcessiveMethodCallsDetector());
    }

    /**
     * Tüm kuralları çalıştırır ve bulunan kokuları döndürür.
     */
    public List<CodeSmell> analyze(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> allSmells = new ArrayList<>();

        for (SmellDetector detector : detectors) {
            try {
                List<CodeSmell> smells = detector.detect(cu, filePath, content);
                allSmells.addAll(smells);
            } catch (Exception e) {
                // Bir dedektör hata verse bile diğerleri çalışmaya devam etsin
                System.err.println("Detector error: " + detector.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        return allSmells;
    }

    /**
     * Özel bir dedektör ekler.
     */
    public void addDetector(SmellDetector detector) {
        detectors.add(detector);
    }

    /**
     * Kayıtlı tüm dedektörleri döndürür.
     */
    public List<SmellDetector> getDetectors() {
        return new ArrayList<>(detectors);
    }
}
