package tv.caffeine.app.util

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.Locale

/* https://medium.com/@vanniktech/writing-your-own-junit-rule-3df41997b10c */
/** JUnit rule for taking control over the Locale.  */
class DefaultLocaleTestRule : TestRule {
    internal val preference: Locale?

    /**
     * Creates the rule and will restore the default locale
     * for each test.
     */
    constructor() {
        preference = null
    }

    /**
     * Creates the rule and will set the preferred locale
     * for each test.
     */
    constructor(preference: Locale) {
        this.preference = preference
    }

    override fun apply(
        base: Statement,
        description: Description
    ): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                val defaultLocale = Locale.getDefault()

                try {
                    if (preference != null) {
                        Locale.setDefault(preference)
                    }

                    base.evaluate()
                } finally {
                    Locale.setDefault(defaultLocale)
                }
            }
        }
    }
}