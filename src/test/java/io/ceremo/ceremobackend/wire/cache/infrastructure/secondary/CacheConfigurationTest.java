package io.ceremo.ceremobackend.wire.cache.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.URLClassLoader;
import javax.cache.Caching;
import org.junit.jupiter.api.Test;

class CacheConfigurationTest {
  @Test
  void createsCacheAndClearsExistingEntries() throws Exception {
    var provider = Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
    try (var loader = new URLClassLoader(new URL[0], getClass().getClassLoader());
        var manager = provider.getCacheManager(provider.getDefaultURI(), loader)) {
      var configuration = new CacheConfiguration(new EhcacheProperties());
      configuration.createCache(manager, "providers");
      var cache = manager.getCache("providers");
      cache.put("provider-id", "profile");
      assertThat(cache.get("provider-id")).isEqualTo("profile");
      configuration.createCache(manager, "providers");
      assertThat(manager.getCache("providers")).isSameAs(cache);
      assertThat(cache.get("provider-id")).isNull();
    }
  }
}
