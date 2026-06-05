package com.llmscoring.scorer;

import java.util.Map;

public interface Scorer {

    // Unique name for this scorer — used as key in scores map
    String name();

    // Accepts a generic context map so any scorer can work
    // with any input shape (single turn or conversation)
    ScorerResult score(Map<String, Object> context);
}
