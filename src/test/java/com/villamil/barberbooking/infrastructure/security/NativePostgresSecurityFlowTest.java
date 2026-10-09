package com.villamil.barberbooking.infrastructure.security;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villamil.barberbooking.application.port.out.*;
import com.villamil.barberbooking.domain.model.Role;

@SpringBootTest(properties={"spring.config.import=", "app.bootstrap.token=", "billing.enforce-subscription=true", "billing.manual.instructions=QA transfer destination", "app.cors.allowed-origins=https://frontend.example.test"})
@AutoConfigureMockMvc
@org.junit.jupiter.api.condition.EnabledIf("com.villamil.barberbooking.support.QaPostgres#available")
class NativePostgresSecurityFlowTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        String name = "qa_security_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(com.villamil.barberbooking.support.QaPostgres.dataSource("postgres")).execute("CREATE DATABASE " + name);
        var data = com.villamil.barberbooking.support.QaPostgres.dataSource(name);
        properties.add("spring.datasource.url", data::getUrl);
        properties.add("spring.datasource.username", data::getUsername); properties.add("spring.datasource.password", data::getPassword);
        properties.add("app.jwt.secret", () -> UUID.randomUUID().toString() + UUID.randomUUID());
    }
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder encoder;
    @Autowired UserAccountRepositoryPort users; @Autowired JwtTokenPort jwt; @Autowired ObjectMapper json;
    EnumMap<Role, Long> ids = new EnumMap<>(Role.class);
    long company, branch, barber;
    @BeforeEach void seed() {
        String suffix = UUID.randomUUID().toString();
        company = jdbc.queryForObject("INSERT INTO companies(name,slug) VALUES ('QA Company',?) RETURNING id", Long.class, "qa-"+suffix);
        branch = jdbc.queryForObject("INSERT INTO branches(company_id,name,slug) VALUES (?,'QA Branch','principal') RETURNING id", Long.class, company);
        barber = jdbc.queryForObject("INSERT INTO barbers(company_id,branch_id,full_name,phone) VALUES (?,?,'QA Barber',?) RETURNING id", Long.class, company, branch, suffix.substring(0,20));
        jdbc.update("INSERT INTO company_subscriptions(company_id,valid_until) VALUES (?,?)", company, java.sql.Timestamp.from(Instant.now().plusSeconds(86400)));
        String hash = encoder.encode("qa-only-password-123!");
        for (Role role : Role.values()) {
            Long id = jdbc.queryForObject("INSERT INTO user_accounts(company_id,branch_id,barber_id,full_name,email,password_hash) VALUES (?,?,?,'QA User',?,?) RETURNING id", Long.class,
              role == Role.PLATFORM_OWNER ? null : company, role == Role.PLATFORM_OWNER ? null : branch,
              role == Role.BARBER ? barber : null, role.name().toLowerCase()+suffix+"@example.test", hash);
            jdbc.update("INSERT INTO user_account_roles(user_account_id,role) VALUES (?,?)", id, role.name()); ids.put(role,id);
        }
    }
    String token(Role role) { return jwt.createAccessToken(users.findById(ids.get(role)).orElseThrow()); }
    @ParameterizedTest @EnumSource(Role.class) void everyRoleUsesRealJwtAndExplicitPermissions(Role role) throws Exception {
        String bearer = "Bearer " + token(role);
        mvc.perform(get("/api/v1/auth/me").header("Authorization",bearer)).andExpect(status().isOk());
        boolean customersAllowed = List.of(Role.COMPANY_OWNER, Role.BRANCH_MANAGER, Role.RECEPTIONIST).contains(role);
        mvc.perform(get("/api/v1/customers").header("Authorization",bearer)).andExpect(customersAllowed ? status().isOk() : status().isForbidden());
        mvc.perform(get("/api/v1/billing/transfers").header("Authorization",bearer)).andExpect(role == Role.COMPANY_OWNER ? status().isOk() : status().isForbidden());
        mvc.perform(get("/api/v1/platform/billing/transfers").header("Authorization",bearer)).andExpect(role == Role.PLATFORM_OWNER ? status().isOk() : status().isForbidden());
        mvc.perform(get("/api/v1/unimplemented-module").header("Authorization",bearer)).andExpect(status().isForbidden());
    }
    @Test void deactivationInvalidatesAnAlreadyIssuedToken() throws Exception {
        String token = token(Role.RECEPTIONIST); jdbc.update("UPDATE user_accounts SET active=false WHERE id=?", ids.get(Role.RECEPTIONIST));
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void forgedTenantHeadersCannotReadAnotherCompany() throws Exception {
        jdbc.update("INSERT INTO customers(company_id,full_name,phone) VALUES (1,'Other tenant private customer',?)", UUID.randomUUID().toString().substring(0,20));
        var result = mvc.perform(get("/api/v1/customers").header("Authorization","Bearer "+token(Role.COMPANY_OWNER))
          .header("X-Company-Id",1).header("X-Branch-Id",1)).andExpect(status().isOk()).andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("Other tenant private customer"));
    }
    @Test void actualLoginReturnsAccessAndWrongCredentialsAreRejected() throws Exception {
        String email = users.findById(ids.get(Role.COMPANY_OWNER)).orElseThrow().email();
        var login = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
          .content(json.writeValueAsString(java.util.Map.of("email",email,"password","qa-only-password-123!")))).andExpect(status().isOk()).andReturn();
        assertTrue(json.readTree(login.getResponse().getContentAsString()).path("accessToken").asText().length() > 20);
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
          .content(json.writeValueAsString(java.util.Map.of("email",email,"password","incorrect")))).andExpect(status().isUnauthorized());
    }
    @Test void freePlanKeepsEssentialsAndBillingButRequiresBusinessForTeamAccounts() throws Exception {
        jdbc.update("UPDATE company_subscriptions SET valid_until=NULL WHERE company_id=?",company);
        String bearer="Bearer "+token(Role.COMPANY_OWNER);
        mvc.perform(get("/api/v1/customers").header("Authorization",bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/user-accounts").header("Authorization",bearer)).andExpect(status().isPaymentRequired());
        mvc.perform(get("/api/v1/company/capabilities").header("Authorization",bearer))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tier").value("FREE"))
            .andExpect(jsonPath("$.basicBooking").value(true)).andExpect(jsonPath("$.teamManagement").value(false));
        mvc.perform(get("/api/v1/auth/me").header("Authorization",bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/billing/subscription").header("Authorization",bearer)).andExpect(status().isOk());
    }
    @Test void suspendedCompanyAndBranchBlockOperationsWithExplicitCode() throws Exception {
        String bearer="Bearer "+token(Role.COMPANY_OWNER);
        jdbc.update("UPDATE companies SET active=false WHERE id=?",company);
        mvc.perform(get("/api/v1/customers").header("Authorization",bearer))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMPANY_SUSPENDED"));
        mvc.perform(get("/api/v1/company/capabilities").header("Authorization",bearer))
            .andExpect(status().isOk()).andExpect(jsonPath("$.companyActive").value(false));
        jdbc.update("UPDATE companies SET active=true WHERE id=?",company);
        jdbc.update("UPDATE branches SET active=false WHERE id=?",branch);
        mvc.perform(get("/api/v1/appointments").header("Authorization",bearer))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMPANY_SUSPENDED"));
    }
    @Test void freeBarberStillHasOwnAgendaWithoutReceivingTeamManagement() throws Exception {
        jdbc.update("UPDATE company_subscriptions SET valid_until=NULL WHERE company_id=?",company);
        String bearer="Bearer "+token(Role.BARBER);
        mvc.perform(get("/api/v1/barbers/"+barber+"/appointments?date=2026-10-08").header("Authorization",bearer))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/user-accounts").header("Authorization",bearer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/company/capabilities").header("Authorization",bearer))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tier").value("FREE"));
    }
    @Test void manualReportNeverActivatesBeforePlatformOwnerReview() throws Exception {
        jdbc.update("UPDATE company_subscriptions SET valid_until=NULL WHERE company_id=?",company);
        String owner="Bearer "+token(Role.COMPANY_OWNER); String bank="bank-"+UUID.randomUUID();
        var result=mvc.perform(post("/api/v1/billing/transfers").header("Authorization",owner).header("Idempotency-Key",UUID.randomUUID())
          .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("transferReference",bank))))
          .andExpect(status().isOk()).andReturn();
        String reference=json.readTree(result.getResponse().getContentAsString()).path("reference").asText();
        assertEquals(users.findById(ids.get(Role.COMPANY_OWNER)).orElseThrow().email(), json.readTree(result.getResponse().getContentAsString()).path("reporterEmail").asText());
        assertEquals(4000000, json.readTree(result.getResponse().getContentAsString()).path("amountInCents").asLong());
        assertNull(jdbc.queryForObject("SELECT valid_until FROM company_subscriptions WHERE company_id=?",java.sql.Timestamp.class,company));
        String body=json.writeValueAsString(java.util.Map.of("bankTransactionId",bank,"confirmedAmountInCents",4000000));
        mvc.perform(patch("/api/v1/platform/billing/transfers/"+reference+"/approve").header("Authorization",owner).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/platform/billing/transfers/"+reference+"/approve").header("Authorization","Bearer "+token(Role.PLATFORM_OWNER))
          .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("bankTransactionId",bank,"confirmedAmountInCents",500000))))
          .andExpect(status().isBadRequest());
        assertNull(jdbc.queryForObject("SELECT valid_until FROM company_subscriptions WHERE company_id=?",java.sql.Timestamp.class,company));
        for(int i=0;i<2;i++) mvc.perform(patch("/api/v1/platform/billing/transfers/"+reference+"/approve").header("Authorization","Bearer "+token(Role.PLATFORM_OWNER))
          .contentType("application/json").content(body)).andExpect(status().isNoContent());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM subscription_payment_reviews WHERE order_reference=?::uuid",Integer.class,reference));
        Instant until=jdbc.queryForObject("SELECT valid_until FROM company_subscriptions WHERE company_id=?",java.sql.Timestamp.class,company).toInstant();
        assertTrue(until.isAfter(Instant.now().plusSeconds(27*86400))); assertTrue(until.isBefore(Instant.now().plusSeconds(32*86400)));
    }
}
