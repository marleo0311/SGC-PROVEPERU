package pe.com.proveperu.sgc.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class PasswordEncoderConfigurationTests {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void cifraYVerificaUnaContrasenaConBcrypt() {
        String contrasenaDePrueba = "clave-de-prueba";

        String hash = passwordEncoder.encode(contrasenaDePrueba);

        assertThat(hash).startsWith("$2");
        assertThat(hash).doesNotContain(contrasenaDePrueba);
        assertThat(passwordEncoder.matches(contrasenaDePrueba, hash)).isTrue();
    }
}
