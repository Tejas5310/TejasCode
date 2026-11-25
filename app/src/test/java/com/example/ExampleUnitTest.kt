package com.example

import com.example.billing.PaymentApiClient
import com.example.data.CurriculumRepository
import com.example.engine.LocalCodeRunner
import com.example.model.PlanConfig
import com.example.model.ProgrammingLanguage
import com.example.model.UserEntitlement
import com.example.util.AppLocaleStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `curriculum covers all 15 programming languages and executes starter code`() {
        assertEquals(15, ProgrammingLanguage.entries.size)
        assertEquals(15, CurriculumRepository.allSupportedLanguages.size)
        ProgrammingLanguage.entries.forEach { lang ->
            val result = LocalCodeRunner.run(lang, lang.starterCode)
            assertFalse("Expected valid starter run for ${lang.displayName}: ${result.output}", result.isError)
        }
    }

    @Test
    fun `entitlement grants pro access for active trialing grace and unexpired cancel`() {
        val futureMs = System.currentTimeMillis() + 86_400_000L
        val pastMs = System.currentTimeMillis() - 86_400_000L

        assertTrue(UserEntitlement(tier = "pro", status = "Active", periodEndEpochMs = futureMs).isProActive)
        assertTrue(UserEntitlement(tier = "pro", status = "Trialing", periodEndEpochMs = futureMs).isProActive)
        assertTrue(UserEntitlement(tier = "pro", status = "GracePeriod", periodEndEpochMs = futureMs).isProActive)
        assertTrue(UserEntitlement(tier = "pro", status = "Cancelled", cancelAtPeriodEnd = true, periodEndEpochMs = futureMs).isProActive)

        assertFalse(UserEntitlement(tier = "pro", status = "Cancelled", periodEndEpochMs = pastMs).isProActive)
        assertFalse(UserEntitlement(tier = "pro", status = "Revoked", periodEndEpochMs = futureMs).isProActive)
        assertFalse(UserEntitlement(tier = "pro", status = "Expired", periodEndEpochMs = futureMs).isProActive)
        assertFalse(UserEntitlement(tier = "free", status = "Free", periodEndEpochMs = futureMs).isProActive)
    }

    @Test
    fun `plan config requires explicit non zero pricing`() {
        val unconfigured = PlanConfig()
        assertFalse(unconfigured.isConfigured)
        assertEquals("Not Configured", unconfigured.formattedMonthlyPrice())

        val configured = PlanConfig(monthlyPriceMinor = 49900L, yearlyPriceMinor = 399900L, currencySymbol = "₹")
        assertTrue(configured.isConfigured)
        assertEquals("₹499", configured.formattedMonthlyPrice())
        assertEquals("₹3999", configured.formattedYearlyPrice())
    }

    @Test
    fun `unconfigured backend and razorpay key placeholders stay disabled`() {
        val apiClient = PaymentApiClient()
        assertFalse(apiClient.isBackendConfigured())
        assertEquals("", apiClient.razorpayKeyId())
    }

    @Test
    fun `locale helper provides distinct translations across supported languages`() {
        val en = AppLocaleStrings.forCode("en")
        val hi = AppLocaleStrings.forCode("hi")
        val ja = AppLocaleStrings.forCode("ja")
        assertEquals("Courses", en.navCourses)
        assertEquals("कोर्स", hi.navCourses)
        assertEquals("コース", ja.navCourses)
    }
}
