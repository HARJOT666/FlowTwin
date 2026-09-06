package com.flowtwin.clinical;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * One-shot bulk loader for the TriageGeist clinical dataset into the clinical_* tables.
 *
 * <p><b>Gated OFF by default.</b> Runs only when {@code flowtwin.clinical.load-on-start=true},
 * so a normal {@code docker compose up} never loads the dataset. Point it at the CSV directory
 * with {@code flowtwin.clinical.data-dir} (no path is hardcoded).
 *
 * <p>Review 1 scope: loads train.csv (dataset_split='train'), patient_history.csv and
 * chief_complaints.csv. test.csv is intentionally NOT loaded yet.
 *
 * <p>Efficient batch inserts (chunked JDBC batches) with {@code ON CONFLICT (patient_id) DO NOTHING}
 * so a re-run is idempotent via the patient_id unique constraint — not the surrogate id.
 * Touches none of the operational tables/services.
 */
@Component
public class ClinicalDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ClinicalDataLoader.class);
    private static final int BATCH = 5000;

    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final String dataDir;

    public ClinicalDataLoader(JdbcTemplate jdbc,
                              @Value("${flowtwin.clinical.load-on-start:false}") boolean enabled,
                              @Value("${flowtwin.clinical.data-dir:}") String dataDir) {
        this.jdbc = jdbc;
        this.enabled = enabled;
        this.dataDir = dataDir;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Clinical loader disabled (flowtwin.clinical.load-on-start=false). Skipping.");
            return;
        }
        if (dataDir == null || dataDir.isBlank()) {
            log.error("Clinical loader enabled but flowtwin.clinical.data-dir is not set. Skipping.");
            return;
        }
        Path dir = Path.of(dataDir);
        try {
            long train = loadClinical(dir.resolve("train.csv"), "train");
            long history = loadHistory(dir.resolve("patient_history.csv"));
            long complaints = loadComplaints(dir.resolve("chief_complaints.csv"));
            log.info("Clinical load complete: clinical_patient_data(train)={}, patient_history={}, chief_complaints={}",
                    train, history, complaints);
        } catch (Exception e) {
            log.error("Clinical load failed: {}", e.getMessage(), e);
        }
    }

    // --- clinical_patient_data (train.csv: 40 source columns + dataset_split) ---

    private long loadClinical(Path file, String split) throws Exception {
        String sql = "insert into clinical_patient_data ("
                + "patient_id,site_id,triage_nurse_id,arrival_mode,arrival_hour,arrival_day,arrival_month,"
                + "arrival_season,shift,age,age_group,sex,language,insurance_type,transport_origin,pain_location,"
                + "mental_status_triage,chief_complaint_system,num_prior_ed_visits_12m,num_prior_admissions_12m,"
                + "num_active_medications,num_comorbidities,systolic_bp,diastolic_bp,mean_arterial_pressure,"
                + "pulse_pressure,heart_rate,respiratory_rate,temperature_c,spo2,gcs_total,pain_score,weight_kg,"
                + "height_cm,bmi,shock_index,news2_score,disposition,ed_los_hours,triage_acuity,dataset_split) "
                + "values (" + placeholders(41) + ") on conflict (patient_id) do nothing";

        int[] types = {
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.INTEGER,
                Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
                Types.INTEGER, Types.INTEGER, Types.DOUBLE, Types.DOUBLE, Types.DOUBLE,
                Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.INTEGER, Types.INTEGER, Types.DOUBLE,
                Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.INTEGER, Types.VARCHAR, Types.DOUBLE, Types.INTEGER, Types.VARCHAR
        };

        return load(file, sql, types, 40, cols -> new Object[]{
                str(cols[0]), str(cols[1]), str(cols[2]), str(cols[3]), intg(cols[4]), str(cols[5]), intg(cols[6]),
                str(cols[7]), str(cols[8]), intg(cols[9]), str(cols[10]), str(cols[11]), str(cols[12]), str(cols[13]),
                str(cols[14]), str(cols[15]), str(cols[16]), str(cols[17]), intg(cols[18]), intg(cols[19]),
                intg(cols[20]), intg(cols[21]), dbl(cols[22]), dbl(cols[23]), dbl(cols[24]),
                dbl(cols[25]), dbl(cols[26]), dbl(cols[27]), dbl(cols[28]), dbl(cols[29]), intg(cols[30]), intg(cols[31]), dbl(cols[32]),
                dbl(cols[33]), dbl(cols[34]), dbl(cols[35]), intg(cols[36]), str(cols[37]), dbl(cols[38]), intg(cols[39]), split
        });
    }

    // --- patient_history (patient_id + 25 hx_* flags, kept as 0/1 integers) ---

    private long loadHistory(Path file) throws Exception {
        String sql = "insert into patient_history ("
                + "patient_id,hx_hypertension,hx_diabetes_type2,hx_diabetes_type1,hx_asthma,hx_copd,hx_heart_failure,"
                + "hx_atrial_fibrillation,hx_ckd,hx_liver_disease,hx_malignancy,hx_obesity,hx_depression,hx_anxiety,"
                + "hx_dementia,hx_epilepsy,hx_hypothyroidism,hx_hyperthyroidism,hx_hiv,hx_coagulopathy,hx_immunosuppressed,"
                + "hx_pregnant,hx_substance_use_disorder,hx_coronary_artery_disease,hx_stroke_prior,hx_peripheral_vascular_disease) "
                + "values (" + placeholders(26) + ") on conflict (patient_id) do nothing";

        int[] types = new int[26];
        types[0] = Types.VARCHAR;
        for (int i = 1; i < 26; i++) types[i] = Types.INTEGER;

        return load(file, sql, types, 26, cols -> {
            Object[] row = new Object[26];
            row[0] = str(cols[0]);
            for (int i = 1; i < 26; i++) row[i] = intg(cols[i]);
            return row;
        });
    }

    // --- chief_complaints (patient_id, raw text, system) ---

    private long loadComplaints(Path file) throws Exception {
        String sql = "insert into chief_complaints (patient_id,chief_complaint_raw,chief_complaint_system) "
                + "values (" + placeholders(3) + ") on conflict (patient_id) do nothing";
        int[] types = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
        return load(file, sql, types, 3, cols -> new Object[]{str(cols[0]), str(cols[1]), str(cols[2])});
    }

    // --- generic CSV -> chunked batch insert ---

    private interface RowMapper { Object[] map(String[] cols); }

    private long load(Path file, String sql, int[] types, int expectedCols, RowMapper mapper) throws Exception {
        if (!Files.exists(file)) {
            log.warn("CSV not found, skipping: {}", file);
            return 0;
        }
        long count = 0;
        List<Object[]> batch = new ArrayList<>(BATCH);
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            r.readLine(); // header
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = parseCsvLine(line);
                if (cols.length < expectedCols) continue; // malformed row, skip defensively
                batch.add(mapper.map(cols));
                if (batch.size() >= BATCH) {
                    jdbc.batchUpdate(sql, batch, types);
                    count += batch.size();
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            jdbc.batchUpdate(sql, batch, types);
            count += batch.size();
        }
        log.info("Loaded {} rows from {}", count, file.getFileName());
        return count;
    }

    private static String placeholders(int n) {
        return "?" + ",?".repeat(n - 1);
    }

    /** Minimal RFC-4180-ish parser: handles double-quoted fields with embedded commas and "" escapes. */
    static String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                    else inQuotes = false;
                } else sb.append(c);
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { out.add(sb.toString()); sb.setLength(0); }
                else sb.append(c);
            }
        }
        out.add(sb.toString());
        return out.toArray(new String[0]);
    }

    private static String str(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static Integer intg(String v) {
        String t = str(v);
        if (t == null) return null;
        try { return Integer.valueOf(t); }
        catch (NumberFormatException e) {
            try { return (int) Math.round(Double.parseDouble(t)); } // tolerate "3.0"
            catch (NumberFormatException e2) { return null; }
        }
    }

    private static Double dbl(String v) {
        String t = str(v);
        if (t == null) return null;
        try { return Double.valueOf(t); } catch (NumberFormatException e) { return null; }
    }
}
