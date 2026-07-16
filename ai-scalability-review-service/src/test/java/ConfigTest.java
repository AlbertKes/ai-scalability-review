import org.junit.jupiter.api.Test;

import static core.framework.test.Assertions.assertConfDirectory;

class ConfigTest extends IntegrationTest {
    @Test
    void conf() {
        assertConfDirectory().overridesDefaultResources();
    }
}
