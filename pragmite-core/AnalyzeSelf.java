import com.pragmite.analyzer.ProjectAnalyzer;
import com.pragmite.model.AnalysisResult;
import com.pragmite.output.ConsoleReportWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Paths;

public class AnalyzeSelf {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("  PRAGMITE SELF-ANALYSIS - Analyzing Pragmite's Own Code");
        System.out.println("=".repeat(70));
        System.out.println();

        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        AnalysisResult result = analyzer.analyze("src/main/java");

        // Console Report
        ConsoleReportWriter consoleWriter = new ConsoleReportWriter(true);
        consoleWriter.write(result);

        // JSON Export
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(result);
        Files.writeString(Paths.get("pragmite-analysis-result.json"), json);

        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("Analysis complete! Report saved to: pragmite-analysis-result.json");
        System.out.println("=".repeat(70));
    }
}
