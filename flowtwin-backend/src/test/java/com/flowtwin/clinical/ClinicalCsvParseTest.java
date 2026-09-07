package com.flowtwin.clinical;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test for the CSV line parser: quoted commas, escaped quotes, trailing empty field. */
class ClinicalCsvParseTest {

    @Test
    void splitsPlainFields() {
        assertThat(ClinicalDataLoader.parseCsvLine("a,b,c")).containsExactly("a", "b", "c");
    }

    @Test
    void keepsCommasInsideQuotes() {
        assertThat(ClinicalDataLoader.parseCsvLine("TG-1,\"headache, worsening\",neuro"))
                .containsExactly("TG-1", "headache, worsening", "neuro");
    }

    @Test
    void handlesEscapedQuotesAndEmptyField() {
        assertThat(ClinicalDataLoader.parseCsvLine("x,\"say \"\"hi\"\"\",,z"))
                .containsExactly("x", "say \"hi\"", "", "z");
    }
}
